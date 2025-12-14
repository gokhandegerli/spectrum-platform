package com.degerli.loadbalancer.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Load Balancer Configuration Properties
 *
 * <p><b>CONFIGURATION OVERRIDE HIERARCHY (Priority Order - Highest to Lowest):</b></p>
 * <ol>
 *   <li><b>application.yml / application.properties</b> - YAML/Properties dosyasındaki değerler EN YÜKSEK önceliğe sahiptir</li>
 *   <li><b>Environment Variables</b> - Sistem ortam değişkenleri (örn: LOADBALANCER_ALGORITHM=RANDOM)</li>
 *   <li><b>Command Line Arguments</b> - JVM başlatma parametreleri (örn: --loadbalancer.algorithm=RANDOM)</li>
 *   <li><b>@ConfigurationProperties Default Values</b> - Bu class'taki field'ların initial değerleri (EN DÜŞÜK öncelik)</li>
 * </ol>
 *
 * <p><b>ÖNEMLI:</b> YAML dosyasında tanımlı OLAN değerler bu class'taki default değerleri OVERRIDE eder.
 * YAML'da tanımlı OLMAYAN değerler için bu class'taki default değerler kullanılır.</p>
 *
 * <p><b>Örnek:</b></p>
 * <pre>
 * // Java class'ta:
 * private boolean healthCheckEnabled = true;  // Default: açık
 *
 * // YAML'da belirtilirse:
 * loadbalancer:
 *   health-check-enabled: false  # YAML KAZANIR → false olur
 *
 * // YAML'da belirtilmezse:
 * loadbalancer:
 *   algorithm: ROUND_ROBIN
 *   # health-check-enabled YOK → Java default kullanılır → true olur
 * </pre>
 *
 * <p><b>Spring Boot Property Binding:</b></p>
 * <ul>
 *   <li>Kebab-case (YAML): health-check-enabled</li>
 *   <li>CamelCase (Java): healthCheckEnabled</li>
 *   <li>Snake_case (Env): LOADBALANCER_HEALTH_CHECK_ENABLED</li>
 * </ul>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@ConfigurationProperties(prefix = "loadbalancer")
public class LoadBalancerProperties {

  /**
   * Load balancing algoritması
   * Olası değerler: ROUND_ROBIN, LEAST_CONNECTIONS, IP_HASH, WEIGHTED_ROUND_ROBIN, RANDOM
   * Default: ROUND_ROBIN (YAML'da override edilebilir)
   */
  private Algorithm algorithm = Algorithm.ROUND_ROBIN;

  /**
   * Health check aktif mi?
   * Default: true (YAML'da override edilebilir)
   */
  private boolean healthCheckEnabled = true;

  /**
   * Health check interval (milisaniye)
   * Default: 5000ms (YAML'da override edilebilir)
   */
  private long healthCheckInterval = 5000;

  /**
   * Health check timeout (milisaniye)
   * Default: 2000ms (YAML'da override edilebilir)
   */
  private long healthCheckTimeout = 2000;

  /**
   * Backend servislerin tanımları
   * YAML'dan doldurulması zorunludur (null ise NPE riski!)
   */
  private Map<String, ServiceConfig> services;

  @Data
  public static class ServiceConfig {
    /**
     * Servis için upstream serverlar
     * Default: boş liste (YAML'da doldurulmalı)
     */
    private List<ServerConfig> upstreams = new ArrayList<>();

    /**
     * Bu servis için özel algoritma (opsiyonel)
     * Default: null → parent algorithm kullanılır
     */
    private Algorithm algorithm;
  }

  @Data
  public static class ServerConfig {
    /**
     * Server URL'i (örn: http://localhost:8081)
     * YAML'dan doldurulması zorunludur
     */
    private String url;

    /**
     * Server weight (weighted algoritmalarda kullanılır)
     * Default: 1 (YAML'da override edilebilir)
     */
    private int weight = 1;

    /**
     * Max connections (least connections'da kullanılır)
     * Default: 100 (YAML'da override edilebilir)
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
    /**
     * Circuit breaker aktif mi?
     * Default: true (YAML'da override edilebilir)
     */
    private boolean enabled = true;

    /**
     * Kaç hata sonrası OPEN state'e geçilir
     * Default: 5 (YAML'da override edilebilir)
     */
    private int failureThreshold = 5;

    /**
     * HALF_OPEN'da kaç başarılı istek sonrası CLOSED'a dönülür
     * Default: 2 (YAML'da override edilebilir)
     */
    private int successThreshold = 2;

    /**
     * OPEN -> HALF_OPEN geçiş süresi (saniye)
     * Default: 60s (YAML'da override edilebilir)
     */
    private long timeoutSeconds = 60;

    /**
     * Hata sayacını sıfırlama süresi (saniye)
     * Default: 300s (YAML'da override edilebilir)
     */
    private long resetTimeoutSeconds = 300;
  }

  // Rate Limiting Configuration
  @Data
  public static class RateLimitConfig {
    /**
     * Rate limiting aktif mi?
     * Default: false (YAML'da override edilebilir)
     */
    private boolean enabled = false;

    /**
     * Window içinde maksimum request sayısı
     * Default: 100 (YAML'da override edilebilir)
     */
    private int maxRequests = 100;

    /**
     * Window süresi (saniye)
     * Default: 60s (YAML'da override edilebilir)
     */
    private long windowSeconds = 60;

    /**
     * Rate limiting algoritması
     * Olası değerler: TOKEN_BUCKET, SLIDING_WINDOW
     * Default: TOKEN_BUCKET (YAML'da override edilebilir)
     */
    private String algorithm = "TOKEN_BUCKET";
  }

  // SSL Configuration
  @Data
  public static class SslConfig {
    /**
     * SSL/TLS aktif mi?
     * Default: false (YAML'da override edilebilir)
     */
    private boolean enabled = false;

    /**
     * HTTPS port numarası
     * Default: 8443 (YAML'da override edilebilir)
     */
    private int httpsPort = 8443;

    /**
     * Keystore dosya yolu
     * Default: classpath:keystore.p12 (YAML'da override edilebilir)
     */
    private String keystoreFile = "classpath:keystore.p12";

    /**
     * Keystore şifresi
     * Default: changeit (YAML'da override edilebilir - ÖNERİLİR!)
     */
    private String keystorePassword = "changeit";
  }

  // Sticky Session Configuration
  @Data
  public static class StickySessionConfig {
    /**
     * Sticky session aktif mi?
     * Default: false (YAML'da override edilebilir)
     */
    private boolean enabled = false;

    /**
     * Session timeout süresi (dakika)
     * Default: 30 dakika (YAML'da override edilebilir)
     */
    private long sessionTimeoutMinutes = 30;

    /**
     * Session cookie ismi
     * Default: LB_SESSION_ID (YAML'da override edilebilir)
     */
    private String cookieName = "LB_SESSION_ID";
  }

  // Features (default values burada tanımlı)
  private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
  private RateLimitConfig rateLimit = new RateLimitConfig();
  private SslConfig ssl = new SslConfig();
  private StickySessionConfig stickySession = new StickySessionConfig();
}