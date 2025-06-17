package com.mo.gateway.util;

import com.mo.gateway.model.dto.GatewayRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for HTTP request processing
 * Converts between different request formats
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * Convert HttpServletRequest to GatewayRequest
     */
    public static GatewayRequest fromHttpServletRequest(HttpServletRequest request, byte[] body) {
        return GatewayRequest.builder()
                .path(request.getRequestURI())
                .method(request.getMethod())
                .headers(extractHeaders(request))
                .queryParams(extractQueryParams(request))
                .body(body)
                .build();
    }

    /**
     * Extract headers from HttpServletRequest
     */
    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        var headers = new HashMap<String, String>();
        var headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                var headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    /**
     * Extract query parameters from HttpServletRequest
     */
    private static Map<String, String> extractQueryParams(HttpServletRequest request) {
        var queryString = request.getQueryString();
        if (queryString == null) {
            return Map.of();
        }
        var params = new HashMap<String, String>();
        var pairs = queryString.split("&");
        for (var pair : pairs) {
            var keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    /**
     * Extract client IP address from request
     * Handles various proxy headers
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        return switch (getForwardedIp(request)) {
            case String ip when ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip) -> ip;
            default -> request.getRemoteAddr();
        };
    }

    private static String getForwardedIp(HttpServletRequest request) {
        var headers = new String[]{
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };
        for (var header : headers) {
            var ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated IPs (take the first one)
                return ip.split(",")[0].trim();
            }
        }
        return null;
    }
}
