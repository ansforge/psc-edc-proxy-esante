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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Override
    public WebClient.Builder apply(WebClient.Builder builder, String clientId, Credential credential) {
        return builder.defaultHeaders(h -> h.setBasicAuth(clientId, credential.secret));
    }
    
  }, MTLS {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
      Objects.requireNonNull(file);
    }

    @Override
    public ClientAuthentication buildAuth(String clientId, Credential credential) {
      try (InputStream certIs = new FileInputStream(credential.file)) {
        return new PKITLSClientAuthentication(new ClientID(clientId), (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certIs));
      } catch (CertificateException | IOException ex) {
        throw new TechnicalFailure("Failed to load certificate " + credential.file, ex);
      }
    }

    @Override
    public WebClient.Builder apply(WebClient.Builder builder, String clientId, Credential credential) {
      throw new UnsupportedOperationException("Not yet");
    }
    
    
  };

  public abstract void validate(String secret, String file);

  public abstract ClientAuthentication buildAuth(String clientId, Credential credential);
  
  public abstract WebClient.Builder apply(WebClient.Builder builder, String clientId, Credential credential);
}

}
