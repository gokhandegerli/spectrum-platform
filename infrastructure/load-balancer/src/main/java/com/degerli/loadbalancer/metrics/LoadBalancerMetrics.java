package com.degerli.loadbalancer.metrics;

import com.degerli.loadbalancer.model.Server;
import com.degerli.loadbalancer.registry.ServiceRegistry;
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
 * Percentiles (P50, P95, P99) ile geliştirilmiş
 */
@Component
@RequiredArgsConstructor
public class LoadBalancerMetrics {

  private final MeterRegistry meterRegistry;
  private final ServiceRegistry serviceRegistry;

  // Counters
  private final ConcurrentMap<String, Counter> requestCounters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

  // Timers with percentiles
  private final ConcurrentMap<String, Timer> requestTimers = new ConcurrentHashMap<>();

  @PostConstruct
  public void initMetrics() {
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

      // Request timer with percentiles
      requestTimers.put(serviceName, Timer.builder("loadbalancer.request.duration")
          .tag("service", serviceName)
          .description("Request duration in seconds")
          .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
          .publishPercentileHistogram()
          .minimumExpectedValue(java.time.Duration.ofMillis(1))
          .maximumExpectedValue(java.time.Duration.ofSeconds(10))
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
            .description("Server health status (1=healthy, 0=unhealthy)")
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

        // Success rate
        Gauge.builder("loadbalancer.server.success.rate", server, s -> {
              long total = s.getTotalRequests().get();
              long failed = s.getFailedRequests().get();
              if (total == 0) {
                return 1.0;
              }
              return (double) (total - failed) / total;
            })
            .tag("service", serviceName)
            .tag("server", server.getUrl())
            .description("Success rate (0.0 to 1.0)")
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
 * Prometheus Queries:
 *
 * # P95 response time
 * loadbalancer_request_duration_seconds{quantile="0.95",service="kisakes"}
 *
 * # P99 response time
 * loadbalancer_request_duration_seconds{quantile="0.99",service="kisakes"}
 *
 * # Error rate (%)
 * (rate(loadbalancer_errors_total[5m]) / rate(loadbalancer_requests_total[5m])) * 100
 *
 * # Success rate (%)
 * (1 - (rate(loadbalancer_errors_total[5m]) / rate(loadbalancer_requests_total[5m]))) * 100
 *
 * # Average response time (ms)
 * rate(loadbalancer_request_duration_seconds_sum[5m]) / rate(loadbalancer_request_duration_seconds_count[5m]) * 1000
 */