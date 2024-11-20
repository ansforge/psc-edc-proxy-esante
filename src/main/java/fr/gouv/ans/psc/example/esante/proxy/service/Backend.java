/*
 * The MIT License
 * Copyright © 2024-2024 Agence du Numérique en Santé (ANS)
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
