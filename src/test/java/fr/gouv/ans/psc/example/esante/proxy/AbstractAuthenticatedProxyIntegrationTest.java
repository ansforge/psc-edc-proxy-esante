/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author edegenetais
 */
public class AbstractAuthenticatedProxyIntegrationTest extends AbstractProxyIntegrationTest {

  protected String sessionId;

  @BeforeEach
  public void getSession() {
    Session session = getSession(testClient);
    sessionId = session.proxySessionId();
  }

  @AfterEach
  public void cleanSession() {
    killSession(testClient, sessionId);
  }

}
