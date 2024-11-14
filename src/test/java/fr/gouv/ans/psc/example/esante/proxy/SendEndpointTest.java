/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This test suite aims at testing the /send endpoint.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient
public class SendEndpointTest extends AbstractProxyIntegrationTest {
  private String sessionId;

  @BeforeEach
  public void getSession() {
    Session session = getSession(testClient);
    sessionId=session.proxySessionId();
  }
  
  @AfterEach
  public void cleanSession() {
    killSession(testClient, sessionId);
  }
  
  @Test
  public void getFromBackendOne() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-1/rsc1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(reponseBody);
    
    backend2.verify(WireMock.exactly(0), WireMock.anyRequestedFor(UrlPattern.ANY));
  }
  
   @Test
  public void getFromBackendTwo() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend2.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc2")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-2/rsc2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(reponseBody);

    backend1.verify(WireMock.exactly(0), WireMock.anyRequestedFor(UrlPattern.ANY));
  }

  @Test
  public void useDifferentCertificatesFromDifferentSessions() {

    try (SessionScope mTlsOkSessionScope =
        sessionScope("client-with-cert"); ) {
    }
  }

}
