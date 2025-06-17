package com.mo.gateway.service.loadbalancer;

import com.mo.gateway.model.loadbalancer.HealthStatus;
import com.mo.gateway.model.loadbalancer.ServiceInstance;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service Discovery Interface
 * Abstracts different service discovery mechanisms
 */
public interface ServiceDiscoveryService {

    /**
     * Get all healthy instances for a service
     */
    CompletableFuture<List<ServiceInstance>> getHealthyInstances(String serviceId);

    /**
     * Register a new service instance
     */
    void registerInstance(ServiceInstance instance);

    /**
     * Deregister a service instance
     */
    void deregisterInstance(String serviceId, String instanceId);

    /**
     * Update health status of an instance
     */
    void updateInstanceHealth(String serviceId, String instanceId, HealthStatus status);
}
