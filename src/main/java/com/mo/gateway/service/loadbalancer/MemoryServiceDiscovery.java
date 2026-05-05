package com.mo.gateway.service.loadbalancer;

import com.mo.gateway.model.loadbalancer.HealthStatus;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Service Discovery Implementation for local development
 */
@Service
@ConditionalOnProperty(name = "gateway.discovery.type", havingValue = "memory", matchIfMissing = true)
public class MemoryServiceDiscovery implements ServiceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceDiscovery.class);

    private final Map<String, Map<String, ServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<List<ServiceInstance>> getHealthyInstances(String serviceId) {
        return CompletableFuture.supplyAsync(() -> {
            var instances = serviceRegistry.get(serviceId);
            if (instances == null) {
                log.debug("No instances found for service: {}", serviceId);
                return List.of();
            }
            return instances.values().stream()
                    .filter(ServiceInstance::isHealthy)
                    .toList();
        });
    }

    @Override
    public void registerInstance(ServiceInstance instance) {
        serviceRegistry
                .computeIfAbsent(instance.serviceId(), k -> new ConcurrentHashMap<>())
                .put(instance.id(), instance);
        log.info("Registered instance: {} -> {}:{}", instance.serviceId(), instance.host(), instance.port());
    }

    @Override
    public void deregisterInstance(String serviceId, String instanceId) {
        var instances = serviceRegistry.get(serviceId);
        if (instances != null) {
            instances.remove(instanceId);
            log.info("Deregistered instance: {} / {}", serviceId, instanceId);
        }
    }

    @Override
    public void updateInstanceHealth(String serviceId, String instanceId, HealthStatus status) {
        var instances = serviceRegistry.get(serviceId);
        if (instances != null) {
            var instance = instances.get(instanceId);
            if (instance != null) {
                var updated = ServiceInstance.builder()
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
                instances.put(instanceId, updated);
                log.debug("Updated health for instance {} to {}", instanceId, status);
            }
        }
    }
}
