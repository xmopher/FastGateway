package com.mo.gateway.controller;

import com.mo.gateway.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Frontend Controller
 * Handles all non-API requests and forwards them to the frontend service
 */
@RestController
public class FrontendController {

    private static final Logger log = LoggerFactory.getLogger(FrontendController.class);

    private final WebClient webClient;

    @Value("${gateway.frontend.service-url:http://frontend-service.export-control.svc.cluster.local:80}")
    private String frontendServiceUrl;

    public FrontendController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Handle all non-API requests (excluding /api/** and /admin/**)
     * Forward them to the frontend service
     * 
     * Note: This controller has lower priority than GatewayController and AdminController
     * Spring will match /api/** and /admin/** to their respective controllers first
     */
    @RequestMapping(value = {"", "/", "/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public CompletableFuture<ResponseEntity<byte[]>> handleFrontendRequest(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) byte[] body) {
        
        var path = httpRequest.getRequestURI();
        var queryString = httpRequest.getQueryString();
        
        // Skip API and admin routes (safety check, though Spring should handle this)
        if (path.startsWith("/api/") || path.startsWith("/admin/")) {
            log.warn("FrontendController received API/Admin request: {}, this should not happen", path);
            return CompletableFuture.completedFuture(
                    ResponseEntity.notFound().build());
        }

        var gatewayRequest = RequestUtils.fromHttpServletRequest(httpRequest, body);
        log.debug("Forwarding frontend request: {} {} to {}", 
                gatewayRequest.method(), 
                gatewayRequest.path(),
                frontendServiceUrl);

        var targetUrl = buildTargetUrl(path, queryString);
        
        return webClient
                .method(HttpMethod.valueOf(gatewayRequest.method()))
                .uri(targetUrl)
                .headers(headers -> addHeaders(headers, gatewayRequest))
                .bodyValue(body != null ? body : new byte[0])
                .retrieve()
                .toEntity(byte[].class)
                .timeout(Duration.ofSeconds(30))
                .toFuture()
                .thenApply(responseEntity -> {
                    var builder = ResponseEntity.status(responseEntity.getStatusCode().value());
                    if (responseEntity.getHeaders() != null) {
                        responseEntity.getHeaders().forEach((key, values) -> {
                            if (!key.equalsIgnoreCase("X-Gateway-Request-ID") && 
                                !key.equalsIgnoreCase("X-Gateway-Timestamp")) {
                                values.forEach(value -> builder.header(key, value));
                            }
                        });
                    }
                    builder.header("X-Gateway-Forwarded", "true");
                    return builder.body(responseEntity.getBody());
                })
                .exceptionally(throwable -> {
                    log.error("Error forwarding request to frontend service", throwable);
                    return ResponseEntity.status(502)
                            .header("Content-Type", "text/html")
                            .body("<html><body><h1>502 Bad Gateway</h1><p>Frontend service unavailable</p></body></html>".getBytes());
                });
    }

    private String buildTargetUrl(String path, String queryString) {
        var url = frontendServiceUrl + path;
        if (queryString != null && !queryString.isEmpty()) {
            url += "?" + queryString;
        }
        return url;
    }

    private void addHeaders(HttpHeaders headers, com.mo.gateway.model.dto.GatewayRequest request) {
        if (request.headers() != null) {
            request.headers().forEach((key, value) -> {
                if (!key.equalsIgnoreCase("host")) {
                    headers.add(key, value);
                }
            });
        }
    }
}

