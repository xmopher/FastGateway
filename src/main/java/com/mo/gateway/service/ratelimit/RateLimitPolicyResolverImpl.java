package com.mo.gateway.service.ratelimit;

import com.mo.gateway.config.properties.RateLimitProperties;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitRequest;
import com.mo.gateway.model.ratelimit.RateLimitRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of RateLimitPolicyResolver
 * Provides rule-based policy resolution with caching and fallback to default policies
 */
@Service
public class RateLimitPolicyResolverImpl implements RateLimitPolicyResolver {

    private static final Logger log = LoggerFactory.getLogger(RateLimitPolicyResolverImpl.class);

    private final RateLimitProperties properties;

    private final Map<String, RateLimitPolicy> policyCache = new ConcurrentHashMap<>();

    private final List<RateLimitRule> rules;

    public RateLimitPolicyResolverImpl(RateLimitProperties properties) {
        this.properties = properties;
        this.rules = initializeDefaultRules();
        initializePredefinedPolicies();
    }

    @Override
    public CompletableFuture<RateLimitPolicy> resolvePolicy(RateLimitRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var matchingRule = findMatchingRule(request);
                if (matchingRule != null) {
                    log.debug("Found matching rule for client: {} resource: {}",
                            request.clientId(), request.resource());
                    return matchingRule.policy();
                }
                var clientPolicy = getClientSpecificPolicy(request.clientId());
                if (clientPolicy != null) {
                    log.debug("Using client-specific policy for: {}", request.clientId());
                    return clientPolicy;
                }
                var resourcePolicy = getResourceSpecificPolicy(request.resource());
                if (resourcePolicy != null) {
                    log.debug("Using resource-specific policy for: {}", request.resource());
                    return resourcePolicy;
                }
                log.debug("Using default policy for client: {} resource: {}",
                        request.clientId(), request.resource());
                return getDefaultPolicy();
            } catch (Exception e) {
                log.error("Error resolving rate limit policy, using default", e);
                return getDefaultPolicy();
            }
        });
    }

    @Override
    public RateLimitPolicy getDefaultPolicy() {
        return policyCache.computeIfAbsent("default", k ->
                RateLimitPolicy.builder()
                        .name("default")
                        .capacity(properties.defaultCapacity())
                        .refillRate(properties.defaultRefillRate())
                        .windowSizeMs(properties.defaultWindowSizeMs())
                        .algorithmType(properties.defaultAlgorithm())
                        .requestCost(1)
                        .rules(List.of())
                        .build()
        );
    }

    @Override
    public boolean hasCustomPolicy(String clientId) {
        return policyCache.containsKey(STR."client:\{clientId}") ||
                rules.stream().anyMatch(rule -> rule.matches(
                        RateLimitRequest.builder()
                                .clientId(clientId)
                                .resource("")
                                .build()));
    }

    @Override
    public void refreshPolicies() {
        log.info("Refreshing rate limit policies");
        policyCache.clear();
        initializePredefinedPolicies();
    }

    /**
     * Find the first matching rule for the request
     */
    private RateLimitRule findMatchingRule(RateLimitRequest request) {
        return rules.stream()
                .filter(rule -> rule.matches(request))
                .min((r1, r2) -> Integer.compare(r2.priority(), r1.priority())) // Higher priority first
                .orElse(null);
    }

    /**
     * Get client-specific policy
     */
    private RateLimitPolicy getClientSpecificPolicy(String clientId) {
        if (clientId == null) return null;
        return policyCache.computeIfAbsent(STR."client:\{clientId}", k -> {
            return switch (getClientType(clientId)) {
                case "premium" -> createPremiumPolicy();
                case "basic" -> createBasicPolicy();
                case "free" -> createFreePolicy();
                case "admin" -> createAdminPolicy();
                default -> null;
            };
        });
    }

    /**
     * Get resource-specific policy
     */
    private RateLimitPolicy getResourceSpecificPolicy(String resource) {
        if (resource == null) return null;
        return policyCache.computeIfAbsent(STR."resource:\{resource}", k -> {
            if (resource.contains("/admin/")) {
                return createAdminResourcePolicy();
            } else if (resource.contains("/api/upload/")) {
                return createUploadPolicy();
            } else if (resource.contains("/api/auth/")) {
                return createAuthPolicy();
            }
            return null;
        });
    }

    /**
     * Determine client type from client ID
     */
    private String getClientType(String clientId) {
        if (clientId == null) return "anonymous";
        return switch (clientId) {
            case String id when id.startsWith("premium-") -> "premium";
            case String id when id.startsWith("admin-") -> "admin";
            case String id when id.startsWith("basic-") -> "basic";
            case String id when id.startsWith("free-") -> "free";
            case String id when id.contains("@") -> "user"; // Email-based
            case "anonymous" -> "anonymous";
            default -> "basic";
        };
    }

    /**
     * Initialize predefined policies
     */
    private void initializePredefinedPolicies() {
        policyCache.put("premium", createPremiumPolicy());
        policyCache.put("basic", createBasicPolicy());
        policyCache.put("free", createFreePolicy());
        policyCache.put("admin", createAdminPolicy());
        policyCache.put("upload", createUploadPolicy());
        policyCache.put("auth", createAuthPolicy());
    }

    /**
     * Initialize default rules
     */
    private List<RateLimitRule> initializeDefaultRules() {
        return List.of(
                new RateLimitRule("admin-.*", ".*", ".*", createAdminPolicy(), 100),
                new RateLimitRule("premium-.*", ".*", ".*", createPremiumPolicy(), 90),
                new RateLimitRule(".*", ".*/auth/.*", "POST", createAuthPolicy(), 80),
                new RateLimitRule(".*", ".*/upload/.*", "POST|PUT", createUploadPolicy(), 70),
                new RateLimitRule("free-.*", ".*", ".*", createFreePolicy(), 60)
        );
    }

    private RateLimitPolicy createPremiumPolicy() {
        return RateLimitPolicy.builder()
                .name("premium")
                .capacity(10000)
                .refillRate(1000)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }

    private RateLimitPolicy createBasicPolicy() {
        return RateLimitPolicy.builder()
                .name("basic")
                .capacity(1000)
                .refillRate(100)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }

    private RateLimitPolicy createFreePolicy() {
        return RateLimitPolicy.builder()
                .name("free")
                .capacity(100)
                .refillRate(10)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }

    private RateLimitPolicy createAdminPolicy() {
        return RateLimitPolicy.builder()
                .name("admin")
                .capacity(50000)
                .refillRate(5000)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }

    private RateLimitPolicy createUploadPolicy() {
        return RateLimitPolicy.builder()
                .name("upload")
                .capacity(10)
                .refillRate(1)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(5)
                .build();
    }

    private RateLimitPolicy createAuthPolicy() {
        return RateLimitPolicy.builder()
                .name("auth")
                .capacity(20)
                .refillRate(2)
                .windowSizeMs(300000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }

    private RateLimitPolicy createAdminResourcePolicy() {
        return RateLimitPolicy.builder()
                .name("admin-resource")
                .capacity(1000)
                .refillRate(100)
                .windowSizeMs(60000)
                .algorithmType("tokenBucket")
                .requestCost(1)
                .build();
    }
}
