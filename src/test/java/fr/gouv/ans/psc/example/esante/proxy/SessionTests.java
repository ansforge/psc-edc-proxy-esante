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

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jwt.JWTParser;
import fr.gouv.ans.psc.example.esante.proxy.model.Connection;
import fr.gouv.ans.psc.example.esante.proxy.model.ErrorDescriptor;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import java.text.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

/**
 * Cette suite de test valide les bonnes interactions avec ProSanteConnect.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient(timeout = "PT30S")
public class SessionTests extends AbstractProxyIntegrationTest {
  private static final String MY_CLIENT_SECRET = "my_client_secret";

  @Test
  public void passingConnectQueryExhangesIDPTokens() {
    testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Mono.just(
                new Connection(
                    ID_NAT, 
                    "00", 
                    TEST_CLIENT_ID, 
                    "CARD")),
            Connection.class
        )
        .exchange()
        .expectStatus()
        .isOk();

    backend1IDP.verify(1,WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
    backend2IDP.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
    /*
     * L'IDP 3 est mutualisé avec le backend volontairement non-fonctionnel qui sert à tester 
     * le retour d'erreur de connexion. Il est donc sollicité deux fois et non une comme les autres.
     */
    backend3IDP.verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
  }
  
  @Test
  public void invalidChannelOnConnectGiveErrorDescriptor() {
    ErrorDescriptor error =  testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
                  Mono.just(
                      new Connection(
                          ID_NAT,
                          "00", 
                          TEST_CLIENT_ID, 
                          "INVALID")),
                  Connection.class
            )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("400", error.code());
    Assertions.assertTrue(error.message().contains("channel value INVALID is invalid"));
  }
  
  @Test
  public void missingChannelOnConnectGiveErrorDescriptor() {
    ErrorDescriptor error =  testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
                  Mono.just(
                      new Connection(
                          ID_NAT,
                          "00", 
                          TEST_CLIENT_ID, 
                          null)),
                  Connection.class
            )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("400", error.code());
    Assertions.assertTrue(error.message().contains("channel is missing"));
  }
  
  @Test
  public void missingIdNationalOnConnectGiveErrorDescriptor() {
    ErrorDescriptor error =  testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
                  Mono.just(
                      new Connection(
                          null,
                          "00", 
                          TEST_CLIENT_ID, 
                          "CARD")),
                  Connection.class
            )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("400", error.code());
    Assertions.assertTrue(error.message().contains("nationalId is missing"));
  }
  
  @Test
  public void missingbindingMessageOnConnectGiveErrorDescriptor() {
    ErrorDescriptor error =  testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
                  Mono.just(
                      new Connection(
                          ID_NAT,
                          null, 
                          TEST_CLIENT_ID, 
                          "CARD")),
                  Connection.class
            )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("400", error.code());
    Assertions.assertTrue(error.message().contains("bindingMessage is missing"));
  }
  
  @Test
  public void missingClientIdOnConnectGiveErrorDescriptor() {
    ErrorDescriptor error =  testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
                  Mono.just(
                      new Connection(
                          ID_NAT,
                          "00", 
                          null, 
                          "CARD")),
                  Connection.class
            )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("400", error.code());
    Assertions.assertTrue(error.message().contains("clientId is missing"));
  }
  
  @Test
  public void passingConnectQueryReturnsSession() throws ParseException {
    String expectedSessionState=JWTParser.parse(TEST_ACCESS_TOKEN).getJWTClaimsSet().getStringClaim("session_state");
    EntityExchangeResult<Session> result =
        testClient
            .post()
            .uri((UriBuilder b) -> b.path("/connect").build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                  Mono.just(
                      new Connection(
                          ID_NAT,
                          "00", 
                          TEST_CLIENT_ID, 
                          "CARD")),
                  Connection.class
            )
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Session.class)
            .returnResult();
    Session session = result.getResponseBody();
    
    Assertions.assertNotNull(session);
    Assertions.assertFalse(session.proxySessionId().isBlank(), "Session Id must no be empty.");
    Assertions.assertEquals(expectedSessionState,session.sessionState());
    final ResponseCookie sessionIdCookie = result.getResponseCookies().getFirst(SESSION_COOKIE_NAME);
    Assertions.assertNotNull(sessionIdCookie,"SessionIdCookie must exist.");
    String sessionIdFromCookie = sessionIdCookie.getValue();
    Assertions.assertEquals(sessionIdFromCookie, session.proxySessionId(),"Session object id and \"proxy_session_id\" value should match.");
  }
  
  @Test
  public void unavailablePSCReturns503() {
    pscMock.stubFor(
        WireMock.any(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
            .willReturn(WireMock.aResponse().withStatus(503)));
    
    testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Mono.just(
                new Connection(
                    ID_NAT,
                    "00", 
                    TEST_CLIENT_ID, 
                    "CARD")
                ),
                Connection.class
            )
        .exchange()
        .expectStatus()
        .isEqualTo(503);
  }
  
  @Test
  public void callingDisconnectWithNoSessionGives401() {
    testClient
        .delete()
        .uri(b -> b.path("/disconnect").build())
        .exchange()
        .expectStatus()
        .isUnauthorized();
    pscMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout")));
  }
  
  @Test
  public void callingDisconnectWithBogusSessionGives401() {
    testClient
        .delete()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, "this_is_bogus")
        .exchange()
        .expectStatus()
        .isUnauthorized();
    pscMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout")));
  }
  
  @Test
  public void callingDisconnectWithBogusSessionGivesExpectedErrorPayload() {
    ErrorDescriptor error =
        testClient
            .delete()
            .uri(b -> b.path("/disconnect").build())
            .cookie(SESSION_COOKIE_NAME, "this_is_bogus")
            .exchange()
            .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();

    Assertions.assertEquals("401", error.code());
    Assertions.assertEquals("Session ID not found.", error.message());
    // Pas de métadonnées sur ce cas d'erreur, puisqu'elles ne peuvent être extraites que de la session, qui n'est ici pas disponible.
  }
  
  @Test
  public void callingDisconnectWithRealSessionGives200() {
    Session session = getSession(testClient);
    testClient
        .delete()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isOk();

    pscMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout")));
  }

  
  @Test
  public void callingDisconnectWithDisconnectedRealSessionGives401() {
    Session session = getSession(testClient);
    testClient
        .delete()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isOk();
    pscMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout")));
    
    testClient
        .delete()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isUnauthorized();
    //Le compte doit rester à 1, AKA le second appel à `/disconnect` n'a pas déclenché d'appel à ProSantéConnect
    pscMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout")));
  }
  
  @Test
  public void passingConnectQueryCallsAuthEndpoint() {
    testClient
      .post()
      .uri((UriBuilder b) -> b.path("/connect").build())
      .contentType(MediaType.APPLICATION_JSON)
      .body(
            Mono.just(
                new Connection(
                    ID_NAT,
                    "00", 
                    TEST_CLIENT_ID, 
                    "CARD")),
            Connection.class
      )
      .exchange()
        .expectStatus().isOk();
    
    pscMock.verify(1, 
      WireMock.postRequestedFor(
        WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth")
      ).withFormParam("binding_message", WireMock.equalTo("00"))
       .withFormParam("login_hint", WireMock.equalTo(ID_NAT))
       .withFormParam("scope", WireMock.equalTo("openid scope_all"))
    );
  }
  
  @Test
  public void secretClientCredsGiveBasic() {
    testClient
      .post()
      .uri((UriBuilder b) -> b.path("/connect").build())
      .contentType(MediaType.APPLICATION_JSON)
      .body(
            Mono.just(
                new Connection(
                    ID_NAT,
                    "00", 
                    TEST_CLIENT_ID, 
                    "CARD")),
            Connection.class
      )
      .exchange()
        .expectStatus().isOk();

     pscMock.verify(1,
        WireMock.postRequestedFor(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
            .withBasicAuth(new BasicCredentials(TEST_CLIENT_ID, MY_CLIENT_SECRET)));
    
    pscMock.verify(2,
        WireMock.postRequestedFor(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/token"))
            .withBasicAuth(new BasicCredentials(TEST_CLIENT_ID, MY_CLIENT_SECRET)));
  }

  
  @Test
  public void sslCredReturnsSession() throws ParseException {
    String expectedSessionState=JWTParser.parse(TEST_ACCESS_TOKEN).getJWTClaimsSet().getStringClaim("session_state");
    Session session =
        testClient
            .post()
            .uri((UriBuilder b) -> b.path("/connect").build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Mono.just(
                    new Connection(
                        ID_NAT, 
                        "00", 
                        "client-with-cert", 
                        "CARD")
                ),
                Connection.class
            )
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Session.class)
            .returnResult()
            .getResponseBody();
    
    Assertions.assertNotNull(session);
    Assertions.assertFalse(session.proxySessionId().isBlank(), "Session Id must no be empty.");
    Assertions.assertEquals(expectedSessionState,session.sessionState());
  }
  
  @Test
  public void connectingWithUnknownClientIdGives404() {
    testClient
        .post()
        .uri("/connect")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Mono.just(
                new Connection(
                    ID_NAT,
                    "00",
                    "unknown_client",
                    "CARD"
                )
            ),
            Connection.class
        )
        .exchange()
        .expectStatus()
        .isNotFound();
  }
  
  @Test
  public void connectingWithUnknownClientIdGivesExpectedErrorPayload() {
    final String unknownClientId = "unknown_client";
    ErrorDescriptor payload = testClient
        .post()
        .uri("/connect")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(new Connection(ID_NAT, "00", unknownClientId, "CARD")), Connection.class)
        .exchange()
        .expectBody(ErrorDescriptor.class)
        .returnResult().getResponseBody();

    Assertions.assertEquals("404", payload.code());
    Assertions.assertEquals("User National ID or Software Client ID Not Found.", payload.message());
    Assertions.assertEquals(ID_NAT, payload.metadata().nationalId());
    Assertions.assertEquals(unknownClientId, payload.metadata().clientId());
  }
  
  @Test
  public void recyclageSessionSiDejaSession(){
    Session session =
        testClient
            .post()
            .uri((UriBuilder b) -> b.path("/connect").build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(new Connection(ID_NAT, "00", TEST_CLIENT_ID, "CARD")), Connection.class)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Session.class).returnResult().getResponseBody();
    
    pscMock.stubFor(
        WireMock.post(
                "/auth/realms/esante-wallet/protocol/openid-connect/token")
            .willReturn(
                WireMock.okJson(
                    "{\"access_token\": \""
                        + SessionTests.TEST_ACCESS_TOKEN
                        + "\",\"expires_in\": 120,\"refresh_token\": \""
                        + SessionTests.REFRESH_TOKEN
                        + "\",\"refresh_expires_in\": 350,\"token_type\":\"Bearer\",\"id_token\":\""
                        + SessionTests.TEST_ID_TOKEN
                        + "\",\"scope\": \"openid ciba\", \"session_state\": \"session-state-512-xxx\"}")));

        testClient
            .post()
            .uri((UriBuilder b) -> b.path("/connect").build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(new Connection(ID_NAT, "00", TEST_CLIENT_ID, "CARD")), Connection.class)
            .cookie("proxy_session_id", session.proxySessionId())
            .exchange()
            .expectStatus()
            .isEqualTo(304)
            .expectBody().isEmpty();

  //On vérifie que les serveurs aval ne sont pas rappelés - la session reste valide.
    pscMock.verify(1, 
      WireMock.postRequestedFor(
        WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth")
      ).withFormParam("binding_message", WireMock.equalTo("00"))
       .withFormParam("login_hint", WireMock.equalTo(ID_NAT))
       .withFormParam("scope", WireMock.equalTo("openid scope_all"))
    );    
    
    backend1IDP.verify(1,WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
    backend2IDP.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
    backend3IDP.verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI)));
  }
  
  @Test
  public void pasDeSessionActiveSiErreurConnect() {
    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
        .willReturn(
            WireMock.unauthorized()
        )
    );

    testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(new Connection(ID_NAT, "00", TEST_CLIENT_ID, "CARD")), Connection.class)
        .exchange()
        .expectStatus()
        .isEqualTo(401)
        .expectCookie()
        .doesNotExist(SESSION_COOKIE_NAME);
  }
  
  @Test
  public void pscTriggered401ErrorPayloadShouldBeAPICompliant() {
      pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
        .willReturn(
            WireMock.unauthorized()
        )
    );

    ErrorDescriptor error = testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(new Connection(ID_NAT, "00", TEST_CLIENT_ID, "CARD")), Connection.class)
        .exchange()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();
    
    Assertions.assertEquals("401", error.code());
    Assertions.assertFalse(error.message().isBlank());
    Assertions.assertEquals(ID_NAT, error.metadata().nationalId());
    Assertions.assertEquals(TEST_CLIENT_ID, error.metadata().clientId());
  }
  
  @Test
  public void tokenExchangeTriggered401ErrorPayloadShouldBeAPICompliant() {
      backend1IDP.stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_EXCHANGE_URI))
          .willReturn(WireMock.unauthorized()));

    ErrorDescriptor error = testClient
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(new Connection(ID_NAT, "00", TEST_CLIENT_ID, "CARD")), Connection.class)
        .exchange()
        .expectBody(ErrorDescriptor.class).returnResult().getResponseBody();
    
    Assertions.assertEquals("401", error.code());
    Assertions.assertFalse(error.message().isBlank());
    Assertions.assertEquals(ID_NAT, error.metadata().nationalId());
    Assertions.assertEquals(TEST_CLIENT_ID, error.metadata().clientId());
  }
  
  private void killSessionIfAny(Session session) {
    if(session!=null) {
      killSession(testClient, session.proxySessionId());
    }
  }
}
