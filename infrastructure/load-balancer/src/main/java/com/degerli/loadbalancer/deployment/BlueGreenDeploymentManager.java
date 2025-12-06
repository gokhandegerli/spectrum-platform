package com.degerli.loadbalancer.deployment;

import com.degerli.loadbalancer.model.Server;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Blue-Green Deployment Manager
 * <p>
 * Blue Environment: Şu an aktif olan versiyon
 * Green Environment: Yeni deploy edilen versiyon
 * <p>
 * Switching stratejileri:
 * 1. Instant: Anında switch
 * 2. Gradual: Kademeli trafik artırma (canary)
 * 3. Scheduled: Zamanlanmış switch
 */
@Slf4j
public class BlueGreenDeploymentManager {

  private final Map<String, DeploymentEnvironment> services = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  /**
   * Servis için environment'ları tanımla
   */
  public void defineEnvironments(String serviceName, List<Server> blueServers,
      List<Server> greenServers) {
    DeploymentEnvironment env = new DeploymentEnvironment(serviceName, blueServers,
        greenServers);
    services.put(serviceName, env);
    log.info("Defined environments for service: {} (blue: {}, green: {})", serviceName,
        blueServers.size(), greenServers.size());
  }

  /**
   * Aktif serverları al (mevcut traffic dağılımına göre)
   */
  public List<Server> getActiveServers(String serviceName) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    return env.getActiveServers();
  }

  /**
   * Anında switch yap
   */
  public void switchInstant(String serviceName) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    env.switchInstant();
    log.info("Instant switch completed for service: {} (now active: {})", serviceName,
        env.getActiveEnvironment());
  }

  /**
   * Kademeli switch başlat (Canary deployment)
   */
  public void switchGradual(String serviceName, Duration duration, int steps) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    long delayMs = duration.toMillis() / steps;
    int incrementPerStep = 100 / steps;

    log.info("Starting gradual switch for service: {} ({} steps over {} seconds)", serviceName,
        steps, duration.getSeconds());

    for (int i = 1; i <= steps; i++) {
      final int greenPercentage = Math.min(i * incrementPerStep, 100);
      scheduler.schedule(() -> {
        env.setGreenTrafficPercentage(greenPercentage);
        log.info("Gradual switch progress for {}: {}% green traffic", serviceName,
            greenPercentage);
      }, delayMs * i, TimeUnit.MILLISECONDS);
    }

    // Son adımda tam switch
    scheduler.schedule(() -> {
      env.switchInstant();
      log.info("Gradual switch completed for service: {}", serviceName);
    }, duration.toMillis() + 1000, TimeUnit.MILLISECONDS);
  }

  /**
   * Zamanlanmış switch
   */
  public void switchScheduled(String serviceName, LocalDateTime switchTime) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    Duration delay = Duration.between(LocalDateTime.now(), switchTime);
    if (delay.isNegative()) {
      throw new IllegalArgumentException("Switch time must be in the future");
    }

    log.info("Scheduled switch for service: {} at {}", serviceName, switchTime);

    scheduler.schedule(() -> {
      env.switchInstant();
      log.info("Scheduled switch executed for service: {}", serviceName);
    }, delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Rollback yap (green'den blue'ya geri dön)
   */
  public void rollback(String serviceName) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    if (env.getActiveEnvironment() == Environment.BLUE) {
      log.warn("Already on blue environment, cannot rollback: {}", serviceName);
      return;
    }

    env.switchInstant();
    log.info("Rollback completed for service: {} (reverted to blue)", serviceName);
  }

  /**
   * Deployment durumu
   */
  public DeploymentStatus getStatus(String serviceName) {
    DeploymentEnvironment env = services.get(serviceName);
    if (env == null) {
      throw new IllegalArgumentException("Unknown service: " + serviceName);
    }

    return new DeploymentStatus(serviceName, env.getActiveEnvironment(),
        env.getGreenTrafficPercentage(), env.getBlueServers().size(),
        env.getGreenServers().size());
  }

  /**
   * Deployment Environment
   */
  @Data
  private static class DeploymentEnvironment {
    private final String serviceName;
    private final List<Server> blueServers;
    private final List<Server> greenServers;

    private volatile Environment activeEnvironment = Environment.BLUE;
    private volatile int greenTrafficPercentage = 0; // 0-100

    DeploymentEnvironment(String serviceName, List<Server> blueServers,
        List<Server> greenServers) {
      this.serviceName = serviceName;
      this.blueServers = new ArrayList<>(blueServers);
      this.greenServers = new ArrayList<>(greenServers);
    }

    void switchInstant() {
      activeEnvironment = (activeEnvironment == Environment.BLUE) ? Environment.GREEN
          : Environment.BLUE;
      greenTrafficPercentage = (activeEnvironment == Environment.GREEN) ? 100 : 0;
    }

    List<Server> getActiveServers() {
      if (greenTrafficPercentage == 0) {
        return new ArrayList<>(blueServers);
      } else if (greenTrafficPercentage == 100) {
        return new ArrayList<>(greenServers);
      } else {
        // Karma: Percentage'e göre blue ve green'den server seç
        List<Server> mixed = new ArrayList<>();

        int totalCount = 10; // Sabit pool size
        int greenCount = (totalCount * greenTrafficPercentage) / 100;
        int blueCount = totalCount - greenCount;

        for (int i = 0; i < blueCount && i < blueServers.size(); i++) {
          mixed.add(blueServers.get(i % blueServers.size()));
        }
        for (int i = 0; i < greenCount && i < greenServers.size(); i++) {
          mixed.add(greenServers.get(i % greenServers.size()));
        }

        return mixed;
      }
    }
  }

  public enum Environment {
    BLUE,
    GREEN
  }

  public record DeploymentStatus(String serviceName, Environment activeEnvironment,
      int greenTrafficPercentage, int blueServerCount, int greenServerCount) {}
}

/**
 * Admin API için endpoint'ler:
 * <p>
 * POST /admin/deployment/{serviceName}/switch/instant
 * POST /admin/deployment/{serviceName}/switch/gradual?durationSeconds=300&steps=10
 * POST /admin/deployment/{serviceName}/switch/scheduled?switchTime=2024-12-07T02:00:00
 * POST /admin/deployment/{serviceName}/rollback
 * GET  /admin/deployment/{serviceName}/status
 */