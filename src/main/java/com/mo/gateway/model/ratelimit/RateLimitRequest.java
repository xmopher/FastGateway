package com.mo.gateway.model.ratelimit;

import java.util.Map;

/**
 * Rate Limit Request Model
 */
public record RateLimitRequest(
        String clientId,
        String resource,
        String method,
        Map<String, String> headers,
        int requestCost
) {
    /**
     * Compact constructor with validation
     */
    public RateLimitRequest {
        if (requestCost <= 0) requestCost = 1;
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
        private String clientId;
        private String resource;
        private String method;
        private Map<String, String> headers;
        private int requestCost = 1;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder requestCost(int requestCost) {
            this.requestCost = requestCost;
            return this;
        }

        public RateLimitRequest build() {
            return new RateLimitRequest(clientId, resource, method, headers, requestCost);
        }
    }
}
