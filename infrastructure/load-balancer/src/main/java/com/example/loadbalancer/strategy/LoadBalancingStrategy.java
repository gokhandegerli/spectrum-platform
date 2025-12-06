package com.example.loadbalancer.strategy;

import com.example.loadbalancer.model.Server;
import java.util.List;

/**
 * Load balancing stratejilerinin temel interface'i
 */
public interface LoadBalancingStrategy {

  /**
   * Verilen server listesinden bir server seç
   *
   * @param servers  Seçilebilecek serverlar
   * @param clientIp Client IP adresi (IP_HASH stratejisi için)
   * @return Seçilen server
   */
  Server selectServer(List<Server> servers, String clientIp);

  /**
   * Bir request tamamlandığında bildirim
   *
   * @param server Request'i işleyen server
   */
  default void onRequestComplete(Server server) {
    // Alt sınıflar override edebilir
  }

  /**
   * Bir request başladığında bildirim
   *
   * @param server Request'i işleyecek server
   */
  default void onRequestStart(Server server) {
    // Alt sınıflar override edebilir
  }
}