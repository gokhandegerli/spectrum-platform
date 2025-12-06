package com.degerli.loadbalancer.websocket;

import com.degerli.loadbalancer.model.Server;
import com.degerli.loadbalancer.registry.ServiceRegistry;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * WebSocket Proxy Support
 * <p>
 * Client <-> Load Balancer <-> Backend WebSocket
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

  private final ServiceRegistry serviceRegistry;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // WebSocket proxy handler
    registry.addHandler(new WebSocketProxyHandler(serviceRegistry), "/ws/{serviceName}/**")
        .setAllowedOrigins("*");
  }

  /**
   * WebSocket Proxy Handler
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

      // Backend server seç
      Server server = serviceRegistry.selectServer(serviceName, clientIp);

      // Backend'e bağlan
      String backendWsUrl = server.getUrl().replace("http://", "ws://") + "/ws";

      WebSocketSession backendSession = wsClient.execute(new AbstractWebSocketHandler() {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
          // Backend'den gelen mesajı client'a ilet
          if (clientSession.isOpen()) {
            clientSession.sendMessage(message);
          }
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
            throws Exception {
          // Binary mesajı client'a ilet
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
      }, String.valueOf(URI.create(backendWsUrl))).get();

      backendSessions.put(clientSession.getId(), backendSession);
      log.info("WebSocket proxying: {} -> {}", clientSession.getId(), backendWsUrl);
    }

    @Override
    protected void handleTextMessage(WebSocketSession clientSession, TextMessage message)
        throws Exception {
      // Client'dan gelen mesajı backend'e ilet
      WebSocketSession backendSession = backendSessions.get(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        backendSession.sendMessage(message);
      }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession clientSession, BinaryMessage message)
        throws Exception {
      // Binary mesajı backend'e ilet
      WebSocketSession backendSession = backendSessions.get(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        backendSession.sendMessage(message);
      }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, CloseStatus status) {
      log.info("Client WebSocket closed: {}", status);

      // Backend bağlantısını kapat
      WebSocketSession backendSession = backendSessions.remove(clientSession.getId());
      if (backendSession != null && backendSession.isOpen()) {
        try {
          backendSession.close(status);
        } catch (IOException e) {
          log.error("Error closing backend session", e);
        }
      }
    }

    private String extractServiceName(WebSocketSession session) {
      String path = session.getUri().getPath();
      // /ws/{serviceName}/... formatından serviceName'i çıkar
      String[] parts = path.split("/");
      return parts.length > 2 ? parts[2] : "unknown";
    }

    private String getClientIp(WebSocketSession session) {
      return session.getRemoteAddress() != null ? session.getRemoteAddress()
          .getAddress()
          .getHostAddress() : "unknown";
    }
  }
}