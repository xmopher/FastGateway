package com.mo.gateway.model.loadbalancer;

import lombok.Getter;

/**
 * Health Status Enum for service instances
 */
@Getter
public enum HealthStatus {
    HEALTHY("Service is healthy and ready to receive traffic"),

    UNHEALTHY("Service is unhealthy and should not receive traffic"),

    UNKNOWN("Service health status is unknown");

    private final String description;

    HealthStatus(String description) {
        this.description = description;
    }

    /**
     * Check if status allows traffic routing
     */
    public boolean allowsTraffic() {
        return this == HEALTHY;
    }

    /**
     * Parse health status from string
     */
    public static HealthStatus fromString(String status) {
        return switch (status != null ? status.toUpperCase() : "") {
            case "HEALTHY", "UP", "OK" -> HEALTHY;
            case "UNHEALTHY", "DOWN", "ERROR" -> UNHEALTHY;
            default -> UNKNOWN;
        };
    }
}