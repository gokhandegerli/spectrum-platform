package com.degerli.loadbalancer.session;

import com.degerli.loadbalancer.model.Server;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Sticky Session Manager
 * <p>
 * Aynı session'a sahip requestler her zaman aynı backend'e gider
 */
@Slf4j
public class StickySessionManager {

  // Session ID -> Server mapping
  private final Map<String, SessionBinding> sessionBindings = new ConcurrentHashMap<>();

  // Session timeout (varsayılan 30 dakika)
  private final Duration sessionTimeout;

  public StickySessionManager(Duration sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
    startCleanupTask();
  }

  /**
   * Session için server al veya ata
   */
  public Server getOrAssignServer(String sessionId, Server defaultServer) {
    if (sessionId == null || sessionId.isEmpty()) {
      // Session ID yoksa yeni oluştur
      sessionId = generateSessionId();
    }

    // Cleanup expired sessions
    cleanupExpiredSessions();

    String finalSessionId = sessionId;
    SessionBinding binding = sessionBindings.compute(sessionId, (key, existing) -> {
      if (existing == null || existing.isExpired(sessionTimeout)) {
        // Yeni binding oluştur
        log.info("Creating new session binding: {} -> {}", finalSessionId,
            defaultServer.getUrl());
        return new SessionBinding(defaultServer);
      } else {
        // Mevcut binding'i yenile
        existing.touch();
        return existing;
      }
    });

    return binding.getServer();
  }

  /**
   * Session'ı kaldır
   */
  public void removeSession(String sessionId) {
    SessionBinding removed = sessionBindings.remove(sessionId);
    if (removed != null) {
      log.info("Session removed: {} (was bound to: {})", sessionId,
          removed.getServer().getUrl());
    }
  }

  /**
   * Session ID oluştur
   */
  public String generateSessionId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Expired session'ları temizle
   */
  private void cleanupExpiredSessions() {
    sessionBindings.entrySet().removeIf(entry -> {
      boolean expired = entry.getValue().isExpired(sessionTimeout);
      if (expired) {
        log.debug("Removing expired session: {} (bound to: {})", entry.getKey(),
            entry.getValue().getServer().getUrl());
      }
      return expired;
    });
  }

  /**
   * Periyodik cleanup görevi başlat
   */
  private void startCleanupTask() {
    Thread cleanupThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(60000); // Her dakika
          cleanupExpiredSessions();
          log.debug("Session cleanup completed. Active sessions: {}", sessionBindings.size());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
    cleanupThread.setDaemon(true);
    cleanupThread.setName("session-cleanup");
    cleanupThread.start();
  }

  /**
   * İstatistikler
   */
  public SessionStats getStats() {
    int totalSessions = sessionBindings.size();

    // Server'lara göre session dağılımı
    Map<String, Integer> sessionsByServer = new ConcurrentHashMap<>();
    sessionBindings.values().forEach(binding -> {
      String serverUrl = binding.getServer().getUrl();
      sessionsByServer.merge(serverUrl, 1, Integer::sum);
    });

    return new SessionStats(totalSessions, sessionsByServer);
  }

  /**
   * Session binding (session -> server ilişkisi)
   */
  private static class SessionBinding {
    private final Server server;
    private volatile LocalDateTime lastAccessTime;

    SessionBinding(Server server) {
      this.server = server;
      this.lastAccessTime = LocalDateTime.now();
    }

    Server getServer() {
      return server;
    }

    void touch() {
      this.lastAccessTime = LocalDateTime.now();
    }

    boolean isExpired(Duration timeout) {
      return Duration.between(lastAccessTime, LocalDateTime.now()).compareTo(timeout) > 0;
    }
  }

  /**
   * Session istatistikleri
   */
  public record SessionStats(int totalSessions, Map<String, Integer> sessionsByServer) {}
}