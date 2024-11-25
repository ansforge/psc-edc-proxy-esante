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

import fr.gouv.ans.psc.example.esante.proxy.controller.SessionAttributes;
import fr.gouv.ans.psc.example.esante.proxy.model.Request;
import fr.gouv.ans.psc.example.esante.proxy.model.Trace;
import fr.gouv.ans.psc.example.esante.proxy.model.TraceType;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebSession;

/**
 * @author edegenetais
 */
@Component
public class TraceService {

  public final List<Trace> store;

  public TraceService() {
    store = new LinkedList<>();
  }
  
  public void record(TraceType traceType, final WebSession session, final String sourceAddress, final List<Integer> sourcePorts, final Request outGoingRequest) {
    final String clientId = session.getAttribute(SessionAttributes.CLIENT_ID);
    final String nationalId = session.getAttribute(SessionAttributes.NATIONAL_ID);
    final BackendAuthentication backendAuth = session.getAttribute(SessionAttributes.BACKEND_AUTH_ATTR);
    Optional<X509Certificate> crt = backendAuth.credential.getClientCert();
    final Trace newTrace = new Trace(
        traceType, 
        clientId, 
        nationalId, 
        sourceAddress, 
        sourcePorts, 
        session.getId(), 
        crt.isPresent() ? crt.get().getSubjectX500Principal().toString() : null, 
        OffsetDateTime.now(), outGoingRequest);
    store.add(newTrace);
  }
  
  public synchronized List<Trace> getTraces() {
    return List.copyOf(store);
  }
}
