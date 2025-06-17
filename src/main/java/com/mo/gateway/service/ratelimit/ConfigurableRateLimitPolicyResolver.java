package com.mo.gateway.service.ratelimit;

import com.mo.gateway.config.properties.RateLimitPolicyProperties;
import com.mo.gateway.config.properties.RateLimitProperties;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configurable Rate Limit Policy Resolver
 * Uses externalized configuration for policy management
 */
@Service
@ConditionalOnProperty(name = "gateway.ratelimit.resolver.type", havingValue = "configurable")
public class ConfigurableRateLimitPolicyResolver implements RateLimitPolicyResolver {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableRateLimitPolicyResolver.class);

    private final RateLimitProperties properties;

    private final RateLimitPolicyProperties policyProperties;

    private final Map<String, RateLimitPolicy> configuredPolicies = new ConcurrentHashMap<>();

    public ConfigurableRateLimitPolicyResolver(RateLimitProperties properties, RateLimitPolicyProperties policyProperties) {
        this.properties = properties;
        this.policyProperties = policyProperties;
    }

    @PostConstruct
    public void initializePolicies() {
        log.info("Initializing configurable rate limit policies");
        policyProperties.policies().forEach((name, config) -> {
            var policy = RateLimitPolicy.builder()
                    .name(name)
                    .capacity(config.capacity())
                    .refillRate(config.refillRate())
                    .windowSizeMs(config.windowSizeMs())
                    .algorithmType(config.algorithmType())
                    .requestCost(config.requestCost())
                    .build();
            configuredPolicies.put(name, policy);
            log.debug("Loaded policy: {} with capacity: {}", name, config.capacity());
        });
        log.info("Loaded {} rate limit policies", configuredPolicies.size());
    }

    @Override
    public CompletableFuture<RateLimitPolicy> resolvePolicy(RateLimitRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var matchingRule = policyProperties.rules().stream()
                        .filter(rule -> matchesRule(rule, request))
                        .max((r1, r2) -> Integer.compare(r1.priority(), r2.priority())) // Highest priority first
                        .orElse(null);
                if (matchingRule != null) {
                    var policy = configuredPolicies.get(matchingRule.policyName());
                    if (policy != null) {
                        log.debug("Applied policy '{}' for client: {} resource: {}",
                                matchingRule.policyName(), request.clientId(), request.resource());
                        return policy;
                    }
                }
                log.debug("No matching rule found, using default policy");
                return getDefaultPolicy();
            } catch (Exception e) {
                log.error("Error resolving configurable rate limit policy", e);
                return getDefaultPolicy();
            }
        });
    }

    @Override
    public RateLimitPolicy getDefaultPolicy() {
        return configuredPolicies.getOrDefault("default",
                RateLimitPolicy.builder()
                        .name("default")
                        .capacity(properties.defaultCapacity())
                        .refillRate(properties.defaultRefillRate())
                        .windowSizeMs(properties.defaultWindowSizeMs())
                        .algorithmType(properties.defaultAlgorithm())
                        .requestCost(1)
                        .build());
    }

    @Override
    public boolean hasCustomPolicy(String clientId) {
        return policyProperties.rules().stream()
                .anyMatch(rule -> rule.clientPattern() != null &&
                        clientId != null &&
                        clientId.matches(rule.clientPattern()));
    }

    @Override
    public void refreshPolicies() {
        log.info("Refreshing configurable rate limit policies");
        configuredPolicies.clear();
        initializePolicies();
    }

    private boolean matchesRule(RateLimitPolicyProperties.PolicyRule rule, RateLimitRequest request) {
        return matchesPattern(rule.clientPattern(), request.clientId()) &&
                matchesPattern(rule.resourcePattern(), request.resource()) &&
                matchesPattern(rule.methodPattern(), request.method());
    }

    private boolean matchesPattern(String pattern, String value) {
        if (pattern == null) return true;
        if (value == null) return false;
        try {
            return value.matches(pattern);
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {}", pattern, e);
            return false;
        }
    }
}
