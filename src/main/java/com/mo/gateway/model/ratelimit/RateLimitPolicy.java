package com.mo.gateway.model.ratelimit;

import java.util.List;

/**
 * Rate Limit Policy
 */
public record RateLimitPolicy(
        String name,
        long capacity,
        long refillRate,
        long windowSizeMs,
        String algorithmType,
        int requestCost,
        List<RateLimitRule> rules
) {
    /**
     * Compact constructor with defaults
     */
    public RateLimitPolicy {
        if (algorithmType == null) algorithmType = "tokenBucket";
        if (requestCost <= 0) requestCost = 1;
    }

    /**
     * Static factory method for builder pattern
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for fluent construction
     */
    public static class Builder {
        private String name;
        private long capacity;
        private long refillRate;
        private long windowSizeMs;
        private String algorithmType = "tokenBucket";
        private int requestCost = 1;
        private List<RateLimitRule> rules;
        public Builder name(String name) { this.name = name; return this; }
        public Builder capacity(long capacity) { this.capacity = capacity; return this; }
        public Builder refillRate(long refillRate) { this.refillRate = refillRate; return this; }
        public Builder windowSizeMs(long windowSizeMs) { this.windowSizeMs = windowSizeMs; return this; }
        public Builder algorithmType(String algorithmType) { this.algorithmType = algorithmType; return this; }
        public Builder requestCost(int requestCost) { this.requestCost = requestCost; return this; }
        public Builder rules(List<RateLimitRule> rules) { this.rules = rules; return this; }
        public RateLimitPolicy build() {
            return new RateLimitPolicy(name, capacity, refillRate, windowSizeMs, algorithmType, requestCost, rules);
        }
    }
}