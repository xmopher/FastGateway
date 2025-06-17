package com.mo.gateway.model.ratelimit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rate Limit Bucket for Token Bucket algorithm
 * Using class instead of record due to JSON serialization requirements
 */
public final class RateLimitBucket {
    private final long tokens;

    private final long lastRefill;

    @JsonCreator
    public RateLimitBucket(@JsonProperty("tokens") long tokens,
                           @JsonProperty("lastRefill") long lastRefill) {
        this.tokens = tokens;
        this.lastRefill = lastRefill;
    }

    public long tokens() {
        return tokens;
    }

    public long lastRefill() {
        return lastRefill;
    }

    /**
     * Create new bucket with consumed tokens
     */
    public RateLimitBucket consumeTokens(long tokensToConsume) {
        return new RateLimitBucket(tokens - tokensToConsume, lastRefill);
    }

    /**
     * Check if bucket has enough tokens
     */
    public boolean hasTokens(long requiredTokens) {
        return tokens >= requiredTokens;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RateLimitBucket bucket &&
                tokens == bucket.tokens &&
                lastRefill == bucket.lastRefill;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(tokens) ^ Long.hashCode(lastRefill);
    }

    @Override
    public String toString() {
        return STR."RateLimitBucket[tokens=\{tokens}, lastRefill=\{lastRefill}]";
    }
}
