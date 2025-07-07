package com.mo.gateway.service.auth;

import com.mo.gateway.component.auth.plugin.AuthPluginManager;
import com.mo.gateway.model.auth.AuthenticationRequest;
import com.mo.gateway.model.auth.AuthenticationResult;
import com.mo.gateway.model.dto.GatewayRequest;
import com.mo.gateway.spi.auth.AuthenticationContext;
import com.mo.gateway.spi.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Authentication Service
 * Coordinates multiple authentication providers and manages authentication flow
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthPluginManager pluginManager;

    private final RedisTemplate<String, Object> redisTemplate;

    private final AuthContextFactory contextFactory;

    public AuthenticationService(AuthPluginManager pluginManager, RedisTemplate<String, Object> redisTemplate) {
        this.pluginManager = pluginManager;
        this.redisTemplate = redisTemplate;
        this.contextFactory = new AuthContextFactory(redisTemplate);
    }

    /**
     * Perform authentication
     *
     * @param gatewayRequest Original gateway request
     * @return Authentication result
     */
    public CompletableFuture<AuthenticationResult> authenticate(GatewayRequest gatewayRequest) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                AuthenticationRequest authRequest = buildAuthenticationRequest(gatewayRequest);
                AuthenticationContext context = contextFactory.createContext(gatewayRequest);
                List<AuthenticationProvider> providers = getApplicableProviders(authRequest);
                if (providers.isEmpty()) {
                    log.warn("No authentication providers found for request: {}", authRequest.path());
                    return AuthenticationResult.failure("NO_AUTH_PROVIDER", "No authentication provider available");
                }
                AuthenticationResult result = null;
                for (AuthenticationProvider provider : providers) {
                    try {
                        log.debug("Trying authentication with provider: {}", provider.getProviderName());
                        result = provider.authenticate(authRequest, context);
                        if (result.success()) {
                            log.info("Authentication successful with provider: {}", provider.getProviderName());
                            break;
                        } else {
                            log.debug("Authentication failed with provider: {}, reason: {}",
                                    provider.getProviderName(), result.errorMessage());
                        }
                    } catch (Exception e) {
                        log.error("Error during authentication with provider: {}", provider.getProviderName(), e);
                    }
                }
                if (result == null) {
                    result = AuthenticationResult.failure("AUTH_FAILED", "Authentication failed with all providers");
                }
                if (result.success()) {
                    cacheAuthenticationResult(authRequest, result);
                }
                long processingTime = System.currentTimeMillis() - startTime;
                log.debug("Authentication completed in {}ms", processingTime);
                return result;
            } catch (Exception e) {
                log.error("Unexpected error during authentication", e);
                long processingTime = System.currentTimeMillis() - startTime;
                log.debug("Authentication failed in {}ms", processingTime);
                return AuthenticationResult.failure("INTERNAL_ERROR", "Internal authentication error");
            }
        });
    }

    /**
     * Check if authentication is required for the given request
     *
     * @param gatewayRequest Gateway request
     * @return Whether authentication is required
     */
    public boolean isAuthenticationRequired(GatewayRequest gatewayRequest) {
        String path = gatewayRequest.path();
        if (path.startsWith("/health") ||
                path.startsWith("/metrics") ||
                path.startsWith("/actuator")) {
            return false;
        }
        // TODO: Can be configured through application properties
        return true;
    }

    /**
     * Convert gateway request to authentication request
     */
    private AuthenticationRequest buildAuthenticationRequest(GatewayRequest gatewayRequest) {
        Map<String, String> headers = gatewayRequest.headers() != null ?
                new HashMap<>(gatewayRequest.headers()) : new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        return new AuthenticationRequest(
                gatewayRequest.path(),
                gatewayRequest.method(),
                headers,
                queryParams,
                gatewayRequest.getClientIdentifier(),
                headers.getOrDefault("User-Agent", "unknown"),
                gatewayRequest.timestamp()
        );
    }

    /**
     * Get applicable authentication providers
     */
    private List<AuthenticationProvider> getApplicableProviders(AuthenticationRequest authRequest) {
        List<AuthenticationProvider> applicable = new ArrayList<>();
        for (AuthenticationProvider provider : pluginManager.getEnabledProviders()) {
            try {
                if (provider.supports(authRequest)) {
                    applicable.add(provider);
                }
            } catch (Exception e) {
                log.error("Error checking provider support: {}", provider.getProviderName(), e);
            }
        }
        applicable.sort((a, b) -> a.getProviderName().compareTo(b.getProviderName()));
        return applicable;
    }

    /**
     * Cache authentication result
     */
    private void cacheAuthenticationResult(AuthenticationRequest authRequest, AuthenticationResult result) {
        try {
            String cacheKey = generateCacheKey(authRequest);
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(30));
            log.debug("Cached authentication result for: {}", authRequest.path());
        } catch (Exception e) {
            log.error("Error caching authentication result", e);
        }
    }

    /**
     * Generate cache key
     */
    private String generateCacheKey(GatewayRequest gatewayRequest) {
        return "auth:" + gatewayRequest.getClientIdentifier() + ":" + gatewayRequest.path();
    }

    private String generateCacheKey(AuthenticationRequest authRequest) {
        return "auth:" + authRequest.getClientIdentifier() + ":" + authRequest.path();
    }

    /**
     * Get authentication statistics
     */
    public AuthenticationStats getStats() {
        List<AuthenticationProvider> allProviders = pluginManager.getAllProviders();
        List<AuthenticationProvider> enabledProviders = new ArrayList<>(pluginManager.getEnabledProviders());
        return new AuthenticationStats(
                allProviders.size(),
                enabledProviders.size(),
                enabledProviders.stream().map(AuthenticationProvider::getProviderName).toList()
        );
    }

    /**
     * Authentication statistics record
     */
    public record AuthenticationStats(
            int totalProviders,
            int enabledProviders,
            List<String> enabledProviderNames
    ) {
    }
}