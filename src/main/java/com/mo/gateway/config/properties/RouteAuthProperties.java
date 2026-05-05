package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.auth")
public record RouteAuthProperties(List<RouteRule> routeRules) {

    public RouteAuthProperties() {
        this(List.of());
    }

    public record RouteRule(String path, String plugin) {}
}
