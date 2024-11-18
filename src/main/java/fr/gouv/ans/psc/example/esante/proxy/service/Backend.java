/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import java.util.Map;

/**
 *
 * @author edegenetais
 */
public record Backend (String id,Map<String,String> metadata){}
