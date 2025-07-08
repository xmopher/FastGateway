package com.mo.gateway.controller;

import com.mo.gateway.component.auth.plugin.AuthPluginManager;
import com.mo.gateway.model.dto.ApiResponse;
import com.mo.gateway.service.auth.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Management Controller
 * Provides REST endpoints for authentication plugin management
 */
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final AuthPluginManager pluginManager;

    private final AuthenticationService authService;

    public AuthController(AuthPluginManager pluginManager, AuthenticationService authService) {
        this.pluginManager = pluginManager;
        this.authService = authService;
    }

    /**
     * Get all loaded authentication plugins
     */
    @GetMapping("/plugins")
    public ResponseEntity<ApiResponse<Map<String, AuthPluginManager.PluginStatus>>> getPlugins() {
        Map<String, AuthPluginManager.PluginStatus> plugins = pluginManager.getPluginStatus();
        return ResponseEntity.ok(ApiResponse.success(plugins));
    }

    /**
     * Reload all plugins
     */
    @PostMapping("/plugins/reload")
    public ResponseEntity<ApiResponse<String>> reloadPlugins() {
        pluginManager.scanAndLoadPlugins();
        return ResponseEntity.ok(ApiResponse.success("Plugins reloaded successfully"));
    }

    /**
     * Enable authentication provider
     */
    @PostMapping("/providers/{providerName}/enable")
    public ResponseEntity<ApiResponse<String>> enableProvider(@PathVariable String providerName) {
        boolean success = pluginManager.enableProvider(providerName);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Provider enabled: " + providerName));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to enable provider: " + providerName));
        }
    }

    /**
     * Disable authentication provider
     */
    @PostMapping("/providers/{providerName}/disable")
    public ResponseEntity<ApiResponse<String>> disableProvider(@PathVariable String providerName) {
        pluginManager.disableProvider(providerName);
        return ResponseEntity.ok(ApiResponse.success("Provider disabled: " + providerName));
    }

    /**
     * Get authentication statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AuthenticationService.AuthenticationStats>> getAuthStats() {
        AuthenticationService.AuthenticationStats stats = authService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get all enabled providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getEnabledProviders() {
        var providers = pluginManager.getEnabledProviders().stream()
                .map(provider -> Map.of(
                        "name", provider.getProviderName(),
                        "version", provider.getVersion(),
                        "health", provider.health().name()
                ))
                .toList();
        Map<String, Object> response = Map.of(
                "providers", providers,
                "count", providers.size()
        );
        return ResponseEntity.ok(response);
    }
}