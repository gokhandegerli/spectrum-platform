package com.example.loadbalancer.strategy;

import com.example.loadbalancer.model.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Weighted Round Robin: Server weight'ine göre ağırlıklı dağıtım
 * Weight=3 olan server, weight=1 olan serverdan 3 kat daha fazla request alır
 */
@Slf4j
public class WeightedRoundRobinStrategy implements LoadBalancingStrategy {

  private final AtomicInteger currentIndex = new AtomicInteger(0);
  private volatile List<Server> weightedServers = new ArrayList<>();
  private volatile List<Server> lastServers = new ArrayList<>();

  @Override
  public Server selectServer(List<Server> servers, String clientIp) {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No available servers");
    }

    // Server listesi değiştiyse, weighted listeyi yeniden oluştur
    if (!servers.equals(lastServers)) {
      buildWeightedList(servers);
      lastServers = new ArrayList<>(servers);
    }

    if (weightedServers.isEmpty()) {
      buildWeightedList(servers);
    }

    int maxAttempts = weightedServers.size();
    int attempts = 0;

    while (attempts < maxAttempts) {
      int index = Math.abs(currentIndex.getAndIncrement() % weightedServers.size());
      Server server = weightedServers.get(index);

      if (server.isHealthy() && !server.isAtCapacity()) {
        log.debug("Weighted RR selected: {} (weight: {}, index: {})", server.getUrl(),
            server.getWeight(), index);
        return server;
      }

      attempts++;
    }

    // Fallback: en az yüklü server
    return servers.stream()
        .filter(Server::isHealthy)
        .min((s1, s2) -> Integer.compare(s1.getActiveConnections().get(),
            s2.getActiveConnections().get()))
        .orElseThrow(() -> new IllegalStateException("No healthy servers available"));
  }

  /**
   * Weight'e göre server listesi oluştur
   * Örnek: Server A (weight=3), Server B (weight=1)
   * Liste: [A, A, A, B]
   */
  private void buildWeightedList(List<Server> servers) {
    List<Server> weighted = new ArrayList<>();

    for (Server server : servers) {
      int weight = Math.max(1, server.getWeight()); // Minimum 1
      for (int i = 0; i < weight; i++) {
        weighted.add(server);
      }
    }

    this.weightedServers = weighted;
    log.info("Built weighted server list: {} total entries for {} servers", weighted.size(),
        servers.size());
  }
}