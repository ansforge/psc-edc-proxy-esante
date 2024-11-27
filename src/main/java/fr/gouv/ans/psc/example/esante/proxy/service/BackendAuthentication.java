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

import java.util.HashMap;
import java.util.Map;

/**
 * Contexte d'authentification auprès des services backend.
 * 
 * @author edegenetais
 */
public class BackendAuthentication {
  public final Credential credential;
  private Map<String,BackendAccess> backendAccessTokens=new HashMap<>();
  public BackendAuthentication(Credential credential) {
    this.credential = credential;
  }
  
  public BackendAccess switchBackendToken(String backendId, BackendAccess tokenFuture) {
    return this.backendAccessTokens.put(backendId, tokenFuture);
  }
  
  public BackendAccess findBackendToken(String backendId) {
    return backendAccessTokens.get(backendId);
  }
}
