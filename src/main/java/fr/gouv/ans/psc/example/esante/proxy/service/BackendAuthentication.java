/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

/**
 * Contexte d'authentification auprès des services backend.
 * 
 * @author edegenetais
 */
public class BackendAuthentication {
  public final Credential credential;

  public BackendAuthentication(Credential credential) {
    this.credential = credential;
  }
  
}
