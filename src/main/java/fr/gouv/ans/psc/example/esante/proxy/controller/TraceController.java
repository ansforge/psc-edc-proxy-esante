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
package fr.gouv.ans.psc.example.esante.proxy.controller;

import fr.gouv.ans.psc.example.esante.proxy.model.Request;
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Ce contrôleur propose le service getTrace défini par la spécification.
 *
 * @author edegenetais
 */
@RestController
public class TraceController
    implements GlobalFilter // Raccourci : lors de la vraie implémentation fonctionnelle, séparer les deux fonctions.
{
  private final List<Trace> store;

  public TraceController() {
    store = new LinkedList<>();
  }
  
  @GetMapping("/gettrace")
  public Flux<Trace> gettraces(@RequestParam("start") OffsetDateTime startDate) {
    final List<Trace> traceStoreList = store;
    return Flux.fromStream(traceStoreList.stream().filter(t -> startDate.isBefore(t.timestamp())));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    final ServerHttpRequest request = exchange.getRequest();
    final String requestMethod = request.getMethod().name();
    final String requestPath = request.getPath().value();
    
    final Request newRequest = new Request("wip", requestMethod, requestPath);
    final Trace newTrace = new Trace(OffsetDateTime.now(),newRequest);
    store.add(newTrace);
    return chain.filter(exchange);
  }

}
