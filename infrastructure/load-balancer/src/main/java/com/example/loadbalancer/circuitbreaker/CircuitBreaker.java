package com.example.loadbalancer.circuitbreaker;

import com.example.loadbalancer.model.Server;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Circuit Breaker Pattern Implementation
 * <p>
 * States:
 * - CLOSED: Normal operation, requests go through
 * - OPEN: Too many failures, requests are blocked
 * - HALF_OPEN: Testing if server recovered
 */
@Slf4j
public class CircuitBreaker {

  private final Map<Server, CircuitState> serverStates = new ConcurrentHashMap<>();

  // Configuration
  private final int failureThreshold;      // Kaç hata sonrası OPEN olsun
  private final int successThreshold;      // HALF_OPEN'da kaç başarı sonrası CLOSED olsun
  private final Duration timeout;          // OPEN'dan HALF_OPEN'a geçiş süresi
  private final Duration resetTimeout;     // Hata sayacını sıfırlama süresi

  public CircuitBreaker(int failureThreshold, int successThreshold, Duration timeout,
      Duration resetTimeout) {
    this.failureThreshold = failureThreshold;
    this.successThreshold = successThreshold;
    this.timeout = timeout;
    this.resetTimeout = resetTimeout;
  }

  /**
   * Server kullanılabilir mi kontrol et
   */
  public boolean isAvailable(Server server) {
    CircuitState state = getOrCreateState(server);

    switch (state.getState()) {
      case CLOSED:
        return true;

      case OPEN:
        // Timeout geçtiyse HALF_OPEN'a geç
        if (Duration.between(state.getOpenedAt(), LocalDateTime.now()).compareTo(timeout)
            > 0) {
          log.info("Circuit breaker transitioning to HALF_OPEN: {}", server.getUrl());
          state.transitionToHalfOpen();
          return true;
        }
        return false;

      case HALF_OPEN:
        return true;

      default:
        return false;
    }
  }

  /**
   * Başarılı request kaydı
   */
  public void recordSuccess(Server server) {
    CircuitState state = getOrCreateState(server);

    switch (state.getState()) {
      case HALF_OPEN:
        state.incrementSuccess();
        if (state.getConsecutiveSuccesses() >= successThreshold) {
          log.info("✓ Circuit breaker recovered, transitioning to CLOSED: {}",
              server.getUrl());
          state.transitionToClosed();
        }
        break;

      case CLOSED:
        // Başarı sayacını sıfırla
        if (Duration.between(state.getLastFailureTime(), LocalDateTime.now())
            .compareTo(resetTimeout) > 0) {
          state.resetFailures();
        }
        break;
    }
  }

  /**
   * Başarısız request kaydı
   */
  public void recordFailure(Server server) {
    CircuitState state = getOrCreateState(server);

    switch (state.getState()) {
      case CLOSED:
        state.incrementFailure();
        if (state.getConsecutiveFailures() >= failureThreshold) {
          log.error("✗ Circuit breaker OPENED due to failures: {} (failures: {})",
              server.getUrl(), state.getConsecutiveFailures());
          state.transitionToOpen();
        }
        break;

      case HALF_OPEN:
        log.warn("Circuit breaker failed in HALF_OPEN, returning to OPEN: {}",
            server.getUrl());
        state.transitionToOpen();
        break;
    }
  }

  /**
   * Server'ın circuit breaker durumunu al
   */
  public State getState(Server server) {
    return getOrCreateState(server).getState();
  }

  /**
   * Server'ın state'ini al veya oluştur
   */
  private CircuitState getOrCreateState(Server server) {
    return serverStates.computeIfAbsent(server, s -> new CircuitState());
  }

  /**
   * Circuit Breaker State
   */
  @Getter
  private static class CircuitState {
    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private int consecutiveSuccesses = 0;
    private LocalDateTime openedAt;
    private LocalDateTime lastFailureTime = LocalDateTime.now();

    void transitionToOpen() {
      this.state = State.OPEN;
      this.openedAt = LocalDateTime.now();
      this.consecutiveSuccesses = 0;
    }

    void transitionToHalfOpen() {
      this.state = State.HALF_OPEN;
      this.consecutiveSuccesses = 0;
      this.consecutiveFailures = 0;
    }

    void transitionToClosed() {
      this.state = State.CLOSED;
      this.consecutiveFailures = 0;
      this.consecutiveSuccesses = 0;
    }

    void incrementFailure() {
      this.consecutiveFailures++;
      this.consecutiveSuccesses = 0;
      this.lastFailureTime = LocalDateTime.now();
    }

    void incrementSuccess() {
      this.consecutiveSuccesses++;
      this.consecutiveFailures = 0;
    }

    void resetFailures() {
      this.consecutiveFailures = 0;
    }
  }

  public enum State {
    CLOSED,      // Normal işlem
    OPEN,        // Bloklanmış
    HALF_OPEN    // Test ediliyor
  }
}