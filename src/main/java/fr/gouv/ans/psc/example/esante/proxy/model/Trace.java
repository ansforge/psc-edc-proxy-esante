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
package fr.gouv.ans.psc.example.esante.proxy.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Élément de trace produit par le système.
 * 
 * @author edegenetais
 */
public record Trace(
    TraceType type,
    String clientId,
    String IdRPPS,
    String ipAddress,
    List<Integer> ports,
    String proxy_id_session,
    String session_state,
    String dn,
    OffsetDateTime timestamp,
    @JsonInclude(JsonInclude.Include.NON_NULL) Request apiRequest) {

  public Trace(TraceType type,String clientId, String IdRPPS, String ipAddress, List<Integer> ports, String proxy_id_session, String session_state,String dn, OffsetDateTime timestamp, Request apiRequest) {
    this.type = type;
    this.clientId = clientId;
    this.IdRPPS = IdRPPS;
    this.ipAddress = ipAddress;
    this.ports = ports;
    this.proxy_id_session = proxy_id_session;
    this.session_state = session_state;
    this.dn = dn;
    this.timestamp = timestamp;
    this.apiRequest = apiRequest;
    this.type.validate(this);
  }
  
}
