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
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
public class GlobalSendBehaviorsTest  extends AbstractAuthenticatedProxyIntegrationTest {
  
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
                    b.path("/gettrace")
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
                b.path("/gettrace")
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
}
