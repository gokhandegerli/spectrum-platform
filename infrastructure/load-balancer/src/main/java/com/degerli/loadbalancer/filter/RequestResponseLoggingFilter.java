package com.degerli.loadbalancer.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Detaylı request/response logging
 */
@Slf4j
@Component
public class RequestResponseLoggingFilter implements Filter {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
      new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final Set<String> excludePaths = Set.of("/actuator", "/admin/status");

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse)) {
      chain.doFilter(request, response);
      return;
    }

    // Admin endpoint'leri loglama
    if (shouldExclude(httpRequest.getRequestURI())) {
      chain.doFilter(request, response);
      return;
    }

    // Wrappable request/response
    ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(
        httpRequest);
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(
        httpResponse);

    long startTime = System.currentTimeMillis();
    String requestId = UUID.randomUUID().toString();

    // Request log
    logRequest(requestWrapper, requestId);

    try {
      chain.doFilter(requestWrapper, responseWrapper);
    } finally {
      long duration = System.currentTimeMillis() - startTime;

      // Response log
      logResponse(responseWrapper, requestId, duration);

      // Response'u client'a gönder
      responseWrapper.copyBodyToResponse();
    }
  }

  private void logRequest(ContentCachingRequestWrapper request, String requestId) {
    try {
      Map<String, Object> logData = new LinkedHashMap<>();
      logData.put("type", "REQUEST");
      logData.put("requestId", requestId);
      logData.put("timestamp", Instant.now());
      logData.put("method", request.getMethod());
      logData.put("uri", request.getRequestURI());
      logData.put("queryString", request.getQueryString());
      logData.put("clientIp", getClientIp(request));
      logData.put("userAgent", request.getHeader("User-Agent"));

      // Headers
      Map<String, String> headers = new HashMap<>();
      Collections.list(request.getHeaderNames()).forEach(headerName -> {
        headers.put(headerName, request.getHeader(headerName));
      });
      logData.put("headers", headers);

      // Body (sadece JSON/text için)
      String contentType = request.getContentType();
      if (contentType != null && (contentType.contains("json") || contentType.contains(
          "text"))) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0 && content.length < 10000) { // Max 10KB
          logData.put("body", new String(content));
        }
      }

      log.info("→ {}", objectMapper.writeValueAsString(logData));
    } catch (Exception e) {
      log.error("Error logging request", e);
    }
  }

  private void logResponse(ContentCachingResponseWrapper response, String requestId,
      long duration) {
    try {
      Map<String, Object> logData = new LinkedHashMap<>();
      logData.put("type", "RESPONSE");
      logData.put("requestId", requestId);
      logData.put("timestamp", Instant.now());
      logData.put("status", response.getStatus());
      logData.put("durationMs", duration);

      // Headers
      Map<String, String> headers = new HashMap<>();
      response.getHeaderNames().forEach(headerName -> {
        headers.put(headerName, response.getHeader(headerName));
      });
      logData.put("headers", headers);

      // Body (sadece JSON/text için)
      String contentType = response.getContentType();
      if (contentType != null && (contentType.contains("json") || contentType.contains(
          "text"))) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0 && content.length < 10000) { // Max 10KB
          logData.put("body", new String(content));
        }
      }

      String logLevel = response.getStatus() >= 400 ? "ERROR" : "INFO";
      if (logLevel.equals("ERROR")) {
        log.error("← {}", objectMapper.writeValueAsString(logData));
      } else {
        log.info("← {}", objectMapper.writeValueAsString(logData));
      }
    } catch (Exception e) {
      log.error("Error logging response", e);
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private boolean shouldExclude(String path) {
    return excludePaths.stream().anyMatch(path::startsWith);
  }
}