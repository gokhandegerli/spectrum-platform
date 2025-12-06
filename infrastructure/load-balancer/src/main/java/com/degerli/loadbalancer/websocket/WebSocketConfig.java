package com.degerli.loadbalancer.websocket;

import com.degerli.loadbalancer.model.Server;
import com.degerli.loadbalancer.registry.ServiceRegistry;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * WebSocket Proxy Support with Timeout
 * <p>
 * Client <-> Load Balancer <-> Backend WebSocket
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

  private final ServiceRegistry serviceRegistry;
  private static final long CONNECTION_TIMEOUT_SECONDS = 5;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(new WebSocketProxyHandler(serviceRegistry), "/ws/{serviceName}/**")
        .setAllowedOrigins("*");
  }

  /**
   * WebSocket Proxy Handler with timeout support
   */
  @Slf4j
  @RequiredArgsConstructor
  static class WebSocketProxyHandler extends AbstractWebSocketHandler {

    private final ServiceRegistry serviceRegistry;
    private final StandardWebSocketClient wsClient = new StandardWebSocketClient();

    // Client session -> Backend session mapping
    private final Map<String, WebSocketSession> backendSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
      String serviceName = extractServiceName(clientSession);
      String clientIp = getClientIp(clientSession);

      log.info("WebSocket connection established: {} (client: {})", serviceName, clientIp);

      try {
        // Backend server seç
        Server server = serviceRegistry.selectServer(serviceName, clientIp);

        // Backend'e bağlan
        String backendWsUrl = server.getUrl().replace("http://", "ws://") + "/ws";

        // Timeout ile backend connection
        WebSocketSession backendSession = wsClient.execute(new AbstractWebSocketHandler() {
              @Override
              protected void handleTextMessage(WebSocketSession session, TextMessage message)
                  throws Exception {
                if (clientSession.isOpen()) {
                  clientSession.sendMessage(message);
                }
              }

              @Override
              protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
                  throws Exception {
                if (clientSession.isOpen()) {
                  clientSession.sendMessage(message);
                }
              }

              @Override
              public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                log.info("Backend WebSocket closed: {}", status);
                try {
                  if (clientSession.isOpen()) {
                    clientSession.close(status);
                  }
                } catch (IOException e) {
                  log.error("Error closing client session", e);
                }
              }

              @Override
              public void handleTransportError(WebSocketSession session, Throwable exception) {
                log.error("Backend WebSocket transport error", exception);
                try {
                  if (clientSession.isOpen()) {
                    clientSession.close(CloseStatus.SERVER_ERROR);
                  }
                } catch (IOException e) {
                  log.error("Error closing client session after transport error", e);
                }
              }
            }, String.valueOf(URI.create(backendWsUrl)))
            .get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        backendSessions.put(clientSession.getId(), backendSession);
        log.info("WebSocket proxying: {} -> {}", clientSession.getId(), backendWsUrl);

      } catch (TimeoutException e) {
        log.error("WebSocket connection timeout for service: {}", serviceName, e);
        if (clientSession.isOpen()) {
          clientSession.close(new CloseStatus(1001, "Backend connection timeout"));
        }
      } catch (ExecutionException e) {
        log.error("WebSocket connection failed for service: {}", serviceName, e);
        if (clientSession.isOpen()) {
          clientSession.close(new CloseStatus(1011, "Backend connection failed"));
        }
      } catch (Exception e) {
        log.error("Unexpected error establishing WebSocket connection", e);
        if (clientSession.isOpen()) {
          clientSession.close(CloseStatus.SERVER_ERROR);
        }
      }
    }

    @Override
    protected void handleTextMessage(WebSocketSession clientSession, TextMessage message)
        throws Exception {
      WebSocketSession backendSession = backendSessions.get(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        backendSession.sendMessage(message);
      } else {
        log.warn("Backend session not available for client: {}", clientSession.getId());
      }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession clientSession, BinaryMessage message)
        throws Exception {
      WebSocketSession backendSession = backendSessions.get(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        backendSession.sendMessage(message);
      } else {
        log.warn("Backend session not available for client: {}", clientSession.getId());
      }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, CloseStatus status) {
      log.info("Client WebSocket closed: {}", status);

      WebSocketSession backendSession = backendSessions.remove(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        try {
          backendSession.close(status);
        } catch (IOException e) {
          log.error("Error closing backend session", e);
        }
      }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
      log.error("Client WebSocket transport error", exception);
      WebSocketSession backendSession = backendSessions.remove(session.getId());
      if (backendSession != null && backendSession.isOpen()) {
        try {
          backendSession.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
          log.error("Error closing backend session after transport error", e);
        }
      }
    }

    private String extractServiceName(WebSocketSession session) {
      String path = session.getUri().getPath();
      String[] parts = path.split("/");
      return parts.length > 2 ? parts[2] : "unknown";
    }

    private String getClientIp(WebSocketSession session) {
      return session.getRemoteAddress() != null
          ? session.getRemoteAddress().getAddress().getHostAddress()
          : "unknown";
    }
  }
}