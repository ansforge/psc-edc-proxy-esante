/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;

/**
 * Cet objet représente la réponse d'une requête de polling de l'enpoint token.
 *
 * @author edegenetais
 */
public record CIBASession(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_expires_in") Integer refreshExpiresIn,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("id_token") String idToken,
    @JsonProperty("scope") String scope,
    @JsonProperty("session_state") String sessionState
) {
  public JWT idTokenAsJWT() {
    try {
      return SignedJWT.parse(this.idToken);
    } catch (ParseException ex) {
      throw new TechnicalFailure("Échec d'interprétation du jeton JWT idToken CIBA", ex);
    }
  }
}
