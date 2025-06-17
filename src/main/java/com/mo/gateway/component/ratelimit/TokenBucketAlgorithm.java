package com.mo.gateway.component.ratelimit;

import com.mo.gateway.component.ratelimit.storage.RateLimitStorage;
import com.mo.gateway.model.ratelimit.RateLimitBucket;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Token Bucket Rate Limiting Algorithm
 * Allows burst traffic up to bucket capacity with steady refill rate
 */
@Component("tokenBucket")
public class TokenBucketAlgorithm implements RateLimitingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketAlgorithm.class);

    @Override
    public CompletableFuture<RateLimitResult> checkLimit(String key, RateLimitPolicy policy, RateLimitStorage storage) {
        return storage.getBucket(key)
                .thenCompose(bucket -> processTokenBucket(key, bucket, policy, storage))
                .exceptionally(throwable -> {
                    log.error("Token bucket check failed for key: {}", key, throwable);
                    return RateLimitResult.allowed(policy.capacity(), policy.capacity());
                });
    }

    private CompletableFuture<RateLimitResult> processTokenBucket(
            String key, RateLimitBucket bucket, RateLimitPolicy policy, RateLimitStorage storage) {
        var now = System.currentTimeMillis();
        var updatedBucket = refillBucket(bucket, policy, now);
        if (hasEnoughTokens(updatedBucket, policy)) {
            var consumedBucket = updatedBucket.consumeTokens(policy.requestCost());
            return storage.setBucket(key, consumedBucket, policy.windowSizeMs())
                    .thenApply(v -> RateLimitResult.allowed(consumedBucket.tokens(), policy.capacity()));
        } else {
            return storage.setBucket(key, updatedBucket, policy.windowSizeMs())
                    .thenApply(v -> RateLimitResult.rejected(
                            updatedBucket.tokens(),
                            policy.capacity(),
                            calculateRetryAfter(updatedBucket, policy)
                    ));
        }
    }

    private boolean hasEnoughTokens(RateLimitBucket bucket, RateLimitPolicy policy) {
        return bucket.tokens() >= policy.requestCost();
    }

    private RateLimitBucket refillBucket(RateLimitBucket bucket, RateLimitPolicy policy, long now) {
        if (bucket == null) {
            return new RateLimitBucket(policy.capacity(), now);
        }
        var timePassed = now - bucket.lastRefill();
        var tokensToAdd = (timePassed * policy.refillRate()) / 1000;
        var newTokens = Math.min(policy.capacity(), bucket.tokens() + tokensToAdd);
        return new RateLimitBucket(newTokens, now);
    }

    private long calculateRetryAfter(RateLimitBucket bucket, RateLimitPolicy policy) {
        if (bucket.tokens() >= policy.requestCost()) {
            return 0;
        }
        var tokensNeeded = policy.requestCost() - bucket.tokens();
        return (tokensNeeded * 1000) / policy.refillRate();
    }
}
