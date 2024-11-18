/*
 * (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
 */
package fr.gouv.ans.psc.example.esante.proxy.service;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception utilisée pour signaler un échec fonctionnel de l'authentification (et non un incident technique).
 * 
 * @author edegenetais
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationFailure extends RuntimeException {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFailure.class);

	public AuthenticationFailure(ErrorResponse response) {
		super(parseError(response));
	}

	private static String parseError(ErrorResponse response) {
		try {
			if (response.indicatesSuccess()) {
				throw new IllegalArgumentException("The response is a success response :\n"
						+ response.toHTTPResponse().getBodyAsJSONObject().toJSONString());
			}

			final ErrorObject errorObject = response.getErrorObject();
			return errorObject.getHTTPStatusCode() + " - " + errorObject.getCode() + " : "
					+ errorObject.getDescription();

		} catch (ParseException ex) {
			LOGGER.warn("Failed to parse error inforamtion for authentication failuer", ex);
			return "Failed to get error information.";
		}

	}
}
