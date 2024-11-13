/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.sendGW;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.X509TrustManager;
import org.slf4j.LoggerFactory;

/**
 *
 * @author edegenetais
 */
class InsecureX509TrustManager implements X509TrustManager {

  public InsecureX509TrustManager() {
    try {
      LoggerFactory.getLogger(Class.forName("fr.gouv.ans.psc.example.esante.proxytest.UnitTestMarker")).debug("Activated {}", InsecureX509TrustManager.class);
    } catch (ClassNotFoundException ex) {
      throw new IllegalStateException("Calling insecure out of unit tests", ex);
    }
  }

  @Override
  public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
    LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("Trusting everyting, I was asked about {},{}", xcs, string);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
    LoggerFactory.getLogger(SslSwitchRoutingFilter.class).debug("Trusting everyting for server {}", string);
    LoggerFactory.getLogger(SslSwitchRoutingFilter.class).trace("server trust for certs {}", new Object() {
      @Override
      public String toString() {
        return xcs == null ? "null" : Arrays.deepToString(xcs);
      }
    });
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from
    // nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

}
