package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for rate limit policies
 * Allows external configuration of rate limiting policies via application.yml
 */
@ConfigurationProperties(prefix = "gateway.ratelimit.policies")
public record RateLimitPolicyProperties(
        Map<String, PolicyConfig> policies,
        List<PolicyRule> rules
) {
    /**
     * Default constructor with empty collections
     */
    public RateLimitPolicyProperties() {
        this(Map.of(), List.of());
    }

    /**
     * Configuration record for policy rules
     * Defines which policy to apply based on client, resource, and method patterns
     */
    public record PolicyRule(
            String clientPattern,
            String resourcePattern,
            String methodPattern,
            String policyName,
            int priority
    ) {
        /**
         * Default constructor
         */
        public PolicyRule() {
            this(null, null, null, "default", 0);
        }

        /**
         * Convenience constructor for client-based rules
         */
        public static PolicyRule forClient(String clientPattern, String policyName, int priority) {
            return new PolicyRule(clientPattern, null, null, policyName, priority);
        }

        /**
         * Convenience constructor for resource-based rules
         */
        public static PolicyRule forResource(String resourcePattern, String policyName, int priority) {
            return new PolicyRule(null, resourcePattern, null, policyName, priority);
        }

        /**
         * Convenience constructor for method-based rules
         */
        public static PolicyRule forMethod(String methodPattern, String policyName, int priority) {
            return new PolicyRule(null, null, methodPattern, policyName, priority);
        }
    }

    /**
     * Configuration record for individual policies
     * Defines the rate limiting parameters for a specific policy
     */
    public record PolicyConfig(
            long capacity,
            long refillRate,
            long windowSizeMs,
            String algorithmType,
            int requestCost
    ) {
        /**
         * Default constructor with basic limits
         */
        public PolicyConfig() {
            this(100, 10, 60000, "tokenBucket", 1);
        }

        /**
         * Constructor with validation
         */
        public PolicyConfig {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            if (refillRate <= 0) {
                throw new IllegalArgumentException("Refill rate must be positive");
            }
            if (windowSizeMs <= 0) {
                throw new IllegalArgumentException("Window size must be positive");
            }
            if (requestCost <= 0) {
                throw new IllegalArgumentException("Request cost must be positive");
            }
            if (algorithmType == null || algorithmType.trim().isEmpty()) {
                algorithmType = "tokenBucket";
            }
        }

        /**
         * Create a high-capacity policy
         */
        public static PolicyConfig premium() {
            return new PolicyConfig(10000, 1000, 60000, "tokenBucket", 1);
        }

        /**
         * Create a basic policy
         */
        public static PolicyConfig basic() {
            return new PolicyConfig(1000, 100, 60000, "tokenBucket", 1);
        }

        /**
         * Create a free tier policy
         */
        public static PolicyConfig free() {
            return new PolicyConfig(100, 10, 60000, "tokenBucket", 1);
        }

        /**
         * Create an admin policy (high limits)
         */
        public static PolicyConfig admin() {
            return new PolicyConfig(50000, 5000, 60000, "tokenBucket", 1);
        }

        /**
         * Create a strict policy for sensitive operations
         */
        public static PolicyConfig strict() {
            return new PolicyConfig(20, 2, 300000, "tokenBucket", 1);
        }
    }
}
