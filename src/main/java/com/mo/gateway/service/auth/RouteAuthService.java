package com.mo.gateway.service.auth;

import com.mo.gateway.component.auth.plugin.AuthPluginManager;
import com.mo.gateway.config.properties.RouteAuthProperties;
import com.mo.gateway.model.auth.AuthenticationRequest;
import com.mo.gateway.model.auth.AuthenticationResult;
import com.mo.gateway.model.dto.GatewayRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class RouteAuthService {

    private static final Logger log = LoggerFactory.getLogger(RouteAuthService.class);
    private static final String PLUGIN_NONE = "none";

    private final RouteAuthProperties properties;
    private final AuthPluginManager pluginManager;
    private final AuthContextFactory contextFactory;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RouteAuthService(RouteAuthProperties properties,
                            AuthPluginManager pluginManager,
                            AuthContextFactory contextFactory) {
        this.properties = properties;
        this.pluginManager = pluginManager;
        this.contextFactory = contextFactory;
    }

    public CompletableFuture<Optional<AuthenticationResult>> authenticate(GatewayRequest request) {
        var ruleOpt = resolveRouteRule(request.path());

        if (ruleOpt.isEmpty()) {
            log.debug("No auth rule for path: {}, passing through", request.path());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var rule = ruleOpt.get();

        if (PLUGIN_NONE.equalsIgnoreCase(rule.plugin())) {
            log.debug("Route {} explicitly set to no-auth", rule.path());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return executeAuth(request, rule);
    }

    private Optional<RouteAuthProperties.RouteRule> resolveRouteRule(String path) {
        return properties.routeRules().stream()
                .filter(rule -> pathMatcher.match(rule.path(), path))
                .findFirst();
    }

    private CompletableFuture<Optional<AuthenticationResult>> executeAuth(
            GatewayRequest request, RouteAuthProperties.RouteRule rule) {

        var providerOpt = pluginManager.getProvider(rule.plugin());

        if (providerOpt.isEmpty()) {
            log.warn("Auth plugin '{}' required by route '{}' but not loaded or enabled",
                    rule.plugin(), rule.path());
            return CompletableFuture.completedFuture(
                    Optional.of(AuthenticationResult.failure(
                            STR."Auth plugin '\{rule.plugin()}' is not available", 503, "gateway")));
        }

        var provider = providerOpt.get();
        var authRequest = buildAuthRequest(request);
        var context = contextFactory.createContext(request);

        return CompletableFuture.supplyAsync(() -> {
            if (!provider.supports(authRequest)) {
                log.warn("Plugin '{}' does not support request to {}", rule.plugin(), request.path());
                return Optional.of(AuthenticationResult.failure(
                        "Authentication method not supported by plugin", provider.getProviderName()));
            }
            var result = provider.authenticate(authRequest, context);
            log.debug("Auth [{}] path={} success={}", result.providerName(), request.path(), result.success());
            return Optional.of(result);
        });
    }

    private AuthenticationRequest buildAuthRequest(GatewayRequest request) {
        var headers = request.headers() != null ? request.headers() : Map.<String, String>of();
        var queryParams = request.queryParams() != null ? request.queryParams() : Map.<String, String>of();
        var clientIp = headers.getOrDefault("X-Forwarded-For",
                headers.getOrDefault("X-Real-IP", "unknown"));
        var userAgent = headers.getOrDefault("User-Agent", "");
        return new AuthenticationRequest(
                request.path(), request.method(),
                headers, queryParams,
                clientIp, userAgent, request.timestamp());
    }
}
