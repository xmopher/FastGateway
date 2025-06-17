package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Exception thrown when no healthy service instances are available
 * for load balancing
 */
@Getter
public class NoHealthyInstanceException extends GatewayException {

    private final String serviceId;

    public NoHealthyInstanceException(String message) {
        super(message, "NO_HEALTHY_INSTANCES", 503);
        this.serviceId = null;
    }

    public NoHealthyInstanceException(String message, String serviceId) {
        super(message, "NO_HEALTHY_INSTANCES", 503);
        this.serviceId = serviceId;
    }

    public NoHealthyInstanceException(String message, String serviceId, Throwable cause) {
        super(message, "NO_HEALTHY_INSTANCES", 503, cause);
        this.serviceId = serviceId;
    }
}

