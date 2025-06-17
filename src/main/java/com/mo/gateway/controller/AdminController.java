package com.mo.gateway.controller;

import com.mo.gateway.model.dto.ApiResponse;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import com.mo.gateway.service.loadbalancer.LoadBalancerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Controller for gateway management
 * Provides endpoints for service registration and monitoring
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final LoadBalancerService loadBalancerService;

    public AdminController(LoadBalancerService loadBalancerService) {
        this.loadBalancerService = loadBalancerService;
    }

    /**
     * Register a new service instance
     */
    @PostMapping("/services/{serviceId}/instances")
    public ResponseEntity<ApiResponse<Void>> addServiceInstance(
            @PathVariable String serviceId,
            @Valid @RequestBody ServiceInstance instance) {
        try {
            loadBalancerService.addServiceInstances(serviceId, List.of(instance));
            log.info("Successfully registered service instance: {} for service: {}",
                    instance.id(), serviceId);
            return ResponseEntity.ok(ApiResponse.success("Service instance registered successfully"));
        } catch (Exception e) {
            log.error("Failed to register service instance", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(STR."Failed to register service instance: \{e.getMessage()}"));
        }
    }

    /**
     * Remove a service instance
     */
    @DeleteMapping("/services/{serviceId}/instances/{instanceId}")
    public ResponseEntity<ApiResponse<Void>> removeServiceInstance(@PathVariable String serviceId,
         @PathVariable String instanceId) {
        try {
            loadBalancerService.removeServiceInstance(serviceId, instanceId);
            log.info("Successfully removed service instance: {} from service: {}",
                    instanceId, serviceId);
            return ResponseEntity.ok(ApiResponse.success("Service instance removed successfully"));
        } catch (Exception e) {
            log.error("Failed to remove service instance", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(STR."Failed to remove service instance: \{e.getMessage()}"));
        }
    }

    /**
     * Get health status endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Gateway is healthy"));
    }
}
