/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author edegenetais
 */
@ConfigurationProperties("spring.cloud.gateway.httpclient")
public class SendGatewayClientConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendGatewayClientConfig.class);
  
  private Ssl sslCfg;

  public SendGatewayClientConfig(Ssl ssl) {
    LOGGER.debug("{} instance. ssl={}",getClass(),ssl);
    this.sslCfg=Objects.requireNonNullElse(ssl, new Ssl());
    if(sslCfg.useInsecureTrustManager()) {
      LOGGER.warn("Insecure manager activated. DO NOT use this configuration in production.");
    }
  }
  
  public boolean useInsecureTrustManager() {
    return sslCfg.useInsecureTrustManager();
  }
  
  static class Ssl extends HashMap<String,Object>{
    public boolean useInsecureTrustManager() {
      return Boolean.TRUE.equals(get("useInsecureTrustManager"));
    }
  }
}
