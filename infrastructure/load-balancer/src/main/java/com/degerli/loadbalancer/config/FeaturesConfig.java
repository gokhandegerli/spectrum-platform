package com.degerli.loadbalancer.config;

import com.degerli.loadbalancer.circuitbreaker.CircuitBreaker;
import com.degerli.loadbalancer.ratelimit.RateLimiter;
import com.degerli.loadbalancer.session.StickySessionManager;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Load balancer özelliklerinin konfigürasyonu
 */
@Configuration
public class FeaturesConfig {

  /**
   * Circuit Breaker Bean
   */
  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.circuit-breaker",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public CircuitBreaker circuitBreaker(LoadBalancerProperties properties) {
    LoadBalancerProperties.CircuitBreakerConfig config = properties.getCircuitBreaker();

    return new CircuitBreaker(config.getFailureThreshold(), config.getSuccessThreshold(),
        Duration.ofSeconds(config.getTimeoutSeconds()),
        Duration.ofSeconds(config.getResetTimeoutSeconds()));
  }

  /**
   * Rate Limiter Bean
   */
  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.rate-limit",
      name = "enabled",
      havingValue = "true")
  public RateLimiter rateLimiter(LoadBalancerProperties properties) {
    LoadBalancerProperties.RateLimitConfig config = properties.getRateLimit();

    RateLimiter.Algorithm algorithm = config.getAlgorithm().equals("SLIDING_WINDOW")
        ? RateLimiter.Algorithm.SLIDING_WINDOW : RateLimiter.Algorithm.TOKEN_BUCKET;

    return new RateLimiter(config.getMaxRequests(),
        Duration.ofSeconds(config.getWindowSeconds()), algorithm);
  }

  /**
   * Sticky Session Manager Bean
   */
  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.sticky-session",
      name = "enabled",
      havingValue = "true")
  public StickySessionManager stickySessionManager(LoadBalancerProperties properties) {
    LoadBalancerProperties.StickySessionConfig config = properties.getStickySession();

    return new StickySessionManager(Duration.ofMinutes(config.getSessionTimeoutMinutes()));
  }

  /**
   * Placeholder beans (features disabled olduğunda)
   */
  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.circuit-breaker",
      name = "enabled",
      havingValue = "false")
  public CircuitBreaker noOpCircuitBreaker() {
    return new CircuitBreaker(Integer.MAX_VALUE, 1, Duration.ofDays(1), Duration.ofDays(1));
  }

  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.rate-limit",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true)
  public RateLimiter noOpRateLimiter() {
    return new RateLimiter(Integer.MAX_VALUE, Duration.ofHours(1),
        RateLimiter.Algorithm.TOKEN_BUCKET);
  }

  @Bean
  @ConditionalOnProperty(prefix = "loadbalancer.sticky-session",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true)
  public StickySessionManager noOpStickySessionManager() {
    return new StickySessionManager(Duration.ofMinutes(30));
  }
}