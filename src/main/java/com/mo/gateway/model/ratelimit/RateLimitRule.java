package com.mo.gateway.model.ratelimit;

import lombok.Builder;

/**
 * Rate Limit Rule
 */
@Builder
public record RateLimitRule(
        String clientPattern,
        String resourcePattern,
        String methodPattern,
        RateLimitPolicy policy,
        int priority
) {
    /**
     * Check if this rule matches the given request
     */
    public boolean matches(RateLimitRequest request) {
        return matchesClient(request.clientId()) &&
                matchesResource(request.resource()) &&
                matchesMethod(request.method());
    }

    private boolean matchesClient(String clientId) {
        return clientPattern == null ||
                (clientId != null && clientId.matches(clientPattern));
    }

    private boolean matchesResource(String resource) {
        return resourcePattern == null ||
                (resource != null && resource.matches(resourcePattern));
    }

    private boolean matchesMethod(String method) {
        return methodPattern == null ||
                (method != null && method.matches(methodPattern));
    }
}
