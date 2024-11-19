/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import fr.gouv.ans.psc.example.esante.proxy.config.BackendAuthenticationConfig;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import java.io.IOException;
import java.net.URI;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Ce service est reponsable de créer / détruire les essions vers les backends.
 * @author edegenetais
 */
@Component
public class BackendAuthenticationService {
  private static final String SUBJECT_ISSUER_VALUE = "psc";
  private static final String SUBJECT_ISSUER_KEY = "subject_issuer";
  
  private PSCConfiguration pscCfg;
  private BackendAuthenticationConfig backendCfg;

  public BackendAuthenticationService(@Autowired PSCConfiguration pscCfg, 
      @Autowired  BackendAuthenticationConfig backendCfg) {
    this.pscCfg = pscCfg;
    this.backendCfg = backendCfg;
  }

  
  public BackendAuthentication authenticate(CIBASession session, String clientId) {
    Credential cred = pscCfg.getSecret(clientId);
    final BackendAuthentication backendAuthentication = new BackendAuthentication(cred);
    this.backendCfg
        .routes()
        .forEach(
            b -> {
              try {
                LoggerFactory.getLogger(getClass())
                    .debug("Init token exchange on backend {}. Client id is {}", b.id(), clientId);
                URI exchangeURI = b.exchangeUri();
                BearerAccessToken token = new BearerAccessToken(session.accessToken());
                TokenRequest exchangeReq =
                    new TokenRequest.Builder(
                            exchangeURI,
                            cred.buildAuth(clientId),
                            new TokenExchangeGrant(token, TokenTypeURI.ACCESS_TOKEN))
                        .customParameter(SUBJECT_ISSUER_KEY, SUBJECT_ISSUER_VALUE)
                        .build();
                TokenResponse reponse =
                    OIDCTokenResponseParser.parse(exchangeReq.toHTTPRequest().send());
                if (reponse.indicatesSuccess()) {
                  OIDCTokenResponse successResponse =
                      (OIDCTokenResponse) reponse.toSuccessResponse();
                  final AccessToken accessTokenValue =
                      successResponse.getOIDCTokens().getAccessToken();
                  Long accessTokenLifetime =
                      successResponse
                          .toSuccessResponse()
                          .getOIDCTokens()
                          .getAccessToken()
                          .getLifetime();
                  String accessToken = accessTokenValue.getValue();
                  String refreshToken =
                      successResponse.getOIDCTokens().getRefreshToken().getValue();
                  Long refreshTokenLifetime =
                      successResponse
                          .toSuccessResponse()
                          .toJSONObject()
                          .getAsNumber("refresh_expires_in")
                          .longValue();
                  final Long renewalTime = Math.max(accessTokenLifetime, refreshTokenLifetime);
                  BackendAccess access = new BackendAccess(accessToken, renewalTime, refreshToken);
                  backendAuthentication.switchFutureBackendToken(
                      b.id(), Mono.just(access).toFuture());
                  LoggerFactory.getLogger(getClass()).debug("Token registered for {}",b.id());
                } else {
                  LoggerFactory.getLogger(getClass())
                      .error(
                          "Token exchange for " + b.id() + " failed for client " + clientId,
                          reponse.toErrorResponse().toJSONObject());
                  throw new AuthenticationFailure(reponse.toErrorResponse());
                }
              } catch (IOException | ParseException ex) {
                throw new TechnicalFailure(
                    "Failed to call ID server for token echange for " + clientId, ex);
              }
            });
    return backendAuthentication;
  }
}
