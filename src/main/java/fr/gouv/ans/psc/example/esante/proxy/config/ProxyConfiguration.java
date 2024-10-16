/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * @author edegenetais
 */
@Configuration
public class ProxyConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfiguration.class);
  
  public ProxyConfiguration() {
    LOGGER.debug("Proxy configuration loaded");
  }
}
