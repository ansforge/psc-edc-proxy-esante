/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author edegenetais
 */
@Configuration
public class ProxyConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfiguration.class);

  private URI pscDiscoveryURl;

  public ProxyConfiguration(@Value("${psc.discovery.url}") String pscDiscoveryURl) throws URISyntaxException {
    this.pscDiscoveryURl = new URI(pscDiscoveryURl);
    LOGGER.debug("Proxy configuration loaded");
  }

  public URI getPscDiscoveryURl() {
    return pscDiscoveryURl;
  }
  
}
