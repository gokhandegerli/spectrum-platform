package com.degerli.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "loadbalancer")
public class LoadBalancerProperties {

  /**
   * Load balancing algoritması
   * Olası değerler: ROUND_ROBIN, LEAST_CONNECTIONS, IP_HASH, WEIGHTED_ROUND_ROBIN, RANDOM
   */
  private Algorithm algorithm = Algorithm.ROUND_ROBIN;

  /**
   * Health check aktif mi?
   */
  private boolean healthCheckEnabled = true;

  /**
   * Health check interval (milisaniye)
   */
  private long healthCheckInterval = 5000;

  /**
   * Health check timeout (milisaniye)
   */
  private long healthCheckTimeout = 2000;

  /**
   * Backend servislerin tanımları
   */
  private Map<String, ServiceConfig> services;

  @Data
  public static class ServiceConfig {
    /**
     * Servis için upstream serverlar
     */
    private List<ServerConfig> upstreams = new ArrayList<>();

    /**
     * Bu servis için özel algoritma (opsiyonel)
     */
    private Algorithm algorithm;
  }

  @Data
  public static class ServerConfig {
    /**
     * Server URL'i (örn: http://localhost:8081)
     */
    private String url;

    /**
     * Server weight (weighted algoritmalarda kullanılır)
     */
    private int weight = 1;

    /**
     * Max connections (least connections'da kullanılır)
     */
    private int maxConnections = 100;
  }

  public enum Algorithm {
    ROUND_ROBIN,           // Sırayla dağıt
    LEAST_CONNECTIONS,     // En az bağlantısı olana gönder
    IP_HASH,              // Client IP'sine göre hash
    WEIGHTED_ROUND_ROBIN, // Ağırlıklı dağıtım
    RANDOM                // Rastgele
  }

  // Circuit Breaker Configuration
  @Data
  public static class CircuitBreakerConfig {
    private boolean enabled = true;
    private int failureThreshold = 5;        // Kaç hata sonrası OPEN
    private int successThreshold = 2;        // HALF_OPEN'da kaç başarı
    private long timeoutSeconds = 60;        // OPEN -> HALF_OPEN süresi
    private long resetTimeoutSeconds = 300;  // Hata sayacını sıfırlama
  }

  // Rate Limiting Configuration
  @Data
  public static class RateLimitConfig {
    private boolean enabled = false;
    private int maxRequests = 100;           // Window içinde max request
    private long windowSeconds = 60;         // Window süresi
    private String algorithm = "TOKEN_BUCKET"; // TOKEN_BUCKET veya SLIDING_WINDOW
  }

  // SSL Configuration
  @Data
  public static class SslConfig {
    private boolean enabled = false;
    private int httpsPort = 8443;
    private String keystoreFile = "classpath:keystore.p12";
    private String keystorePassword = "changeit";
  }

  // Sticky Session Configuration
  @Data
  public static class StickySessionConfig {
    private boolean enabled = false;
    private long sessionTimeoutMinutes = 30;
    private String cookieName = "LB_SESSION_ID";
  }

  // Features
  private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
  private RateLimitConfig rateLimit = new RateLimitConfig();
  private SslConfig ssl = new SslConfig();
  private StickySessionConfig stickySession = new StickySessionConfig();
}