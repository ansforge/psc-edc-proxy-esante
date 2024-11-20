/*
 * The MIT License
 * Copyright © 2024-2024 Agence du Numérique en Santé (ANS)
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
package fr.gouv.ans.psc.example.esante.proxy.controller.sendGW;

import fr.gouv.ans.psc.example.esante.proxy.UnauthorizedException;
import fr.gouv.ans.psc.example.esante.proxy.config.SendGatewayClientConfig;
import fr.gouv.ans.psc.example.esante.proxy.controller.SessionAttributes;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthentication;
import fr.gouv.ans.psc.example.esante.proxy.service.Credential;
import fr.gouv.ans.psc.example.esante.proxy.service.TechnicalFailure;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.netty.http.client.HttpClient;

/**
 * Ce composant associe pour chaque session un contexte SSL adapté au sein du proxy (méthode <code>/send</code>).s
 * Il surcharge le routeur par défaut du spring-gateway@netty pour lui ajouter cette fonctionalité.
 * 
 * @author edegenetais
 */
@Component
public class SslSwitchRoutingFilter extends NettyRoutingFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SslSwitchRoutingFilter.class);
  
  @Autowired
  private SendGatewayClientConfig cfg;
  
  public SslSwitchRoutingFilter(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider, HttpClientProperties properties) {
    super(httpClient, headersFiltersProvider, properties);
  }

  @Override
  protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
    try {
      final WebSession session = exchange.getSession().toFuture().get();
      if(session==null || !session.isStarted()) {
        throw new UnauthorizedException("Pas de session utilisateur.");
      }
      HttpClient defaultClient = super.getHttpClient(route, exchange);
      
      SslContextBuilder sslBuilder = SslContextBuilder.forClient();
      
      if(cfg.useInsecureTrustManager()){
          sslBuilder=sslBuilder.trustManager(new InsecureX509TrustManager());
      }
      
      BackendAuthentication backendAuthentication = session.getAttribute(SessionAttributes.BACKEND_AUTH_ATTR);
      final Credential credential = backendAuthentication.credential;
      KeyManagerFactory kmf = credential
          .buildKeyManagerFactory();
      LOGGER.debug("Provided kmf : {} for credential {}",kmf,credential);
      final SslContext theCtx = sslBuilder.keyManager(kmf).build();

      return defaultClient.secure(s ->
              s.sslContext(new SslContextSpy(theCtx, session)));
    } catch (InterruptedException | SSLException ex) {
      throw new TechnicalFailure("Échec du paramétrage SSL pour la connexion sortante.",ex);
    } catch (ExecutionException ex) {
      LOGGER.debug("Contexte complet pour Execution error.",ex);
      throw new TechnicalFailure("Échec du paramétrage SSL pour la connexion sortante.",ex.getCause());
    }
  }
  
  @Override
  public int getOrder() {
    return super.getOrder()-1;
  }

  /**
   * Ce delegate wrapper permettra d'ajouter quelques logs traçant l'utilisation du contexte SSL pour une session.
   */
  private class SslContextSpy extends SslContext {

    private final SslContext theCtx;
    private final WebSession session;

    public SslContextSpy(SslContext theCtx, WebSession session) {
      LOGGER.debug("Création d'un contexte SSL pour la session {}",session.getId());
      this.theCtx = theCtx;
      this.session = session;
    }
   
    @Override
    public boolean isClient() {
      LOGGER.debug("isClient called from session {}",session.getId());
      return theCtx.isClient();
    }

    @Override
    public List<String> cipherSuites() {
      LOGGER.debug("cipherSuite called from session {}",session.getId());
      return theCtx.cipherSuites();
    }

    @Override
    public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
      LOGGER.debug("applicationProtocolNegotiator called from session {}",session.getId());
      return theCtx.applicationProtocolNegotiator();
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator bba) {
      LOGGER.debug("newEngine(Bba) called from session {}",session.getId());
      return theCtx.newEngine(bba);
    }

    @Override
    public SSLEngine newEngine(ByteBufAllocator bba, String string, int i) {
      LOGGER.debug("newEngine(Bba,{},{}) called from session {}",string,i,session.getId());
      return theCtx.newEngine(bba,string,i);
    }

    @Override
    public SSLSessionContext sessionContext() {
      LOGGER.debug("sessionContext called from session {}",session.getId());
      return theCtx.sessionContext();
    }
  }

  
  
}
