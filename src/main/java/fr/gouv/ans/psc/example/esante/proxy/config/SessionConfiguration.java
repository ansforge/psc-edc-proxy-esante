/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import java.util.HashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Configuration de la gestion de session du proxy.
 * @author edegenetais
 */
@Configuration
@EnableSpringWebSession
public class SessionConfiguration {
  private static final String SESSION_COOKIE_NAME = "proxy_session_id";
  
  @Bean
  public ReactiveSessionRepository<? extends Session> getSessionRepository(){
    final ReactiveMapSessionRepository reactiveMapSessionRepository = new ReactiveMapSessionRepository(new HashMap<>());
    return reactiveMapSessionRepository;
  }
  
  @Bean
  public WebSessionIdResolver getSessionResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieName(SESSION_COOKIE_NAME);
    return resolver;
  }
}
