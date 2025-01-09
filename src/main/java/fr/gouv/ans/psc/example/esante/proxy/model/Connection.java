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
package fr.gouv.ans.psc.example.esante.proxy.model;

import fr.gouv.ans.psc.example.esante.proxy.controller.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Contenu de la requête `/connect`.
 * @author edegenetais
 */
public record Connection (
    String nationalId,
    String bindingMessage,
    String clientId,
    String channel
    ){
  public void validate() {
    List<String> messages = new ArrayList<>();

    if(nationalId==null) {
      messages.add("nationalId is missing");
    }
    if(bindingMessage==null){
      messages.add("bindingMessage is missing");
    }
    if(clientId==null) {
      messages.add("clientId is missing");
    }
    if(channel==null) {
      messages.add("channel is missing");
    } else if(!List.of("CARD","MOBILE").contains(channel)) {
      messages.add("channel value "+channel+" is invalid");
    }
    
    if(messages.isEmpty()) {
      LoggerFactory.getLogger(Connection.class).debug("Connection query payload valid.");
    } else {
      throw new ValidationException(messages.toString(), clientId, nationalId);
    }
  }
}
