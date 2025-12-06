package com.example.loadbalancer.controller;

import com.example.loadbalancer.LoadBalancerProperties;
import com.example.loadbalancer.circuitbreaker.CircuitBreaker;
import com.example.loadbalancer.metrics.LoadBalancerMetrics;
import com.example.loadbalancer.model.Server;
import com.example.loadbalancer.ratelimit.RateLimiter;
import com.example.loadbalancer.registry.ServiceRegistry;
import com.example.loadbalancer.session.StickySessionManager;
import com.example.loadbalancer.strategy.LoadBalancingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Reverse proxy controller - tüm istekleri backend'lere yönlendirir
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ProxyController {

  private final ServiceRegistry serviceRegistry;
  private final RestTemplate restTemplate = new RestTemplate();
  private final LoadBalancerProperties properties;
  private final CircuitBreaker circuitBreaker;
  private final RateLimiter rateLimiter;
  private final StickySessionManager stickySessionManager;
  private final LoadBalancerMetrics metrics;

  /**
   * Tüm istekleri yakala ve ilgili backend'e yönlendir
   * Pattern: /{serviceName}/**
   */
  @RequestMapping(value = "/{serviceName}/**",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
          RequestMethod.PATCH, RequestMethod.OPTIONS})
  public ResponseEntity<?> proxyRequest(
      @PathVariable
      String serviceName, HttpServletRequest request,
      @RequestBody(required = false)
      String body) {

    long startTime = System.currentTimeMillis();
    String clientIp = getClientIp(request);

    // Rate Limiting kontrolü
    if (properties.getRateLimit().isEnabled()) {
      if (!rateLimiter.allowRequest(clientIp)) {
        log.warn("Rate limit exceeded for client: {}", clientIp);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("X-RateLimit-Limit",
                String.valueOf(properties.getRateLimit().getMaxRequests()))
            .header("X-RateLimit-Remaining", "0")
            .body("Rate limit exceeded. Please try again later.");
      }
    }

    try {
      // Session ID al (sticky session için)
      String sessionId = null;
      if (properties.getStickySession().isEnabled()) {
        sessionId = getSessionId(request);
      }

      // Backend server seç
      Server server;
      if (properties.getStickySession().isEnabled() && sessionId != null) {
        Server tempServer = serviceRegistry.selectServer(serviceName, clientIp);
        server = stickySessionManager.getOrAssignServer(sessionId, tempServer);
      } else {
        server = serviceRegistry.selectServer(serviceName, clientIp);
      }

      // Circuit breaker kontrolü
      if (properties.getCircuitBreaker().isEnabled()) {
        if (!circuitBreaker.isAvailable(server)) {
          log.warn("Circuit breaker OPEN for server: {}", server.getUrl());
          return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
              .body("Service temporarily unavailable");
        }
      }

      LoadBalancingStrategy strategy = serviceRegistry.getStrategy(serviceName);

      // Request'i başlat (connection tracking için)
      strategy.onRequestStart(server);

      // Original path'i al (serviceName'den sonraki kısım)
      String originalPath = request.getRequestURI().substring(serviceName.length() + 1);
      String queryString = request.getQueryString();
      String fullPath = originalPath + (queryString != null ? "?" + queryString : "");

      // Backend URL oluştur
      String backendUrl = server.getUrl() + "/" + fullPath;

      log.info("Proxying: {} {} -> {} (client: {})", request.getMethod(),
          request.getRequestURI(), backendUrl, clientIp);

      // Headers'ları kopyala
      HttpHeaders headers = new HttpHeaders();
      Collections.list(request.getHeaderNames()).forEach(headerName -> {
        List<String> headerValues = Collections.list(request.getHeaders(headerName));
        headers.addAll(headerName, headerValues);
      });

      // X-Forwarded-* headers ekle
      headers.set("X-Forwarded-For", clientIp);
      headers.set("X-Forwarded-Proto", request.getScheme());
      headers.set("X-Forwarded-Host", request.getServerName());
      headers.set("X-Real-IP", clientIp);

      // Request oluştur
      HttpEntity<String> entity = new HttpEntity<>(body, headers);

      // Backend'e istek gönder
      ResponseEntity<String> response = restTemplate.exchange(backendUrl,
          HttpMethod.valueOf(request.getMethod()), entity, String.class);

      long responseTime = System.currentTimeMillis() - startTime;
      server.updateResponseTime(responseTime);

      // Circuit breaker - başarılı
      if (properties.getCircuitBreaker().isEnabled()) {
        circuitBreaker.recordSuccess(server);
      }

      // Metrics kaydet
      metrics.recordSuccess(serviceName, responseTime);

      log.info("Response: {} {} in {}ms (backend: {})", response.getStatusCode(),
          request.getRequestURI(), responseTime, server.getUrl());

      // Request'i bitir
      strategy.onRequestComplete(server);

      // Sticky session cookie ekle
      ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(
          response.getStatusCode()).headers(response.getHeaders());

      if (properties.getStickySession().isEnabled()) {
        sessionId = getSessionId(request);
        if (sessionId == null) {
          sessionId = stickySessionManager.generateSessionId();
        }
        responseBuilder.header("Set-Cookie",
            properties.getStickySession().getCookieName() + "=" + sessionId
                + "; Path=/; HttpOnly; Max-Age=" + (
                properties.getStickySession().getSessionTimeoutMinutes() * 60));
      }

      return responseBuilder.body(response.getBody());

    } catch (Exception e) {
      long responseTime = System.currentTimeMillis() - startTime;

      // Circuit breaker - hata
      if (properties.getCircuitBreaker().isEnabled()) {
        Server server = serviceRegistry.selectServer(serviceName, clientIp);
        circuitBreaker.recordFailure(server);
      }

      // Metrics kaydet
      metrics.recordError(serviceName, responseTime);

      log.error("Proxy error for {} in {}ms: {}", request.getRequestURI(), responseTime,
          e.getMessage());

      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body("Load Balancer Error: " + e.getMessage());
    }
  }

  /**
   * Session ID al (cookie'den)
   */
  private String getSessionId(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
        if (properties.getStickySession().getCookieName().equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Client IP adresini al (X-Forwarded-For veya RemoteAddr)
   */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}