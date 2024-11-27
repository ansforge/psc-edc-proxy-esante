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

import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import fr.gouv.ans.psc.example.esante.proxy.service.TraceService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Ce contrôleur propose le service getTrace défini par la spécification.
 *
 * @author edegenetais
 */
@RestController
public class TraceController {
  private TraceService traceSrv;

  public TraceController(@Autowired TraceService traceSrv) {
    this.traceSrv = traceSrv;
  }
  

  @GetMapping("/traces")
  public Flux<Trace> gettraces(@RequestParam("start") OffsetDateTime startDate, @RequestParam(required = false, name = "end") OffsetDateTime end) {
    final List<Trace> traceStoreList = traceSrv.getTraces();
    final OffsetDateTime effectiveEnd = Objects.requireNonNullElse(end, OffsetDateTime.now());
    return Flux.fromStream(
        traceStoreList.stream()
            .filter(t -> startDate.isBefore(t.timestamp()))
            .filter(t -> effectiveEnd.isAfter(t.timestamp())));
  }

}
