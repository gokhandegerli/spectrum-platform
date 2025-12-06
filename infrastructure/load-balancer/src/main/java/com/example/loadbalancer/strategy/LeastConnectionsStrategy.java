package com.example.loadbalancer.strategy;

import com.example.loadbalancer.model.Server;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Least Connections: En az aktif connection'a sahip servera gönderir
 */
@Slf4j
public class LeastConnectionsStrategy implements LoadBalancingStrategy {

  @Override
  public Server selectServer(List<Server> servers, String clientIp) {
    if (servers.isEmpty()) {
      throw new IllegalStateException("No available servers");
    }

    Server selected = servers.stream()
        .filter(Server::isHealthy)
        .filter(s -> !s.isAtCapacity())
        .min((s1, s2) -> {
          // Önce connection sayısına göre
          int connCompare = Integer.compare(
              s1.getActiveConnections().get(),
              s2.getActiveConnections().get()
          );
          if (connCompare != 0) {
            return connCompare;
          }
          // Eşitse, toplam request sayısına göre
          return Long.compare(s1.getTotalRequests().get(), s2.getTotalRequests().get());
        })
        .orElseThrow(() -> new IllegalStateException("No healthy servers available"));

    log.debug("Least Connections selected: {} (active: {})",
        selected.getUrl(), selected.getActiveConnections().get());
    return selected;
  }

  @Override
  public void onRequestStart(Server server) {
    server.incrementConnections();
    log.trace("Connection started: {} (now: {})",
        server.getUrl(), server.getActiveConnections().get());
  }

  @Override
  public void onRequestComplete(Server server) {
    server.decrementConnections();
    log.trace("Connection completed: {} (now: {})",
        server.getUrl(), server.getActiveConnections().get());
  }
}