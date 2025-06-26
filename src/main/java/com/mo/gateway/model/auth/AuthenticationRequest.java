package com.mo.gateway.model.auth;

import java.util.Map;
import java.util.Optional;

/**
 * Authentication Request
 */
public record AuthenticationRequest(
        String path,
        String method,
        Map<String, String> headers,
        Map<String, String> queryParams,
        String clientIp,
        String userAgent,
        long timestamp
) {

    /**
     * Get request header
     */
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    /**
     * Get Authorization header
     */
    public Optional<String> getAuthorizationHeader() {
        return getHeader("Authorization");
    }

    /**
     * Get Bearer Token
     */
    public Optional<String> getBearerToken() {
        return getAuthorizationHeader()
                .filter(auth -> auth.startsWith("Bearer "))
                .map(auth -> auth.substring(7));
    }

    /**
     * Get Basic Auth
     */
    public Optional<String> getBasicAuth() {
        return getAuthorizationHeader()
                .filter(auth -> auth.startsWith("Basic "))
                .map(auth -> auth.substring(6));
    }

    /**
     * Get API Key (from Header or Query)
     */
    public Optional<String> getApiKey() {
        // First check headers
        Optional<String> headerApiKey = getHeader("X-API-Key")
                .or(() -> getHeader("Api-Key"))
                .or(() -> getHeader("apikey"));

        if (headerApiKey.isPresent()) {
            return headerApiKey;
        }

        // Then check query parameters
        return Optional.ofNullable(queryParams.get("apikey"))
                .or(() -> Optional.ofNullable(queryParams.get("api_key")))
                .or(() -> Optional.ofNullable(queryParams.get("key")));
    }

    /**
     * Get query parameter
     */
    public Optional<String> getQueryParam(String name) {
        return Optional.ofNullable(queryParams.get(name));
    }

    /**
     * Check if request contains specific header
     */
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    /**
     * Check if path matches pattern
     */
    public boolean pathMatches(String pattern) {
        return path.matches(pattern);
    }

    /**
     * Get client identifier (IP + User-Agent)
     */
    public String getClientIdentifier() {
        return clientIp + ":" + userAgent;
    }
}