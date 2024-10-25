/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.nimbusds.jwt.JWTParser;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Cette suite de test valide les bonnes interactions avec ProSanteConnect.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient(timeout = "PT30S")
public class SessionTests {
  private static final String ID_NAT = "500000001815646/CPAT00045";
  private static final String TEST_CLIENT_ID = "client-id-of-test";
  private static final String AUT_REQ_ID = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Im15LWF1dGgtcmVxLWlkLTI1NSJ9.zCIf0ngT65O3wXeWsUetWasqAYBNsq1_m-wEUc_QhkQ";
  private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX2lkIjoibXktYXV0aC1yZXEtaWQtMjU1In0._kXdSg6CSbCGidMzlw2CWoZ37QeSLSg9WyLja1ToBs4";
  private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3Mjk2MjAwMDAsImlhdCI6MTUxNjIzOTAyMiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50LWlkLW9mLXRlc3QiLCJzZXNzaW9uX3N0YXRlIjoic2Vzc2lvbi1zdGF0ZS0yNTYteHh4In0.ut7H8Xpxz-6HobZdhH9UF6o5Hdzuv_hdvur-VhDAf4Y";
  private static final String TEST_ID_TOKEN = TEST_ACCESS_TOKEN;//FIXME later
  private static final String MY_CLIENT_SECRET = "my_client_secret";
  
  @Autowired 
  private WebTestClient testClient;
  private String discoveryData;

  @RegisterExtension
  protected static WireMockExtension pscMock =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(8443))
          .build();

  @BeforeEach
  public void setBasePscMockBehavior() throws IOException{
    discoveryData=IOUtils.resourceToString("/mock_discovery_response.json", Charset.forName("UTF-8"));
    pscMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/.well-known/wallet-openid-configuration"))
            .willReturn(WireMock.okJson(discoveryData)));
    
        pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
            .willReturn(WireMock.okJson(
                "{\"auth_req_id\": \""+AUT_REQ_ID+"\", \"expires_in\": 120, \"interval\": 1}"
            )));

    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/token"))
            .inScenario("Poll once then get token")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("First probe done")
            .willReturn(
                WireMock.jsonResponse(
"""
{
  "error":"authorization_pending",
  "error_description":"The authorization request is still pending as the end-user hasn't yet been authenticated."
}
""",
                400)
            )
    );
    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/token"))
            .inScenario("Poll once then get token")
            .whenScenarioStateIs("First probe done")
            .willReturn(
                WireMock.okJson(
                    "{\"access_token\": \""
                        + TEST_ACCESS_TOKEN
                        + "\",\"expires_in\": 120,\"refresh_token\": \""
                        + REFRESH_TOKEN
                        + "\",\"refresh_expires_in\": 350,\"token_type\":\"Bearer\",\"id_token\":\""
                        + TEST_ID_TOKEN
                        + "\",\"scope\": \"openid ciba\"}")));
  }

  @Test
  public void passingConnectQueryReturnsSession() throws ParseException {
    String expectedSessionState=JWTParser.parse(TEST_ACCESS_TOKEN).getJWTClaimsSet().getStringClaim("session_state");
    Session session =
        testClient
            .get()
            .uri((UriBuilder b) ->
                    b.path("/connect")
                        .queryParam("nationalId", ID_NAT)
                        .queryParam("bindingMessage", "00")
                        .queryParam("clientId", TEST_CLIENT_ID)
                        .build())
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
  public void passingConnectQueryCallsAuthEndpoint() {
    testClient
      .get()
      .uri((UriBuilder b) ->
          b.path("/connect")
            .queryParam("nationalId", ID_NAT)
            .queryParam("bindingMessage", "00")
            .queryParam("clientId", TEST_CLIENT_ID)
            .build())
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
      .get()
      .uri((UriBuilder b) ->
          b.path("/connect")
            .queryParam("nationalId", ID_NAT)
            .queryParam("bindingMessage", "00")
            .queryParam("clientId", TEST_CLIENT_ID)
            .build())
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
            .get()
            .uri((UriBuilder b) ->
                    b.path("/connect")
                        .queryParam("nationalId", ID_NAT)
                        .queryParam("bindingMessage", "00")
                        .queryParam("clientId", "client-with-cert")
                        .build())
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
