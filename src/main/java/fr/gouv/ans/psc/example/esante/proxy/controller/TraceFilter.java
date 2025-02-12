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
package fr.gouv.ans.psc.example.esante.proxy.controller;

import fr.gouv.ans.psc.example.esante.proxy.service.BaseTraceData;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author edegenetais
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceFilter implements WebFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraceFilter.class);
  
  public static final String REQUEST = "REQUEST";
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    LOGGER.trace("TraceFilter applied in query {} for session {}.",
            exchange.getRequest().getId(),
            new SessionIdRetriever(exchange));
    BaseTraceData baseTraceData = TraceHelper.getBaseTraceData(exchange);
    
    exchange
            .getAttributes()
            .put(TraceHelper.BASE_TRACE_DATA_ATTR, baseTraceData);
    return chain.filter(
        exchange);
  }

  private static class SessionIdRetriever extends Object {

    private final ServerWebExchange exchange;

    public SessionIdRetriever(ServerWebExchange exchange) {
      this.exchange = exchange;
    }

    @Override
    public String toString() {
      try {
        if (exchange.getSession().toFuture().get() != null) {
          return exchange.getSession().toFuture().get().getId();
        } else {
          return null;
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.warn("Session retrieval interrupted during TraceFilter trace attempt.",e);
        return null;
      }
    }
  }
 
}
