/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * This test suite aims at testing the /send endpoint.
 *
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient
public class SendEndpointTest {

  private static final int BAKCEND_1_PORT = 8081;
  private static final int BAKCEND_2_PORT = 8082;

  @Autowired private WebTestClient testClient;

  @RegisterExtension
  static WireMockExtension backend1 =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(BAKCEND_1_PORT))
          .build();
  
   @RegisterExtension
  static WireMockExtension backend2 =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(BAKCEND_2_PORT))
          .build();

  @Test
  public void getFromBackendOne() {
    final String reponseBody = "{\"status\": \"OK\"}";
    backend1.stubFor(
        WireMock.get(WireMock.urlEqualTo("/rsc1")).willReturn(WireMock.okJson(reponseBody)));

    testClient
        .get()
        .uri("/send/backend-1/rsc1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(reponseBody);
    
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
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(reponseBody);

    backend1.verify(WireMock.exactly(0), WireMock.anyRequestedFor(UrlPattern.ANY));
  }
}
