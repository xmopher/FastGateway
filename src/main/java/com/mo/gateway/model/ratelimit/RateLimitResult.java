package com.mo.gateway.model.ratelimit;

/**
 * Immutable result of rate limiting check
 */
public record RateLimitResult(
        boolean allowed,
        long remaining,
        long limit,
        long retryAfterMs,
        String reason
) {
    /**
     * Factory method for successful rate limit check
     */
    public static RateLimitResult allowed(long remaining, long limit) {
        return new RateLimitResult(true, remaining, limit, 0, null);
    }

    /**
     * Factory method for rejected rate limit check
     */
    public static RateLimitResult rejected(long remaining, long limit, long retryAfterMs) {
        return new RateLimitResult(false, remaining, limit, retryAfterMs, "Rate limit exceeded");
    }

    /**
     * Factory method for rejected with custom reason
     */
    public static RateLimitResult rejected(long remaining, long limit, long retryAfterMs, String reason) {
        return new RateLimitResult(false, remaining, limit, retryAfterMs, reason);
    }

    /**
     * Check if request should be retried
     */
    public boolean shouldRetry() {
        return !allowed && retryAfterMs > 0;
    }
}
