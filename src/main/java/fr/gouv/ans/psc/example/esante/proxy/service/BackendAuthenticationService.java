/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author edegenetais
 */
@Component
public class BackendAuthenticationService {
  
  private PSCConfiguration pscCfg;

  public BackendAuthenticationService(@Autowired PSCConfiguration pscCfg) {
    this.pscCfg = pscCfg;
  }
  
  public BackendAuthentication authenticate(CIBASession session, String clientId) {
    Credential cred = pscCfg.getSecret(clientId);
    return new BackendAuthentication(cred);
  }
}
