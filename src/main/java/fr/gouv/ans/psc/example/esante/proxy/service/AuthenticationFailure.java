/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ciba.CIBAResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception utilisée pour signaler un échec fonctionnel de l'authentification (et non un incident technique).
 * 
 * @author edegenetais
 */
public class AuthenticationFailure extends RuntimeException{
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFailure.class);
  
  public AuthenticationFailure(CIBAResponse response) {
    super(parseError(response));
  }

  private static String parseError(CIBAResponse response) {
    try {
    if(response.indicatesSuccess()) {
      throw new IllegalArgumentException("The response is a success response :\n"+response.toHTTPResponse().getBodyAsJSONObject().toJSONString());
    }
    final ErrorObject errorObject = response.toErrorResponse().getErrorObject();
    return errorObject.getHTTPStatusCode()+" - "+
        errorObject.getCode()+" : "+errorObject.getDescription();
    } catch(ParseException ex) {
      LOGGER.warn("Failed to parse error inforamtion for authentication failuer", ex);
      return "Failed to get error information.";
    }
  }
}
