package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Exception thrown when load balancing operations fail
 */
@Getter
public class LoadBalancerException extends GatewayException {

    private final String serviceId;

    private final String strategy;

    public LoadBalancerException(String message) {
        super(message, "LOAD_BALANCER_ERROR", 503);
        this.serviceId = null;
        this.strategy = null;
    }

    public LoadBalancerException(String message, String serviceId) {
        super(message, "LOAD_BALANCER_ERROR", 503);
        this.serviceId = serviceId;
        this.strategy = null;
    }

    public LoadBalancerException(String message, String serviceId, String strategy) {
        super(message, "LOAD_BALANCER_ERROR", 503);
        this.serviceId = serviceId;
        this.strategy = strategy;
    }

    public LoadBalancerException(String message, String serviceId, String strategy, Throwable cause) {
        super(message, "LOAD_BALANCER_ERROR", 503, cause);
        this.serviceId = serviceId;
        this.strategy = strategy;
    }
}
