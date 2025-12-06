package com.degerli.loadbalancer.health;

import com.degerli.loadbalancer.LoadBalancerProperties;
import com.degerli.loadbalancer.model.Server;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Backend servislerin sağlık kontrolünü yapar
 * Retry mechanism ile geliştirilmiş versiyon
 */
@Slf4j
@Component
public class HealthChecker {

  private final RestTemplate restTemplate;
  private final LoadBalancerProperties properties;
  private final ScheduledExecutorService scheduler;

  // Retry configuration
  private static final int MAX_RETRIES = 3;
  private static final long RETRY_DELAY_MS = 100;

  public HealthChecker(LoadBalancerProperties properties) {
    this.properties = properties;
    this.restTemplate = new RestTemplate();
    this.restTemplate.setRequestFactory(
        new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
          setConnectTimeout((int) properties.getHealthCheckTimeout());
          setReadTimeout((int) properties.getHealthCheckTimeout());
        }});
    this.scheduler = Executors.newScheduledThreadPool(4);
  }

  /**
   * Bir server için health check başlat
   */
  public void startHealthCheck(Server server) {
    if (!properties.isHealthCheckEnabled()) {
      log.info("Health check disabled, marking server as healthy: {}", server.getUrl());
      server.setHealthy(true);
      return;
    }

    // İlk check hemen yap
    checkHealthWithRetry(server);

    // Periyodik check başlat
    scheduler.scheduleAtFixedRate(() -> checkHealthWithRetry(server),
        properties.getHealthCheckInterval(), properties.getHealthCheckInterval(),
        TimeUnit.MILLISECONDS);

    log.info("Health check started for: {} (interval: {}ms)", server.getUrl(),
        properties.getHealthCheckInterval());
  }

  /**
   * Retry mechanism ile health check
   */
  private void checkHealthWithRetry(Server server) {
    int attempt = 0;
    boolean success = false;
    Exception lastException = null;

    while (attempt < MAX_RETRIES && !success) {
      attempt++;
      try {
        long startTime = System.currentTimeMillis();

        String healthUrl = server.getUrl() + "/actuator/health";
        String response = restTemplate.getForObject(healthUrl, String.class);

        long responseTime = System.currentTimeMillis() - startTime;

        boolean wasHealthy = server.isHealthy();
        server.setHealthy(true);
        server.setLastHealthCheck(LocalDateTime.now());
        server.updateResponseTime(responseTime);

        if (!wasHealthy) {
          log.info("✓ Server recovered: {} (response time: {}ms, attempt: {}/{})",
              server.getUrl(), responseTime, attempt, MAX_RETRIES);
        } else {
          log.debug("✓ Health check OK: {} (response time: {}ms)", server.getUrl(),
              responseTime);
        }

        success = true;

      } catch (Exception e) {
        lastException = e;

        if (attempt < MAX_RETRIES) {
          try {
            long delay = RETRY_DELAY_MS * attempt;
            log.debug("Health check failed for {}, retrying in {}ms (attempt {}/{})",
                server.getUrl(), delay, attempt, MAX_RETRIES);
            Thread.sleep(delay);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    if (!success) {
      boolean wasHealthy = server.isHealthy();
      server.setHealthy(false);
      server.setLastHealthCheck(LocalDateTime.now());
      server.recordFailure();

      if (wasHealthy) {
        log.error("✗ Server became unhealthy after {} attempts: {} - {}",
            MAX_RETRIES, server.getUrl(),
            lastException != null ? lastException.getMessage() : "Unknown error");
      } else {
        log.debug("✗ Health check failed after {} attempts: {} - {}", MAX_RETRIES,
            server.getUrl(),
            lastException != null ? lastException.getMessage() : "Unknown error");
      }
    }
  }

  /**
   * Shutdown
   */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}