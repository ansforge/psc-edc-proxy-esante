/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.controller;

import fr.gouv.ans.psc.example.esante.proxy.model.Request;
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
