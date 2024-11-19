/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Contexte d'authentification auprès des services backend.
 * 
 * @author edegenetais
 */
public class BackendAuthentication {
  public final Credential credential;
  private Map<String,Future<BackendAccess>> backendAccessTokens=new HashMap<>();
  public BackendAuthentication(Credential credential) {
    this.credential = credential;
  }
  
  public Future<BackendAccess> switchFutureBackendToken(String backendId, Future<BackendAccess> tokenFuture) {
    return this.backendAccessTokens.put(backendId, tokenFuture);
  }
  
  public Future<BackendAccess> findBackendToken(String backendId) {
    return backendAccessTokens.get(backendId);
  }
}
