/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.ciba.AuthRequestID;
import com.nimbusds.oauth2.sdk.ciba.CIBAGrant;
import com.nimbusds.oauth2.sdk.ciba.CIBARequest;
import com.nimbusds.oauth2.sdk.ciba.CIBARequestAcknowledgement;
import com.nimbusds.oauth2.sdk.ciba.CIBAResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service de gestion de la session CIBA.
 *
 * @author edegenetais
 */
@Component
public class PSCSessionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PSCSessionService.class);

  private static final String PSC_CIBA_SCOPES = "openid scope_all";

  private PSCConfiguration cfg;

  public PSCSessionService(@Autowired PSCConfiguration cfg) {
    this.cfg = cfg;
  }

  public CIBASession cibaAuthentication(
      String bindingMessage, String nationalId, String clientId, String channel)
      throws IOException, ParseException, InterruptedException, ExecutionException {

    LOGGER.debug("Trying provider URL {}", cfg.getDiscoveryURL().toString());

    OIDCProviderMetadata providerMetadata = getMetadata();

    final Credential credential = this.cfg.getSecret(clientId);

    final ClientSecretBasic clientAuthentication =
        new ClientSecretBasic(new ClientID(clientId), new Secret(credential.secret()));

    CIBARequest req =
        new CIBARequest.Builder(clientAuthentication, new Scope(PSC_CIBA_SCOPES))
            .endpointURI(providerMetadata.getBackChannelAuthenticationEndpointURI())
            .acrValues(List.of(new ACR("eidas1")))
            .bindingMessage(bindingMessage)
            .loginHint(nationalId)
            .customParameter("channel", channel)
            .build();

    CIBAResponse response = CIBAResponse.parse(req.toHTTPRequest().send());
    if (response.indicatesSuccess()) {
      URI tokenURI = providerMetadata.getTokenEndpointURI();

      CIBARequestAcknowledgement acknowledgement = response.toRequestAcknowledgement();
      AuthRequestID cibaRequestID = acknowledgement.getAuthRequestID();
      int expiresIn = acknowledgement.getExpiresIn();
      int pollInterval = acknowledgement.getMinWaitInterval();
      LOGGER.info("Préparation de la requête de polling");

      TokenRequest tokenRequest =
          new TokenRequest.Builder(tokenURI, clientAuthentication, new CIBAGrant(cibaRequestID))
              .build();
      TokenResponse tokenResponse = null;

      do {
        Thread.sleep(Duration.ofSeconds(pollInterval));
        expiresIn -= pollInterval;
        tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());
        LOGGER.debug("TokenResponse : {}", tokenResponse.toHTTPResponse().getBody());
        LOGGER.debug("ExpiresIn : {}", expiresIn);
        LOGGER.debug("tokenResponse Indicates Success : {}", tokenResponse.indicatesSuccess());
      } while (!tokenResponse.indicatesSuccess() && expiresIn >= 0);

      if (tokenResponse.indicatesSuccess()) {
        OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
        JWT idToken = successResponse.getOIDCTokens().getIDToken();
        BearerAccessToken accessToken = successResponse.getOIDCTokens().getBearerAccessToken();
        RefreshToken refreshToken = successResponse.getOIDCTokens().getRefreshToken();
        LOGGER.debug("refreshToken : {}", refreshToken.toJSONString());
        LOGGER.debug("accessToken : {}", accessToken.toJSONString());
        return new CIBASession(
            accessToken.getValue(),
            (int) accessToken.getLifetime(),
            refreshToken.toString(),
            Integer.valueOf(successResponse.toJSONObject().getAsString("refresh_expires_in")),
            accessToken.getType().getValue(),
            idToken.getParsedString(),
            accessToken.toJSONObject().getAsString("scope"),
            successResponse.toJSONObject().getAsString("session_state")
        );
      } else {
        throw new AuthenticationFailure(tokenResponse.toErrorResponse());
      }
    } else {
      throw new AuthenticationFailure(response.toErrorResponse());
    }
  }

  private OIDCProviderMetadata getMetadata() {
    try (InputStream providerConfigurationIn = cfg.getDiscoveryURL().toURL().openStream()) {
      String pscConfigData =
          IOUtils.readInputStreamToString(providerConfigurationIn, Charset.forName("UTF-8"));
      return OIDCProviderMetadata.parse(pscConfigData);
    } catch (ParseException | IOException e) {
      throw new TechnicalFailure("Échec lors du chargement des metadonnées PSC", e);
    }
  }
}
