package com.example.loadbalancer.controller;

import com.example.loadbalancer.LoadBalancerProperties;
import com.example.loadbalancer.LoadBalancerProperties.Algorithm;
import com.example.loadbalancer.circuitbreaker.CircuitBreaker;
import com.example.loadbalancer.model.Server;
import com.example.loadbalancer.ratelimit.RateLimiter;
import com.example.loadbalancer.registry.ServiceRegistry;
import com.example.loadbalancer.session.StickySessionManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Load balancer yönetimi için admin API
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final ServiceRegistry serviceRegistry;
  private final CircuitBreaker circuitBreaker;
  private final RateLimiter rateLimiter;
  private final StickySessionManager stickySessionManager;
  private final LoadBalancerProperties properties;

  /**
   * Tüm servislerin durumunu göster
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    Map<String, Object> status = new HashMap<>();

    serviceRegistry.getServiceNames().forEach(serviceName -> {
      List<Server> servers = serviceRegistry.getServers(serviceName);

      Map<String, Object> serviceInfo = new HashMap<>();
      serviceInfo.put("algorithm",
          serviceRegistry.getStrategy(serviceName).getClass().getSimpleName());
      serviceInfo.put("servers",
          servers.stream().map(this::serverToMap).collect(Collectors.toList()));
      serviceInfo.put("healthyCount", servers.stream().filter(Server::isHealthy).count());
      serviceInfo.put("totalServers", servers.size());

      status.put(serviceName, serviceInfo);
    });

    return ResponseEntity.ok(status);
  }

  /**
   * Belirli bir servisin detaylı bilgisi
   */
  @GetMapping("/services/{serviceName}")
  public ResponseEntity<Map<String, Object>> getServiceDetails(
      @PathVariable
      String serviceName) {
    List<Server> servers = serviceRegistry.getServers(serviceName);
    if (servers.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Map<String, Object> details = new HashMap<>();
    details.put("serviceName", serviceName);
    details.put("algorithm",
        serviceRegistry.getStrategy(serviceName).getClass().getSimpleName());
    details.put("servers",
        servers.stream().map(this::serverToMap).collect(Collectors.toList()));

    // Aggregate stats
    long totalRequests = servers.stream().mapToLong(s -> s.getTotalRequests().get()).sum();
    long totalFailures = servers.stream().mapToLong(s -> s.getFailedRequests().get()).sum();
    int activeConnections = servers.stream()
        .mapToInt(s -> s.getActiveConnections().get())
        .sum();

    details.put("totalRequests", totalRequests);
    details.put("totalFailures", totalFailures);
    details.put("activeConnections", activeConnections);
    details.put("successRate",
        totalRequests > 0 ? (1.0 - (double) totalFailures / totalRequests) * 100 : 100.0);

    return ResponseEntity.ok(details);
  }

  /**
   * Algoritma değiştir
   */
  @PostMapping("/services/{serviceName}/algorithm")
  public ResponseEntity<Map<String, String>> changeAlgorithm(
      @PathVariable
      String serviceName,
      @RequestBody
      AlgorithmChangeRequest request) {

    try {
      serviceRegistry.changeAlgorithm(serviceName, request.getAlgorithm());
      return ResponseEntity.ok(
          Map.of("message", "Algorithm changed successfully", "service", serviceName,
              "newAlgorithm", request.getAlgorithm().toString()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Tüm servislerin listesi
   */
  @GetMapping("/services")
  public ResponseEntity<List<String>> listServices() {
    return ResponseEntity.ok(serviceRegistry.getServiceNames());
  }

  /**
   * Server bilgisini Map'e çevir
   */
  private Map<String, Object> serverToMap(Server server) {
    Map<String, Object> map = new HashMap<>();
    map.put("url", server.getUrl());
    map.put("healthy", server.isHealthy());
    map.put("weight", server.getWeight());
    map.put("activeConnections", server.getActiveConnections().get());
    map.put("totalRequests", server.getTotalRequests().get());
    map.put("failedRequests", server.getFailedRequests().get());
    map.put("averageResponseTime", server.getAverageResponseTime());
    map.put("lastHealthCheck", server.getLastHealthCheck());
    return map;
  }

  @Data
  public static class AlgorithmChangeRequest {
    private Algorithm algorithm;
  }

  /**
   * Circuit breaker durumları
   */
  @GetMapping("/circuit-breaker/status")
  public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
    Map<String, Object> status = new HashMap<>();

    serviceRegistry.getServiceNames().forEach(serviceName -> {
      List<Server> servers = serviceRegistry.getServers(serviceName);
      Map<String, String> serverStates = new HashMap<>();

      servers.forEach(server -> {
        CircuitBreaker.State state = circuitBreaker.getState(server);
        serverStates.put(server.getUrl(), state.toString());
      });

      status.put(serviceName, serverStates);
    });

    return ResponseEntity.ok(status);
  }

  /**
   * Rate limit bilgileri
   */
  @GetMapping("/rate-limit/client/{clientId}")
  public ResponseEntity<RateLimiter.RateLimitInfo> getRateLimitInfo(
      @PathVariable
      String clientId) {
    if (!properties.getRateLimit().isEnabled()) {
      return ResponseEntity.ok(new RateLimiter.RateLimitInfo(0, 0, 0));
    }

    return ResponseEntity.ok(rateLimiter.getRateLimitInfo(clientId));
  }

  /**
   * Sticky session istatistikleri
   */
  @GetMapping("/sticky-sessions/stats")
  public ResponseEntity<StickySessionManager.SessionStats> getSessionStats() {
    if (!properties.getStickySession().isEnabled()) {
      return ResponseEntity.ok(new StickySessionManager.SessionStats(0, Map.of()));
    }

    return ResponseEntity.ok(stickySessionManager.getStats());
  }

  /**
   * Özellik durumları
   */
  @GetMapping("/features")
  public ResponseEntity<Map<String, Object>> getFeatures() {
    Map<String, Object> features = new HashMap<>();
    features.put("circuitBreaker",
        Map.of("enabled", properties.getCircuitBreaker().isEnabled(), "failureThreshold",
            properties.getCircuitBreaker().getFailureThreshold(), "timeoutSeconds",
            properties.getCircuitBreaker().getTimeoutSeconds()));
    features.put("rateLimit",
        Map.of("enabled", properties.getRateLimit().isEnabled(), "maxRequests",
            properties.getRateLimit().getMaxRequests(), "windowSeconds",
            properties.getRateLimit().getWindowSeconds(), "algorithm",
            properties.getRateLimit().getAlgorithm()));
    features.put("stickySession",
        Map.of("enabled", properties.getStickySession().isEnabled(), "sessionTimeoutMinutes",
            properties.getStickySession().getSessionTimeoutMinutes()));
    features.put("ssl", Map.of("enabled", properties.getSsl().isEnabled(), "httpsPort",
        properties.getSsl().getHttpsPort()));

    return ResponseEntity.ok(features);
  }
}