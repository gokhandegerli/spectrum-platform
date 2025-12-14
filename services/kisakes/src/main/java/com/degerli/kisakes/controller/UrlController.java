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
@RequestMapping("/kisakes")
public class UrlController {

  private final UrlService urlService;

  @PostMapping("/api/v1/urls")
  public ResponseEntity<UrlDto> createShortUrl(
      @Valid @RequestBody UrlCreateRequest request,
      HttpServletRequest servletRequest) {

    Url createdUrl = urlService.createShortUrl(request);
    UrlDto response = toUrlResponse(createdUrl, servletRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
    String originalUrl = urlService.getOriginalUrl(shortCode);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(originalUrl))
        .build();
  }

  private UrlDto toUrlResponse(Url url, HttpServletRequest request) {
    String shortUrl;

    String forwardedHost = request.getHeader("X-Forwarded-Host");
    String forwardedProto = request.getHeader("X-Forwarded-Proto");

    if (forwardedHost != null && !forwardedHost.isEmpty()) {
      String scheme = forwardedProto != null ? forwardedProto : "http";

      // ✅ Artık doğru path
      shortUrl = String.format("%s://%s/kisakes/%s",
          scheme,
          forwardedHost,
          url.getShortCode()
      );

      log.debug("Generated shortUrl via Load Balancer: {}", shortUrl);

    } else {
      String scheme = request.getScheme();
      String serverName = request.getServerName();
      int serverPort = request.getServerPort();

      String portPart = (serverPort != 80 && serverPort != 443)
          ? ":" + serverPort
          : "";

      // Direct access: /kisakes/{code}
      shortUrl = String.format("%s://%s%s/kisakes/%s",
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