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

import java.util.Objects;

/**
 *
 * @author edegenetais
 */
public enum TraceType {
  CONNECT_SUCCESS {
    @Override
    public void validateImpl(Trace t) {
      if(t.request()!=null) {
        throw new IllegalArgumentException("No request data allowed in " + name()+" traces.");
      }
      Objects.requireNonNull(t.clientId(), "clientId is mandatory");
      Objects.requireNonNull(t.proxy_id_session(), "proxy_id_session is mandatory");
    }
  }
  , CONNECT_FAILURE{
    @Override
    protected void validateImpl(Trace t) {
      if(t.request()!=null) {
        throw new IllegalArgumentException("No request data allowed in " + name()+" traces.");
      }
    }
  }, SEND {
    @Override
    protected void validateImpl(Trace t) {
      Objects.requireNonNull(t.request(),"request is mandatory");
      Objects.requireNonNull(t.clientId(), "clientId is mandatory");
      Objects.requireNonNull(t.proxy_id_session(), "proxy_id_session is mandatory");
    }
  }, DISCONNECT {
    @Override
    protected void validateImpl(Trace t) {
      if(t.request()!=null) {
        throw new IllegalArgumentException("No request data allowed in " + name()+" traces.");
      }
      Objects.requireNonNull(t.clientId(), "clientId is mandatory");
      Objects.requireNonNull(t.proxy_id_session(), "proxy_id_session is mandatory");
    }
  };
  
  public void validate(Trace t) {
    if (t.type() == null) {
      throw new NullPointerException("Trace type is mandatory.");
    }
    if (t.type() != this) {
      throw new IllegalArgumentException("For use only on the right type");
    }
    Objects.requireNonNull(t.IdRPPS(), "IdRPPS is mandatory");
    Objects.requireNonNull(t.ipAddress(), "ipAddress is mandatory");
    Objects.requireNonNull(t.ports(), "remote port is mandatory");
    Objects.requireNonNull(t.timestamp(), "timestamp is mandatory");
    
    validateImpl(t);
  }
  protected abstract void validateImpl(Trace t);
}
