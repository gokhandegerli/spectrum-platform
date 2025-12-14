package com.degerli.loadbalancer.controller;

import com.degerli.loadbalancer.LoadBalancerProperties;
import com.degerli.loadbalancer.circuitbreaker.CircuitBreaker;
import com.degerli.loadbalancer.metrics.LoadBalancerMetrics;
import com.degerli.loadbalancer.model.Server;
import com.degerli.loadbalancer.ratelimit.RateLimiter;
import com.degerli.loadbalancer.registry.ServiceRegistry;
import com.degerli.loadbalancer.session.StickySessionManager;
import com.degerli.loadbalancer.strategy.LoadBalancingStrategy;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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

  @RequestMapping(value = "/{serviceName}/**",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
          RequestMethod.PATCH, RequestMethod.OPTIONS})
  public ResponseEntity<?> proxyRequest(
      @PathVariable String serviceName,
      HttpServletRequest request,
      @RequestBody(required = false) String body) {

    long startTime = System.currentTimeMillis();
    String clientIp = getClientIp(request);

    // 1. Rate Limit Check
    if (isRateLimited(clientIp)) {
      return createRateLimitResponse();
    }

    // 2. Server Selection
    Server server = resolveTargetServer(serviceName, clientIp, request);

    // 3. Circuit Breaker Check
    if (isCircuitOpen(server)) {
      return createServiceUnavailableResponse(server);
    }

    try {
      // 4. Prepare Request
      LoadBalancingStrategy strategy = serviceRegistry.getStrategy(serviceName);
      strategy.onRequestStart(server);

      String backendUrl = buildBackendUrl(request, server, serviceName);
      HttpEntity<String> httpEntity = prepareRequestEntity(request, body, clientIp);

      log.info("Proxying: {} {} -> {} (client: {})", request.getMethod(), request.getRequestURI(), backendUrl, clientIp);

      // 5. Execute Request
      ResponseEntity<String> response = restTemplate.exchange(
          backendUrl,
          HttpMethod.valueOf(request.getMethod()),
          httpEntity,
          String.class);

      // 6. Handle Success
      long duration = System.currentTimeMillis() - startTime;
      handleSuccess(server, serviceName, duration, strategy);

      log.info("Response: {} in {}ms from {}", response.getStatusCode(), duration, server.getUrl());

      return createResponseWithSession(response, request);

    } catch (Exception e) {
      // 7. Handle Failure
      long duration = System.currentTimeMillis() - startTime;
      handleFailure(server, serviceName, duration, e);

      return createErrorResponse(e);
    }
  }

  // --- Helper Methods ---

  private boolean isRateLimited(String clientIp) {
    if (properties.getRateLimit().isEnabled()) {
      if (!rateLimiter.allowRequest(clientIp)) {
        log.warn("Rate limit exceeded for client: {}", clientIp);
        return true;
      }
    }
    return false;
  }

  private Server resolveTargetServer(String serviceName, String clientIp, HttpServletRequest request) {
    if (properties.getStickySession().isEnabled()) {
      String sessionId = getSessionId(request);
      if (sessionId != null) {
        Server tempServer = serviceRegistry.selectServer(serviceName, clientIp);
        return stickySessionManager.getOrAssignServer(sessionId, tempServer);
      }
    }
    return serviceRegistry.selectServer(serviceName, clientIp);
  }

  private boolean isCircuitOpen(Server server) {
    if (properties.getCircuitBreaker().isEnabled()) {
      if (!circuitBreaker.isAvailable(server)) {
        log.warn("Circuit breaker OPEN for server: {}", server.getUrl());
        return true;
      }
    }
    return false;
  }

  private String buildBackendUrl(HttpServletRequest request, Server server, String serviceName) {
    String fullPath = request.getRequestURI();
    String queryString = request.getQueryString();

    String pathSuffix = fullPath.substring(("/" + serviceName).length());
    String backendPath;

    // Actuator Path Stripping Logic
    if (pathSuffix.startsWith("/actuator")) {
      backendPath = pathSuffix;
      log.debug("Actuator request detected, stripping service name. Path: {}", backendPath);
    } else {
      backendPath = fullPath;
    }

    if (queryString != null) {
      backendPath += "?" + queryString;
    }

    return server.getUrl() + backendPath;
  }

  private HttpEntity<String> prepareRequestEntity(HttpServletRequest request, String body, String clientIp) {
    HttpHeaders headers = new HttpHeaders();
    Collections.list(request.getHeaderNames()).forEach(headerName -> {
      List<String> headerValues = Collections.list(request.getHeaders(headerName));
      headers.addAll(headerName, headerValues);
    });

    // Standard Proxy Headers
    headers.set("X-Forwarded-For", clientIp);
    headers.set("X-Forwarded-Proto", request.getScheme());
    headers.set("X-Real-IP", clientIp);

    // Port Handling
    String host = request.getServerName();
    int port = request.getServerPort();
    String hostWithPort = (port == 80 || port == 443) ? host : host + ":" + port;
    headers.set("X-Forwarded-Host", hostWithPort);

    return new HttpEntity<>(body, headers);
  }

  private void handleSuccess(Server server, String serviceName, long duration, LoadBalancingStrategy strategy) {
    server.updateResponseTime(duration);
    metrics.recordSuccess(serviceName, duration);
    strategy.onRequestComplete(server);

    if (properties.getCircuitBreaker().isEnabled()) {
      circuitBreaker.recordSuccess(server);
    }
  }

  private void handleFailure(Server server, String serviceName, long duration, Exception e) {
    metrics.recordError(serviceName, duration);
    log.error("Proxy error for {} in {}ms: {}", server.getUrl(), duration, e.getMessage());

    if (properties.getCircuitBreaker().isEnabled()) {
      // Re-select server to ensure we are recording failure for the correct instance logic
      // (Though passing 'server' directly is usually safe here)
      circuitBreaker.recordFailure(server);
    }
  }

  private ResponseEntity<?> createResponseWithSession(ResponseEntity<String> response, HttpServletRequest request) {
    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode())
        .headers(response.getHeaders());

    if (properties.getStickySession().isEnabled()) {
      String sessionId = getSessionId(request);
      if (sessionId == null) {
        sessionId = stickySessionManager.generateSessionId();
      }
      addStickySessionCookie(responseBuilder, sessionId);
    }

    return responseBuilder.body(response.getBody());
  }

  private void addStickySessionCookie(ResponseEntity.BodyBuilder responseBuilder, String sessionId) {
    long maxAge = properties.getStickySession().getSessionTimeoutMinutes() * 60;
    String cookieValue = String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d",
        properties.getStickySession().getCookieName(), sessionId, maxAge);
    responseBuilder.header("Set-Cookie", cookieValue);
  }

  private ResponseEntity<?> createErrorResponse(Exception e) {
    HttpStatus status = HttpStatus.BAD_GATEWAY;
    String message = "Load Balancer Error: " + e.getMessage();

    if (e instanceof HttpStatusCodeException) {
      status = (HttpStatus) ((HttpStatusCodeException) e).getStatusCode();
      message = ((HttpStatusCodeException) e).getResponseBodyAsString();
    }

    return ResponseEntity.status(status).body(message);
  }

  private ResponseEntity<?> createRateLimitResponse() {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("X-RateLimit-Limit", String.valueOf(properties.getRateLimit().getMaxRequests()))
        .header("X-RateLimit-Remaining", "0")
        .body("Rate limit exceeded. Please try again later.");
  }

  private ResponseEntity<?> createServiceUnavailableResponse(Server server) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Service temporarily unavailable: " + server.getUrl());
  }

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

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}