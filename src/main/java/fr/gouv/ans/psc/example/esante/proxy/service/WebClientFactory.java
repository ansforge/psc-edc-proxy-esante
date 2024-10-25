/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author edegenetais
 */
public class WebClientFactory {
  private Credential credential;
  private String clientId;

  public WebClientFactory(String clientID, Credential credential) {
    this.credential = credential;
    this.clientId = clientID;
  }

  public WebClient build(String baseUrl) {
    final Credential.CredentialType credentialType = this.credential.type();
    final WebClient.Builder builder = WebClient.builder().baseUrl(baseUrl);
    return credentialType.apply(builder, clientId, credential).build();
  }
}
