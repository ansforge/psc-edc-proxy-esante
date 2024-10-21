/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
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
@AutoConfigureWebTestClient
public class SessionTests {
  @Autowired 
  private WebTestClient testClient;
  private String discoveryData;
  
  @RegisterExtension
  protected static WireMockExtension pscMock = WireMockExtension.newInstance()
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
  }
  
  @Test
  public void passingConnectQuery() {

    pscMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    "/auth/realms/esante-wallet/protocol/openid-connect/ext/ciba/auth"))
            .willReturn(WireMock.okJson(
"""
{
  "auth_req_id"="notAvalidToken",
  "expires_in": 120,
  "interval": 5
}
"""
            )));

    Properties session =
        testClient
            .get()
            .uri(
              (UriBuilder b) ->
                b.path("/connect")
                  .queryParam("nationalId", "500000001815646/CPAT00045")
                  .queryParam("bindingMessage", "00")
                  .queryParam("clientId", "client-id-of-test")
                  .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Properties.class)
            .returnResult().getResponseBody();
    Assertions.assertNotNull(session);
    Assertions.assertNotNull(session.get("proxy_session_id"));
    Assertions.assertNotNull(session.get("session_state"));
  }
}
