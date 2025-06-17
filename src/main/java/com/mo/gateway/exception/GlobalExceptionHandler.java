package com.mo.gateway.exception;

import com.mo.gateway.model.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Global exception handler for gateway-specific exceptions
 * Provides consistent error responses across the application
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle NoHealthyInstanceException
     */
    @ExceptionHandler(NoHealthyInstanceException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHealthyInstance(
            NoHealthyInstanceException ex, WebRequest request) {
        log.error("No healthy instances available for service: {}", ex.getServiceId(), ex);
        Object errorData = ex.getServiceId() != null ?
                Map.of("serviceId", ex.getServiceId()) : null;
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle RateLimitException
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimit(
            RateLimitException ex, WebRequest request) {
        log.warn("Rate limit exceeded for client: {}", ex.getClientId());
        Object errorData = Map.of(
                "clientId", ex.getClientId() != null ? ex.getClientId() : "unknown",
                "retryAfterMs", ex.getRetryAfterMs()
        );
        var response = ApiResponse.error(ex.getMessage(), errorData);
        var responseEntity = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        if (ex.getRetryAfterMs() > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(ex.getRetryAfterMs() / 1000))
                    .body(response);
        }
        return responseEntity;
    }

    /**
     * Handle LoadBalancerException
     */
    @ExceptionHandler(LoadBalancerException.class)
    public ResponseEntity<ApiResponse<Object>> handleLoadBalancer(
            LoadBalancerException ex, WebRequest request) {
        log.error("Load balancer error for service: {} with strategy: {}",
                ex.getServiceId(), ex.getStrategy(), ex);
        Object errorData = Map.of(
                "serviceId", ex.getServiceId() != null ? ex.getServiceId() : "unknown",
                "strategy", ex.getStrategy() != null ? ex.getStrategy() : "unknown"
        );
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle ServiceUnavailableException
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleServiceUnavailable(
            ServiceUnavailableException ex, WebRequest request) {
        log.error("Service unavailable: {} instance: {}",
                ex.getServiceId(), ex.getInstanceId(), ex);
        Object errorData = Map.of(
                "serviceId", ex.getServiceId() != null ? ex.getServiceId() : "unknown",
                "instanceId", ex.getInstanceId() != null ? ex.getInstanceId() : "unknown"
        );
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle ServiceDiscoveryException
     */
    @ExceptionHandler(ServiceDiscoveryException.class)
    public ResponseEntity<ApiResponse<Object>> handleServiceDiscovery(
            ServiceDiscoveryException ex, WebRequest request) {
        log.error("Service discovery error with type: {}", ex.getDiscoveryType(), ex);
        Object errorData = ex.getDiscoveryType() != null ?
                Map.of("discoveryType", ex.getDiscoveryType()) : null;
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle ConfigurationException
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConfiguration(
            ConfigurationException ex, WebRequest request) {
        log.error("Configuration error for key: {}", ex.getConfigurationKey(), ex);
        Object errorData = ex.getConfigurationKey() != null ?
                Map.of("configurationKey", ex.getConfigurationKey()) : null;
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle generic GatewayException
     */
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiResponse<Object>> handleGatewayException(
            GatewayException ex, WebRequest request) {
        log.error("Gateway exception: {} (code: {})", ex.getMessage(), ex.getErrorCode(), ex);
        Object errorData = Map.of(
                "errorCode", ex.getErrorCode(),
                "httpStatus", ex.getHttpStatus()
        );
        var response = ApiResponse.error(ex.getMessage(), errorData);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error", ex);
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        org.springframework.validation.FieldError::getDefaultMessage,
                        (existing, replacement) -> existing + "; " + replacement
                ));
        Object errorData = Map.of("validationErrors", errors);
        var response = ApiResponse.error("Validation failed", errorData);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Spring WebClient errors
     */
    @ExceptionHandler(org.springframework.web.reactive.function.client.WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Object>> handleWebClientError(
            org.springframework.web.reactive.function.client.WebClientResponseException ex, WebRequest request) {
        log.error("WebClient error: {} {}", ex.getStatusCode(), ex.getStatusText(), ex);
        Object errorData = Map.of(
                "upstreamStatus", ex.getStatusCode().value(),
                "upstreamStatusText", ex.getStatusText(),
                "upstreamResponse", ex.getResponseBodyAsString()
        );
        var response = ApiResponse.error("Upstream service error", errorData);
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            RuntimeException ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        Object errorData = Map.of(
                "exceptionType", ex.getClass().getSimpleName(),
                "timestamp", System.currentTimeMillis()
        );
        var response = ApiResponse.error("An unexpected error occurred", errorData);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
