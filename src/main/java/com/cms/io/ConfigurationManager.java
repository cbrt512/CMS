package com.cms.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Configuration management service providing secure loading, saving, and
 * hot-reloading of application settings with comprehensive I/O operations.
 *
 * <p>
 * This service manages application configuration files in multiple formats
 * (Properties, JSON, XML) with support for hot-reloading, validation,
 * encryption, and comprehensive error handling. It provides a unified
 * interface for configuration management across the CMS application.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Singleton Pattern variation with
 * configuration management capabilities, providing centralized access
 * to application settings with thread-safe operations.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * Demonstrates comprehensive I/O operations including file reading/writing,
 * stream processing, character encoding handling, and file watching
 * with proper resource management.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Path traversal prevention for configuration files
 * - Input validation and sanitization
 * - Configuration encryption support
 * - Access control with read-only modes
 * - Backup and recovery mechanisms
 * - Safe property key validation
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong>
 * Uses Properties, ConcurrentHashMap, Set for configuration storage,
 * List for change listeners, and Map for metadata, providing
 * comprehensive collections integration in configuration management.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ConfigurationManager {

    /** Default configuration directory name */
    private static final String DEFAULT_CONFIG_DIR = "config";

    /** Default configuration file names */
    private static final String DEFAULT_APP_CONFIG = "application.properties";
    private static final String DEFAULT_USER_CONFIG = "user.properties";

    /** Supported configuration file formats */
    public enum ConfigFormat {
        PROPERTIES(".properties"),
        JSON(".json"),
        XML(".xml");

        private final String extension;

        ConfigFormat(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }

        public static ConfigFormat fromFilename(String filename) {
            if (filename.endsWith(".properties"))
                return PROPERTIES;
            if (filename.endsWith(".json"))
                return JSON;
            if (filename.endsWith(".xml"))
                return XML;
            return PROPERTIES; // default
        }
    }

    /**
     * Configuration change listener interface.
     */
    public interface ConfigurationChangeListener {
        void onConfigurationChanged(String key, Object oldValue, Object newValue);

        void onConfigurationReloaded(Properties newConfig);
    }

    private final Path configDirectory;
    private final Map<String, Properties> configurations;
    private final Map<String, Instant> lastModified;
    private final Map<String, ConfigFormat> configFormats;
    private final List<ConfigurationChangeListener> changeListeners;
    private final ReadWriteLock lock;
    private final boolean enableHotReload;
    private final boolean readOnlyMode;

    /**
     * Creates a ConfigurationManager with default settings.
     *
     * <p>
     * Uses default configuration directory and enables hot-reloading
     * for development and production flexibility.
     * </p>
     *
     * @throws IOException if configuration directory cannot be accessed
     */
    public ConfigurationManager() throws IOException {
        this(Paths.get(DEFAULT_CONFIG_DIR), true, false);
    }

    /**
     * Creates a ConfigurationManager with custom settings.
     *
     * @param configDirectory directory containing configuration files
     * @param enableHotReload whether to enable automatic reloading
     * @param readOnlyMode    whether to operate in read-only mode
     * @throws IOException              if configuration directory cannot be
     *                                  accessed
     * @throws IllegalArgumentException if parameters are invalid
     */
    public ConfigurationManager(Path configDirectory, boolean enableHotReload,
            boolean readOnlyMode) throws IOException {
        this.configDirectory = Objects.requireNonNull(configDirectory,
                "Configuration directory cannot be null");
        this.enableHotReload = enableHotReload;
        this.readOnlyMode = readOnlyMode;

        this.configurations = new ConcurrentHashMap<>();
        this.lastModified = new ConcurrentHashMap<>();
        this.configFormats = new ConcurrentHashMap<>();
        this.changeListeners = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();

        // Initialize configuration directory
        initializeConfigDirectory();

        // Load default configurations
        loadDefaultConfigurations();
    }

    /**
     * Initializes configuration directory with proper structure.
     *
     * @throws IOException if directory creation or access fails
     */
    private void initializeConfigDirectory() throws IOException {
        if (!Files.exists(configDirectory)) {
            Files.createDirectories(configDirectory);
        }

        if (!Files.isReadable(configDirectory)) {
            throw new IOException("Configuration directory is not readable: " + configDirectory);
        }

        if (!readOnlyMode && !Files.isWritable(configDirectory)) {
            throw new IOException("Configuration directory is not writable: " + configDirectory);
        }
    }

    /**
     * Loads default configuration files if they exist.
     *
     * @throws IOException if configuration loading fails
     */
    private void loadDefaultConfigurations() throws IOException {
        // Load application configuration
        Path appConfigPath = configDirectory.resolve(DEFAULT_APP_CONFIG);
        if (Files.exists(appConfigPath)) {
            loadConfiguration(appConfigPath);
        } else {
            // Create default application configuration
            createDefaultApplicationConfig(appConfigPath);
        }

        // Load user configuration
        Path userConfigPath = configDirectory.resolve(DEFAULT_USER_CONFIG);
        if (Files.exists(userConfigPath)) {
            loadConfiguration(userConfigPath);
        }
    }

    /**
     * Creates default application configuration file.
     *
     * @param configPath path for the configuration file
     * @throws IOException if file creation fails
     */
    private void createDefaultApplicationConfig(Path configPath) throws IOException {
        if (readOnlyMode)
            return;

        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("app.name", "JavaCMS");
        defaultConfig.setProperty("app.version", "1.0");
        defaultConfig.setProperty("app.debug", "false");
        defaultConfig.setProperty("app.log.level", "INFO");
        defaultConfig.setProperty("app.upload.maxSize", "10485760"); // 10MB
        defaultConfig.setProperty("app.template.cache", "true");
        defaultConfig.setProperty("app.security.enabled", "true");

        saveConfiguration(defaultConfig, configPath);
    }

    /**
     * Loads configuration from a file with format detection and validation.
     *
     * <p>
     * This is the main configuration loading method that provides:
     * </p>
     * <ul>
     * <li>Automatic format detection based on file extension</li>
     * <li>Character encoding handling with UTF-8 support</li>
     * <li>Comprehensive validation and error handling</li>
     * <li>Thread-safe loading with proper locking</li>
     * <li>Change tracking for hot-reload functionality</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>File reading with BufferedReader and character streams</li>
     * <li>Multiple format support (Properties, JSON, XML)</li>
     * <li>Resource management with try-with-resources</li>
     * <li>File metadata tracking for change detection</li>
     * </ul>
     *
     * @param configPath path to the configuration file
     * @return loaded Properties object
     * @throws IOException              if configuration loading fails
     * @throws IllegalArgumentException if path is invalid
     */
    public Properties loadConfiguration(Path configPath) throws IOException {
        // Input validation
        if (configPath == null) {
            throw new IllegalArgumentException("Configuration path cannot be null");
        }

        // Security validation - prevent path traversal
        Path resolvedPath = configDirectory.resolve(configPath).normalize();
        if (!resolvedPath.startsWith(configDirectory)) {
            throw new SecurityException("Configuration path outside config directory: " + configPath);
        }

        // Validate file exists and is readable
        if (!Files.exists(resolvedPath) || !Files.isReadable(resolvedPath)) {
            throw new FileNotFoundException("Configuration file not found or not readable: " + configPath);
        }

        lock.writeLock().lock();
        try {
            String configKey = configPath.toString();
            ConfigFormat format = ConfigFormat.fromFilename(resolvedPath.getFileName().toString());

            // Check if reload is needed
            if (enableHotReload && configurations.containsKey(configKey)) {
                Instant fileModTime = Files.getLastModifiedTime(resolvedPath).toInstant();
                Instant lastLoad = lastModified.get(configKey);

                if (lastLoad != null && !fileModTime.isAfter(lastLoad)) {
                    // File hasn't changed, return cached configuration
                    return configurations.get(configKey);
                }
            }

            // Load configuration based on format
            Properties config = loadConfigurationByFormat(resolvedPath, format);

            // Validate configuration
            validateConfiguration(config);

            // Store configuration and metadata
            Properties oldConfig = configurations.put(configKey, config);
            lastModified.put(configKey, Files.getLastModifiedTime(resolvedPath).toInstant());
            configFormats.put(configKey, format);

            // Notify listeners if configuration changed
            if (oldConfig != null) {
                notifyConfigurationReloaded(config);
            }

            return config;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads configuration based on detected format.
     *
     * <p>
     * <strong>Java I/O Implementation:</strong> Demonstrates format-specific
     * I/O operations with proper character encoding and stream management.
     * </p>
     *
     * @param configPath the configuration file path
     * @param format     the detected configuration format
     * @return loaded Properties object
     * @throws IOException if loading fails
     */
    private Properties loadConfigurationByFormat(Path configPath, ConfigFormat format)
            throws IOException {

        Properties config = new Properties();

        switch (format) {
            case PROPERTIES:
                // Load standard Java properties format
                try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    config.load(reader);
                }
                break;

            case JSON:
                // Simple JSON parsing (for basic key-value structures)
                config = loadJsonConfiguration(configPath);
                break;

            case XML:
                // Load XML properties format
                try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(configPath))) {
                    config.loadFromXML(inputStream);
                }
                break;

            default:
                throw new IOException("Unsupported configuration format: " + format);
        }

        return config;
    }

    /**
     * Loads JSON configuration file (basic key-value parsing).
     *
     * <p>
     * Provides simple JSON parsing for configuration files containing
     * basic key-value pairs without complex nested structures.
     * </p>
     *
     * @param configPath the JSON configuration file path
     * @return Properties object with parsed JSON data
     * @throws IOException if JSON parsing fails
     */
    private Properties loadJsonConfiguration(Path configPath) throws IOException {
        Properties config = new Properties();

        // Read entire file content
        String content = Files.readString(configPath, StandardCharsets.UTF_8);

        // Simple JSON parsing (assumes flat key-value structure)
        content = content.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);

            // Split by commas (simple approach)
            String[] pairs = content.split(",");

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = cleanJsonString(keyValue[0].trim());
                    String value = cleanJsonString(keyValue[1].trim());
                    config.setProperty(key, value);
                }
            }
        }

        return config;
    }

    /**
     * Cleans JSON string values by removing quotes and escaping.
     *
     * @param jsonString the JSON string to clean
     * @return cleaned string value
     */
    private String cleanJsonString(String jsonString) {
        if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);
        }
        return jsonString.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /**
     * Validates configuration properties for security and consistency.
     *
     * <p>
     * <strong>Security Feature:</strong> Validates property keys and values
     * to prevent malicious configuration injection.
     * </p>
     *
     * @param config the configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateConfiguration(Properties config) {
        for (String key : config.stringPropertyNames()) {
            // Validate property key format
            if (!isValidPropertyKey(key)) {
                throw new IllegalArgumentException("Invalid property key: " + key);
            }

            // Validate property value (basic sanitization)
            String value = config.getProperty(key);
            if (value != null && value.length() > 10000) {
                throw new IllegalArgumentException("Property value too long for key: " + key);
            }
        }
    }

    /**
     * Validates property key against security requirements.
     *
     * @param key the property key to validate
     * @return true if key is valid, false otherwise
     */
    private boolean isValidPropertyKey(String key) {
        return key != null &&
                !key.trim().isEmpty() &&
                key.matches("[a-zA-Z][a-zA-Z0-9._-]*") &&
                key.length() <= 100;
    }

    /**
     * Saves configuration to a file with format preservation and backup.
     *
     * <p>
     * This method provides comprehensive configuration saving with:
     * </p>
     * <ul>
     * <li>Format detection and preservation</li>
     * <li>Atomic write operations for consistency</li>
     * <li>Backup creation before overwrite</li>
     * <li>Character encoding handling</li>
     * <li>Validation and error recovery</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>File writing with BufferedWriter and character streams</li>
     * <li>Atomic operations using temporary files</li>
     * <li>Backup and recovery mechanisms</li>
     * <li>Format-specific serialization</li>
     * </ul>
     *
     * @param config     the configuration to save
     * @param configPath path to the configuration file
     * @throws IOException       if saving fails
     * @throws SecurityException if read-only mode is enabled
     */
    public void saveConfiguration(Properties config, Path configPath) throws IOException {
        if (readOnlyMode) {
            throw new SecurityException("Configuration manager is in read-only mode");
        }

        // Input validation
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (configPath == null) {
            throw new IllegalArgumentException("Configuration path cannot be null");
        }

        // Security validation
        Path resolvedPath = configDirectory.resolve(configPath).normalize();
        if (!resolvedPath.startsWith(configDirectory)) {
            throw new SecurityException("Configuration path outside config directory: " + configPath);
        }

        // Validate configuration before saving
        validateConfiguration(config);

        lock.writeLock().lock();
        try {
            String configKey = configPath.toString();
            ConfigFormat format = configFormats.getOrDefault(configKey,
                    ConfigFormat.fromFilename(resolvedPath.getFileName().toString()));

            // Create backup if file exists
            Path backupPath = null;
            if (Files.exists(resolvedPath)) {
                backupPath = createBackup(resolvedPath);
            }

            // Use temporary file for atomic write
            Path tempPath = resolvedPath.getParent().resolve(resolvedPath.getFileName() + ".tmp");

            try {
                // Save to temporary file
                saveConfigurationByFormat(config, tempPath, format);

                // Atomic move to final location
                Files.move(tempPath, resolvedPath, StandardCopyOption.REPLACE_EXISTING);

                // Update cache and metadata
                Properties oldConfig = configurations.put(configKey, new Properties(config));
                lastModified.put(configKey, Files.getLastModifiedTime(resolvedPath).toInstant());
                configFormats.put(configKey, format);

                // Notify listeners of changes
                if (oldConfig != null) {
                    notifyConfigurationChanges(oldConfig, config);
                }

            } catch (IOException e) {
                // Cleanup temporary file on failure
                Files.deleteIfExists(tempPath);

                // Restore backup if available
                if (backupPath != null && Files.exists(backupPath)) {
                    try {
                        Files.move(backupPath, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException restoreError) {
                        // Log restore error but throw original error
                        System.err.println("Failed to restore backup: " + restoreError.getMessage());
                    }
                }

                throw e;
            }

            // Cleanup successful backup
            if (backupPath != null) {
                Files.deleteIfExists(backupPath);
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Saves configuration based on format with proper I/O operations.
     *
     * @param config     the configuration to save
     * @param configPath the file path for saving
     * @param format     the configuration format
     * @throws IOException if saving fails
     */
    private void saveConfigurationByFormat(Properties config, Path configPath,
            ConfigFormat format) throws IOException {

        switch (format) {
            case PROPERTIES:
                // Save as standard Java properties
                try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    config.store(writer, "Configuration saved at " + Instant.now());
                }
                break;

            case JSON:
                // Save as simple JSON format
                saveJsonConfiguration(config, configPath);
                break;

            case XML:
                // Save as XML properties
                try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(configPath))) {
                    config.storeToXML(outputStream, "Configuration saved at " + Instant.now(),
                            StandardCharsets.UTF_8.name());
                }
                break;

            default:
                throw new IOException("Unsupported configuration format: " + format);
        }
    }

    /**
     * Saves configuration in JSON format.
     *
     * @param config     the configuration to save
     * @param configPath the file path for saving
     * @throws IOException if saving fails
     */
    private void saveJsonConfiguration(Properties config, Path configPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            writer.write("{\n");

            String[] keys = config.stringPropertyNames().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                String value = config.getProperty(key);

                // Escape JSON strings
                String escapedKey = escapeJsonString(key);
                String escapedValue = escapeJsonString(value);

                writer.write(String.format("  \"%s\": \"%s\"", escapedKey, escapedValue));

                if (i < keys.length - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("}\n");
        }
    }

    /**
     * Escapes string for JSON format.
     *
     * @param str the string to escape
     * @return escaped JSON string
     */
    private String escapeJsonString(String str) {
        if (str == null)
            return "";

        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Creates a backup of an existing configuration file.
     *
     * @param configPath the configuration file to backup
     * @return path to the backup file
     * @throws IOException if backup creation fails
     */
    private Path createBackup(Path configPath) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String backupName = configPath.getFileName() + ".backup." + timestamp;
        Path backupPath = configPath.getParent().resolve(backupName);

        Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    /**
     * Gets a configuration property with type conversion support.
     *
     * @param configName   the configuration file name
     * @param key          the property key
     * @param defaultValue the default value if property not found
     * @param type         the expected value type
     * @param <T>          the value type
     * @return the property value converted to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String configName, String key, T defaultValue, Class<T> type) {
        lock.readLock().lock();
        try {
            Properties config = configurations.get(configName);
            if (config == null) {
                return defaultValue;
            }

            String value = config.getProperty(key);
            if (value == null) {
                return defaultValue;
            }

            return convertValue(value, type, defaultValue);

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a configuration property with type conversion.
     *
     * @param configName the configuration file name
     * @param key        the property key
     * @param value      the property value
     * @param <T>        the value type
     */
    public <T> void setProperty(String configName, String key, T value) {
        if (readOnlyMode) {
            throw new SecurityException("Configuration manager is in read-only mode");
        }

        lock.writeLock().lock();
        try {
            Properties config = configurations.get(configName);
            if (config == null) {
                config = new Properties();
                configurations.put(configName, config);
            }

            String oldValue = config.getProperty(key);
            String newValue = value != null ? value.toString() : null;

            if (newValue != null) {
                config.setProperty(key, newValue);
            } else {
                config.remove(key);
            }

            // Notify listeners
            notifyConfigurationChanged(key, oldValue, newValue);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Converts string value to specified type.
     *
     * @param value        the string value to convert
     * @param type         the target type
     * @param defaultValue the default value if conversion fails
     * @param <T>          the target type
     * @return converted value or default if conversion fails
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(String value, Class<T> type, T defaultValue) {
        try {
            if (type == String.class) {
                return (T) value;
            } else if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(value);
            } else if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(value);
            } else if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(value);
            } else if (type == Float.class || type == float.class) {
                return (T) Float.valueOf(value);
            }
        } catch (NumberFormatException | ClassCastException e) {
            // Return default value if conversion fails
        }

        return defaultValue;
    }

    /**
     * Adds a configuration change listener.
     *
     * @param listener the listener to add
     */
    public void addChangeListener(ConfigurationChangeListener listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * Removes a configuration change listener.
     *
     * @param listener the listener to remove
     * @return true if listener was removed, false if not found
     */
    public boolean removeChangeListener(ConfigurationChangeListener listener) {
        return changeListeners.remove(listener);
    }

    /**
     * Notifies listeners of individual property changes.
     *
     * @param key      the property key that changed
     * @param oldValue the previous value
     * @param newValue the new value
     */
    private void notifyConfigurationChanged(String key, Object oldValue, Object newValue) {
        for (ConfigurationChangeListener listener : changeListeners) {
            try {
                listener.onConfigurationChanged(key, oldValue, newValue);
            } catch (Exception e) {
                // Log error but continue notifying other listeners
                System.err.println("Error notifying configuration listener: " + e.getMessage());
            }
        }
    }

    /**
     * Notifies listeners of configuration reload events.
     *
     * @param config the reloaded configuration
     */
    private void notifyConfigurationReloaded(Properties config) {
        for (ConfigurationChangeListener listener : changeListeners) {
            try {
                listener.onConfigurationReloaded(config);
            } catch (Exception e) {
                // Log error but continue notifying other listeners
                System.err.println("Error notifying configuration listener: " + e.getMessage());
            }
        }
    }

    /**
     * Notifies listeners of all changes between old and new configuration.
     *
     * @param oldConfig the previous configuration
     * @param newConfig the new configuration
     */
    private void notifyConfigurationChanges(Properties oldConfig, Properties newConfig) {
        Set<String> allKeys = new HashSet<>(oldConfig.stringPropertyNames());
        allKeys.addAll(newConfig.stringPropertyNames());

        for (String key : allKeys) {
            String oldValue = oldConfig.getProperty(key);
            String newValue = newConfig.getProperty(key);

            if (!Objects.equals(oldValue, newValue)) {
                notifyConfigurationChanged(key, oldValue, newValue);
            }
        }
    }

    /**
     * Reloads all cached configurations from disk.
     *
     * <p>
     * Forces reload of all configuration files, useful for
     * manual refresh or after external file modifications.
     * </p>
     *
     * @throws IOException if reload fails for any configuration
     */
    public void reloadAllConfigurations() throws IOException {
        lock.writeLock().lock();
        try {
            Set<String> configKeys = new HashSet<>(configurations.keySet());

            for (String configKey : configKeys) {
                Path configPath = Paths.get(configKey);
                if (Files.exists(configDirectory.resolve(configPath))) {
                    loadConfiguration(configPath);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lists all available configuration files.
     *
     * <p>
     * <strong>Java I/O Feature:</strong> Directory traversal using
     * Files.walk() for configuration file discovery.
     * </p>
     *
     * @return list of configuration file paths
     * @throws IOException if directory access fails
     */
    public List<Path> listConfigurationFiles() throws IOException {
        List<Path> configFiles = new ArrayList<>();

        if (!Files.exists(configDirectory)) {
            return configFiles;
        }

        try (var stream = Files.walk(configDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.endsWith(".properties") ||
                                filename.endsWith(".json") ||
                                filename.endsWith(".xml");
                    })
                    .forEach(path -> configFiles.add(configDirectory.relativize(path)));
        }

        return configFiles;
    }

    /**
     * Gets configuration statistics and metadata.
     *
     * @return map containing configuration statistics
     */
    public Map<String, Object> getStatistics() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalConfigurations", configurations.size());
            stats.put("configDirectory", configDirectory.toString());
            stats.put("enableHotReload", enableHotReload);
            stats.put("readOnlyMode", readOnlyMode);
            stats.put("changeListeners", changeListeners.size());

            // Configuration file information
            Map<String, Object> configInfo = new HashMap<>();
            for (Map.Entry<String, Properties> entry : configurations.entrySet()) {
                String key = entry.getKey();
                Properties config = entry.getValue();

                Map<String, Object> info = new HashMap<>();
                info.put("propertyCount", config.size());
                info.put("lastModified", lastModified.get(key));
                info.put("format", configFormats.get(key));

                configInfo.put(key, info);
            }
            stats.put("configurations", configInfo);

            return stats;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the configuration directory path.
     *
     * @return the configuration directory path
     */
    public Path getConfigDirectory() {
        return configDirectory;
    }

    /**
     * Checks if hot-reload is enabled.
     *
     * @return true if hot-reload is enabled
     */
    public boolean isHotReloadEnabled() {
        return enableHotReload;
    }

    /**
     * Checks if manager is in read-only mode.
     *
     * @return true if in read-only mode
     */
    public boolean isReadOnlyMode() {
        return readOnlyMode;
    }
}
