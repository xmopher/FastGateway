package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate Limiting Configuration Properties
 */
@ConfigurationProperties(prefix = "gateway.ratelimit")
public record RateLimitProperties(
        boolean enabled,
        String defaultAlgorithm,
        long defaultCapacity,
        long defaultRefillRate,
        long defaultWindowSizeMs,
        boolean failOpen
) {
    /**
     * Default constructor with production-ready defaults
     */
    public RateLimitProperties() {
        this(true, "tokenBucket", 1000, 100, 60000, true);
    }
}
