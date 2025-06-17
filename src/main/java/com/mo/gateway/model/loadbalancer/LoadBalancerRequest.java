package com.mo.gateway.model.loadbalancer;

import com.mo.gateway.model.dto.GatewayRequest;

import java.util.Map;

/**
 * Load Balancer Request Model
 */
public record LoadBalancerRequest(
        String serviceId,
        GatewayRequest originalRequest,
        Map<String, Object> context
) {
    /**
     * Get client identifier from original request
     */
    public String getClientId() {
        return originalRequest != null ? originalRequest.getClientIdentifier() : "unknown";
    }

    /**
     * Get request path for routing decisions
     */
    public String getPath() {
        return originalRequest != null ? originalRequest.path() : "";
    }

    /**
     * Get request method for routing decisions
     */
    public String getMethod() {
        return originalRequest != null ? originalRequest.method() : "GET";
    }
}