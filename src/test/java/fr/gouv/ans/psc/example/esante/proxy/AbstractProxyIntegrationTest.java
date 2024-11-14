/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriBuilder;

/**
 * This class defines Integration tests base configuration.
 * 
 * @author edegenetais
 */
public class AbstractProxyIntegrationTest {

  protected static final int BAKCEND_1_PORT = 8081;
  protected static final int BAKCEND_2_PORT = 8082;
  
  @RegisterExtension
  protected static WireMockExtension backend1 = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().httpsPort(BAKCEND_1_PORT).dynamicPort())
      .build();
  @RegisterExtension
  protected static WireMockExtension backend2 = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().httpsPort(BAKCEND_2_PORT).dynamicPort())
      .build();
   @RegisterExtension
  protected static WireMockExtension pscMock =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(8443))
          .build();
  protected static final String AUT_REQ_ID = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Im15LWF1dGgtcmVxLWlkLTI1NSJ9.zCIf0ngT65O3wXeWsUetWasqAYBNsq1_m-wEUc_QhkQ";
  protected static final String ID_NAT = "500000001815646/CPAT00045";
  protected static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3Mjk2MjAwMDAsImlhdCI6MTUxNjIzOTAyMiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50LWlkLW9mLXRlc3QiLCJzZXNzaW9uX3N0YXRlIjoic2Vzc2lvbi1zdGF0ZS0yNTYteHh4In0.ut7H8Xpxz-6HobZdhH9UF6o5Hdzuv_hdvur-VhDAf4Y";
  protected static final String TEST_ID_TOKEN = TEST_ACCESS_TOKEN; //FIXME later
  protected static final String TEST_CLIENT_ID = "client-id-of-test";
  protected static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX2lkIjoibXktYXV0aC1yZXEtaWQtMjU1In0._kXdSg6CSbCGidMzlw2CWoZ37QeSLSg9WyLja1ToBs4";
  public static final String SESSION_COOKIE_NAME = "proxy_session_id";

  public static Session getSession(WebTestClient client) {
    return getSession(client, ID_NAT);
  }

  public static Session getSession(WebTestClient client, String idNat) {
    Session session = client.get().uri((UriBuilder b) -> b.path("/connect").queryParam("nationalId", idNat).queryParam("bindingMessage", "00").queryParam("clientId", SessionTests.TEST_CLIENT_ID).queryParam("channel", "CARD").build()).exchange().expectStatus().isOk().expectBody(Session.class).returnResult().getResponseBody();
    return session;
  }
   
  @Autowired
  protected WebTestClient testClient;
  protected String discoveryData;
  
  protected AbstractProxyIntegrationTest(){}

  @BeforeEach
  public void setBasePscMockBehavior() throws IOException {
    discoveryData = IOUtils.resourceToString("/mock_discovery_response.json", Charset.forName("UTF-8"));
    pscMock.stubFor(WireMock.get(WireMock.urlEqualTo("/auth/realms/esante-wallet/.well-known/wallet-openid-configuration")).willReturn(WireMock.okJson(discoveryData)));
    pscMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth")).willReturn(WireMock.okJson("{\"auth_req_id\": \"" + SessionTests.AUT_REQ_ID + "\", \"expires_in\": 120, \"interval\": 1}")));
    pscMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/token")).inScenario("Poll once then get token").whenScenarioStateIs(Scenario.STARTED).willSetStateTo("First probe done").willReturn(WireMock.jsonResponse("{\n  \"error\":\"authorization_pending\",\n  \"error_description\":\"The authorization request is still pending as the end-user hasn't yet been authenticated.\"\n}\n", 400)));
    pscMock.stubFor(WireMock.post(WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/token")).inScenario("Poll once then get token").whenScenarioStateIs("First probe done").willReturn(WireMock.okJson("{\"access_token\": \"" + SessionTests.TEST_ACCESS_TOKEN + "\",\"expires_in\": 120,\"refresh_token\": \"" + SessionTests.REFRESH_TOKEN + "\",\"refresh_expires_in\": 350,\"token_type\":\"Bearer\",\"id_token\":\"" + SessionTests.TEST_ID_TOKEN + "\",\"scope\": \"openid ciba\", \"session_state\": \"session-state-256-xxx\"}")));
  }

  protected void killSession(final WebTestClient testClient, final String sessionId) {
    testClient.delete().uri(b -> b.path("/disconnect").build()).cookie(SESSION_COOKIE_NAME, sessionId).exchange(); //nothing expected : we just want to send the query
  }

  protected SessionScope sessionScope(String idNat) {
    return new SessionScope(idNat);
  }

  protected class SessionScope implements AutoCloseable {
    private String sessionId = null;
    
    public SessionScope(String idNat) {
      this.sessionId = getSession(testClient,idNat).proxySessionId();
    }

    @Override
    public void close() {
      if (sessionId != null) {
        killSession(testClient, sessionId);
      }
    }

    public String getSessionId() {
      return sessionId;
    }
    
  }
}
