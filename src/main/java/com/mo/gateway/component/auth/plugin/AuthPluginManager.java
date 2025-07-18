package com.mo.gateway.component.auth.plugin;

import com.mo.gateway.spi.auth.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Authentication Plugin Manager
 * Supports dynamic loading, unloading and management of authentication plugins
 */
@Component
public class AuthPluginManager {

    private static final Logger log = LoggerFactory.getLogger(AuthPluginManager.class);

    private final Map<String, PluginContainer> loadedPlugins = new ConcurrentHashMap<>();
    private final Map<String, AuthenticationProvider> enabledProviders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watcherService = Executors.newSingleThreadScheduledExecutor();
    private final String pluginDirectory;

    public AuthPluginManager() {
        this.pluginDirectory = System.getProperty("gateway.plugin.directory", "plugins");
        initializePluginDirectory();
        startPluginWatcher();
    }

    /**
     * Initialize plugin directory
     */
    private void initializePluginDirectory() {
        File dir = new File(pluginDirectory);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("Created plugin directory: {}", pluginDirectory);
            }
        }
    }

    /**
     * Start plugin directory monitoring
     */
    private void startPluginWatcher() {
        watcherService.scheduleWithFixedDelay(() -> {
            try {
                scanAndLoadPlugins();
            } catch (Exception e) {
                log.error("Error scanning plugins", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Scan and load plugins
     */
    public void scanAndLoadPlugins() {
        log.debug("Scanning plugin directory: {}", pluginDirectory);
        File dir = new File(pluginDirectory);
        if (!dir.exists()) {
            return;
        }
        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return;
        }
        Set<String> currentJars = new HashSet<>();
        for (File jarFile : jarFiles) {
            String jarName = jarFile.getName();
            currentJars.add(jarName);
            long lastModified = jarFile.lastModified();
            PluginContainer existing = loadedPlugins.get(jarName);
            if (existing == null || existing.lastModified != lastModified) {
                try {
                    if (existing != null) {
                        unloadPlugin(jarName);
                    }
                    loadPlugin(jarFile);
                } catch (Exception e) {
                    log.error("Failed to load plugin: {}", jarName, e);
                }
            }
        }
        Set<String> toRemove = new HashSet<>(loadedPlugins.keySet());
        toRemove.removeAll(currentJars);
        toRemove.forEach(this::unloadPlugin);
    }

    /**
     * Load a single plugin
     */
    public void loadPlugin(File jarFile) {
        String jarName = jarFile.getName();
        log.info("Loading plugin: {}", jarName);
        try {
            URL[] urls = {jarFile.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
            ServiceLoader<AuthenticationProvider> serviceLoader =
                    ServiceLoader.load(AuthenticationProvider.class, classLoader);
            List<AuthenticationProvider> providers = new ArrayList<>();
            for (AuthenticationProvider provider : serviceLoader) {
                providers.add(provider);
                log.info("Found authentication provider: {} v{}",
                        provider.getProviderName(), provider.getVersion());
            }
            if (!providers.isEmpty()) {
                PluginContainer container = new PluginContainer(
                        jarName,
                        jarFile.lastModified(),
                        classLoader,
                        providers
                );
                loadedPlugins.put(jarName, container);
                for (AuthenticationProvider provider : providers) {
                    try {
                        provider.initialize(loadPluginConfig(provider.getProviderName()));
                        log.info("Initialized authentication provider: {}", provider.getProviderName());
                    } catch (Exception e) {
                        log.error("Failed to initialize provider: {}", provider.getProviderName(), e);
                    }
                }
                log.info("Successfully loaded plugin: {} with {} providers", jarName, providers.size());
            } else {
                log.warn("No authentication providers found in plugin: {}", jarName);
                try {
                    classLoader.close();
                } catch (Exception e) {
                    log.warn("Failed to close class loader for: {}", jarName, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load plugin: {}", jarName, e);
        }
    }

    /**
     * Load plugin configuration
     */
    private Properties loadPluginConfig(String providerName) {
        Properties props = new Properties();
        // Load from application.yaml or environment variables
        // props.setProperty("key", "value");
        return props;
    }

    /**
     * Unload plugin
     */
    public void unloadPlugin(String jarName) {
        PluginContainer container = loadedPlugins.remove(jarName);
        if (container != null) {
            log.info("Unloading plugin: {}", jarName);
            // Disable all providers
            for (AuthenticationProvider provider : container.providers) {
                disableProvider(provider.getProviderName());
                try {
                    provider.destroy();
                } catch (Exception e) {
                    log.error("Error destroying provider: {}", provider.getProviderName(), e);
                }
            }
            try {
                container.classLoader.close();
            } catch (Exception e) {
                log.warn("Failed to close class loader for: {}", jarName, e);
            }
            log.info("Successfully unloaded plugin: {}", jarName);
        }
    }

    /**
     * Enable authentication provider
     */
    public boolean enableProvider(String providerName) {
        for (PluginContainer container : loadedPlugins.values()) {
            for (AuthenticationProvider provider : container.providers) {
                if (provider.getProviderName().equals(providerName)) {
                    enabledProviders.put(providerName, provider);
                    log.info("Enabled authentication provider: {}", providerName);
                    return true;
                }
            }
        }
        log.warn("Authentication provider not found: {}", providerName);
        return false;
    }

    /**
     * Disable authentication provider
     */
    public void disableProvider(String providerName) {
        AuthenticationProvider removed = enabledProviders.remove(providerName);
        if (removed != null) {
            log.info("Disabled authentication provider: {}", providerName);
        }
    }

    /**
     * Get enabled authentication provider
     */
    public Optional<AuthenticationProvider> getProvider(String providerName) {
        return Optional.ofNullable(enabledProviders.get(providerName));
    }

    /**
     * Get all enabled authentication providers
     */
    public Collection<AuthenticationProvider> getEnabledProviders() {
        return new ArrayList<>(enabledProviders.values());
    }

    /**
     * Get all loaded authentication providers
     */
    public List<AuthenticationProvider> getAllProviders() {
        List<AuthenticationProvider> all = new ArrayList<>();
        for (PluginContainer container : loadedPlugins.values()) {
            all.addAll(container.providers);
        }
        return all;
    }

    /**
     * Get plugin status
     */
    public Map<String, PluginStatus> getPluginStatus() {
        Map<String, PluginStatus> status = new HashMap<>();
        for (Map.Entry<String, PluginContainer> entry : loadedPlugins.entrySet()) {
            PluginContainer container = entry.getValue();
            List<String> providerNames = container.providers.stream()
                    .map(AuthenticationProvider::getProviderName)
                    .toList();
            boolean enabled = providerNames.stream()
                    .anyMatch(enabledProviders::containsKey);
            status.put(entry.getKey(), new PluginStatus(
                    container.jarName,
                    container.lastModified,
                    providerNames,
                    enabled
            ));
        }
        return status;
    }

    /**
     * Destroy plugin manager
     */
    public void destroy() {
        watcherService.shutdown();
        for (String jarName : new ArrayList<>(loadedPlugins.keySet())) {
            unloadPlugin(jarName);
        }
        try {
            if (!watcherService.awaitTermination(5, TimeUnit.SECONDS)) {
                watcherService.shutdownNow();
            }
        } catch (InterruptedException e) {
            watcherService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Plugin container record
     */
    private record PluginContainer(
            String jarName,
            long lastModified,
            URLClassLoader classLoader,
            List<AuthenticationProvider> providers
    ) {}

    /**
     * Plugin status record
     */
    public record PluginStatus(
            String jarName,
            long lastModified,
            List<String> providers,
            boolean enabled
    ) {}
}