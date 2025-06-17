package com.mo.gateway.service.ratelimit;

import com.mo.gateway.model.ratelimit.RateLimitRequest;
import com.mo.gateway.model.ratelimit.RateLimitResult;

import java.util.concurrent.CompletableFuture;

/**
 * Rate Limiter Service Interface
 * Provides rate limiting functionality for the gateway
 */
public interface RateLimiterService {
    /**
     * Check rate limit for a client and resource
     */
    CompletableFuture<RateLimitResult> checkRateLimit(String clientId, String resource);

    /**
     * Check rate limit with full request context
     */
    CompletableFuture<RateLimitResult> checkRateLimit(RateLimitRequest request);
}
