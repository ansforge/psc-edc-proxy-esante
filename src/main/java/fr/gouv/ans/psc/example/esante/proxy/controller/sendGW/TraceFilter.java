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

import fr.gouv.ans.psc.example.esante.proxy.controller.SessionAttributes;
import fr.gouv.ans.psc.example.esante.proxy.model.Request;
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import fr.gouv.ans.psc.example.esante.proxy.model.TraceType;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthentication;
import fr.gouv.ans.psc.example.esante.proxy.service.TechnicalFailure;
import fr.gouv.ans.psc.example.esante.proxy.service.TraceService;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * @author edegenetais
 */
@Component
public class TraceFilter implements GlobalFilter {
  private TraceService traceSrv;

  public TraceFilter(@Autowired TraceService traceSrv) {
    this.traceSrv = traceSrv;
  }
  
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    try {
      final ServerHttpRequest request = exchange.getRequest();
      final String requestMethod = request.getMethod().name();
      final String requestPath = request.getPath().value();
      
      final String sourceAddress;
      final List<Integer> sourcePorts;
      if(request.getHeaders().containsKey("X-Forwarded-For")) {
        sourceAddress = request.getHeaders().get("X-Forwarded-For").getFirst();
        sourcePorts = List.of();
      } else {
        sourceAddress = request.getRemoteAddress().getAddress().toString();
        final ArrayList<Integer> portList = new ArrayList<>();
        if(request.getRemoteAddress().getPort()!=0) {
          portList.add(request.getRemoteAddress().getPort());
        }
        sourcePorts = portList;
      }
      
      final Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      final String nomApiPsc = route.getId();

      final Request newRequest = new Request(nomApiPsc, requestMethod, requestPath);
      
      final WebSession session = exchange.getSession().toFuture().get();
      final String clientId = session.getAttribute(SessionAttributes.CLIENT_ID);
      final String nationalId = session.getAttribute(SessionAttributes.NATIONAL_ID);
      final BackendAuthentication backendAuth = session.getAttribute(SessionAttributes.BACKEND_AUTH_ATTR);
      Optional<X509Certificate> crt = backendAuth.credential.getClientCert();
      
      final Trace newTrace = new Trace(
          TraceType.SEND,
          clientId,
          nationalId,
          sourceAddress,
          sourcePorts,
          session.getId(),
          crt.isPresent()?crt.get().getSubjectX500Principal().toString():null,
          OffsetDateTime.now(),
          newRequest
      );
      
      traceSrv.record(newTrace);
      
      return chain.filter(exchange);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new TechnicalFailure("Interrupted while fetching session data.",ex);
    } catch (ExecutionException ex) {
       throw new TechnicalFailure("Failure while fetching session data",ex.getCause());
    }
  }
}
