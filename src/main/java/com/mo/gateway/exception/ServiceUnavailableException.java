package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Exception thrown when a service is unavailable or unreachable
 */
@Getter
public class ServiceUnavailableException extends GatewayException {

    private final String serviceId;

    private final String instanceId;

    public ServiceUnavailableException(String message) {
        super(message, "SERVICE_UNAVAILABLE", 503);
        this.serviceId = null;
        this.instanceId = null;
    }

    public ServiceUnavailableException(String message, String serviceId) {
        super(message, "SERVICE_UNAVAILABLE", 503);
        this.serviceId = serviceId;
        this.instanceId = null;
    }

    public ServiceUnavailableException(String message, String serviceId, String instanceId) {
        super(message, "SERVICE_UNAVAILABLE", 503);
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }

    public ServiceUnavailableException(String message, String serviceId, String instanceId, Throwable cause) {
        super(message, "SERVICE_UNAVAILABLE", 503, cause);
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }
}
