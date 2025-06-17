package com.mo.gateway.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Kubernetes Client Configuration
 * Provides KubernetesClient bean for service discovery
 */
@Configuration
@ConditionalOnProperty(name = "gateway.discovery.type", havingValue = "kubernetes")
public class KubernetesClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfiguration.class);

    private final Environment environment;

    public KubernetesClientConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * Creates KubernetesClient bean
     * Automatically detects if running inside Kubernetes cluster or uses local config
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            Config config = createKubernetesConfig();
            var client = new KubernetesClientBuilder().withConfig(config).build();
            log.info("Kubernetes client initialized successfully");
            log.debug("Kubernetes API server: {}", config.getMasterUrl());
            log.debug("Kubernetes namespace: {}", config.getNamespace());
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes client", e);
            throw new RuntimeException("Kubernetes client initialization failed", e);
        }
    }

    /**
     * Creates Kubernetes configuration
     * Supports both in-cluster and external configurations
     */
    private Config createKubernetesConfig() {
        var configBuilder = new ConfigBuilder();
        // Check if running inside Kubernetes cluster
        if (isRunningInKubernetes()) {
            log.info("Detected Kubernetes cluster environment, using in-cluster config");
            return configBuilder.build();
        }
        // External configuration
        log.info("Using external Kubernetes configuration");
        // Allow configuration via environment variables or properties
        var masterUrl = environment.getProperty("kubernetes.master.url");
        var token = environment.getProperty("kubernetes.auth.token");
        var namespace = environment.getProperty("kubernetes.namespace", "default");
        var caCertFile = environment.getProperty("kubernetes.ca.cert.file");
        var trustCerts = environment.getProperty("kubernetes.trust.certs", Boolean.class, false);
        if (masterUrl != null) {
            configBuilder.withMasterUrl(masterUrl);
        }
        if (token != null) {
            configBuilder.withOauthToken(token);
        }
        if (namespace != null) {
            configBuilder.withNamespace(namespace);
        }
        if (caCertFile != null) {
            configBuilder.withCaCertFile(caCertFile);
        }
        configBuilder.withTrustCerts(trustCerts);
        return configBuilder.build();
    }

    /**
     * Detect if running inside Kubernetes cluster
     */
    private boolean isRunningInKubernetes() {
        // Check for service account token file (standard in Kubernetes pods)
        var tokenFile = new java.io.File("/var/run/secrets/kubernetes.io/serviceaccount/token");
        var namespaceFile = new java.io.File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        return tokenFile.exists() && namespaceFile.exists();
    }

    /**
     * Fallback configuration for development environments
     */
    @Bean
    @ConditionalOnProperty(name = "gateway.discovery.type", havingValue = "kubernetes-mock")
    public KubernetesClient mockKubernetesClient() {
        log.warn("Using mock Kubernetes client for development");
        // Create a mock client for development/testing
        var config = new ConfigBuilder()
                .withMasterUrl("https://mock-k8s-api:6443")
                .withNamespace("default")
                .withTrustCerts(true)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
