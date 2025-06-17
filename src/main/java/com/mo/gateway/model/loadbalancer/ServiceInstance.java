package com.mo.gateway.model.loadbalancer;

import java.util.Map;

/**
 * Represents a backend service instance
 */
public record ServiceInstance(
        String id,
        String serviceId,
        String host,
        int port,
        String protocol,
        int weight,
        HealthStatus healthStatus,
        Map<String, String> metadata,
        long lastHealthCheck
) {
    /**
     * Compact constructor with validation and defaults
     */
    public ServiceInstance {
        if (protocol == null) protocol = "http";
        if (weight <= 0) weight = 1;
        if (healthStatus == null) healthStatus = HealthStatus.HEALTHY;
    }

    /**
     * Get complete URL for this service instance
     */
    public String getUrl() {
        return STR."\{protocol}://\{host}:\{port}";
    }

    /**
     * Check if instance is healthy
     */
    public boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY;
    }

    /**
     * Check if health check is stale
     */
    public boolean isHealthCheckStale(long maxAgeMs) {
        return System.currentTimeMillis() - lastHealthCheck > maxAgeMs;
    }

    /**
     * Static factory method for builder pattern
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent construction
     */
    public static class Builder {
        private String id;
        private String serviceId;
        private String host;
        private int port;
        private String protocol = "http";
        private int weight = 1;
        private HealthStatus healthStatus = HealthStatus.HEALTHY;
        private Map<String, String> metadata;
        private long lastHealthCheck;
        public Builder id(String id) { this.id = id; return this; }
        public Builder serviceId(String serviceId) { this.serviceId = serviceId; return this; }
        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder protocol(String protocol) { this.protocol = protocol; return this; }
        public Builder weight(int weight) { this.weight = weight; return this; }
        public Builder healthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder lastHealthCheck(long lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; return this; }
        public ServiceInstance build() {
            return new ServiceInstance(id, serviceId, host, port, protocol, weight, healthStatus, metadata, lastHealthCheck);
        }
    }
}
