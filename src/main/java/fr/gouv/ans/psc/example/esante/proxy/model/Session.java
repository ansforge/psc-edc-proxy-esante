/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Proxy session identifier.
 * 
 * @author edegenetais
 */
public record Session(
    @JsonProperty("proxy_session_id")String proxySessionId,
    @JsonProperty("session_state") String sessionState
){}
