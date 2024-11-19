/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import fr.gouv.ans.psc.example.esante.proxy.config.BackendAuthenticationConfig;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import org.slf4j.Logger;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendAuthenticationService.class);
  
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
    final TokenExchangeProcess tokenExchange = new TokenExchangeProcess(clientId, session, cred);
    
    this.backendCfg
        .routes()
        .forEach(
            b -> {
                  BackendAccess access = tokenExchange.getBackendAccessFromPSC(b);
                  
                  backendAuthentication.switchFutureBackendToken(
                      b.id(), Mono.just(access).toFuture());
                  LOGGER.debug("Token registered for {}",b.id());
                  
            });
    return backendAuthentication;
  }
}
