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
 * Cet objet est utilisé pour porter les identifiants utilisés à la fois pour :
 * <li> obtenir un jeton ProsanteConnect
 * <li> obtenir des jetons auprès des services token exchange des backends
 * <li> fournir les certificats clients demandés pour les backends.
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
  
  public KeyManagerFactory buildKeyManagerFactory() {
    return this.type.keyManagerFactory(this);
  }
  
  /**
   * Types d'identifiants poour un client.
   * 
   */
  public static enum CredentialType {
    /**
     * Ce type d'identifiant a servi aux phases initiales précédant la mise en place de 
     * l'authentification mTLS pour les clients CIBA, et est utilisé dans les TI pour vérifier le bon fonctionnement du code.
     * @deprecated : ne pass utiliser en production.
     */
    @Deprecated
  SECRET {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
    }

    @Override
    public ClientAuthentication buildAuth(String clientId, Credential credential) {
      return new ClientSecretBasic(new ClientID(clientId), new Secret(credential.secret));
    }

    @Override
    public KeyManagerFactory keyManagerFactory(Credential credential) {
      return null;
    }
    /**
     * Type d'identifiants à utiliser en production pour l'authentification CIBA: certificat client mTLS.
     */
  }, MTLS {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
      Objects.requireNonNull(file);
    }

      @Override
      public ClientAuthentication buildAuth(String clientId, Credential credential) {
        try {
          TrustManagerFactory tf =
              TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
          tf.init((KeyStore) null);
          
          KeyManagerFactory kmf = keyManagerFactory(credential);
          
          SSLContext sslCtx = SSLContext.getInstance("TLS");
          sslCtx.init(kmf.getKeyManagers(), tf.getTrustManagers(), null);
          return new PKITLSClientAuthentication(new ClientID(clientId), sslCtx.getSocketFactory());
          
        } catch (
            KeyManagementException |
            NoSuchAlgorithmException |
            KeyStoreException e) {
          throw new TechnicalFailure("Failed to build mTLS auth for client " + clientId, e);
        }
      }
      
      @Override
      public KeyManagerFactory keyManagerFactory(Credential credential) {

        try (InputStream certIs = new FileInputStream(credential.file)) {
          
          final char[] password = credential.secret.toCharArray();
          KeyStore store = KeyStore.getInstance("pkcs12");
          store.load(certIs, password);
          KeyManagerFactory kmf =
              KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
          kmf.init(store, password);
          return kmf;
          
        } catch (
            UnrecoverableKeyException |
            KeyStoreException         |
            NoSuchAlgorithmException  |
            CertificateException      |
            IOException ex) {
          throw new TechnicalFailure("Failed to load certificate " + credential.file, ex);
        }
      }


  };

  /**
   * Logique de validation des données de configuration.
   * @param secret secret utilisé.
   * @param file fichier utilisé.
   */
  public abstract void validate(String secret, String file);

  /**
   * Insérer ces données d'identification dans la configuratioon d'authentification de la transaction CIBA.
   * 
   * @param clientId
   * @param credential
   * @return un descripteur d'authentification CIBA.
   */
  public abstract ClientAuthentication buildAuth(String clientId, Credential credential);
  
  /**
   * Traduire ces identifiants sous forme de keyManagerFactory en vue des requêtes <code>/send</code>.
   * 
   * @param credential
   * @return 
   */
  public abstract KeyManagerFactory keyManagerFactory(Credential credential);
  
  }

}
