/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jwt.JWTParser;
import fr.gouv.ans.psc.example.esante.proxy.model.Connection;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import java.text.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
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
  
  @Autowired 
  private WebTestClient testClient;

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
  public void callingDisconnectWithNoSessionGives404() {
    testClient
        .post()
        .uri(b -> b.path("/disconnect").build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }
  
  @Test
  public void callingDisconnectWithBogusSessionGives404() {
    testClient
        .post()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, "this_is_bogus")
        .exchange()
        .expectStatus()
        .isNotFound();
  }
  
  @Test
  public void callingDisconnectWithRealSessionGives200() {
    Session session = getSession(testClient);
    testClient
        .post()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isOk();
  }

  
  @Test
  public void callingDisconnectWithDisconnectedRealSessionGives404() {
    Session session = getSession(testClient);
    testClient
        .post()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isOk();
    
    testClient
        .post()
        .uri(b -> b.path("/disconnect").build())
        .cookie(SESSION_COOKIE_NAME, session.proxySessionId())
        .exchange()
        .expectStatus()
        .isNotFound();
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
  
}
