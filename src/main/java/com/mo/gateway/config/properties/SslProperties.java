package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SSL Configuration Properties
 * Using Records for immutable SSL configuration
 */
@ConfigurationProperties(prefix = "server.ssl")
public record SslProperties(
        boolean enabled,
        String keyStore,
        String keyStorePassword,
        String keyStoreType,
        String keyAlias,
        String trustStore,
        String trustStorePassword,
        String protocol,
        String[] ciphers,
        String[] protocols
) {
    public SslProperties() {
        this(false, null, null, "PKCS12", null, null, null, "TLS", null, null);
    }
}

