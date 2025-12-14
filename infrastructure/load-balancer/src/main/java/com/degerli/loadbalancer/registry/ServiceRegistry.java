package com.degerli.loadbalancer.registry;

import com.degerli.loadbalancer.config.LoadBalancerProperties;
import com.degerli.loadbalancer.config.LoadBalancerProperties.Algorithm;
import com.degerli.loadbalancer.health.HealthChecker;
import com.degerli.loadbalancer.model.Server;
import com.degerli.loadbalancer.strategy.LoadBalancingStrategy;
import com.degerli.loadbalancer.strategy.LoadBalancingStrategyFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Backend servisleri ve load balancing stratejilerini yönetir
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRegistry {

    private final LoadBalancerProperties properties;
    private final LoadBalancingStrategyFactory strategyFactory;
    private final HealthChecker healthChecker;

    // Service name -> Server list mapping
    private final Map<String, List<Server>> serviceServers = new ConcurrentHashMap<>();

    // Service name -> Strategy mapping
    private final Map<String, LoadBalancingStrategy> serviceStrategies = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing Service Registry with {} services", properties.getServices().size());

        properties.getServices().forEach((serviceName, config) -> {
            // Serverları oluştur
            List<Server> servers = config.getUpstreams().stream()
                    .map(upstream -> new Server(
                            upstream.getUrl(),
                            upstream.getWeight(),
                            upstream.getMaxConnections()
                    ))
                    .collect(Collectors.toList());

            serviceServers.put(serviceName, servers);

            // Strategy oluştur (servis özel veya global)
            Algorithm algorithm = config.getAlgorithm() != null
                    ? config.getAlgorithm()
                    : properties.getAlgorithm();

            LoadBalancingStrategy strategy = strategyFactory.createStrategy(algorithm);
            serviceStrategies.put(serviceName, strategy);

            log.info("Registered service '{}' with {} upstreams using {} algorithm",
                    serviceName, servers.size(), algorithm);

            // Health check başlat
            servers.forEach(healthChecker::startHealthCheck);
        });

        log.info("Service Registry initialized successfully");
    }

    /**
     * Bir servis için server seç
     */
    public Server selectServer(String serviceName, String clientIp) {
        List<Server> servers = serviceServers.get(serviceName);
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }

        LoadBalancingStrategy strategy = serviceStrategies.get(serviceName);
        return strategy.selectServer(servers, clientIp);
    }

    /**
     * Servis için strategy al
     */
    public LoadBalancingStrategy getStrategy(String serviceName) {
        return serviceStrategies.get(serviceName);
    }

    /**
     * Tüm servis isimlerini döner
     */
    public List<String> getServiceNames() {
        return List.copyOf(serviceServers.keySet());
    }

    /**
     * Bir servisin tüm serverlarını döner
     */
    public List<Server> getServers(String serviceName) {
        return serviceServers.getOrDefault(serviceName, List.of());
    }

    /**
     * Runtime'da algoritma değiştir
     */
    public void changeAlgorithm(String serviceName, Algorithm algorithm) {
        if (!serviceServers.containsKey(serviceName)) {
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }

        LoadBalancingStrategy newStrategy = strategyFactory.createStrategy(algorithm);
        serviceStrategies.put(serviceName, newStrategy);

        log.info("Algorithm changed for service '{}' to {}", serviceName, algorithm);
    }
}