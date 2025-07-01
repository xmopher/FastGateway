package com.mo.gateway.plugin.auth.jwt;

import com.mo.gateway.model.auth.AuthenticationRequest;
import com.mo.gateway.model.auth.AuthenticationResult;
import com.mo.gateway.spi.auth.AuthenticationContext;
import com.mo.gateway.spi.auth.AuthenticationProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * JWT Authentication Provider
 * Validates JWT tokens and extracts user information
 */
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

    private String secretKey;
    private String issuer;
    private boolean enabled = true;

    @Override
    public String getProviderName() {
        return "jwt";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean supports(AuthenticationRequest request) {
        return enabled && request.getBearerToken().isPresent();
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request, AuthenticationContext context) {
        try {
            String token = request.getBearerToken().orElse("");
            if (token.isEmpty()) {
                return AuthenticationResult.failure("Missing JWT token", getProviderName());
            }
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
            JwtParser parser = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build();
            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            Set<String> roles = Set.copyOf((java.util.List<String>) claims.getOrDefault("roles", java.util.List.of()));
            Set<String> permissions = Set.copyOf((java.util.List<String>) claims.getOrDefault("permissions", java.util.List.of()));
            Map<String, Object> userAttributes = Map.of(
                    "email", claims.getOrDefault("email", ""),
                    "iat", claims.getIssuedAt(),
                    "exp", claims.getExpiration()
            );
            context.info("JWT authentication successful for user: {}", userId);
            return AuthenticationResult.success(
                    userId, username, roles, permissions, userAttributes, getProviderName()
            );
        } catch (Exception e) {
            context.error("JWT authentication failed: {}", e.getMessage());
            return AuthenticationResult.failure("Invalid JWT token: " + e.getMessage(), getProviderName());
        }
    }

    @Override
    public void initialize(Properties config) {
        this.secretKey = config.getProperty("jwt.secret", "default-secret-key-change-in-production");
        this.issuer = config.getProperty("jwt.issuer", "gateway");
        this.enabled = Boolean.parseBoolean(config.getProperty("jwt.enabled", "true"));
    }

    @Override
    public void destroy() {}

    @Override
    public HealthStatus health() {
        return enabled ? HealthStatus.UP : HealthStatus.DOWN;
    }
}