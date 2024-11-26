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
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import fr.gouv.ans.psc.example.esante.proxy.model.Connection;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

/**
 * This class defines Integration tests base configuration.
 * 
 * @author edegenetais
 */
public class AbstractProxyIntegrationTest {

  protected static final String AUT_REQ_ID = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Im15LWF1dGgtcmVxLWlkLTI1NSJ9.zCIf0ngT65O3wXeWsUetWasqAYBNsq1_m-wEUc_QhkQ";
  protected static final String ID_NAT = "500000001815646/CPAT00045";
  protected static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3Mjk2MjAwMDAsImlhdCI6MTUxNjIzOTAyMiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50LWlkLW9mLXRlc3QiLCJzZXNzaW9uX3N0YXRlIjoic2Vzc2lvbi1zdGF0ZS0yNTYteHh4In0.ut7H8Xpxz-6HobZdhH9UF6o5Hdzuv_hdvur-VhDAf4Y";
  protected static final String TEST_ID_TOKEN = TEST_ACCESS_TOKEN; //FIXME later
  protected static final String TEST_CLIENT_ID = "client-id-of-test";
  protected static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWZyZXNoX2lkIjoibXktYXV0aC1yZXEtaWQtMjU1In0._kXdSg6CSbCGidMzlw2CWoZ37QeSLSg9WyLja1ToBs4";
  protected static final String SESSION_COOKIE_NAME = "proxy_session_id";
  protected static final int BACKEND_1_PORT = 8081;
  protected static final int BACKEND_1_IDP_PORT = 8084;
  protected static final int BACKEND_2_PORT = 8082;
  protected static final int BACKEND_2_IDP_PORT = 8085;
  protected static final int BACKEND_3_IDP_PORT = 8086;
  protected static final String TOKEN_EXCHANGE_URI = "/realms/signsessiondata/protocol/openid-connect/token";
  
