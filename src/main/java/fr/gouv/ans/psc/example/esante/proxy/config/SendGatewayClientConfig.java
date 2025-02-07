/*
 * The MIT License
 * Copyright © 2024-2025 Agence du Numérique en Santé (ANS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
