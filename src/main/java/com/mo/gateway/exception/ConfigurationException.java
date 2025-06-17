package com.mo.gateway.exception;

/**
 * Exception thrown when gateway configuration is invalid
 */
public class ConfigurationException extends GatewayException {

    private final String configurationKey;

    public ConfigurationException(String message) {
        super(message, "CONFIGURATION_ERROR", 500);
        this.configurationKey = null;
    }

    public ConfigurationException(String message, String configurationKey) {
        super(message, "CONFIGURATION_ERROR", 500);
        this.configurationKey = configurationKey;
    }

    public ConfigurationException(String message, String configurationKey, Throwable cause) {
        super(message, "CONFIGURATION_ERROR", 500, cause);
        this.configurationKey = configurationKey;
    }

    public String getConfigurationKey() {
        return configurationKey;
    }
}
