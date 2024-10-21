/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.model;

/**
 * Modèle d'une requête unitaire au sein d'une trace (whatever it means).
 * 
 * @author edegenetais
 */
public record Request(String nomApiPsc, String methode, String path){}
