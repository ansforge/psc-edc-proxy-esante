/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.controller;

import com.nimbusds.oauth2.sdk.ParseException;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthentication;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthenticationService;
import fr.gouv.ans.psc.example.esante.proxy.service.CIBASession;
import fr.gouv.ans.psc.example.esante.proxy.service.NotFoundException;
import fr.gouv.ans.psc.example.esante.proxy.service.PSCSessionService;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Ce contrôleur gère les sessions (méthodes `/connect` et `/disconnect`).
 *
 * @author edegenetais
 */
@RestController
public class SessionController {
  private final PSCSessionService cibaService;
  private final BackendAuthenticationService backendAuthService;
  
  public SessionController(
      @Autowired PSCSessionService cibaService,
      @Autowired BackendAuthenticationService backendAuthService
  ) {
    this.cibaService = cibaService;
    this.backendAuthService = backendAuthService;
  }
  
  @GetMapping("/connect")
  public Mono<Session> connect(
      @RequestParam("nationalId") String nationalId, 
      @RequestParam("bindingMessage") String bindingMessage,
      @RequestParam("clientId") String clientId,
      @RequestParam("channel") String channel,
      WebSession webSession
      ) throws IOException, ParseException, InterruptedException, ExecutionException, java.text.ParseException{
    
    Callable<Session> sessionSupplier =
        () -> {
          
          String sessionId = webSession.getId();
          CIBASession cibaSession = this.cibaService.cibaAuthentication(bindingMessage,nationalId,clientId, channel);
          
          BackendAuthentication backendAuth = this.backendAuthService.authenticate(cibaSession,clientId);
          webSession.getAttributes().put(SessionAttributes.BACKEND_AUTH_ATTR, backendAuth);
          
          webSession.start();
          return new Session(sessionId, cibaSession.sessionState());
        };

    return Mono.fromCallable(sessionSupplier);
  }

  @PostMapping("/disconnect")
  public Mono<Void> disconnect(WebSession webSession) {
    if(webSession==null || !webSession.isStarted()) {
      return Mono.error(new NotFoundException("No session"));
    } else {
      return webSession.invalidate();
    }
  }
}
