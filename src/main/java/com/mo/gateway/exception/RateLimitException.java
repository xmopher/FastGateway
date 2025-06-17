package com.mo.gateway.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limiting is violated
 */
@Getter
public class RateLimitException extends GatewayException {

    private final String clientId;
    private final long retryAfterMs;

    public RateLimitException(String message, String clientId) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.clientId = clientId;
        this.retryAfterMs = 0;
    }

    public RateLimitException(String message, String clientId, long retryAfterMs) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.clientId = clientId;
        this.retryAfterMs = retryAfterMs;
    }

    public RateLimitException(String message, String clientId, long retryAfterMs, Throwable cause) {
        super(message, "RATE_LIMIT_EXCEEDED", 429, cause);
        this.clientId = clientId;
        this.retryAfterMs = retryAfterMs;
    }
}