package com.mo.gateway.service.ratelimit;

import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for resolving rate limiting policies based on request context
 * Allows for flexible policy determination based on client, resource, and other factors
 */
public interface RateLimitPolicyResolver {
    /**
     * Resolve the appropriate rate limiting policy for the given request
     *
     * @param request The rate limit request containing client and resource information
     * @return CompletableFuture containing the resolved policy
     */
    CompletableFuture<RateLimitPolicy> resolvePolicy(RateLimitRequest request);

    /**
     * Get the default rate limiting policy
     *
     * @return Default rate limiting policy
     */
    RateLimitPolicy getDefaultPolicy();

    /**
     * Check if a specific policy exists for the given client
     *
     * @param clientId The client identifier
     * @return true if a specific policy exists, false otherwise
     */
    boolean hasCustomPolicy(String clientId);

    /**
     * Refresh policy cache (if applicable)
     */
    default void refreshPolicies() {
    }
}
