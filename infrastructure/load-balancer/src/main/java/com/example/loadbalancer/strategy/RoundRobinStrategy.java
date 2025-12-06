package com.example.loadbalancer.strategy;

import com.example.loadbalancer.model.Server;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Round Robin: Serverları sırayla kullanır
 */
@Slf4j
public class RoundRobinStrategy implements LoadBalancingStrategy {

  private final AtomicInteger currentIndex = new AtomicInteger(0);

  @Override
  public Server selectServer(List<Server> servers, String clientIp) {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No available servers");
    }

    int maxAttempts = servers.size();
    int attempts = 0;

    while (attempts < maxAttempts) {
      // Circular index: 0, 1, 2, ..., n-1, 0, 1, ...
      int index = Math.abs(currentIndex.getAndIncrement() % servers.size());
      Server server = servers.get(index);

      if (server.isHealthy() && !server.isAtCapacity()) {
        log.debug("Round Robin selected: {} (index: {})", server.getUrl(), index);
        return server;
      }

      attempts++;
    }

    // Tüm serverlar dolu veya unhealthy, en az yüklü olanı seç
    log.warn("All servers at capacity or unhealthy, selecting least loaded");
    return servers.stream()
        .filter(Server::isHealthy)
        .min((s1, s2) -> Integer.compare(s1.getActiveConnections().get(),
            s2.getActiveConnections().get()))
        .orElseThrow(() -> new IllegalStateException("No healthy servers available"));
  }
}