package com.mo.gateway.spi.auth;

import com.mo.gateway.model.dto.GatewayRequest;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Context
 * Provides access to gateway capabilities for authentication providers
 */
public class AuthenticationContext {

    /**
     * -- GETTER --
     *  Get the original gateway request
     */
    @Getter
    private final GatewayRequest request;
    private final CacheManager cacheManager;
    private final ConfigManager configManager;
    private final LoggerManager loggerManager;
    private final Map<String, Object> attributes;

    public AuthenticationContext(GatewayRequest request,
                                 CacheManager cacheManager,
                                 ConfigManager configManager,
                                 LoggerManager loggerManager,
                                 Map<String, Object> attributes) {
        this.request = request;
        this.cacheManager = cacheManager;
        this.configManager = configManager;
        this.loggerManager = loggerManager;
        this.attributes = attributes;
    }

    /**
     * Get configuration value
     */
    public <T> Optional<T> getConfig(String key, Class<T> type) {
        return configManager.get(key, type);
    }

    /**
     * Get configuration value with default
     */
    public <T> T getConfig(String key, Class<T> type, T defaultValue) {
        return configManager.get(key, type).orElse(defaultValue);
    }

    /**
     * Cache operations
     */
    public void putCache(String key, Object value, Duration ttl) {
        cacheManager.put(key, value, ttl);
    }

    public <T> Optional<T> getCache(String key, Class<T> type) {
        return cacheManager.get(key, type);
    }

    public void removeCache(String key) {
        cacheManager.remove(key);
    }

    /**
     * Logging operations
     */
    public void info(String message, Object... args) {
        loggerManager.info(message, args);
    }

    public void warn(String message, Object... args) {
        loggerManager.warn(message, args);
    }

    public void error(String message, Object... args) {
        loggerManager.error(message, args);
    }

    public void error(String message, Throwable throwable, Object... args) {
        loggerManager.error(message, throwable, args);
    }

    /**
     * Attribute operations
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * Cache Manager Interface
     */
    public interface CacheManager {
        void put(String key, Object value, Duration ttl);
        <T> Optional<T> get(String key, Class<T> type);
        void remove(String key);
        void clear();
    }

    /**
     * Configuration Manager Interface
     */
    public interface ConfigManager {
        <T> Optional<T> get(String key, Class<T> type);
        boolean containsKey(String key);
    }

    /**
     * Logger Manager Interface
     */
    public interface LoggerManager {
        void info(String message, Object... args);
        void warn(String message, Object... args);
        void error(String message, Object... args);
        void error(String message, Throwable throwable, Object... args);
    }
}