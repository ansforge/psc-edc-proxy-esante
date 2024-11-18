/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

/**
 *
 * @author edegenetais
 */
public class InvalidConfigurationException extends RuntimeException{

  public InvalidConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidConfigurationException(String message) {
    super(message);
  }
  
}
