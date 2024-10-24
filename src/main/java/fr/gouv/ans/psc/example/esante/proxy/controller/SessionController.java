/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.controller;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.ParseException;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.service.CIBASession;
import fr.gouv.ans.psc.example.esante.proxy.service.PSCSessionService;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Ce contrôleur gère les sessions (méthodes `/connect` et `/disconnect`).
 *
 * @author edegenetais
 */
@RestController
public class SessionController {
  private PSCSessionService cibaService;
  
  public SessionController(@Autowired PSCSessionService cibaService) {
    this.cibaService = cibaService;
  }
  
  @GetMapping("/connect")
  public Mono<Session> connect(
      @RequestParam("nationalId") String nationalId, 
      @RequestParam("bindingMessage") String bindingMessage,
      @RequestParam("clientId") String clientId) throws IOException, ParseException, InterruptedException, ExecutionException, java.text.ParseException{
    
    Callable<Session> sessionSupplier =
        () -> {
          CIBASession session = this.cibaService.cibaAuthentication(bindingMessage,nationalId,clientId);
          JWT payload = JWTParser.parse(session.accessToken());

          String sessionState = payload.getJWTClaimsSet().getClaim("session_state").toString();
          return new Session(UUID.randomUUID().toString(), sessionState);
        };

    return Mono.fromCallable(sessionSupplier);
  }
}
