package com.mo.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service Discovery Configuration Properties
 * Supports different discovery mechanisms (memory, kubernetes)
 */
@ConfigurationProperties(prefix = "gateway.discovery")
public record ServiceDiscoveryProperties(
        String type,
        KubernetesDiscoveryProperties kubernetes
) {
    public ServiceDiscoveryProperties() {
        this("memory", new KubernetesDiscoveryProperties());
    }

    /**
     * Kubernetes-specific discovery configuration
     */
    public record KubernetesDiscoveryProperties(
            String namespace,
            String labelSelector,
            int servicePort,
            String healthCheckPath
    ) {
        public KubernetesDiscoveryProperties() {
            this("services", "gateway.enabled=true", 8080, "/actuator/health");
        }
    }
}
