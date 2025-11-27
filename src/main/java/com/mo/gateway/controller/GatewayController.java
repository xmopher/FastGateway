package com.mo.gateway.controller;

import com.mo.gateway.model.dto.GatewayResponse;
import com.mo.gateway.service.GatewayService;
import com.mo.gateway.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Main Gateway Controller
 * Handles all incoming requests and routes them through the gateway
 */
@RestController
@RequestMapping("/api/**")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * Handle all HTTP methods for gateway routing
     */
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public CompletableFuture<ResponseEntity<byte[]>> handleRequest(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) byte[] body) {
        var gatewayRequest = RequestUtils.fromHttpServletRequest(httpRequest, body);
        log.info("Processing request: {} {} from client: {}",
                gatewayRequest.method(),
                gatewayRequest.path(),
                gatewayRequest.getClientIdentifier());
        return gatewayService.processRequest(gatewayRequest)
                .thenApply(this::toResponseEntity)
                .exceptionally(throwable -> {
                    log.error("Exception in handleRequest for {} {}", 
                            gatewayRequest.method(), gatewayRequest.path(), throwable);
                    return handleException(throwable);
                });
    }

    private ResponseEntity<byte[]> toResponseEntity(GatewayResponse gatewayResponse) {
        try {
            var builder = ResponseEntity.status(gatewayResponse.statusCode());
            if (gatewayResponse.headers() != null) {
                gatewayResponse.headers().forEach(builder::header);
            }
            builder.header("X-Gateway-Instance", gatewayResponse.instanceId());
            builder.header("X-Gateway-Processing-Time", String.valueOf(gatewayResponse.processingTimeMs()));
            // Handle null body - use empty array if body is null
            var body = gatewayResponse.body() != null ? gatewayResponse.body() : new byte[0];
            log.debug("Building response: status={}, bodySize={} bytes", gatewayResponse.statusCode(), body.length);
            return builder.body(body);
        } catch (Exception e) {
            log.error("Error converting GatewayResponse to ResponseEntity", e);
            throw new RuntimeException("Failed to build response entity", e);
        }
    }

    private ResponseEntity<byte[]> handleException(Throwable throwable) {
        return switch (throwable) {
            case java.util.concurrent.TimeoutException e -> {
                log.error("Request timeout", e);
                yield ResponseEntity.status(408)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Request timeout\"}".getBytes());
            }
            case java.nio.file.AccessDeniedException e -> {
                log.error("Access denied", e);
                yield ResponseEntity.status(403)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Access denied\"}".getBytes());
            }
            case IllegalArgumentException e -> {
                log.error("Bad request", e);
                yield ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body(STR."{\"error\":\"Bad request: \{e.getMessage()}\"}".getBytes());
            }
            default -> {
                log.error("Internal server error", throwable);
                yield ResponseEntity.internalServerError()
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Internal server error\"}".getBytes());
            }
        };
    }
}
