package com.degerli.loadbalancer.strategy;

import com.degerli.loadbalancer.model.Server;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

/**
 * Random: Rastgele bir server seçer
 */
@Slf4j
public class RandomStrategy implements LoadBalancingStrategy {

  private final Random random = new Random();

  @Override
  public Server selectServer(List<Server> servers, String clientIp) {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No available servers");
    }

    // Healthy serverları filtrele
    List<Server> healthyServers = servers.stream()
        .filter(Server::isHealthy)
        .filter(s -> !s.isAtCapacity())
        .toList();

    if (healthyServers.isEmpty()) {
      throw new IllegalStateException("No healthy servers available");
    }

    int index = random.nextInt(healthyServers.size());
    Server selected = healthyServers.get(index);

    log.debug("Random selected: {} (index: {} of {})", selected.getUrl(), index,
        healthyServers.size());
    return selected;
  }
}