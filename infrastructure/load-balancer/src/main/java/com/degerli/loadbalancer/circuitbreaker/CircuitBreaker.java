package com.degerli.loadbalancer.circuitbreaker;

import com.degerli.loadbalancer.model.Server;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-Safe Circuit Breaker Pattern Implementation
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
  private final int failureThreshold;
  private final int successThreshold;
  private final Duration timeout;
  private final Duration resetTimeout;

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
        if (Duration.between(state.getOpenedAt(), LocalDateTime.now()).compareTo(timeout) > 0) {
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
          log.info("✓ Circuit breaker recovered, transitioning to CLOSED: {}", server.getUrl());
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
        log.warn("Circuit breaker failed in HALF_OPEN, returning to OPEN: {}", server.getUrl());
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
   * Thread-Safe Circuit Breaker State
   */
  @Getter
  private static class CircuitState {
    private volatile State state = State.CLOSED;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
    private volatile LocalDateTime openedAt;
    private volatile LocalDateTime lastFailureTime = LocalDateTime.now();

    synchronized void transitionToOpen() {
      this.state = State.OPEN;
      this.openedAt = LocalDateTime.now();
      this.consecutiveSuccesses.set(0);
    }

    synchronized void transitionToHalfOpen() {
      this.state = State.HALF_OPEN;
      this.consecutiveSuccesses.set(0);
      this.consecutiveFailures.set(0);
    }

    synchronized void transitionToClosed() {
      this.state = State.CLOSED;
      this.consecutiveFailures.set(0);
      this.consecutiveSuccesses.set(0);
    }

    void incrementFailure() {
      this.consecutiveFailures.incrementAndGet();
      this.consecutiveSuccesses.set(0);
      this.lastFailureTime = LocalDateTime.now();
    }

    void incrementSuccess() {
      this.consecutiveSuccesses.incrementAndGet();
      this.consecutiveFailures.set(0);
    }

    void resetFailures() {
      this.consecutiveFailures.set(0);
    }

    int getConsecutiveFailures() {
      return consecutiveFailures.get();
    }

    int getConsecutiveSuccesses() {
      return consecutiveSuccesses.get();
    }
  }

  public enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }
}