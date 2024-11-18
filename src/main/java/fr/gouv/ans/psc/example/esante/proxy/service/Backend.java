/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import fr.gouv.ans.psc.example.esante.proxy.config.InvalidConfigurationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author edegenetais
 */
public record Backend (String id,Map<String,String> metadata){
  private static final String TOKEN_EXCHANGE_ENDPOINT = "token-exchange-endpoint";
  
  public Backend(String id,Map<String,String> metadata) {
    this.id=id;
    this.metadata=Map.copyOf(metadata);
    try{
      URI exchangeUri = new URI(metadata.get(TOKEN_EXCHANGE_ENDPOINT));
      LoggerFactory.getLogger(Backend.class).debug("Backend {} exchange token is {}", id,exchangeUri);
    } catch(URISyntaxException e) {
      throw new InvalidConfigurationException("Bad token-exchange-endpoint URI for "+id, e);
    }
  }
  
  public URI exchangeUri() {
    try {
      return new URI(metadata.get(TOKEN_EXCHANGE_ENDPOINT));
    } catch (URISyntaxException ex) {
      throw new InvalidConfigurationException("Bad token-exchange-endpoint URI for "+id, ex);
    }
  }
  
}
