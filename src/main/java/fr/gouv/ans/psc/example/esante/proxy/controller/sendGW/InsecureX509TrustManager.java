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
package fr.gouv.ans.psc.example.esante.proxy.controller.sendGW;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.X509TrustManager;
import org.slf4j.LoggerFactory;

/**
 * Trust manager non-sécurisé (accepte tout certificat serveur) poour les tests d'intégration HTTPS.
 * NB : ce composant est conçu pour bloquer le démarrage de l'application packagée car cette configuration 
 * est bien entendu complètemment inadaptée à une situation de production.
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
