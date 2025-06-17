package com.mo.gateway.service.ratelimit;

import com.mo.gateway.component.ratelimit.RateLimitingAlgorithm;
import com.mo.gateway.component.ratelimit.storage.RateLimitStorage;
import com.mo.gateway.config.properties.RateLimitProperties;
import com.mo.gateway.model.ratelimit.RateLimitPolicy;
import com.mo.gateway.model.ratelimit.RateLimitRequest;
import com.mo.gateway.model.ratelimit.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway Rate Limiter Implementation
 * Uses configurable algorithms and storage backends
 */
@Service
public class GatewayRateLimiterImpl implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(GatewayRateLimiterImpl.class);

    private final Map<String, RateLimitingAlgorithm> algorithms;

    private final RateLimitStorage storage;

    private final RateLimitPolicyResolver policyResolver;

    private final RateLimitProperties properties;

    public GatewayRateLimiterImpl(Map<String, RateLimitingAlgorithm> algorithms, RateLimitStorage storage,
         RateLimitPolicyResolver policyResolver, RateLimitProperties properties) {
        this.algorithms = algorithms;
        this.storage = storage;
        this.policyResolver = policyResolver;
        this.properties = properties;
    }

    @Override
    public CompletableFuture<RateLimitResult> checkRateLimit(String clientId, String resource) {
        var request = RateLimitRequest.builder()
                .clientId(clientId)
                .resource(resource)
                .build();
        return checkRateLimit(request);
    }

    @Override
    public CompletableFuture<RateLimitResult> checkRateLimit(RateLimitRequest request) {
        if (!properties.enabled()) {
            return CompletableFuture.completedFuture(
                    RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE));
        }
        return policyResolver.resolvePolicy(request)
                .thenCompose(policy -> {
                    var key = generateKey(request, policy);
                    var algorithm = algorithms.get(policy.algorithmType());
                    if (algorithm == null) {
                        log.warn("Unknown rate limiting algorithm: {}, using default", policy.algorithmType());
                        algorithm = algorithms.get(properties.defaultAlgorithm());
                    }
                    return algorithm.checkLimit(key, policy, storage);
                })
                .exceptionally(throwable -> {
                    log.error("Rate limit check failed for client: {}", request.clientId(), throwable);
                    return handleRateLimitFailure();
                });
    }

    private String generateKey(RateLimitRequest request, RateLimitPolicy policy) {
        return STR."rate_limit:\{policy.name()}:\{request.clientId()}:\{request.resource()}";
    }

    private RateLimitResult handleRateLimitFailure() {
        if (properties.failOpen()) {
            log.debug("Rate limiter failing open - allowing request");
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        } else {
            log.debug("Rate limiter failing closed - rejecting request");
            return RateLimitResult.rejected(0, 0, 60000, "Rate limiter unavailable");
        }
    }
}
