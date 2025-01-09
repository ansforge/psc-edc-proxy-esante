/*
 * The MIT License
 * Copyright © 2024-2025 Agence du Numérique en Santé (ANS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.gouv.ans.psc.example.esante.proxy.config;

import java.util.HashMap;

import org.springframework.boot.autoconfigure.session.SessionProperties;
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
  

  private SessionProperties sessionProperties;
  
  
  public SessionConfiguration(SessionProperties sessionProerties) {
	  this.sessionProperties = sessionProerties;
  }
  
  @Bean
  public ReactiveSessionRepository<? extends Session> getSessionRepository(){
    final ReactiveMapSessionRepository reactiveMapSessionRepository = new ReactiveMapSessionRepository(new HashMap<>());
    reactiveMapSessionRepository.setDefaultMaxInactiveInterval(sessionProperties.getTimeout());
    return reactiveMapSessionRepository;
  }
  
  @Bean
  public WebSessionIdResolver getSessionResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieName(SESSION_COOKIE_NAME);
    return resolver;
  }
}