  @RegisterExtension
  protected static WireMockExtension backend1 = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().httpsPort(BACKEND_1_PORT).dynamicPort())
      .build();
  @RegisterExtension
  protected static WireMockExtension backend1IDP = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().port(BACKEND_1_IDP_PORT).dynamicHttpsPort())
      .build();
  
  @RegisterExtension
  protected static WireMockExtension backend2 = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().httpsPort(BACKEND_2_PORT).dynamicPort())
      .build();
  @RegisterExtension
  protected static WireMockExtension backend2IDP = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().port(BACKEND_2_IDP_PORT).dynamicHttpsPort())
      .build();
  
  @RegisterExtension
  protected static WireMockExtension backend3IDP = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().port(BACKEND_3_IDP_PORT).dynamicHttpsPort())
      .build();
  
   @RegisterExtension
  protected static WireMockExtension pscMock =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(8443))
          .build();

  public static Session getSession(WebTestClient client) {
    return getSession(client, TEST_CLIENT_ID);
  }

  public static Session getSession(WebTestClient client, String clientId) {
    return client
        .post()
        .uri((UriBuilder b) -> b.path("/connect").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Mono.just(
                new Connection(
            ID_NAT,
            "00",
            clientId,
            "CARD")),
            Connection.class)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Session.class).returnResult().getResponseBody();
  }

   
  @Autowired
  protected WebTestClient testClient;
  protected String discoveryData;
  
  protected AbstractProxyIntegrationTest(){}
  
  @BeforeEach
  public void webTestClientPreset() {
    testClient=testClient.mutateWith(new WebTestClientConfigurer(){
      @Override
      public void afterConfigurerAdded(WebTestClient.Builder builder, WebHttpHandlerBuilder httpHandlerBuilder, ClientHttpConnector connector) {
        builder.defaultHeader("X-Forwarded-For", "127.0.0.1");
      }
    });
  }

  @BeforeEach
  public void setBasePscMockBehavior() throws IOException {
    discoveryData = IOUtils.resourceToString("/mock_discovery_response.json", Charset.forName("UTF-8"));
    pscMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/.well-known/wallet-openid-configuration"))
            .willReturn(WireMock.okJson(discoveryData)));

    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
            .willReturn(
                WireMock.okJson(
                    "{\"auth_req_id\": \""
                        + SessionTests.AUT_REQ_ID
                        + "\", \"expires_in\": 120, \"interval\": 1}")));
    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/token"))
            .inScenario("Poll once then get token")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("First probe done")
            .willReturn(
                WireMock.jsonResponse(
                    "{\n  \"error\":\"authorization_pending\",\n  \"error_description\":\"The authorization request is still pending as the end-user hasn't yet been authenticated.\"\n}\n",
                    400)));
    pscMock.stubFor(
        WireMock.post(
                "/auth/realms/esante-wallet/protocol/openid-connect/token")
            .inScenario("Poll once then get token")
            .whenScenarioStateIs("First probe done")
            .willReturn(
                WireMock.okJson(
                    "{\"access_token\": \""
                        + SessionTests.TEST_ACCESS_TOKEN
                        + "\",\"expires_in\": 120,\"refresh_token\": \""
                        + SessionTests.REFRESH_TOKEN
                        + "\",\"refresh_expires_in\": 350,\"token_type\":\"Bearer\",\"id_token\":\""
                        + SessionTests.TEST_ID_TOKEN
                        + "\",\"scope\": \"openid ciba\", \"session_state\": \"session-state-256-xxx\"}")));
    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo("/auth/realms/esante-wallet/protocol/openid-connect/logout"))
            .willReturn(WireMock.ok()));
  }

  @BeforeEach
  public void setBackendIDPBehavior() {
    addTokenExchangeBehavior(backend1IDP,
"""
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJLZVFCMjgzXzNjY3dBTUZtWHBDYTRfbGhrZVl2VGFWZmxiU3FoSXkxUUJzIn0.eyJleHAiOjE3MzE5NTcxMTAsImlhdCI6MTczMTk0MjcxMCwianRpIjoiMTY5NGI3N2QtN2IzMi00NjVjLThkNjQtMjY5ODM5MzFmOWM3IiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwiYXVkIjpbImFjY291bnQiLCJhbnMtb2RjLWxwczEtYmFzIl0sInN1YiI6Ijc1ZDVmMGZmLTVmNGMtNGVkNi05NDVjLWM0M2IzMjdmYWM3OSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFucy1vZGMtbHBzMS1iYXMiLCJzZXNzaW9uX3N0YXRlIjoiMTg1NjE1NmEtMDM5OC00MmY2LWFlODEtYmQzNWI5ZDE3NDRiIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiIsImRlZmF1bHQtcm9sZXMtc2lnbnNlc3Npb25kYXRhIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IktJVCBET0MwMDQyNzg2IiwiU3ViamVjdE5hbWVJRCI6Ijg5OTcwMDQyNzg2OSIsImNuZiI6eyJ4NXQjUzI1NiI6Ik1tSmpabUkzWldOa00yUTVZVGs1WmpjNE9UQm1OalZsT0RObU1ETXpPR1V3WVdWak5ETXpZVGhsT1RCa1lqVTFNMk14TmpVM1lqTXpNREUyWXpVek13PT0ifSwicHJlZmVycmVkX3VzZXJuYW1lIjoiODk5NzAwNDI3ODY5IiwiZ2l2ZW5fbmFtZSI6IktJVCIsImZhbWlseV9uYW1lIjoiRE9DMDA0Mjc4NiJ9.p8Irq3n9-l5LgkFeig1tHiPAhjdYFcsrclJecXWXj6raezquBbxFtQ70Wxj8mQBzFPqtJ0lGrrjTW4gSPqA2sHm3p5oy9Y6TiNQ7PTjx7w2DDWNkyPDhfjgFrAMcXYPtzh0LjI9rdpzayvNLHqH1oip0i5dlMY89JWS1BidPUAtMA_6QAJKO4SWvsD5d85OkRJZoW0eLsMjRqIWUKIggSxLgthQwpkN-uTyQBbMsZU14M1YvSBxKlzgpaTsukI4RSBiBRxJegJMGD6P4Dd5NnONrG7kRNKTOjUHDI821AAtntr1T-dCGPSF7o8vqZZvAP2YZ5WOp16DxeFHjWFoREw",
    "expires_in": 14400,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5NzIxMGQ5ZC03NTkyLTQwOTQtOTlkNi1kMjY5YjE0NWVkNjAifQ.eyJleHAiOjE3MzE5NDQ1MTAsImlhdCI6MTczMTk0MjcxMCwianRpIjoiMGFlNDM5NTctNzhmMy00ZGZjLWIzOGUtMzZhMmE1Nzc2OGMxIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwiYXVkIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiUmVmcmVzaCIsImF6cCI6ImFucy1vZGMtbHBzMS1iYXMiLCJzZXNzaW9uX3N0YXRlIjoiMTg1NjE1NmEtMDM5OC00MmY2LWFlODEtYmQzNWI5ZDE3NDRiIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiJ9.xNFQnZF9-RnGtu2yK92vrDnQl8u8Cx8hUcaoEU-Nd_E",
    "token_type": "Bearer",
    "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJLZVFCMjgzXzNjY3dBTUZtWHBDYTRfbGhrZVl2VGFWZmxiU3FoSXkxUUJzIn0.eyJleHAiOjE3MzE5NTcxMTAsImlhdCI6MTczMTk0MjcxMCwiYXV0aF90aW1lIjowLCJqdGkiOiJmZGMyMGZmZS0xMDJiLTQyOWMtODhmNi03MGY3M2VkNzdhZWUiLCJpc3MiOiJodHRwczovL2F1dGguc2VydmVyLmFwaS5lZGMtcHNjLmVzYW50ZS5nb3V2LmZyL3JlYWxtcy9zaWduc2Vzc2lvbmRhdGEiLCJhdWQiOiJhbnMtb2RjLWxwczEtYmFzIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiSUQiLCJhenAiOiJhbnMtb2RjLWxwczEtYmFzIiwic2Vzc2lvbl9zdGF0ZSI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImF0X2hhc2giOiJ1UDRDc2hZSlNGQ3o3c0dKNF84dUJBIiwiYWNyIjoiMSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IktJVCBET0MwMDQyNzg2IiwiU3ViamVjdE5hbWVJRCI6Ijg5OTcwMDQyNzg2OSIsImNuZiI6eyJ4NXQjUzI1NiI6Ik1tSmpabUkzWldOa00yUTVZVGs1WmpjNE9UQm1OalZsT0RObU1ETXpPR1V3WVdWak5ETXpZVGhsT1RCa1lqVTFNMk14TmpVM1lqTXpNREUyWXpVek13PT0ifSwicHJlZmVycmVkX3VzZXJuYW1lIjoiODk5NzAwNDI3ODY5IiwiZ2l2ZW5fbmFtZSI6IktJVCIsImZhbWlseV9uYW1lIjoiRE9DMDA0Mjc4NiJ9.nTXU_Gc_snl_RsoPIP_McF3FbGZsUxnrPDN-ZMJXna8MXJrmCwDQtVLzpUPmJ2OB94TKW4NFKz3g47qVqOZRP6nA0pEpBIyqvPj7IhwiKYZlbJyHJvxnX9Jpp8T3jvPxQaw7UBfy5mpi01P5KDzi4t3Gs26omApa9-1GncaoHXFW9cLlzU2c1lK0qQ40piU8I8E3qgblV9_mo3oor4xdZdX2aU6tDWWlhK_aNegaIGkRB0tbTC1hnYd6Dwmj_rM1QWSMZblpWgIuo3IxG9R3S6kXIZ-xZid5o6mMrNyR9ULDIMvcBfhSfMuzjxDYIcg9ouNdh6ngdhf9ZJIHYCDqSQ",
    "not-before-policy": 0,
    "session_state": "1856156a-0398-42f6-ae81-bd35b9d1744b",
    "scope": "openid email profile"
}
"""

        );

        addTokenExchangeBehavior(backend2IDP,
"""
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJzcmMiOiJiYWNrZW5kMklEUCJ9.WCYMgTi5cW3FBOUqUTa1TNwjSGCu8QQ0vRwQ-mQOFnk",
    "expires_in": 14400,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5NzIxMGQ5ZC03NTkyLTQwOTQtOTlkNi1kMjY5YjE0NWVkNjAifQ.eyJleHAiOjE3MzE5NDQ1MTAsImlhdCI6MTczMTk0MjcxMCwianRpIjoiMGFlNDM5NTctNzhmMy00ZGZjLWIzOGUtMzZhMmE1Nzc2OGMxIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwiYXVkIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiUmVmcmVzaCIsImF6cCI6ImFucy1vZGMtbHBzMS1iYXMiLCJzZXNzaW9uX3N0YXRlIjoiMTg1NjE1NmEtMDM5OC00MmY2LWFlODEtYmQzNWI5ZDE3NDRiIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiJ9.xNFQnZF9-RnGtu2yK92vrDnQl8u8Cx8hUcaoEU-Nd_E",
    "token_type": "Bearer",
    "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJLZVFCMjgzXzNjY3dBTUZtWHBDYTRfbGhrZVl2VGFWZmxiU3FoSXkxUUJzIn0.eyJleHAiOjE3MzE5NTcxMTAsImlhdCI6MTczMTk0MjcxMCwiYXV0aF90aW1lIjowLCJqdGkiOiJmZGMyMGZmZS0xMDJiLTQyOWMtODhmNi03MGY3M2VkNzdhZWUiLCJpc3MiOiJodHRwczovL2F1dGguc2VydmVyLmFwaS5lZGMtcHNjLmVzYW50ZS5nb3V2LmZyL3JlYWxtcy9zaWduc2Vzc2lvbmRhdGEiLCJhdWQiOiJhbnMtb2RjLWxwczEtYmFzIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiSUQiLCJhenAiOiJhbnMtb2RjLWxwczEtYmFzIiwic2Vzc2lvbl9zdGF0ZSI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImF0X2hhc2giOiJ1UDRDc2hZSlNGQ3o3c0dKNF84dUJBIiwiYWNyIjoiMSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IktJVCBET0MwMDQyNzg2IiwiU3ViamVjdE5hbWVJRCI6Ijg5OTcwMDQyNzg2OSIsImNuZiI6eyJ4NXQjUzI1NiI6Ik1tSmpabUkzWldOa00yUTVZVGs1WmpjNE9UQm1OalZsT0RObU1ETXpPR1V3WVdWak5ETXpZVGhsT1RCa1lqVTFNMk14TmpVM1lqTXpNREUyWXpVek13PT0ifSwicHJlZmVycmVkX3VzZXJuYW1lIjoiODk5NzAwNDI3ODY5IiwiZ2l2ZW5fbmFtZSI6IktJVCIsImZhbWlseV9uYW1lIjoiRE9DMDA0Mjc4NiJ9.nTXU_Gc_snl_RsoPIP_McF3FbGZsUxnrPDN-ZMJXna8MXJrmCwDQtVLzpUPmJ2OB94TKW4NFKz3g47qVqOZRP6nA0pEpBIyqvPj7IhwiKYZlbJyHJvxnX9Jpp8T3jvPxQaw7UBfy5mpi01P5KDzi4t3Gs26omApa9-1GncaoHXFW9cLlzU2c1lK0qQ40piU8I8E3qgblV9_mo3oor4xdZdX2aU6tDWWlhK_aNegaIGkRB0tbTC1hnYd6Dwmj_rM1QWSMZblpWgIuo3IxG9R3S6kXIZ-xZid5o6mMrNyR9ULDIMvcBfhSfMuzjxDYIcg9ouNdh6ngdhf9ZJIHYCDqSQ",
    "not-before-policy": 0,
    "session_state": "1856156a-0398-42f6-ae81-bd35b9d1744b",
    "scope": "openid email profile"
}
"""

        );

            addTokenExchangeBehavior(backend3IDP,
"""
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJzcmMiOiJiYWNrZW5kLklEUCJ9.WD9o6UXR3Z0ntLEqMDEdDkDRywvWBYrEWBdYpes6NoA",
    "expires_in": 14400,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5NzIxMGQ5ZC03NTkyLTQwOTQtOTlkNi1kMjY5YjE0NWVkNjAifQ.eyJleHAiOjE3MzE5NDQ1MTAsImlhdCI6MTczMTk0MjcxMCwianRpIjoiMGFlNDM5NTctNzhmMy00ZGZjLWIzOGUtMzZhMmE1Nzc2OGMxIiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwiYXVkIjoiaHR0cHM6Ly9hdXRoLnNlcnZlci5hcGkuZWRjLXBzYy5lc2FudGUuZ291di5mci9yZWFsbXMvc2lnbnNlc3Npb25kYXRhIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiUmVmcmVzaCIsImF6cCI6ImFucy1vZGMtbHBzMS1iYXMiLCJzZXNzaW9uX3N0YXRlIjoiMTg1NjE1NmEtMDM5OC00MmY2LWFlODEtYmQzNWI5ZDE3NDRiIiwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiJ9.xNFQnZF9-RnGtu2yK92vrDnQl8u8Cx8hUcaoEU-Nd_E",
    "token_type": "Bearer",
    "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJLZVFCMjgzXzNjY3dBTUZtWHBDYTRfbGhrZVl2VGFWZmxiU3FoSXkxUUJzIn0.eyJleHAiOjE3MzE5NTcxMTAsImlhdCI6MTczMTk0MjcxMCwiYXV0aF90aW1lIjowLCJqdGkiOiJmZGMyMGZmZS0xMDJiLTQyOWMtODhmNi03MGY3M2VkNzdhZWUiLCJpc3MiOiJodHRwczovL2F1dGguc2VydmVyLmFwaS5lZGMtcHNjLmVzYW50ZS5nb3V2LmZyL3JlYWxtcy9zaWduc2Vzc2lvbmRhdGEiLCJhdWQiOiJhbnMtb2RjLWxwczEtYmFzIiwic3ViIjoiNzVkNWYwZmYtNWY0Yy00ZWQ2LTk0NWMtYzQzYjMyN2ZhYzc5IiwidHlwIjoiSUQiLCJhenAiOiJhbnMtb2RjLWxwczEtYmFzIiwic2Vzc2lvbl9zdGF0ZSI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImF0X2hhc2giOiJ1UDRDc2hZSlNGQ3o3c0dKNF84dUJBIiwiYWNyIjoiMSIsInNpZCI6IjE4NTYxNTZhLTAzOTgtNDJmNi1hZTgxLWJkMzViOWQxNzQ0YiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IktJVCBET0MwMDQyNzg2IiwiU3ViamVjdE5hbWVJRCI6Ijg5OTcwMDQyNzg2OSIsImNuZiI6eyJ4NXQjUzI1NiI6Ik1tSmpabUkzWldOa00yUTVZVGs1WmpjNE9UQm1OalZsT0RObU1ETXpPR1V3WVdWak5ETXpZVGhsT1RCa1lqVTFNMk14TmpVM1lqTXpNREUyWXpVek13PT0ifSwicHJlZmVycmVkX3VzZXJuYW1lIjoiODk5NzAwNDI3ODY5IiwiZ2l2ZW5fbmFtZSI6IktJVCIsImZhbWlseV9uYW1lIjoiRE9DMDA0Mjc4NiJ9.nTXU_Gc_snl_RsoPIP_McF3FbGZsUxnrPDN-ZMJXna8MXJrmCwDQtVLzpUPmJ2OB94TKW4NFKz3g47qVqOZRP6nA0pEpBIyqvPj7IhwiKYZlbJyHJvxnX9Jpp8T3jvPxQaw7UBfy5mpi01P5KDzi4t3Gs26omApa9-1GncaoHXFW9cLlzU2c1lK0qQ40piU8I8E3qgblV9_mo3oor4xdZdX2aU6tDWWlhK_aNegaIGkRB0tbTC1hnYd6Dwmj_rM1QWSMZblpWgIuo3IxG9R3S6kXIZ-xZid5o6mMrNyR9ULDIMvcBfhSfMuzjxDYIcg9ouNdh6ngdhf9ZJIHYCDqSQ",
    "not-before-policy": 0,
    "session_state": "1856156a-0398-42f6-ae81-bd35b9d1744b",
    "scope": "openid email profile"
}
"""

        );

  }
  
  private void addTokenExchangeBehavior(WireMockExtension endpoint, String jsonResponse) {
    endpoint.stubFor(WireMock.post(TOKEN_EXCHANGE_URI)
            .willReturn(WireMock.okJson(
                jsonResponse
                            )));
  }
  
  protected void killSession(final WebTestClient testClient, final String sessionId) {
    testClient.delete().uri(b -> b.path("/disconnect").build()).cookie(SESSION_COOKIE_NAME, sessionId).exchange(); //nothing expected : we just want to send the query
  }

  protected SessionScope sessionScope(String clientId) {
    return new SessionScope(clientId);
  }

  protected class SessionScope implements AutoCloseable {
    private String sessionId = null;
    
    public SessionScope(String clientId) {
      this.sessionId = getSession(testClient,clientId).proxySessionId();
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
