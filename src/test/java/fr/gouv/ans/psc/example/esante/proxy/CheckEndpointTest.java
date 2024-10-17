/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Cette suite de test vise à valider le bon fonctionnement d'un contrôleur codé à côté de la gateway.
 * Par ailleurs, ce type de contrôleur est utile lors de déploiments cloud.
 * 
 * @author edegenetais
 */
@SpringBootTest(classes = {EsanteProxyApplication.class})
@AutoConfigureWebTestClient
public class CheckEndpointTest {
  @Autowired
  private WebTestClient testClient;
  
  @Test
  public void testCheckAlive(){
    testClient
        .get()
        .uri("/check/alive")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("OK");
  }
}
