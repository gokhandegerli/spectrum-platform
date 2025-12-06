package com.example.loadbalancer.health;

import com.example.loadbalancer.LoadBalancerProperties;
import com.example.loadbalancer.model.Server;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Backend servislerin sağlık kontrolünü yapar
 */
@Slf4j
@Component
public class HealthChecker {

  private final RestTemplate restTemplate;
  private final LoadBalancerProperties properties;
  private final ScheduledExecutorService scheduler;

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
    checkHealth(server);

    // Periyodik check başlat
    scheduler.scheduleAtFixedRate(() -> checkHealth(server),
        properties.getHealthCheckInterval(), properties.getHealthCheckInterval(),
        TimeUnit.MILLISECONDS);

    log.info("Health check started for: {} (interval: {}ms)", server.getUrl(),
        properties.getHealthCheckInterval());
  }

  /**
   * Bir server için health check yap
   */
  private void checkHealth(Server server) {
    try {
      long startTime = System.currentTimeMillis();

      // /actuator/health endpoint'ini kontrol et
      String healthUrl = server.getUrl() + "/actuator/health";
      String response = restTemplate.getForObject(healthUrl, String.class);

      long responseTime = System.currentTimeMillis() - startTime;

      boolean wasHealthy = server.isHealthy();
      server.setHealthy(true);
      server.setLastHealthCheck(LocalDateTime.now());
      server.updateResponseTime(responseTime);

      if (!wasHealthy) {
        log.info("✓ Server recovered: {} (response time: {}ms)", server.getUrl(),
            responseTime);
      } else {
        log.debug("✓ Health check OK: {} (response time: {}ms)", server.getUrl(),
            responseTime);
      }

    } catch (Exception e) {
      boolean wasHealthy = server.isHealthy();
      server.setHealthy(false);
      server.setLastHealthCheck(LocalDateTime.now());
      server.recordFailure();

      if (wasHealthy) {
        log.error("✗ Server became unhealthy: {} - {}", server.getUrl(), e.getMessage());
      } else {
        log.debug("✗ Health check failed: {} - {}", server.getUrl(), e.getMessage());
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