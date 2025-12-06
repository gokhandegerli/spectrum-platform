package com.degerli.loadbalancer.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;

@Data
public class Server {
  /**
   * Server URL'i
   */
  private String url;

  /**
   * Server weight (weighted algoritmalarda)
   */
  private int weight;

  /**
   * Max connection sayısı
   */
  private int maxConnections;

  /**
   * Server sağlıklı mı?
   */
  private volatile boolean healthy = true;

  /**
   * Son health check zamanı
   */
  private volatile LocalDateTime lastHealthCheck;

  /**
   * Aktif connection sayısı
   */
  private AtomicInteger activeConnections = new AtomicInteger(0);

  /**
   * Toplam işlenen request sayısı
   */
  private AtomicLong totalRequests = new AtomicLong(0);

  /**
   * Başarısız request sayısı
   */
  private AtomicLong failedRequests = new AtomicLong(0);

  /**
   * Ortalama response time (ms)
   */
  private volatile long averageResponseTime = 0;

  public Server(String url, int weight, int maxConnections) {
    this.url = url;
    this.weight = weight;
    this.maxConnections = maxConnections;
  }

  /**
   * Connection başladı
   */
  public void incrementConnections() {
    activeConnections.incrementAndGet();
    totalRequests.incrementAndGet();
  }

  /**
   * Connection bitti
   */
  public void decrementConnections() {
    activeConnections.decrementAndGet();
  }

  /**
   * Başarısız request kaydet
   */
  public void recordFailure() {
    failedRequests.incrementAndGet();
  }

  /**
   * Response time güncelle
   */
  public void updateResponseTime(long responseTime) {
    // Basit moving average
    averageResponseTime = (averageResponseTime + responseTime) / 2;
  }

  /**
   * Server capacity'si doldu mu?
   */
  public boolean isAtCapacity() {
    return activeConnections.get() >= maxConnections;
  }

  @Override
  public String toString() {
    return String.format("Server{url='%s', healthy=%s, connections=%d/%d, totalRequests=%d}",
        url, healthy, activeConnections.get(), maxConnections, totalRequests.get());
  }
}