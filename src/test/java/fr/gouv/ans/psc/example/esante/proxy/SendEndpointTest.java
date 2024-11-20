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
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This test suite aims at testing the /send endpoint.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient
public class SendEndpointTest extends AbstractAuthenticatedProxyIntegrationTest {
  private static final int BACKEND_M_TLS_PORT = 8083;
  

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
  public void sendWithoutSessionGives401() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-1/rsc1")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    backend2.verify(WireMock.exactly(0), WireMock.anyRequestedFor(UrlPattern.ANY));
  }
  
  @Test
  public void sendWithBogusSessionGives401() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-1/rsc1")
        .cookie(SESSION_COOKIE_NAME, "this_is_bogus")
        .exchange()
        .expectStatus()
        .isUnauthorized();

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

  /** Création du mock du backend mTLS à l'usage du cas de test ci-dessous. * */
  @RegisterExtension
  protected static WireMockExtension backend3 =
      WireMockExtension.newInstance()
          .options(
              WireMockConfiguration.wireMockConfig()
                  .httpsPort(BACKEND_M_TLS_PORT)
                  .needClientAuth(true)
                  .trustStorePath("src/test/resources/truststore.jks")
                  .trustStorePassword("TSpass")
                  .trustStoreType("JKS")
                  .dynamicPort())
          .build();

  @Test
  public void useDifferentCertificatesFromDifferentSessions() {
    backend3.stubFor(WireMock.get("/rsc3").willReturn(WireMock.okJson("{\"result\": \"OK\"}")));
    try (SessionScope mTlsOkSessionScope =
        sessionScope("client-with-cert"); ) {
      
      testClient
          .get()
          .uri("/send/backend-mTLS/rsc3")
          .cookie(SESSION_COOKIE_NAME, mTlsOkSessionScope.getSessionId())
          .exchange()
          .expectStatus()
          .is2xxSuccessful();
      //requête reçue
      backend3.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlEqualTo("/rsc3")));

      // Le serveur est volontairement mal configuré pour ce client, pour vérifier que le contexte SSL utilisé est bien celui du client associé à la session.
      testClient
          .get()
          .uri("/send/backend-mTLS/rsc3")
          .cookie(SESSION_COOKIE_NAME, sessionId)
          .exchange()
          .expectStatus()
          .is5xxServerError();

      //pas de seconde requête
      backend3.verify(WireMock.exactly(1), WireMock.getRequestedFor(WireMock.urlEqualTo("/rsc3")));
    }
  }

}
