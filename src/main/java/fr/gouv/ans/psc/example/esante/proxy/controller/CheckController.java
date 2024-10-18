/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author edegenetais
 */
@RestController
@RequestMapping("/check")
public class CheckController {
  @GetMapping("/alive")
  public Mono<String> alive() {
    return Mono.just("OK");
  }
}
