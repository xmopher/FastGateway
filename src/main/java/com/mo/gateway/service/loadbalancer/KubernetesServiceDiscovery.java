package com.mo.gateway.service.loadbalancer;

import com.mo.gateway.config.properties.ServiceDiscoveryProperties;
import com.mo.gateway.model.loadbalancer.HealthStatus;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kubernetes Service Discovery Implementation
 * Automatically discovers services running in Kubernetes cluster
 */
@org.springframework.stereotype.Service
@ConditionalOnProperty(name = "gateway.discovery.type", havingValue = "kubernetes")
public class KubernetesServiceDiscovery implements ServiceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);

    private final KubernetesClient kubernetesClient;

    private final ServiceDiscoveryProperties.KubernetesDiscoveryProperties config;

    private final Map<String, Map<String, ServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();

    public KubernetesServiceDiscovery(KubernetesClient kubernetesClient, ServiceDiscoveryProperties discoveryProperties) {
        this.kubernetesClient = kubernetesClient;
        this.config = discoveryProperties.kubernetes();
    }

    @PostConstruct
    public void initialize() {
        startServiceWatcher();
        performInitialDiscovery();
    }

    @Override
    public CompletableFuture<List<ServiceInstance>> getHealthyInstances(String serviceId) {
        return CompletableFuture.supplyAsync(() -> {
            var instances = serviceRegistry.get(serviceId);
            if (instances == null) {
                return List.of();
            }
            return instances.values().stream()
                    .filter(ServiceInstance::isHealthy)
                    .toList();
        });
    }

    private void startServiceWatcher() {
        kubernetesClient.services()
                .inNamespace(config.namespace())
                .withLabel("gateway.enabled", "true")
                .watch(new ServiceWatcher());
    }

    private void performInitialDiscovery() {
        try {
            var services = kubernetesClient.services()
                    .inNamespace(config.namespace())
                    .withLabel("gateway.enabled", "true")
                    .list();
            for (var service : services.getItems()) {
                var serviceId = extractServiceId(service);
                discoverServiceInstances(service, serviceId);
            }
            log.info("Initial service discovery completed. Found {} services", serviceRegistry.size());
        } catch (Exception e) {
            log.error("Failed to perform initial service discovery", e);
        }
    }

    private void discoverServiceInstances(io.fabric8.kubernetes.api.model.Service service, String serviceId) {
        var serviceName = service.getMetadata().getName();
        var namespace = service.getMetadata().getNamespace();
        var endpoints = kubernetesClient.endpoints()
                .inNamespace(namespace)
                .withName(serviceName)
                .get();
        if (endpoints == null || endpoints.getSubsets().isEmpty()) {
            log.debug("No endpoints found for service: {}", serviceId);
            return;
        }
        // Use Service DNS name instead of Pod IP for better network compatibility
        var serviceHost = STR."\{serviceName}.\{namespace}.svc.cluster.local";
        // Use Service port (not Endpoints port) - Service port is what clients should connect to
        var servicePort = service.getSpec().getPorts().isEmpty() 
                ? config.servicePort() 
                : service.getSpec().getPorts().get(0).getPort();
        var instances = new ConcurrentHashMap<String, ServiceInstance>();
        // Only create instance if endpoints exist (service has healthy pods)
        if (!endpoints.getSubsets().isEmpty()) {
            // Create a single instance per service (using Service DNS, not individual Pod IPs)
            var instanceId = STR."\{serviceId}-service";
            var instance = ServiceInstance.builder()
                    .id(instanceId)
                    .serviceId(serviceId)
                    .host(serviceHost)
                    .port(servicePort)
                    .protocol("http")
                    .healthStatus(HealthStatus.HEALTHY)
                    .metadata(createMetadata(namespace, serviceName, null))
                    .lastHealthCheck(System.currentTimeMillis())
                    .build();
            instances.put(instanceId, instance);
            log.debug("Discovered instance: {} -> {}:{}", serviceId, serviceHost, servicePort);
        }
        serviceRegistry.put(serviceId, instances);
    }

    private Map<String, String> createMetadata(String namespace, String serviceName, EndpointAddress address) {
        var metadata = new java.util.HashMap<String, String>();
        metadata.put("kubernetes.namespace", namespace);
        metadata.put("kubernetes.service", serviceName);
        metadata.put("discovery.type", "kubernetes");
        if (address != null && address.getTargetRef() != null) {
            metadata.put("kubernetes.pod", address.getTargetRef().getName());
        }
        return metadata;
    }

    private String extractServiceId(io.fabric8.kubernetes.api.model.Service service) {
        var labels = service.getMetadata().getLabels();
        return labels != null ? labels.getOrDefault("service-id", service.getMetadata().getName())
                : service.getMetadata().getName();
    }

    @Override
    public void registerInstance(ServiceInstance instance) {
        log.info("Service registration not needed for Kubernetes discovery");
    }

    @Override
    public void deregisterInstance(String serviceId, String instanceId) {
        log.info("Service deregistration not needed for Kubernetes discovery");
    }

    @Override
    public void updateInstanceHealth(String serviceId, String instanceId, HealthStatus status) {
        var instances = serviceRegistry.get(serviceId);
        if (instances != null) {
            var instance = instances.get(instanceId);
            if (instance != null) {
                var updatedInstance = ServiceInstance.builder()
                        .id(instance.id())
                        .serviceId(instance.serviceId())
                        .host(instance.host())
                        .port(instance.port())
                        .protocol(instance.protocol())
                        .weight(instance.weight())
                        .healthStatus(status)
                        .metadata(instance.metadata())
                        .lastHealthCheck(System.currentTimeMillis())
                        .build();
                instances.put(instanceId, updatedInstance);
                log.debug("Updated health status for instance {} to {}", instanceId, status);
            }
        }
    }

    /**
     * Kubernetes Service Watcher
     */
    private class ServiceWatcher implements Watcher<Service> {
        @Override
        public void eventReceived(Action action, io.fabric8.kubernetes.api.model.Service service) {
            var serviceId = extractServiceId(service);
            switch (action) {
                case ADDED, MODIFIED -> discoverServiceInstances(service, serviceId);
                case DELETED -> {
                    serviceRegistry.remove(serviceId);
                    log.info("Removed service: {}", serviceId);
                }
            }
        }

        @Override
        public void onClose(WatcherException cause) {
            log.warn("Service watcher closed, restarting...", cause);
            startServiceWatcher();
        }
    }
}
