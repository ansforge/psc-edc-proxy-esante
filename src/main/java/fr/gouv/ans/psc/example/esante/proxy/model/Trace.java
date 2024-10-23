/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.model;

import java.time.OffsetDateTime;

/**
 * Élément de trace produit par le système.
 * 
 * @author edegenetais
 */
public record Trace(
    OffsetDateTime timestamp,// Non-conforme pour simplification à ce stade, à rendre conforme plus tard.
    Request request
){}
