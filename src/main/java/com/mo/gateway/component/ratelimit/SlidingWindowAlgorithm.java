package com.mo.gateway.component.ratelimit;

import com.mo.gateway.component.ratelimit.storage.RateLimitStorage;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Sliding Window Rate Limiting Algorithm
 * Provides accurate rate limiting over a time window
 */
@Component("slidingWindow")
public class SlidingWindowAlgorithm implements RateLimitingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowAlgorithm.class);

    @Override
    public CompletableFuture<RateLimitResult> checkLimit(String key, RateLimitPolicy policy, RateLimitStorage storage) {
        var now = System.currentTimeMillis();
        var windowKey = STR."\{key}:\{now / 1000}";
        return storage.increment(windowKey, policy.windowSizeMs())
                .thenApply(currentCount -> evaluateLimit(currentCount, policy))
                .exceptionally(throwable -> {
                    log.error("Sliding window check failed for key: {}", key, throwable);
                    return RateLimitResult.allowed(policy.capacity(), policy.capacity());
                });
    }

    private RateLimitResult evaluateLimit(long currentCount, RateLimitPolicy policy) {
        if (currentCount <= policy.capacity()) {
            return RateLimitResult.allowed(policy.capacity() - currentCount, policy.capacity());
        } else {
            return RateLimitResult.rejected(0, policy.capacity(), 1000); // Retry after 1 second
        }
    }
}
