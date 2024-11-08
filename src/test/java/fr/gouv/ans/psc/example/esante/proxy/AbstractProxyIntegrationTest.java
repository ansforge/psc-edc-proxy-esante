/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

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
  @Autowired
  protected WebTestClient testClient;
  
  protected AbstractProxyIntegrationTest(){}
}
