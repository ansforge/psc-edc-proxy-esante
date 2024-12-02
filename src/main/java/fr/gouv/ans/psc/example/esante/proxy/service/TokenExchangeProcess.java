/*
 * The MIT License
 * Copyright © 2024-2024 Agence du Numérique en Santé (ANS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author edegenetais
 */
public class TokenExchangeProcess {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenExchangeProcess.class);
  private static final String SUBJECT_ISSUER_VALUE = "psc";
  private static final String SUBJECT_ISSUER_KEY = "subject_issuer";

  private String clientId;
  private CIBASession session;
  private Credential cred;

  public TokenExchangeProcess(String clientId, CIBASession session, Credential cred) {
    this.clientId = clientId;
    this.session = session;
    this.cred = cred;
  }
  
  
  
  public BackendAccess getBackendAccessFromPSC(Backend b) {
    try {
      LOGGER
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
      TokenResponse reponse = OIDCTokenResponseParser.parse(exchangeReq.toHTTPRequest().send());
      if (reponse.indicatesSuccess()) {
        OIDCTokenResponse successResponse = (OIDCTokenResponse) reponse.toSuccessResponse();
        final AccessToken accessTokenValue = successResponse.getOIDCTokens().getAccessToken();
        Long accessTokenLifetime =
            successResponse.toSuccessResponse().getOIDCTokens().getAccessToken().getLifetime();
        String accessToken = accessTokenValue.getValue();
        String refreshToken = successResponse.getOIDCTokens().getRefreshToken().getValue();
        Long refreshTokenLifetime =
            successResponse
                .toSuccessResponse()
                .toJSONObject()
                .getAsNumber("refresh_expires_in")
                .longValue();
        final Long renewalTime = Math.min(accessTokenLifetime, refreshTokenLifetime);
        BackendAccess access = new BackendAccess(accessToken, renewalTime, refreshToken);
        return access;
      } else {

        LOGGER
            .error(
                "Token exchange for {} failed for client {}. {}",b.id(),clientId,
                reponse.toErrorResponse().toJSONObject());
        String nationalId=null;
        try {
          JWT accessToken = session.idTokenAsJWT();
          nationalId = accessToken.getJWTClaimsSet().getStringClaim("SubjectNameID");
        } catch(Exception e) {
          LOGGER.warn("Failed to extract nationalId from id token while reporting token exchange failure.",e);
        }
        throw new AuthenticationFailure(reponse.toErrorResponse(),clientId,nationalId);
      }
    } catch (IOException | ParseException ex) {
      throw new TechnicalFailure("Failed to call ID server for token echange for " + clientId, ex);
    }
  }
}
