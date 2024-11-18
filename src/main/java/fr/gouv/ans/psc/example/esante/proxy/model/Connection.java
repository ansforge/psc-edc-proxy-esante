/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.model;

/**
 * Contenu de la requête `/connect`.
 * @author edegenetais
 */
public record Connection (
    String nationalId,
    String bindingMessage,
    String clientId,
    String channel
    ){}
