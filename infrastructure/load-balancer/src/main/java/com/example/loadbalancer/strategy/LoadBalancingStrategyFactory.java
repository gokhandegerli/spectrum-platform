package com.example.loadbalancer.strategy;

import com.example.loadbalancer.LoadBalancerProperties.Algorithm;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern için factory class
 */
@Component
public class LoadBalancingStrategyFactory {

  /**
   * Verilen algoritmaya göre strategy instance döner
   */
  public LoadBalancingStrategy createStrategy(Algorithm algorithm) {
    return switch (algorithm) {
      case ROUND_ROBIN -> new RoundRobinStrategy();
      case LEAST_CONNECTIONS -> new LeastConnectionsStrategy();
      case IP_HASH -> new IpHashStrategy();
      case WEIGHTED_ROUND_ROBIN -> new WeightedRoundRobinStrategy();
      case RANDOM -> new RandomStrategy();
    };
  }
}