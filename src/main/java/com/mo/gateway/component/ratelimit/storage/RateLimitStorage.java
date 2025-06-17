package com.mo.gateway.component.ratelimit.storage;

import com.mo.gateway.model.ratelimit.RateLimitBucket;

import java.util.concurrent.CompletableFuture;

/**
 * Rate Limit Storage Interface
 * Abstraction for different storage backends (Redis, In-Memory, etc.)
 */
public interface RateLimitStorage {

    /**
     * Increment counter and return new value
     */
    CompletableFuture<Long> increment(String key, long expiration);

    /**
     * Get rate limit bucket
     */
    CompletableFuture<RateLimitBucket> getBucket(String key);

    /**
     * Set rate limit bucket with TTL
     */
    CompletableFuture<Void> setBucket(String key, RateLimitBucket bucket, long ttl);

    /**
     * Set bucket only if it doesn't exist
     */
    CompletableFuture<Boolean> setIfNotExists(String key, RateLimitBucket bucket, long ttl);
}
