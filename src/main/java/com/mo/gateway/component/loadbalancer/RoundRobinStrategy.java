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
 * Round Robin Load Balancing Strategy
 * Distributes requests evenly across all available instances
 */
@Component("roundRobin")
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance select(List<ServiceInstance> healthyInstances, LoadBalancerRequest request) {
        if (healthyInstances.isEmpty()) {
            throw new NoHealthyInstanceException(
                    STR."No healthy instances available for service: \{request.serviceId()}");
        }
        var counter = counters.computeIfAbsent(request.serviceId(), k -> new AtomicInteger(0));
        var index = Math.abs(counter.getAndIncrement()) % healthyInstances.size();
        return healthyInstances.get(index);
    }
}