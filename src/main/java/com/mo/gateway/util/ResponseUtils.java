package com.mo.gateway.util;

import com.mo.gateway.model.dto.GatewayResponse;
import com.mo.gateway.model.ratelimit.RateLimitResult;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for HTTP response processing
 * Creates standardized response objects
 */
public final class ResponseUtils {

    private ResponseUtils() {
    }

    /**
     * Create rate limit exceeded response
     */
    public static GatewayResponse createRateLimitExceededResponse(RateLimitResult result) {
        var headers = Map.of(
                "Content-Type", "application/json",
                "X-RateLimit-Remaining", String.valueOf(result.remaining()),
                "X-RateLimit-Limit", String.valueOf(result.limit()),
                "Retry-After", String.valueOf(result.retryAfterMs() / 1000)
        );
        var body = STR."""
            {
                "error": "Rate limit exceeded",
                "message": "\{result.reason()}",
                "retryAfter": \{result.retryAfterMs()},
                "remaining": \{result.remaining()},
                "limit": \{result.limit()}
            }
            """;
        return GatewayResponse.builder()
                .statusCode(429)
                .headers(headers)
                .body(body.getBytes())
                .build();
    }

    /**
     * Create generic error response
     */
    public static GatewayResponse createErrorResponse(int statusCode, String message) {
        var headers = Map.of("Content-Type", "application/json");
        var body = STR."""
            {
                "error": "\{getErrorType(statusCode)}",
                "message": "\{message}",
                "timestamp": \{System.currentTimeMillis()}
            }
            """;
        return GatewayResponse.builder()
                .statusCode(statusCode)
                .headers(headers)
                .body(body.getBytes())
                .build();
    }

    /**
     * Convert Spring HttpHeaders to Map
     */
    public static Map<String, String> convertHeaders(HttpHeaders httpHeaders) {
        var headers = new HashMap<String, String>();
        httpHeaders.forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key, values.get(0));
            }
        });
        return headers;
    }

    /**
     * Get error type based on status code
     */
    private static String getErrorType(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 408 -> "Request Timeout";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }
}