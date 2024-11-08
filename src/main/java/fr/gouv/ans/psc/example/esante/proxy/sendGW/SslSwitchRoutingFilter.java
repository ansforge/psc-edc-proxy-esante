/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.sendGW;

import fr.gouv.ans.psc.example.esante.proxy.UnauthorizedException;
import fr.gouv.ans.psc.example.esante.proxy.config.SendGatewayClientConfig;
import fr.gouv.ans.psc.example.esante.proxy.service.AuthenticationFailure;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.X509TrustManager;
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
      
      return defaultClient.secure(
          s ->
              s.sslContext(
                  new SslContext() {
                    private SslContext ctx = theCtx;

                    @Override
                    public boolean isClient() {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("isClient called from {}",this);
                      return ctx.isClient();
                    }

                    @Override
                    public List<String> cipherSuites() {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("cipherSuite called from {}",this);
                      return ctx.cipherSuites();
                    }

                    @Override
                    public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("applicationProtocolNegotiator called from {}",this);
                      return ctx.applicationProtocolNegotiator();
                    }

                    @Override
                    public SSLEngine newEngine(ByteBufAllocator bba) {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("newEngine(Bba) called from {}",this);
                      return ctx.newEngine(bba);
                    }

                    @Override
                    public SSLEngine newEngine(ByteBufAllocator bba, String string, int i) {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("newEngine(Bba,{},{}) called from {}",string,i,this);
                      return ctx.newEngine(bba,string,i);
                    }

                    @Override
                    public SSLSessionContext sessionContext() {
                      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("sessionContext called from {}",this);
                      return ctx.sessionContext();
                    }
                  }));
    } catch (SSLException ex) {
      throw new RuntimeException(ex);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  @Override
  public int getOrder() {
    return super.getOrder()-1;
  }

  private static class InsecureX509TrustManager implements X509TrustManager {

    public InsecureX509TrustManager() {
      try {
        LoggerFactory.getLogger(
            Class.forName("fr.gouv.ans.psc.example.esante.proxytest.UnitTestMarker"))
            .debug("Activated {}",InsecureX509TrustManager.class);
      } catch (ClassNotFoundException ex) {
        throw new IllegalStateException("Calling insecure out of unit tests",ex);
      }
    }
    
    @Override
    public void checkClientTrusted(X509Certificate[] xcs, String string)
        throws CertificateException {
      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("Trusting everyting, I was asked about {},{}",xcs,string);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] xcs, String string)
        throws CertificateException {
      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("Trusting everyting for server {}",string);
      LoggerFactory.getLogger(SslSwitchRoutingFilter.class).trace("server trust for certs {}",
          new Object() {
            @Override
            public String toString() {
              return xcs==null?"null":Arrays.deepToString(xcs);
            }
          }
      );
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      throw new UnsupportedOperationException(
          "Not supported yet."); // Generated from
      // nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
  }
  
  
}
