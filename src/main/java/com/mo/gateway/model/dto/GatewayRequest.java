package com.mo.gateway.model.dto;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable data carrier for HTTP requests
 */
@Slf4j
public record GatewayRequest(
        String id,
        String clientId,
        String path,
        String method,
        Map<String, String> headers,
        Map<String, String> queryParams,
        byte[] body,
        long timestamp,
        String serviceId
) {
    /**
     * Compact constructor with validation and defaults
     */
    public GatewayRequest {
        if (id == null) id = UUID.randomUUID().toString();
        if (timestamp == 0) timestamp = System.currentTimeMillis();
    }

    /**
     * Static factory method for builder pattern
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Extract client identifier using modern switch expression
     */
    public String getClientIdentifier() {
        if (clientId != null) return clientId;
        if (headers != null) {
            return switch (extractFromHeaders()) {
                case String apiKey when apiKey != null -> apiKey;
                default -> "anonymous";
            };
        }
        return "anonymous";
    }

    private String extractFromHeaders() {
        log.info("extractFromHeaders called");
        var apiKey = headers.get("X-API-Key");
        if (apiKey != null && !apiKey.isEmpty()) return apiKey;
        var authHeader = headers.get("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String clientFromAuth = extractClientFromAuth(authHeader);
            if (clientFromAuth != null && !clientFromAuth.isEmpty()) {
                return clientFromAuth;
            }
        }
        var forwardedFor = headers.get("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            String[] ips = forwardedFor.split(",");
            if (ips.length > 0 && ips[0] != null) {
                String ip = ips[0].trim();
                if (!ip.isEmpty()) {
                    return ip;
                }
            }
        }
        String realIp = headers.get("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return "anonymous";
    }

    private String extractClientFromAuth(String authHeader) {
        return switch (authHeader) {
            case String auth when auth.startsWith("Bearer ") ->
                    STR."bearer-\{auth.substring(7, Math.min(auth.length(), 20))}";
            case String auth when auth.startsWith("Basic ") ->
                    STR."basic-\{auth.substring(6, Math.min(auth.length(), 20))}";
            default -> "auth-client";
        };
    }

    /**
     * Builder class for fluent construction
     */
    public static class Builder {
        private String id;
        private String clientId;
        private String path;
        private String method;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private byte[] body;
        private long timestamp;
        private String serviceId;
        public Builder id(String id) { this.id = id; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder queryParams(Map<String, String> queryParams) { this.queryParams = queryParams; return this; }
        public Builder body(byte[] body) { this.body = body; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder serviceId(String serviceId) { this.serviceId = serviceId; return this; }
        public GatewayRequest build() {
            return new GatewayRequest(id, clientId, path, method, headers, queryParams, body, timestamp, serviceId);
        }
    }
}
