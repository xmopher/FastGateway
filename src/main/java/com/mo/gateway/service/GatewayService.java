package com.mo.gateway.service;

import com.mo.gateway.model.dto.GatewayResponse;
import com.mo.gateway.model.loadbalancer.LoadBalancerRequest;
import com.mo.gateway.model.loadbalancer.ServiceInstance;
import com.mo.gateway.model.ratelimit.RateLimitResult;
import com.mo.gateway.model.dto.GatewayRequest;
import com.mo.gateway.service.loadbalancer.LoadBalancerService;
import com.mo.gateway.service.ratelimit.RateLimiterService;
import com.mo.gateway.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Core Gateway Service
 *
 * Orchestrates request processing through:
 * 1. Rate limiting
 * 2. Load balancing
 * 3. Request forwarding
 * 4. Response handling
 */
@Service
public class GatewayService {

    private static final Logger log = LoggerFactory.getLogger(GatewayService.class);

    private final RateLimiterService rateLimiterService;

    private final LoadBalancerService loadBalancerService;

    private final WebClient webClient;

    public GatewayService(RateLimiterService rateLimiterService, LoadBalancerService loadBalancerService, WebClient webClient) {
        this.rateLimiterService = rateLimiterService;
        this.loadBalancerService = loadBalancerService;
        this.webClient = webClient;
    }

    /**
     * Main request processing method
     */
    public CompletableFuture<GatewayResponse> processRequest(GatewayRequest request) {
        var startTime = System.currentTimeMillis();
        log.debug("Processing request: {} {} from client: {}",
                request.method(), request.path(), request.getClientIdentifier());
        return rateLimiterService.checkRateLimit(request.getClientIdentifier(), request.path())
                .thenCompose(rateLimitResult -> processWithRateLimit(request, rateLimitResult))
                .whenComplete((response, throwable) -> {
                    var processingTime = System.currentTimeMillis() - startTime;
                    log.info("Request processed in {}ms", processingTime);
                });
    }

    private CompletableFuture<GatewayResponse> processWithRateLimit(
            GatewayRequest request, RateLimitResult rateLimitResult) {
        if (!rateLimitResult.allowed()) {
            log.warn("Rate limit exceeded for client: {}", request.getClientIdentifier());
            return CompletableFuture.completedFuture(
                    ResponseUtils.createRateLimitExceededResponse(rateLimitResult));
        }
        return processAllowedRequest(request);
    }

    private CompletableFuture<GatewayResponse> processAllowedRequest(GatewayRequest request) {
        var serviceId = extractServiceId(request.path());
        var lbRequest = new LoadBalancerRequest(serviceId, request, null);
        return loadBalancerService.selectInstance(lbRequest)
                .thenCompose(instance -> forwardRequest(request, instance))
                .exceptionally(throwable -> {
                    log.error("Error in processAllowedRequest for path: {}", request.path(), throwable);
                    return handleError(throwable);
                });
    }

    private CompletableFuture<GatewayResponse> forwardRequest(GatewayRequest request, ServiceInstance instance) {
        var targetUrl = buildTargetUrl(instance, request);
        log.debug("Forwarding to: {}", targetUrl);
        return webClient
                .method(HttpMethod.valueOf(request.method()))
                .uri(targetUrl)
                .headers(headers -> addHeaders(headers, request))
                .bodyValue(request.body() != null ? request.body() : new byte[0])
                .retrieve()
                .toEntity(byte[].class)
                .timeout(Duration.ofSeconds(30))
                .toFuture()
                .thenApply(responseEntity -> {
                    var statusCode = responseEntity.getStatusCode().value();
                    var responseBody = responseEntity.getBody();
                    // Handle null response body - some endpoints may return empty body
                    var body = responseBody != null ? responseBody : new byte[0];
                    log.debug("Received response: status={}, bodySize={} bytes from instance: {}", 
                            statusCode, body.length, instance.id());
                    return new GatewayResponse(
                            statusCode,
                            ResponseUtils.convertHeaders(responseEntity.getHeaders()),
                            body,
                            System.currentTimeMillis(),
                            instance.id(),
                            0L
                    );
                })
                .exceptionally(throwable -> {
                    log.error("Error forwarding request to {}: {}", targetUrl, throwable.getMessage(), throwable);
                    return handleError(throwable);
                });
    }

    private String extractServiceId(String path) {
        // /api/user-service/users -> user-service
        if (path.startsWith("/api/")) {
            var parts = path.substring(5).split("/");
            return parts.length > 0 ? parts[0] : "default";
        }
        return "default";
    }

    private String buildTargetUrl(ServiceInstance instance, GatewayRequest request) {
        // Remove /api/service-name prefix and forward to instance
        var path = request.path();
        var cleanPath = path.replaceFirst("/api/[^/]+", "");
        var baseUrl = STR."\{instance.getUrl()}\{cleanPath}";
        
        // Add query parameters if present
        if (request.queryParams() != null && !request.queryParams().isEmpty()) {
            var queryString = request.queryParams().entrySet().stream()
                    .map(entry -> {
                        var key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                        var value = entry.getValue() != null 
                                ? URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8) 
                                : "";
                        return STR."\{key}=\{value}";
                    })
                    .reduce((a, b) -> STR."\{a}&\{b}")
                    .orElse("");
            if (!queryString.isEmpty()) {
                return STR."\{baseUrl}?\{queryString}";
            }
        }
        return baseUrl;
    }

    private void addHeaders(HttpHeaders headers, GatewayRequest request) {
        if (request.headers() != null) {
            request.headers().forEach((key, value) -> {
                if (!key.equalsIgnoreCase("host")) {
                    headers.add(key, value);
                }
            });
        }
        headers.add("X-Gateway-Request-ID", request.id());
        headers.add("X-Gateway-Timestamp", String.valueOf(request.timestamp()));
    }

    private GatewayResponse handleError(Throwable throwable) {
        log.error("Error processing request", throwable);
        return ResponseUtils.createErrorResponse(500, "Internal server error");
    }
}
