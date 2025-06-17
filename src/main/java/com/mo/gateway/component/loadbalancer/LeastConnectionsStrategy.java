package com.mo.gateway.component.loadbalancer;

import com.mo.gateway.exception.NoHealthyInstanceException;
import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Least Connections Load Balancing Strategy
 * Routes requests to the instance with the fewest active connections
 */
@Component("leastConnections")
public class LeastConnectionsStrategy implements LoadBalancingStrategy {

    private final Map<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance select(List<ServiceInstance> healthyInstances, LoadBalancerRequest request) {
        if (healthyInstances.isEmpty()) {
            throw new NoHealthyInstanceException("No healthy instances available");
        }
        return healthyInstances.stream()
                .min(Comparator.comparingInt(instance ->
                        activeConnections.getOrDefault(instance.id(), new AtomicInteger(0)).get()))
                .orElse(healthyInstances.get(0));
    }

    /**
     * Increment connection count for an instance
     */
    public void incrementConnections(String instanceId) {
        activeConnections.computeIfAbsent(instanceId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Decrement connection count for an instance
     */
    public void decrementConnections(String instanceId) {
        var counter = activeConnections.get(instanceId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    /**
     * Get current connection count for an instance
     */
    public int getConnectionCount(String instanceId) {
        var counter = activeConnections.get(instanceId);
        return counter != null ? counter.get() : 0;
    }
}
