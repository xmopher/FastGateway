package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Base exception class for all gateway-related exceptions
 * Provides common functionality for gateway error handling
 */
@Getter
public class GatewayException extends RuntimeException {

    private final String errorCode;

    private final int httpStatus;

    public GatewayException(String message) {
        super(message);
        this.errorCode = "GATEWAY_ERROR";
        this.httpStatus = 500;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GATEWAY_ERROR";
        this.httpStatus = 500;
    }

    public GatewayException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public GatewayException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
