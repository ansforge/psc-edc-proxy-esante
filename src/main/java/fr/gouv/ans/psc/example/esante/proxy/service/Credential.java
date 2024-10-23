/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

/**
 * @author edegenetais
 */
public record Credential(CredentialType type, String secret, String file) {
  
  public Credential(CredentialType type, String secret, String file) {
    this.type = type;
    this.secret = secret;
    this.file = file;
    type.validate(secret, file);
  }

  public ClientAuthentication buildAuth(String clientId) {
    return this.type.buildAuth(clientId, secret, file);
  }
  
}
