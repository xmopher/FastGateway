package com.mo.gateway.component.ratelimit;

import com.mo.gateway.component.ratelimit.storage.RateLimitStorage;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitResult;

import java.util.concurrent.CompletableFuture;

/**
 * Rate Limiting Algorithm Interface
 * Defines contract for different rate limiting strategies
 */
public interface RateLimitingAlgorithm {
    /**
     * Check if request should be allowed based on rate limiting policy
     *
     * @param key Unique key for rate limiting (client + resource)
     * @param policy Rate limiting policy to apply
     * @param storage Storage backend for rate limiting state
     * @return CompletableFuture with rate limit result
     */
    CompletableFuture<RateLimitResult> checkLimit(String key, RateLimitPolicy policy, RateLimitStorage storage);
}