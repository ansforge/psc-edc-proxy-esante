/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.ciba.CIBARequest;
import com.nimbusds.oauth2.sdk.ciba.CIBAResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import fr.gouv.ans.psc.example.esante.proxy.controller.SessionController;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service de gestion de la session CIBA.
 *
 * @author edegenetais
 */
@Component
public class PSCSessionService {
  private static final String PSC_CIBA_SCOPES = "openid scope_all";
  
  private PSCConfiguration cfg;
  
  public PSCSessionService(
      @Autowired PSCConfiguration cfg
  ) {
    this.cfg = cfg;
  }

  public CIBASession cibaAuthentication(String bindingMessage, String nationalId, String clientId)
      throws IOException, ParseException, InterruptedException, ExecutionException {

    LoggerFactory.getLogger(SessionController.class)
        .debug("Trying provider URL {}", cfg.getDiscoveryURL().toString());

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
            .customParameter("channel", "CARD")
            .build();

    CIBAResponse response = CIBAResponse.parse(req.toHTTPRequest().send());
    if (response.indicatesSuccess()) {
      String authRequestId = response.toRequestAcknowledgement().getAuthRequestID().getValue();
      Integer pollInterval = response.toRequestAcknowledgement().getMinWaitInterval();

      URI tokenURI = providerMetadata.getTokenEndpointURI();
      WebClientFactory clientFactory = new WebClientFactory(clientId, credential);
      WebClient client = clientFactory.build(tokenURI.toString());
      
      CIBASession tokenReponse = null;
      do {
        Thread.sleep(Duration.ofSeconds(pollInterval));
        Mono<CIBASession> tokenResponseMono =
            client
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    BodyInserters.fromFormData("grant_type", GrantType.CIBA.getValue())
                        .with("auth_req_id", authRequestId))
                .retrieve()
                .onStatus(c -> HttpStatusCode.valueOf(400).isSameCodeAs(c), r -> Mono.empty())
                .bodyToMono(CIBASession.class);

        tokenReponse = tokenResponseMono.toFuture().get();
      } while (tokenReponse.isPending());
      return tokenReponse;
    } else {
      throw new AuthenticationFailure(response);
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
