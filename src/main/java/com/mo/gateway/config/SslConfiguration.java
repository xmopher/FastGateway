package com.mo.gateway.config;

import com.mo.gateway.config.properties.SslProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SSL Configuration for HTTPS support
 *
 * Enables SSL when server.ssl.enabled=true
 * Supports HTTP to HTTPS redirect
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class SslConfiguration {

    private final SslProperties sslProperties;

    public SslConfiguration(SslProperties sslProperties) {
        this.sslProperties = sslProperties;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setScheme("https");
                connector.setSecure(true);
                connector.setPort(8443);
            });
        };
    }

    /**
     * Optional HTTP to HTTPS redirect connector
     */
    @Bean
    @ConditionalOnProperty(name = "server.ssl.redirect-http", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirect() {
        return factory -> {
            factory.addAdditionalTomcatConnectors(createHttpConnector());
        };
    }

    private org.apache.catalina.connector.Connector createHttpConnector() {
        var connector = new org.apache.catalina.connector.Connector(
                "org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
