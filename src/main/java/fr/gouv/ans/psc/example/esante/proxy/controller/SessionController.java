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
package fr.gouv.ans.psc.example.esante.proxy.controller;

import fr.gouv.ans.psc.example.esante.proxy.model.Connection;
import fr.gouv.ans.psc.example.esante.proxy.model.Session;
import fr.gouv.ans.psc.example.esante.proxy.model.TraceType;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthentication;
import fr.gouv.ans.psc.example.esante.proxy.service.BackendAuthenticationService;
import fr.gouv.ans.psc.example.esante.proxy.service.BaseTraceData;
import fr.gouv.ans.psc.example.esante.proxy.service.CIBASession;
import fr.gouv.ans.psc.example.esante.proxy.service.PSCSessionService;
import fr.gouv.ans.psc.example.esante.proxy.service.TraceService;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Ce contrôleur gère les sessions (méthodes `/connect` et `/disconnect`).
 *
 * @author edegenetais
 */
@RestController
public class SessionController {
  private final PSCSessionService cibaService;
  private final BackendAuthenticationService backendAuthService;
  private final TraceService traceSrv;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);
  
  public SessionController(
      @Autowired PSCSessionService cibaService,
      @Autowired BackendAuthenticationService backendAuthService,
      @Autowired TraceService traceSrv
  ) {
    this.cibaService = cibaService;
    this.backendAuthService = backendAuthService;
    this.traceSrv = traceSrv;
  }

  @PostMapping("/connect")
  public Mono<Session> connect(
      @RequestBody Connection connection, 
      @RequestAttribute(name = TraceHelper.BASE_TRACE_DATA_ATTR) BaseTraceData baseTraceData, 
      WebSession webSession) {
    connection.validate();
    if(webSession!=null && webSession.isStarted()) {
      traceSrv.record(
          TraceType.CONNECT_REPLAY,
          TraceHelper.getSessionTraceData(webSession),
          baseTraceData,
          null);
      throw new Reconnect();
    }
    Callable<Session> sessionSupplier =
        () -> {
          try {
            String sessionId = webSession.getId();
            webSession.getAttributes().put(SessionAttributes.CLIENT_ID, connection.clientId());
            webSession.getAttributes().put(SessionAttributes.NATIONAL_ID, connection.nationalId());
            CIBASession cibaSession =
                this.cibaService.cibaAuthentication(
                    connection.bindingMessage(),
                    connection.nationalId(),
                    connection.clientId(),
                    connection.channel());
            webSession.getAttributes().put(SessionAttributes.CIBA_SESSION, cibaSession);

            BackendAuthentication backendAuth =
                this.backendAuthService.authenticate(cibaSession, connection.clientId());
            webSession.getAttributes().put(SessionAttributes.BACKEND_AUTH_ATTR, backendAuth);

            webSession.start();
            LOGGER.debug("MaxIdleTime in seconds : {}",webSession.getMaxIdleTime().getSeconds());
            final Session session = new Session(sessionId, cibaSession.sessionState());
            webSession.getAttributes().put(SessionAttributes.PROXY_API_SESSION, session);
            this.traceSrv.record(
                TraceType.CONNECT_SUCCESS,
                TraceHelper.getSessionTraceData(webSession),
                baseTraceData,
                null);
            return session;
          } catch (RuntimeException re) {
            if (webSession != null) {
              webSession.invalidate();
            }
            this.traceSrv.record(
                TraceType.CONNECT_FAILURE, 
                TraceHelper.getSessionTraceData(webSession),
                baseTraceData,
                null);
            throw re;
          }
        };

    return Mono.fromCallable(sessionSupplier);
  }

  @DeleteMapping("/disconnect")
  public Mono<Void> disconnect(
      WebSession webSession,
      @RequestAttribute(name = TraceHelper.BASE_TRACE_DATA_ATTR) BaseTraceData baseTraceData) {
    
    if(webSession==null || !webSession.isStarted()) {
      return Mono.error(new UnauthorizedException("Session ID not found."));
    } else {
      this.traceSrv.record(
          TraceType.DISCONNECT,
          TraceHelper.getSessionTraceData(webSession),
          baseTraceData,
          null);
      Callable<Void> sessionDestroyer = () -> {
        Mono<Void> sessionEnd = webSession.invalidate();
        String clientId = webSession.getAttribute(SessionAttributes.CLIENT_ID);
        CIBASession cibaSession = webSession.getAttribute(SessionAttributes.CIBA_SESSION);
      
        this.cibaService.logout(cibaSession, clientId);
        
        BackendAuthentication backendAuth = webSession.getAttribute(SessionAttributes.BACKEND_AUTH_ATTR);
        this.backendAuthService.wipe(backendAuth);
        
        return sessionEnd.block();
      };
      return Mono.fromCallable(sessionDestroyer);
    }
  }
}
