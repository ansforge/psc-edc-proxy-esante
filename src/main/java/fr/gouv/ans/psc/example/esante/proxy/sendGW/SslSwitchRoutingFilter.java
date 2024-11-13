/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.sendGW;

import fr.gouv.ans.psc.example.esante.proxy.UnauthorizedException;
import fr.gouv.ans.psc.example.esante.proxy.config.SendGatewayClientConfig;
import fr.gouv.ans.psc.example.esante.proxy.service.TechnicalFailure;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
      WebSession session = exchange.getSession().toFuture().get();
      if(session==null || !session.isStarted()) {
        throw new UnauthorizedException("Pas de session utilisateur.");
      }
      HttpClient defaultClient = super.getHttpClient(route, exchange);
      
      SslContextBuilder sslBuilder = SslContextBuilder.forClient();
      
      if(cfg.useInsecureTrustManager()){
          sslBuilder=sslBuilder.trustManager(new InsecureX509TrustManager());
      }
      
      final SslContext theCtx=sslBuilder.build();
      
      return defaultClient.secure(s ->
              s.sslContext(new SslContext() {
                    private SslContext ctx = theCtx;

                    @Override
                    public boolean isClient() {
                      LOGGER.debug("isClient called from {}",this);
                      return ctx.isClient();
                    }

                    @Override
                    public List<String> cipherSuites() {
                      LOGGER.debug("cipherSuite called from {}",this);
                      return ctx.cipherSuites();
                    }

                    @Override
                    public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
                      LOGGER.debug("applicationProtocolNegotiator called from {}",this);
                      return ctx.applicationProtocolNegotiator();
                    }

                    @Override
                    public SSLEngine newEngine(ByteBufAllocator bba) {
                      LOGGER.debug("newEngine(Bba) called from {}",this);
                      return ctx.newEngine(bba);
                    }

                    @Override
                    public SSLEngine newEngine(ByteBufAllocator bba, String string, int i) {
                      LOGGER.debug("newEngine(Bba,{},{}) called from {}",string,i,this);
                      return ctx.newEngine(bba,string,i);
                    }

                    @Override
                    public SSLSessionContext sessionContext() {
                      LOGGER.debug("sessionContext called from {}",this);
                      return ctx.sessionContext();
                    }
                  }));
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

  
  
}
