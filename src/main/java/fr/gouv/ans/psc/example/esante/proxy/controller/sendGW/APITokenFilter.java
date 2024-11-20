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
package fr.gouv.ans.psc.example.esante.proxy.controller.sendGW;

import fr.gouv.ans.psc.example.esante.proxy.UnauthorizedException;
import fr.gouv.ans.psc.example.esante.proxy.controller.SessionAttributes;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAccess;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthentication;
import fr.gouv.ans.psc.example.esante.proxy.service.TechnicalFailure;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Ce filtre global enrichit la requête avec l'access Token négocié pour ce backend avec l'IDP
 * associé.
 *
 * @author edegenetais
 */
@Component
@Order(Integer.MIN_VALUE)
public class APITokenFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    try {
      
      WebSession session = exchange.getSession().toFuture().get();
      if(session==null || !session.isStarted()) {
        throw new UnauthorizedException("Pas de session");
      }
      BackendAuthentication backendAuth = session.getAttribute(SessionAttributes.BACKEND_AUTH_ATTR);
      Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      String backendId = route.getId();
      BackendAccess access = backendAuth.findBackendToken(backendId);
      ServerHttpRequest req = exchange.getRequest().mutate().header("Authorization", access.authorizationHeader()).build();
      LoggerFactory.getLogger(APITokenFilter.class).debug("API Token filter applied for backend {}, session {}",backendId,session.getId());
      return chain.filter(exchange.mutate().request(req).build());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new TechnicalFailure("Failed to associate token",ex);
    } catch (ExecutionException ex) {
      throw new TechnicalFailure("Failed to associate token",ex.getCause());
    }
  }
  
}
