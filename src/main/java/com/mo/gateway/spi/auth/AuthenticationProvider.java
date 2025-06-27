package com.mo.gateway.spi.auth;

import com.mo.gateway.model.auth.AuthenticationRequest;
import com.mo.gateway.model.auth.AuthenticationResult;

import java.util.Properties;

/**
 * Authentication Provider SPI Interface
 * Third-party developers need to implement this interface to provide custom authentication logic
 */
public interface AuthenticationProvider {

    /**
     * Get provider name for identification and configuration
     * @return Unique provider name
     */
    String getProviderName();

    /**
     * Get provider version
     * @return Version information
     */
    String getVersion();

    /**
     * Check if this provider supports the given authentication request
     * @param request Authentication request
     * @return Whether this provider can handle the request
     */
    boolean supports(AuthenticationRequest request);

    /**
     * Execute authentication logic
     * @param request Authentication request
     * @param context Authentication context
     * @return Authentication result
     */
    AuthenticationResult authenticate(AuthenticationRequest request, AuthenticationContext context);

    /**
     * Initialize method called when plugin is loaded
     * @param config Configuration parameters
     */
    default void initialize(Properties config) {
        // Default empty implementation
    }

    /**
     * Destroy method called when plugin is unloaded
     */
    default void destroy() {
        // Default empty implementation
    }

    /**
     * Health check for the provider
     * @return Provider health status
     */
    default HealthStatus health() {
        return HealthStatus.UP;
    }

    /**
     * Health status enumeration
     */
    enum HealthStatus {
        UP, DOWN, UNKNOWN
    }
}