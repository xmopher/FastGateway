package com.mo.gateway.component.loadbalancer;

import com.mo.gateway.exception.NoHealthyInstanceException;
import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Weighted Round Robin Load Balancing Strategy
 * Distributes requests based on instance weights
 */
@Component("weightedRoundRobin")
public class WeightedRoundRobinStrategy implements LoadBalancingStrategy {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance select(List<ServiceInstance> healthyInstances, LoadBalancerRequest request) {
        if (healthyInstances.isEmpty()) {
            throw new NoHealthyInstanceException("No healthy instances available");
        }
        var totalWeight = healthyInstances.stream()
                .mapToInt(ServiceInstance::weight)
                .sum();
        if (totalWeight == 0) {
            // Fallback to round-robin if no weights
            var counter = counters.computeIfAbsent(request.serviceId(), k -> new AtomicInteger(0));
            var index = Math.abs(counter.getAndIncrement()) % healthyInstances.size();
            return healthyInstances.get(index);
        }
        var counter = counters.computeIfAbsent(request.serviceId(), k -> new AtomicInteger(0));
        var weightedIndex = Math.abs(counter.getAndIncrement()) % totalWeight;
        var currentWeight = 0;
        for (var instance : healthyInstances) {
            currentWeight += instance.weight();
            if (weightedIndex < currentWeight) {
                return instance;
            }
        }
        return healthyInstances.get(0);
    }
}
