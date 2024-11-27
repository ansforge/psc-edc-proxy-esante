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
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import fr.gouv.ans.psc.example.esante.proxy.model.Connection;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import fr.gouv.ans.psc.example.esante.proxy.model.TraceType;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriBuilder;

/**
 * Cette suite de tests vise à valider la mise en place de comportements globaux sur l'endpoint send
 * (aka : nous n'avons pas besoin d'ajouter explicitement le contenu à la configuration).
 *
 * Ceci évitera que de laisser les comportements importants à la merci de la configuration du proxy.
 * Le cobaye choisi est (unde ébauche grossière de) le fonctionnalité de trace. 
 * Il en résulte que ce test devra évoluer quand les traces définitives seront développées.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient
public class TraceTest  extends AbstractAuthenticatedProxyIntegrationTest {
  
  /**
   * Pour ces tests, le comportement exact du bcakend ne nous intéresse pas, 
   * donc nous allons juste les faire marcher™.
   */
  @BeforeEach
  public void setHappyGoluckyBackends(){
    backend1.stubFor(
        WireMock.any(UrlPattern.ANY).willReturn(WireMock.okJson("OK")));
    backend2.stubFor(
        WireMock.any(UrlPattern.ANY).willReturn(WireMock.okJson("OK")));
  }
  
  @Test
  public void connectOnMTLSClientHasDnInTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .post()
        .uri("/connect")
        .bodyValue(new Connection(ID_NAT, "42", "client-with-cert", "CARD"))
        .exchange().expectStatus().is2xxSuccessful();
    
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    final X500Name effectiveDN = new X500Name(traces.getFirst().dn());
    final X500Name expectedDn = new X500Name("C = FR, ST = Ile-de-France, L = Montrouge, O = Henix, OU = EDC-test-CA, CN = client.edc.proxy.1, emailAddress = \"edegenetais+client.edc.proxy.1@henix.fr\"");
    Assertions.assertEquals(expectedDn, effectiveDN);
  }
  
  @Test
  public void acceptIso8061StartEndAndEnd() {
    String yesterdayNoonISO8601UTC = "2024-11-25T12:00:00.000Z";
    String yesterdayEveningISO8601UTC = "2024-11-25T21:00:00.000Z";
    testClient.get().uri(
        b -> b.path("/traces")
        .queryParam("start", yesterdayNoonISO8601UTC)
        .queryParam("end", yesterdayEveningISO8601UTC)
        .build())
      .exchange().expectStatus().is2xxSuccessful()
        .expectBodyList(Trace.class)
        .hasSize(0);
  }
  
  @Test
  public void sendOnMTLSClientHasDnInTrace() {
    
    Session session =
        testClient
            .post()
            .uri("/connect")
            .bodyValue(new Connection(ID_NAT, "42", "client-with-cert", "CARD"))
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Session.class)
            .returnResult().getResponseBody();

    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient.get().uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange().expectStatus().is2xxSuccessful();
    OffsetDateTime testEnd = OffsetDateTime.now();
    
    testClient.delete().uri("/disconnect")
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange().expectStatus().isOk();

    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .queryParam("end", testEnd.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TraceType.SEND, traces.getFirst().type());
    final X500Name effectiveDN = new X500Name(traces.getFirst().dn());
    final X500Name expectedDn = new X500Name("C = FR, ST = Ile-de-France, L = Montrouge, O = Henix, OU = EDC-test-CA, CN = client.edc.proxy.1, emailAddress = \"edegenetais+client.edc.proxy.1@henix.fr\"");
    Assertions.assertEquals(expectedDn, effectiveDN);
  }
  
  @Test
  public void sendOnSessionHasId() {
    
    Session session =
        testClient
            .post()
            .uri("/connect")
            .bodyValue(new Connection(ID_NAT, "42", "client-with-cert", "CARD"))
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Session.class)
            .returnResult().getResponseBody();

    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient.get().uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange().expectStatus().is2xxSuccessful();
    OffsetDateTime testEnd = OffsetDateTime.now();
    
    testClient.delete().uri("/disconnect")
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange().expectStatus().isOk();

    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .queryParam("end", testEnd.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(session.proxySessionId(), traces.getFirst().proxy_id_session());
  }
  
  @Test
  public void getConnectFailureTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    final String badClientId = "unknown-id";
    testClient
        .post()
        .uri("/connect")
        .bodyValue(new Connection(ID_NAT, "42", badClientId, "CARD"))
        .exchange().expectStatus().is5xxServerError();
    
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TraceType.CONNECT_FAILURE, traces.getFirst().type());
    Assertions.assertEquals(badClientId, traces.getFirst().clientId());
    Assertions.assertEquals(ID_NAT, traces.getFirst().IdRPPS());
  }
  
  @Test
  public void getConnectTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .post()
        .uri("/connect")
        .bodyValue(new Connection(ID_NAT, "42", TEST_CLIENT_ID, "CARD"))
        .exchange().expectStatus().is2xxSuccessful();

    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TraceType.CONNECT_SUCCESS, traces.getFirst().type());
  }
  
  @Test
  public void sendTraceIsSendType() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TraceType.SEND, traces.getFirst().type());
  }
  
  @Test
  public void sendTraceHasSessionId() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(sessionId, traces.getFirst().proxy_id_session());
  }
  
  @Test
  public void sendTraceHasUserId() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(ID_NAT, traces.getFirst().IdRPPS());
  }
  
  @Test
  public void sendTraceHasClientId() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TEST_CLIENT_ID, traces.getFirst().clientId());
  }
  
   
  @Test
  public void sendTraceHasForwardedAddress() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    Assertions.assertEquals(TEST_FORWARDED_FOR, traces.getFirst().ipAddress());
  }
  
  /**
   * In this test, we'll be sending a get query and checking that we get its trace.
   * To be sure we're not getting traces from other calls, we'll use the begin parameter.
   */
  @Test
  public void getSendTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .get()
        .uri("/send/backend-1/carebear1")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    List<Trace> traces =
        testClient
            .get()
            .uri(
                (UriBuilder b) ->
                    b.path("/traces")
                        .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                        .build())
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Trace.class)
            .hasSize(1)
            .returnResult()
            .getResponseBody();
    final Trace trace = traces.get(0);
    Assertions.assertEquals("GET", trace.request().methode());
    Assertions.assertEquals("/carebear1", trace.request().path());
    Assertions.assertTrue(testBegin.isBefore(trace.timestamp()));
    Assertions.assertTrue(OffsetDateTime.now().isAfter(trace.timestamp()));
  }
  
  /**
   * In this test, we'll be sending a get query and checking that we get its trace.
   * To be sure we're not getting traces from other calls, we'll use the begin parameter.
   */
  @Test
  public void postSendTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .post()
        .uri("/send/backend-1/carebear2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    
    List<Trace> traces =
    testClient
        .get()
        .uri(
            (UriBuilder b) ->
                b.path("/traces")
                    .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                    .build()
        )
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Trace.class)
        .hasSize(1)
        .returnResult()
        .getResponseBody();
    final Trace trace = traces.get(0);
    Assertions.assertEquals("POST", trace.request().methode());
    Assertions.assertEquals("/carebear2", trace.request().path());
    Assertions.assertTrue(testBegin.isBefore(trace.timestamp()));
    Assertions.assertTrue(OffsetDateTime.now().isAfter(trace.timestamp()));
  }
  
  /**
   * In this test, we'll be sending a get query and checking that we get its trace.
   * To be sure we're not getting traces from other calls, we'll use the begin parameter.
   */
  @Test
  public void putSendTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .put()
        .uri("/send/backend-1/carebear2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    
    List<Trace> traces =
    testClient
        .get()
        .uri(
            (UriBuilder b) ->
                b.path("/traces")
                    .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                    .build()
        )
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Trace.class)
        .hasSize(1)
        .returnResult()
        .getResponseBody();
    final Trace trace = traces.get(0);
    Assertions.assertEquals("PUT", trace.request().methode());
    Assertions.assertEquals("/carebear2", trace.request().path());
    Assertions.assertTrue(testBegin.isBefore(trace.timestamp()));
    Assertions.assertTrue(OffsetDateTime.now().isAfter(trace.timestamp()));
  }
  
  /**
   * In this test, we'll be sending a get query and checking that we get its trace.
   * To be sure we're not getting traces from other calls, we'll use the begin parameter.
   */
  @Test
  public void patchSendTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .patch()
        .uri("/send/backend-1/carebear2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    
    List<Trace> traces =
    testClient
        .get()
        .uri(
            (UriBuilder b) ->
                b.path("/traces")
                    .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                    .build()
        )
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Trace.class)
        .hasSize(1)
        .returnResult()
        .getResponseBody();
    final Trace trace = traces.get(0);
    Assertions.assertEquals("PATCH", trace.request().methode());
    Assertions.assertEquals("/carebear2", trace.request().path());
    Assertions.assertTrue(testBegin.isBefore(trace.timestamp()));
    Assertions.assertTrue(OffsetDateTime.now().isAfter(trace.timestamp()));
  }
  
   /**
   * In this test, we'll be sending a get query and checking that we get its trace.
   * To be sure we're not getting traces from other calls, we'll use the begin parameter.
   */
  @Test
  public void deleteSendTrace() {
    OffsetDateTime testBegin = OffsetDateTime.now();
    testClient
        .delete()
        .uri("/send/backend-1/carebear2")
        .cookie(SESSION_COOKIE_NAME, sessionId)
        .exchange().expectStatus().is2xxSuccessful();
    
    List<Trace> traces =
    testClient
        .get()
        .uri(
            (UriBuilder b) ->
                b.path("/traces")
                    .queryParam("start", testBegin.format(DateTimeFormatter.ISO_INSTANT))
                    .build()
        )
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBodyList(Trace.class)
        .hasSize(1)
        .returnResult()
        .getResponseBody();
    final Trace trace = traces.get(0);
    Assertions.assertEquals("DELETE", trace.request().methode());
    Assertions.assertEquals("/carebear2", trace.request().path());
    Assertions.assertTrue(testBegin.isBefore(trace.timestamp()));
    Assertions.assertTrue(OffsetDateTime.now().isAfter(trace.timestamp()));
  }
}
