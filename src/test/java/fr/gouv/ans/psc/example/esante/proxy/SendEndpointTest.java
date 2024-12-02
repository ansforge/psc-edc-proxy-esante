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
import fr.gouv.ans.psc.example.esante.proxy.model.ErrorDescriptor;
import org.junit.jupiter.api.Assertions;
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
  private static final String BACKEND2_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJzcmMiOiJiYWNrZW5kMklEUCJ9.WCYMgTi5cW3FBOUqUTa1TNwjSGCu8QQ0vRwQ-mQOFnk";
  private static final String BACKEND1_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJLZVFCMjgzXzNjY3dBTUZtWHBDYTRfbGhrZVl2VGFWZmxiU3FoSXkxUUJzIn0.eyJleHAiOjE3MzE5NTcxMTAsImlhdCI6MTczMTk0MjcxMCwianRpIjoiMTY5NGI3N2QtN2IzMi00NjVjLThkNjQtMjY5ODM5MzFmOWM3IiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwiYXVkIjpbImFjY291bnQiLCJhbnMtb2RjLWxwczEtYmFzIl0sInN1YiI6Ijc1ZDVmMGZmLTVmNGMtNGVkNi05NDVjLWM0M2IzMjdmYWM3OSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFucy1vZGMtbHBzMS1iYXMiLCJzZXNzaW9uX3N0YXRlIjoiMTg1NjE1NmEtMDM5OC00MmY2LWFlODEtYmQzNWI5ZDE3NDRiIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsImRlZmF1bHQtcm9sZXMtc2lnbnNlc3Npb25kYXRhIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IktJVCBET0MwMDQyNzg2IiwiU3ViamVjdE5hbWVJRCI6Ijg5OTcwMDQyNzg2OSIsImNuZiI6eyJ4NXQjUzI1NiI6Ik1tSmpabUkzWldOa00yUTVZVGs1WmpjNE9UQm1OalZsT0RObU1ETXpPR1V3WVdWak5ETXpZVGhsT1RCa1lqVTFNMk14TmpVM1lqTXpNREUyWXpVek13PT0ifSwicHJlZmVycmVkX3VzZXJuYW1lIjoiODk5NzAwNDI3ODY5IiwiZ2l2ZW5fbmFtZSI6IktJVCIsImZhbWlseV9uYW1lIjoiRE9DMDA0Mjc4NiJ9.p8Irq3n9-l5LgkFeig1tHiPAhjdYFcsrclJecXWXj6raezquBbxFtQ70Wxj8mQBzFPqtJ0lGrrjTW4gSPqA2sHm3p5oy9Y6TiNQ7PTjx7w2DDWNkyPDhfjgFrAMcXYPtzh0LjI9rdpzayvNLHqH1oip0i5dlMY89JWS1BidPUAtMA_6QAJKO4SWvsD5d85OkRJZoW0eLsMjRqIWUKIggSxLgthQwpkN-uTyQBbMsZU14M1YvSBxKlzgpaTsukI4RSBiBRxJegJMGD6P4Dd5NnONrG7kRNKTOjUHDI821AAtntr1T-dCGPSF7o8vqZZvAP2YZ5WOp16DxeFHjWFoREw";
  private static final String NO_SESSION_FOUND_ERR_MSG = "No session found.";
  
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
  public void useBackendOneTokenForBackend1() {
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

    backend1.verify(WireMock.exactly(1),
        WireMock.anyRequestedFor(UrlPattern.ANY)
            .withHeader("Authorization", WireMock.equalTo("Bearer "+BACKEND1_ACCESS_TOKEN)));
  }
  
    @Test
  public void useBackendTwoTokenForBackend2() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend2.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-2/rsc1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(reponseBody);

    backend2.verify(WireMock.exactly(1),
        WireMock.anyRequestedFor(UrlPattern.ANY)
            .withHeader("Authorization", WireMock.equalTo("Bearer "+BACKEND2_ACCESS_TOKEN)));
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
  public void sendGETWithoutSessionGivesExpectedPayload() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    ErrorDescriptor error =
        testClient
            .get()
            .uri("/send/backend-1/rsc1")
            .exchange()
            .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals(NO_SESSION_FOUND_ERR_MSG, error.message());
  }
  
  @Test
  public void sendPOSTWithoutSessionGivesExpectedPayload() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    ErrorDescriptor error =
        testClient
            .post()
            .uri("/send/backend-1/rsc1")
            .bodyValue("{\"type\":\"payload\"}")
            .exchange()
            .expectBody(ErrorDescriptor.class)
            .returnResult()
            .getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals(NO_SESSION_FOUND_ERR_MSG, error.message());
  }
  
  @Test
  public void sendPUTWithoutSessionGivesExpectedPayload() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    ErrorDescriptor error =
        testClient
            .put()
            .uri("/send/backend-1/rsc1")
            .bodyValue("{\"type\":\"payload\"}")
            .exchange()
            .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals(NO_SESSION_FOUND_ERR_MSG, error.message());
  }
  
  @Test
  public void sendPATCHWithoutSessionGivesExpectedPayload() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    ErrorDescriptor error =
        testClient
            .patch()
            .uri("/send/backend-1/rsc1")
            .bodyValue("{\"type\":\"payload\"}")
            .exchange()
            .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals(NO_SESSION_FOUND_ERR_MSG, error.message());
  }
  
  @Test
  public void sendDELETEWithoutSessionGivesExpectedPayload() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    ErrorDescriptor error =
        testClient
            .delete()
            .uri("/send/backend-1/rsc1")
            .exchange()
            .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals(NO_SESSION_FOUND_ERR_MSG, error.message());
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
  
  @Test
  public void unavailableBackendShouldTrigger503() {
    testClient
        .get()
        .uri("/send/backend-indisponible/rsc2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange()
        .expectStatus()
        .isEqualTo(503);
  }

}
