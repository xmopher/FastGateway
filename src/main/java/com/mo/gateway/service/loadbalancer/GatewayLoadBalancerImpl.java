package com.mo.gateway.service.loadbalancer;

import com.mo.gateway.component.loadbalancer.LoadBalancingStrategy;
import com.mo.gateway.config.properties.LoadBalancerProperties;
import com.mo.gateway.exception.NoHealthyInstanceException;
import com.mo.gateway.model.loadbalancer.HealthStatus;
import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway Load Balancer Implementation
 * Coordinates service discovery and load balancing strategies
 */
@Service
public class GatewayLoadBalancerImpl implements LoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoadBalancerImpl.class);

    private final Map<String, LoadBalancingStrategy> strategies;

    private final ServiceDiscoveryService serviceDiscovery;

    private final LoadBalancerProperties properties;

    public GatewayLoadBalancerImpl(Map<String, LoadBalancingStrategy> strategies, ServiceDiscoveryService serviceDiscovery, LoadBalancerProperties properties) {
        this.strategies = strategies;
        this.serviceDiscovery = serviceDiscovery;
        this.properties = properties;
    }

    @Override
    public CompletableFuture<ServiceInstance> selectInstance(LoadBalancerRequest request) {
        return serviceDiscovery.getHealthyInstances(request.serviceId())
                .thenApply(instances -> selectInstanceWithStrategy(instances, request))
                .whenComplete((instance, throwable) -> {
                    if (instance != null) {
                        log.debug("Selected instance {} for service {}",
                                instance.id(), request.serviceId());
                    }
                });
    }

    private ServiceInstance selectInstanceWithStrategy(List<ServiceInstance> instances, LoadBalancerRequest request) {
        if (instances.isEmpty()) {
            throw new NoHealthyInstanceException(
                    STR."No healthy instances available for service: \{request.serviceId()}");
        }
        var strategyName = properties.getStrategy(request.serviceId());
        var strategy = strategies.get(strategyName);
        if (strategy == null) {
            log.warn("Unknown load balancing strategy: {}, using default", strategyName);
            strategy = strategies.get(properties.defaultStrategy());
        }
        return strategy.select(instances, request);
    }

    @Override
    public void updateInstanceHealth(String serviceId, String instanceId, HealthStatus status) {
        serviceDiscovery.updateInstanceHealth(serviceId, instanceId, status);
        log.debug("Updated health status for instance {} to {}", instanceId, status);
    }

    @Override
    public void addServiceInstances(String serviceId, List<ServiceInstance> instances) {
        instances.forEach(serviceDiscovery::registerInstance);
        log.info("Added {} instances for service {}", instances.size(), serviceId);
    }

    @Override
    public void removeServiceInstance(String serviceId, String instanceId) {
        serviceDiscovery.deregisterInstance(serviceId, instanceId);
        log.info("Removed instance {} from service {}", instanceId, serviceId);
    }
}
