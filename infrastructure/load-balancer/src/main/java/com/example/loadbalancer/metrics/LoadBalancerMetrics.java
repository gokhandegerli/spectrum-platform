package com.example.loadbalancer.metrics;

import com.example.loadbalancer.model.Server;
import com.example.loadbalancer.registry.ServiceRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Prometheus formatında metrics export
 */
@Component
@RequiredArgsConstructor
public class LoadBalancerMetrics {

  private final MeterRegistry meterRegistry;
  private final ServiceRegistry serviceRegistry;

  // Counters
  private final ConcurrentMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

  // Timers
  private final ConcurrentMap<String, Timer> requestTimers = new ConcurrentHashMap<>();

  @PostConstruct
  public void initMetrics() {
    // Her servis için metrikler
    serviceRegistry.getServiceNames().forEach(serviceName -> {
      // Request counter
      requestCounters.put(serviceName, Counter.builder("loadbalancer.requests.total")
          .tag("service", serviceName)
          .description("Total number of requests")
          .register(meterRegistry));

      // Error counter
      errorCounters.put(serviceName, Counter.builder("loadbalancer.errors.total")
          .tag("service", serviceName)
          .description("Total number of errors")
          .register(meterRegistry));

      // Request timer
      requestTimers.put(serviceName, Timer.builder("loadbalancer.request.duration")
          .tag("service", serviceName)
          .description("Request duration in seconds")
          .register(meterRegistry));

      // Server health gauges
      List<Server> servers = serviceRegistry.getServers(serviceName);
      servers.forEach(server -> {
        // Active connections
        Gauge.builder("loadbalancer.server.connections.active", server,
                s -> s.getActiveConnections().get())
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Active connections to server")
            .register(meterRegistry);

        // Health status (1=healthy, 0=unhealthy)
        Gauge.builder("loadbalancer.server.health", server, s -> s.isHealthy() ? 1.0 : 0.0)
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Server health status")
            .register(meterRegistry);

        // Total requests
        Gauge.builder("loadbalancer.server.requests.total", server,
                s -> s.getTotalRequests().get())
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Total requests to server")
            .register(meterRegistry);

        // Failed requests
        Gauge.builder("loadbalancer.server.requests.failed", server,
                s -> s.getFailedRequests().get())
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Failed requests to server")
            .register(meterRegistry);

        // Average response time
        Gauge.builder("loadbalancer.server.response.time.avg", server,
                Server::getAverageResponseTime)
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Average response time in milliseconds")
            .register(meterRegistry);
      });
    });

    // Global metrics
    Gauge.builder("loadbalancer.services.total", serviceRegistry,
            reg -> reg.getServiceNames().size())
        .description("Total number of services")
        .register(meterRegistry);
  }

  /**
   * Request başarılı
   */
  public void recordSuccess(String serviceName, long durationMs) {
    requestCounters.get(serviceName).increment();
    requestTimers.get(serviceName)
        .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
  }

  /**
   * Request başarısız
   */
  public void recordError(String serviceName, long durationMs) {
    requestCounters.get(serviceName).increment();
    errorCounters.get(serviceName).increment();
    requestTimers.get(serviceName)
        .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
  }
}

/**
 * Prometheus endpoint: http://localhost:8080/actuator/prometheus
 * <p>
 * Örnek metrikler:
 * <p>
 * # HELP loadbalancer_requests_total Total number of requests
 * # TYPE loadbalancer_requests_total counter
 * loadbalancer_requests_total{service="service1"} 1523.0
 * <p>
 * # HELP loadbalancer_server_health Server health status
 * # TYPE loadbalancer_server_health gauge
 * loadbalancer_server_health{service="service1",server="http://service1-pod1:8080"} 1.0
 * <p>
 * # HELP loadbalancer_request_duration_seconds Request duration
 * # TYPE loadbalancer_request_duration_seconds summary
 * loadbalancer_request_duration_seconds_count{service="service1"} 1523.0
 * loadbalancer_request_duration_seconds_sum{service="service1"} 45.234
 */