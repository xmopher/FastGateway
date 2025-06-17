package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Load Balancer Configuration Properties
 */
@ConfigurationProperties(prefix = "gateway.loadbalancer")
public record LoadBalancerProperties(
        Map<String, String> serviceStrategies,
        String defaultStrategy,
        int healthCheckIntervalMs,
        int connectionTimeoutMs,
        int readTimeoutMs,
        boolean enableCircuitBreaker
) {

    /**
     * Default constructor with sensible defaults
     */
    public LoadBalancerProperties() {
        this(new HashMap<>(), "roundRobin", 30000, 5000, 10000, true);
    }

    /**
     * Get load balancing strategy for a specific service
     */
    public String getStrategy(String serviceId) {
        return serviceStrategies.getOrDefault(serviceId, defaultStrategy);
    }
}
