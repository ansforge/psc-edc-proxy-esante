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

/**
 *
 * @author edegenetais
 */
public enum CredentialType {
  SECRET {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
    }

    @Override
    public ClientAuthentication buildAuth(String clientId, String secret, String file) {
      return new ClientSecretBasic(new ClientID(clientId), new Secret(secret));
    }
  }, MTLS {
    @Override
    public void validate(String secret, String file) {
      Objects.requireNonNull(secret);
      Objects.requireNonNull(file);
    }

    @Override
    public ClientAuthentication buildAuth(String clientId, String secret, String file) {
      try (InputStream certIs = new FileInputStream(file)) {
        return new PKITLSClientAuthentication(new ClientID(clientId), (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certIs));
      } catch (CertificateException | IOException ex) {
        throw new TechnicalFailure("Failed to load certificate " + file, ex);
      }
    }
  };

  public abstract void validate(String secret, String file);

  public abstract ClientAuthentication buildAuth(String clientId, String secret, String file);

}
