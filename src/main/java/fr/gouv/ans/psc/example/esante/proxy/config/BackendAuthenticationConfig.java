/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import fr.gouv.ans.psc.example.esante.proxy.service.Backend;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author edegenetais
 */
@ConfigurationProperties("spring.cloud.gateway")
public class BackendAuthenticationConfig {
  private List<Backend> routes;
  public BackendAuthenticationConfig (List<Backend> routes){
    this.routes = routes;
    LoggerFactory.getLogger(BackendAuthenticationConfig.class).debug("{} backends définis.",routes.size());
  }
  
  public List<Backend> routes() {
    return List.copyOf(routes);
  }
}
