/*
 * The MIT License
 * Copyright © 2024-2024 Agence du Numérique en Santé (ANS)
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
public class AuthenticationFailure extends FunctionalError {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFailure.class);

	public AuthenticationFailure(ErrorResponse response,String clientId, String nationalId) {
		super(Category.UNAUTHORIZED,parseError(response),clientId,nationalId);
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
