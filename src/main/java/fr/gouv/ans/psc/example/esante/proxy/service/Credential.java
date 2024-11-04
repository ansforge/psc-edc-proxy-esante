/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.PKITLSClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author edegenetais
 */
public record Credential(CredentialType type, String secret, String file) {
  
  public Credential(CredentialType type, String secret, String file) {
    this.type = type;
    this.secret = secret;
    this.file = file;
    type.validate(secret, file);
  }

  public ClientAuthentication buildAuth(String clientId) {
    return this.type.buildAuth(clientId, this);
  }
  
  public static enum CredentialType {
  SECRET {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
    }

    @Override
    public ClientAuthentication buildAuth(String clientId, Credential credential) {
      return new ClientSecretBasic(new ClientID(clientId), new Secret(credential.secret));
    }

  }, MTLS {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
      Objects.requireNonNull(file);
    }

      @Override
      public ClientAuthentication buildAuth(String clientId, Credential credential) {
        final char[] password = credential.secret.toCharArray();
        try (InputStream certIs = new FileInputStream(credential.file)) {
          SSLContext sslCtx = SSLContext.getInstance("TLS");
          TrustManagerFactory tf =
              TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
          tf.init((KeyStore) null);
          KeyStore store = KeyStore.getInstance("pkcs12");

          store.load(certIs, password);
          KeyManagerFactory kmf =
              KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
          kmf.init(store, password);
          sslCtx.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);

          return new PKITLSClientAuthentication(new ClientID(clientId), sslCtx.getSocketFactory());
        } catch (
            KeyManagementException |
            UnrecoverableKeyException |
            KeyStoreException| 
            NoSuchAlgorithmException |
            CertificateException |
            IOException ex) {
          throw new TechnicalFailure("Failed to load certificate " + credential.file, ex);
        }
      }
  };

  public abstract void validate(String secret, String file);

  public abstract ClientAuthentication buildAuth(String clientId, Credential credential);
  
  }

}
