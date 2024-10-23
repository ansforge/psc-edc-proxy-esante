/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.controller;

import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
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
import fr.gouv.ans.psc.example.esante.proxy.config.CIBAConfiguration;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.service.TokenPollResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Ce contrôleur gère les sessions (méthodes `/connect` et `/disconnect`).
 *
 * @author edegenetais
 */
@RestController
public class SessionController {
  private CIBAConfiguration cfg;

  public SessionController(@Autowired CIBAConfiguration cfg) {
    this.cfg = cfg;
  }
  
  @GetMapping("/connect")
  public Mono<Session> connect(
      @RequestParam("nationalId") String nationalId, 
      @RequestParam("bindingMessage") String bindingMessage,
      @RequestParam("clientId") String clientId) throws IOException, ParseException, InterruptedException, ExecutionException, java.text.ParseException{
    LoggerFactory.getLogger(SessionController.class).info("Trying provider URL {}",cfg.getPscDiscoveryURl().toString());
    try(InputStream providerConfigurationIn=cfg.getPscDiscoveryURl().toURL().openStream()) {
      String pscConfigData=IOUtils.readInputStreamToString(providerConfigurationIn, Charset.forName("UTF-8"));
      OIDCProviderMetadata providerMetadata = OIDCProviderMetadata.parse(pscConfigData);
      
      final ClientSecretBasic clientAuthentication = new ClientSecretBasic(new ClientID(clientId),new Secret("my_client_secret"));

      CIBARequest req =
          new CIBARequest.Builder(clientAuthentication, new Scope("openid all"))
              .endpointURI(providerMetadata.getBackChannelAuthenticationEndpointURI())
              .acrValues(List.of(new ACR("eidas1")))
              .bindingMessage(bindingMessage)
              .loginHint(nationalId)
              .customParameter("channel", "CARD")
              .build();

      CIBAResponse response = CIBAResponse.parse(req.toHTTPRequest().send());
      if(response.indicatesSuccess()) {
        String authRequestId = response.toRequestAcknowledgement().getAuthRequestID().getValue();
        Integer expiresIn = response.toRequestAcknowledgement().getExpiresIn();
        Integer pollInterval = response.toRequestAcknowledgement().getMinWaitInterval();
        
        URI tokenURI = providerMetadata.getTokenEndpointURI();
        WebClient client = WebClient.create(tokenURI.toString());
        TokenPollResponse tokenReponse=null;
        do {
          Mono<TokenPollResponse> tokenResponseMono =
              client
                  .post()
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .body(
                      BodyInserters.fromFormData("grant_type", GrantType.CIBA.getValue())
                          .with("auth_req_id", authRequestId))
                  .retrieve()
                  .onStatus(
                      c -> HttpStatusCode.valueOf(400).isSameCodeAs(c), 
                      r -> Mono.empty()
                  )
                  .bodyToMono(TokenPollResponse.class);

          tokenReponse = tokenResponseMono.toFuture().get();
        } while(tokenReponse.isPending());
        
        JWT payload = JWTParser.parse(tokenReponse.accessToken());
        String sessionState = payload.getJWTClaimsSet().getClaim("session_state").toString();
        return Mono.just(new Session(UUID.randomUUID().toString(), sessionState));
        
      } else {
        return Mono.error(new AuthenticationFailure());
      }
    }
  }
}
