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
package fr.gouv.ans.psc.example.esante.proxy.controller;

import fr.gouv.ans.psc.example.esante.proxy.model.ErrorDescriptor;
import fr.gouv.ans.psc.example.esante.proxy.model.ErrorDescriptor.Metadata;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.service.FunctionalError;
import static fr.gouv.ans.psc.example.esante.proxy.service.FunctionalError.Category.NOT_FOUND;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author edegenetais
 */
@ControllerAdvice
@Order(-3)
public class DescriptorExceptionHandler extends ResponseEntityExceptionHandler {

  public DescriptorExceptionHandler() {
    LoggerFactory.getLogger(DescriptorExceptionHandler.class).debug("Error payload handler created.");
  }

  @ExceptionHandler({Reconnect.class})
  public ResponseEntity<Session> handleReconnect(Reconnect recon) {
    return new ResponseEntity<>(recon.session,HttpStatusCode.valueOf(304));
  }
  @ExceptionHandler({FunctionalError.class})
  public ResponseEntity<ErrorDescriptor> handleUnknownClientId(FunctionalError ex) {
    HttpStatusCode status = switch(ex.category){
          case NOT_FOUND -> HttpStatusCode.valueOf(404);
          case UNAUTHORIZED -> HttpStatusCode.valueOf(401);
          default -> throw new IllegalArgumentException("Unknown category "+ex.category);
    };
    return new ResponseEntity<>(
        new ErrorDescriptor(Integer.toString(status.value()), ex.getMessage(), new Metadata(ex.nationalId, ex.clientId)),
        status
    );
  }
}
