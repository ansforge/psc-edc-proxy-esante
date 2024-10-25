/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

/**
 *
 * @author edegenetais
 */
public class TechnicalFailure extends RuntimeException {

  public TechnicalFailure(String message, Throwable cause) {
    super(message, cause);
  }
  
}
