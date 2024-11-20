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

import fr.gouv.ans.psc.example.esante.proxy.config.BackendAuthenticationConfig;
import fr.gouv.ans.psc.example.esante.proxy.config.PSCConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                  
                  backendAuthentication.switchBackendToken(b.id(), access);
                  LOGGER.debug("Token registered for {}",b.id());
                  
            });
    return backendAuthentication;
  }
  
  public void wipe(BackendAuthentication backendAuth){
    this.backendCfg
        .routes()
        .forEach(b -> {
            backendAuth.switchBackendToken(b.id(), null);
            LOGGER.debug("Token forgotten for {}",b.id());
        });
  }
}
