package com.mo.gateway.model.dto;

import java.util.Map;

/**
 * Immutable data carrier for HTTP responses
 */
public record GatewayResponse(
        int statusCode,
        Map<String, String> headers,
        byte[] body,
        long timestamp,
        String instanceId,
        long processingTimeMs
) {
    /**
     * Compact constructor with defaults
     */
    public GatewayResponse {
        if (timestamp == 0) timestamp = System.currentTimeMillis();
    }

    /**
     * Static factory method for builder pattern
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check if response indicates success
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Check if response indicates client error
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Check if response indicates server error
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }

    /**
     * Builder class for fluent construction
     */
    public static class Builder {
        private int statusCode;
        private Map<String, String> headers;
        private byte[] body;
        private long timestamp;
        private String instanceId;
        private long processingTimeMs;
        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder body(byte[] body) { this.body = body; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder instanceId(String instanceId) { this.instanceId = instanceId; return this; }
        public Builder processingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; return this; }
        public GatewayResponse build() {
            return new GatewayResponse(statusCode, headers, body, timestamp, instanceId, processingTimeMs);
        }
    }
}
