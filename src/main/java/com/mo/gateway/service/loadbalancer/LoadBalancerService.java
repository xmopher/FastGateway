package com.mo.gateway.service.loadbalancer;

import com.mo.gateway.model.loadbalancer.HealthStatus;
import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Load Balancer Service Interface
 * Provides load balancing and service discovery functionality
 */
public interface LoadBalancerService {

    /**
     * Select a service instance for the given request
     */
    CompletableFuture<ServiceInstance> selectInstance(LoadBalancerRequest request);

    /**
     * Update health status of a service instance
     */
    void updateInstanceHealth(String serviceId, String instanceId, HealthStatus status);

    /**
     * Add service instances to the load balancer
     */
    void addServiceInstances(String serviceId, List<ServiceInstance> instances);

    /**
     * Remove a service instance from the load balancer
     */
    void removeServiceInstance(String serviceId, String instanceId);
}
