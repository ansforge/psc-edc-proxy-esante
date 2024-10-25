/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author edegenetais
 */
@SpringBootApplication
@ConfigurationPropertiesScan("fr.gouv.ans.psc.example.esante.proxy.config")
public class EsanteProxyApplication {
  public static void main(String[] args) {
    SpringApplication.run(EsanteProxyApplication.class, args);
  }
}
