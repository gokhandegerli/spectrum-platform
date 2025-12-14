// ============================================
// UrlController.java - X-Forwarded-Host Fix
// ============================================

// File: services/kisakes/src/main/java/com/degerli/kisakes/controller/UrlController.java

package com.degerli.kisakes.controller;

import com.degerli.kisakes.model.dto.UrlCreateRequest;
import com.degerli.kisakes.model.dto.UrlDto;
import com.degerli.kisakes.model.entity.Url;
import com.degerli.kisakes.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kisakes/api/v1/urls")
public class UrlController {

  private final UrlService urlService;

  @PostMapping()
  public ResponseEntity<UrlDto> createShortUrl(
      @Valid @RequestBody UrlCreateRequest request,
      HttpServletRequest servletRequest) {

    Url createdUrl = urlService.createShortUrl(request);
    UrlDto response = toUrlResponse(createdUrl, servletRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirectToOriginalUrl(
      @PathVariable String shortCode) {
    String originalUrl = urlService.getOriginalUrl(shortCode);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(originalUrl))
        .build();
  }

  // ============================================
  // FIX: Use X-Forwarded-Host for shortUrl
  // ============================================
  private UrlDto toUrlResponse(Url url, HttpServletRequest request) {
    String shortUrl;

    // Check for reverse proxy/load balancer headers
    String forwardedHost = request.getHeader("X-Forwarded-Host");
    String forwardedProto = request.getHeader("X-Forwarded-Proto");

    if (forwardedHost != null && !forwardedHost.isEmpty()) {
      // ============================================
      // REQUEST CAME THROUGH LOAD BALANCER
      // ============================================

      String scheme = forwardedProto != null ? forwardedProto : "http";

      // Build public-facing URL
      // Format: http://localhost:8080/kisakes/{shortCode}
      shortUrl = String.format("%s://%s/kisakes/%s",
          scheme,
          forwardedHost,
          url.getShortCode()
      );

      log.debug("Generated shortUrl via Load Balancer: {}", shortUrl);

    } else {
      // ============================================
      // DIRECT ACCESS (NO LOAD BALANCER)
      // ============================================

      // Use local server info
      // Format: http://localhost:8081/{shortCode}
      String scheme = request.getScheme();
      String serverName = request.getServerName();
      int serverPort = request.getServerPort();

      // Include port if not default
      String portPart = (serverPort != 80 && serverPort != 443)
          ? ":" + serverPort
          : "";

      shortUrl = String.format("%s://%s%s/%s",
          scheme,
          serverName,
          portPart,
          url.getShortCode()
      );

      log.debug("Generated shortUrl for direct access: {}", shortUrl);
    }

    return new UrlDto(
        url.getOriginalUrl(),
        shortUrl,
        url.getCreatedAt(),
        url.getExpiresAt()
    );
  }
}

// ============================================
// EXPECTED BEHAVIOR:
// ============================================

/*
SCENARIO 1: Via Load Balancer
Client Request:
POST http://localhost:8080/kisakes/api/v1/urls

Backend Receives:
POST /api/v1/urls
Headers:
  X-Forwarded-Host: localhost:8080
  X-Forwarded-Proto: http

Backend Response:
{
  "shortUrl": "http://localhost:8080/kisakes/abc123"  ✅
}

---

SCENARIO 2: Direct Access
Client Request:
POST http://localhost:8081/api/v1/urls

Backend Receives:
POST /api/v1/urls
Headers:
  Host: localhost:8081
  (No X-Forwarded-* headers)

Backend Response:
{
  "shortUrl": "http://localhost:8081/abc123"  ✅
}
*/