package com.degerli.loadbalancer.ratelimit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate Limiter - Token Bucket ve Sliding Window implementasyonları
 */
@Slf4j
public class RateLimiter {

  private final Map<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
  private final Map<String, SlidingWindow> clientWindows = new ConcurrentHashMap<>();

  private final int maxRequests;
  private final Duration window;
  private final Algorithm algorithm;

  public RateLimiter(int maxRequests, Duration window, Algorithm algorithm) {
    this.maxRequests = maxRequests;
    this.window = window;
    this.algorithm = algorithm;
  }

  /**
   * Request'e izin ver mi?
   */
  public boolean allowRequest(String clientId) {
    return switch (algorithm) {
      case TOKEN_BUCKET -> allowRequestTokenBucket(clientId);
      case SLIDING_WINDOW -> allowRequestSlidingWindow(clientId);
    };
  }

  /**
   * Token Bucket algoritması
   */
  private boolean allowRequestTokenBucket(String clientId) {
    TokenBucket bucket = clientBuckets.computeIfAbsent(clientId,
        k -> new TokenBucket(maxRequests, window));

    boolean allowed = bucket.tryConsume();

    if (!allowed) {
      log.warn("Rate limit exceeded for client: {} (algorithm: TOKEN_BUCKET)", clientId);
    }

    return allowed;
  }

  /**
   * Sliding Window algoritması
   */
  private boolean allowRequestSlidingWindow(String clientId) {
    SlidingWindow window = clientWindows.computeIfAbsent(clientId,
        k -> new SlidingWindow(maxRequests, this.window));

    boolean allowed = window.allowRequest();

    if (!allowed) {
      log.warn("Rate limit exceeded for client: {} (algorithm: SLIDING_WINDOW)", clientId);
    }

    return allowed;
  }

  /**
   * Client'ın rate limit bilgisi
   */
  public RateLimitInfo getRateLimitInfo(String clientId) {
    return switch (algorithm) {
      case TOKEN_BUCKET -> {
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) {
          yield new RateLimitInfo(maxRequests, maxRequests, 0);
        }
        yield new RateLimitInfo(maxRequests, bucket.getAvailableTokens(),
            bucket.getRefillTime());
      }
      case SLIDING_WINDOW -> {
        SlidingWindow window = clientWindows.get(clientId);
        if (window == null) {
          yield new RateLimitInfo(maxRequests, maxRequests, 0);
        }
        int remaining = maxRequests - window.getRequestCount();
        yield new RateLimitInfo(maxRequests, remaining, window.getResetTime());
      }
    };
  }

  /**
   * Token Bucket Implementation
   */
  private static class TokenBucket {
    private final int capacity;
    private final Duration refillInterval;
    private int availableTokens;
    private LocalDateTime lastRefillTime;

    TokenBucket(int capacity, Duration refillInterval) {
      this.capacity = capacity;
      this.refillInterval = refillInterval;
      this.availableTokens = capacity;
      this.lastRefillTime = LocalDateTime.now();
    }

    synchronized boolean tryConsume() {
      refill();

      if (availableTokens > 0) {
        availableTokens--;
        return true;
      }
      return false;
    }

    private void refill() {
      LocalDateTime now = LocalDateTime.now();
      Duration timeSinceRefill = Duration.between(lastRefillTime, now);

      if (timeSinceRefill.compareTo(refillInterval) >= 0) {
        // Tam refill
        availableTokens = capacity;
        lastRefillTime = now;
      }
    }

    int getAvailableTokens() {
      refill();
      return availableTokens;
    }

    long getRefillTime() {
      LocalDateTime nextRefill = lastRefillTime.plus(refillInterval);
      return Duration.between(LocalDateTime.now(), nextRefill).toSeconds();
    }
  }

  /**
   * Sliding Window Implementation
   */
  private static class SlidingWindow {
    private final int maxRequests;
    private final Duration window;
    private final Queue<LocalDateTime> requestTimes = new ConcurrentLinkedQueue<>();

    SlidingWindow(int maxRequests, Duration window) {
      this.maxRequests = maxRequests;
      this.window = window;
    }

    synchronized boolean allowRequest() {
      cleanupOldRequests();

      if (requestTimes.size() < maxRequests) {
        requestTimes.offer(LocalDateTime.now());
        return true;
      }
      return false;
    }

    private void cleanupOldRequests() {
      LocalDateTime cutoff = LocalDateTime.now().minus(window);
      requestTimes.removeIf(time -> time.isBefore(cutoff));
    }

    int getRequestCount() {
      cleanupOldRequests();
      return requestTimes.size();
    }

    long getResetTime() {
      if (requestTimes.isEmpty()) {
        return 0;
      }
      LocalDateTime oldest = requestTimes.peek();
      if (oldest == null) {
        return 0;
      }
      LocalDateTime resetTime = oldest.plus(window);
      return Duration.between(LocalDateTime.now(), resetTime).toSeconds();
    }
  }

  /**
   * Rate limit bilgisi
   */
  public record RateLimitInfo(int limit,           // Maksimum request sayısı
      int remaining,       // Kalan request hakkı
      long resetInSeconds  // Reset olacağı süre (saniye)
  ) {}

  public enum Algorithm {
    TOKEN_BUCKET,
    SLIDING_WINDOW
  }
}