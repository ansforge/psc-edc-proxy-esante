/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import fr.gouv.ans.psc.example.esante.proxy.service.Credential;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration des échanges avec ProSantéConnect.
 * 
 * @author edegenetais
 */
@ConfigurationProperties("psc")
public class PSCConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(PSCConfiguration.class);

  private URI discoveryURL;
  private Map<String,Credential> clients;

  public PSCConfiguration(String discoveryURL, Map<String,Credential> clients) throws URISyntaxException {
    this.discoveryURL = new URI(discoveryURL);
    this.clients = clients;
    LOGGER.debug("Proxy configuration loaded");
  }

  public URI getDiscoveryURL() {
    return discoveryURL;
  }
  
  public Credential getSecret(String clientId) {
    return this.clients.get(clientId);
  }
}
