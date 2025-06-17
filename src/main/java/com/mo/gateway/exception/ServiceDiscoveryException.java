package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Exception thrown when service discovery operations fail
 */
@Getter
public class ServiceDiscoveryException extends GatewayException {

    private final String discoveryType;

    public ServiceDiscoveryException(String message) {
        super(message, "SERVICE_DISCOVERY_ERROR", 500);
        this.discoveryType = null;
    }

    public ServiceDiscoveryException(String message, String discoveryType) {
        super(message, "SERVICE_DISCOVERY_ERROR", 500);
        this.discoveryType = discoveryType;
    }

    public ServiceDiscoveryException(String message, String discoveryType, Throwable cause) {
        super(message, "SERVICE_DISCOVERY_ERROR", 500, cause);
        this.discoveryType = discoveryType;
    }
}
