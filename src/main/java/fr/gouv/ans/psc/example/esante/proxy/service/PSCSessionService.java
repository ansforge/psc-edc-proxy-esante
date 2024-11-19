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

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.ciba.AuthRequestID;
import com.nimbusds.oauth2.sdk.ciba.CIBAGrant;
import com.nimbusds.oauth2.sdk.ciba.CIBARequest;
import com.nimbusds.oauth2.sdk.ciba.CIBARequestAcknowledgement;
import com.nimbusds.oauth2.sdk.ciba.CIBAResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

    LOGGER.debug("Client id {}, found credential for auth type {}",clientId,credential.type());

    final ClientAuthentication clientAuthentication = credential.buildAuth(clientId);

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
        LOGGER.error("Authentication failed : {}",tokenResponse.toErrorResponse());
        throw new AuthenticationFailure(tokenResponse.toErrorResponse());
      }
    } else {
      LOGGER.error("Authentication failed : {}",response.toErrorResponse());
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
  
  public void logout(CIBASession session, String clientId) {
    try {
      URI logoutUri = getMetadata().getEndSessionEndpointURI();
      JWT idTokenHint = session.idTokenAsJWT();
      
      LogoutRequest logoutReq = new LogoutRequest(logoutUri, idTokenHint);
      Credential crd = cfg.getSecret(clientId);
      HTTPRequest httpRequest = logoutReq.toHTTPRequest();
      final Optional<X509Certificate> clientCert = crd.getClientCert();
      if(clientCert.isPresent()) {
        httpRequest.setClientX509Certificate(clientCert.get());
      }
      HTTPResponse response =  httpRequest.send();
      if (!response.indicatesSuccess()) {
        LOGGER.warn("Failed to logout from ProSantéConnect, {} : {}",response.getStatusCode(), response.getBody());
      }

    } catch (IOException ex) {
      throw new TechnicalFailure("Failed to lgout from PSC", ex);
    }
  }
}
