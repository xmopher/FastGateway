package com.mo.gateway.component.loadbalancer;

import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;

import java.util.List;

/**
 * Load Balancing Strategy Interface
 * Defines contract for different load balancing algorithms
 */
public interface LoadBalancingStrategy {
    /**
     * Select a service instance from available healthy instances
     *
     * @param healthyInstances List of healthy service instances
     * @param request Load balancer request context
     * @return Selected service instance
     */
    ServiceInstance select(List<ServiceInstance> healthyInstances, LoadBalancerRequest request);
}