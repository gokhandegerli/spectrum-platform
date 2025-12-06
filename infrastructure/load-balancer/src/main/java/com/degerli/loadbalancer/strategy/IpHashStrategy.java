package com.degerli.loadbalancer.strategy;

import com.degerli.loadbalancer.model.Server;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * IP Hash: Client IP'sine göre consistent hashing ile server seçer
 * Aynı client her zaman aynı servera gider (session affinity)
 */
@Slf4j
public class IpHashStrategy implements LoadBalancingStrategy {

  @Override
  public Server selectServer(List<Server> servers, String clientIp) {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No available servers");
    }

    if (clientIp == null || clientIp.isEmpty()) {
      clientIp = "unknown";
    }

    // Client IP'den hash üret
    int hash = Math.abs(clientIp.hashCode());

    // Hash'e göre server seç
    int index = hash % servers.size();
    Server selected = servers.get(index);

    // Seçilen server unhealthy ise, bir sonrakini dene
    int maxAttempts = servers.size();
    int attempts = 0;

    while ((!selected.isHealthy() || selected.isAtCapacity()) && attempts < maxAttempts) {
      index = (index + 1) % servers.size();
      selected = servers.get(index);
      attempts++;
    }

    if (!selected.isHealthy() && attempts >= maxAttempts) {
      throw new IllegalStateException("No healthy servers available");
    }

    log.debug("IP Hash selected: {} for client: {} (hash: {}, index: {})", selected.getUrl(),
        clientIp, hash, index);
    return selected;
  }
}