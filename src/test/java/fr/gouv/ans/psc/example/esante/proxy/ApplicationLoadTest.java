/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import fr.gouv.ans.psc.example.esante.proxy.config.ProxyConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * This suite is a smoke test intended to catch some potential configuration breakages.
 *
 * @author edegenetais
 */
@SpringBootTest
@ContextConfiguration(classes = {EsanteProxyApplication.class})
public class ApplicationLoadTest {
  @Autowired private ProxyConfiguration proxyConfiguration;
  
  @Test
  public void applicationLoads() {
    Assertions.assertNotNull(this.proxyConfiguration);
  }
}
