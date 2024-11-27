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

import fr.gouv.ans.psc.example.esante.proxy.service.BaseTraceData;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 *
 * @author edegenetais
 */
public class TraceHelper {
  public static final String BASE_TRACE_DATA_ATTR="fr.gouv.ans.psc.example.esante.proxy.controller.BaseTraceData";
  
  public static BaseTraceData getBaseTraceData(ServerWebExchange exchange) {
    final ServerHttpRequest request = exchange.getRequest();
      final String requestMethod = request.getMethod().name();
      
      
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
      
      return new BaseTraceData(sourcePorts, sourceAddress, requestMethod);
  }
}
