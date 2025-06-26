package com.mo.gateway.model.auth;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authentication Result
 */
public record AuthenticationResult(
        boolean success,
        String userId,
        String username,
        String errorMessage,
        int errorCode,
        Set<String> roles,
        Set<String> permissions,
        Map<String, Object> userAttributes,
        String providerName,
        long authenticatedAt
) {

    /**
     * Create successful authentication result
     */
    public static AuthenticationResult success(String userId, String username,
                                               Set<String> roles, Set<String> permissions,
                                               Map<String, Object> userAttributes,
                                               String providerName) {
        return new AuthenticationResult(
                true, userId, username, null, 0,
                roles, permissions, userAttributes,
                providerName, System.currentTimeMillis()
        );
    }

    /**
     * Create failed authentication result
     */
    public static AuthenticationResult failure(String errorMessage, int errorCode, String providerName) {
        return new AuthenticationResult(
                false, null, null, errorMessage, errorCode,
                Set.of(), Set.of(), Map.of(),
                providerName, System.currentTimeMillis()
        );
    }

    /**
     * Create failed authentication result with default error code
     */
    public static AuthenticationResult failure(String errorMessage, String providerName) {
        return failure(errorMessage, 401, providerName);
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has specific permission
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Get user attribute
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserAttribute(String key, Class<T> type, T defaultValue) {
        if (userAttributes == null) {
            return defaultValue;
        }
        Object value = userAttributes.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Check if authentication result is valid (not expired)
     */
    public boolean isValid(long maxAgeMillis) {
        return success && (System.currentTimeMillis() - authenticatedAt) < maxAgeMillis;
    }

    /**
     * User principal entity
     */
    public record Principal(
            String id,
            String username,
            String email,
            Set<String> roles,
            List<String> permissions,
            Map<String, Object> claims
    ) {

        /**
         * Create principal user entity
         */
        public static Principal of(String id, String username) {
            return new Principal(id, username, null, Set.of(), List.of(), Map.of());
        }

        public static Principal of(String id, String username, String email) {
            return new Principal(id, username, email, Set.of(), List.of(), Map.of());
        }

        public static Principal of(String id, String username, Set<String> roles) {
            return new Principal(id, username, null, roles, List.of(), Map.of());
        }

        public static Principal of(String id, String username, Set<String> roles, List<String> permissions) {
            return new Principal(id, username, null, roles, permissions, Map.of());
        }

        /**
         * Check if it has specific role
         */
        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        /**
         * Check if it has any role
         */
        public boolean hasAnyRole(String... roles) {
            for (String role : roles) {
                if (hasRole(role)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check if it has specific permission
         */
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }

        /**
         * Get claimed value
         */
        @SuppressWarnings("unchecked")
        public <T> T getClaim(String name, Class<T> type) {
            Object value = claims.get(name);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
    }
}