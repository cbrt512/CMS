package com.cms.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Configuration class for the CMS logging framework providing flexible
 * and comprehensive logging settings management.
 *
 * <p>
 * This class manages all logging configuration settings for the JavaCMS
 * system, including log levels, file locations, rotation policies, and
 * security-specific logging configurations. It supports runtime configuration
 * updates and provides sensible defaults for all settings.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Implements Configuration Pattern with
 * Builder-like method chaining for easy setup and modification.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Part of Logging Framework
 * provides configurable logging settings that demonstrate proper
 * logging implementation with multiple log levels and security considerations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * - Configurable log levels for different logger categories
 * - Flexible file location and naming configuration
 * - Log rotation settings (size-based and time-based)
 * - Security-specific logging configurations
 * - Performance tuning options
 * - Runtime configuration updates
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class LogConfiguration {

    /** Default log directory location */
    public static final String DEFAULT_LOG_DIR = "logs";

    /** Default maximum log file size (10MB) */
    public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Default number of log files to keep in rotation */
    public static final int DEFAULT_MAX_FILES = 5;

    /** Default log level for application logging */
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    /** Default log level for audit logging (more verbose) */
    public static final Level DEFAULT_AUDIT_LEVEL = Level.INFO;

    /** Default log level for performance logging */
    public static final Level DEFAULT_PERFORMANCE_LEVEL = Level.INFO;

    /** Default log level for system logging */
    public static final Level DEFAULT_SYSTEM_LEVEL = Level.INFO;

    // Configuration Properties
    private String logDirectory;
    private long maxFileSize;
    private int maxFiles;
    private Level applicationLogLevel;
    private Level auditLogLevel;
    private Level performanceLogLevel;
    private Level systemLogLevel;
    private boolean enableConsoleLogging;
    private boolean enableFileLogging;
    private boolean enableAuditLogging;
    private boolean enablePerformanceLogging;
    private String logFilePattern;
    private Map<String, String> customProperties;

    /**
     * Creates a new LogConfiguration with default settings.
     */
    public LogConfiguration() {
        this.logDirectory = DEFAULT_LOG_DIR;
        this.maxFileSize = DEFAULT_MAX_FILE_SIZE;
        this.maxFiles = DEFAULT_MAX_FILES;
        this.applicationLogLevel = DEFAULT_LOG_LEVEL;
        this.auditLogLevel = DEFAULT_AUDIT_LEVEL;
        this.performanceLogLevel = DEFAULT_PERFORMANCE_LEVEL;
        this.systemLogLevel = DEFAULT_SYSTEM_LEVEL;
        this.enableConsoleLogging = true;
        this.enableFileLogging = true;
        this.enableAuditLogging = true;
        this.enablePerformanceLogging = true;
        this.logFilePattern = "yyyy-MM-dd HH:mm:ss.SSS";
        this.customProperties = new HashMap<>();
    }

    /**
     * Creates a default LogConfiguration instance with sensible defaults
     * for a production CMS environment.
     *
     * @return A LogConfiguration with production-ready defaults
     */
    public static LogConfiguration getDefault() {
        LogConfiguration config = new LogConfiguration();
        config.setApplicationLogLevel(Level.INFO)
                .setAuditLogLevel(Level.INFO)
                .setPerformanceLogLevel(Level.INFO)
                .setSystemLogLevel(Level.WARNING)
                .setMaxFileSize(10 * 1024 * 1024) // 10MB
                .setMaxFiles(5)
                .setEnableConsoleLogging(false) // Disable console logging in production
                .setEnableFileLogging(true)
                .setEnableAuditLogging(true)
                .setEnablePerformanceLogging(true);
        return config;
    }

    /**
     * Creates a LogConfiguration optimized for development environments
     * with more verbose logging and console output enabled.
     *
     * @return A LogConfiguration with development-friendly settings
     */
    public static LogConfiguration getDevDefault() {
        LogConfiguration config = new LogConfiguration();
        config.setApplicationLogLevel(Level.FINE)
                .setAuditLogLevel(Level.INFO)
                .setPerformanceLogLevel(Level.INFO)
                .setSystemLogLevel(Level.INFO)
                .setMaxFileSize(5 * 1024 * 1024) // 5MB for faster rotation in dev
                .setMaxFiles(3)
                .setEnableConsoleLogging(true) // Enable console logging for development
                .setEnableFileLogging(true)
                .setEnableAuditLogging(true)
                .setEnablePerformanceLogging(true);
        return config;
    }

    /**
     * Creates a LogConfiguration optimized for testing environments
     * with minimal logging to avoid cluttering test output.
     *
     * @return A LogConfiguration with test-friendly settings
     */
    public static LogConfiguration getTestDefault() {
        LogConfiguration config = new LogConfiguration();
        config.setApplicationLogLevel(Level.WARNING)
                .setAuditLogLevel(Level.WARNING)
                .setPerformanceLogLevel(Level.OFF)
                .setSystemLogLevel(Level.SEVERE)
                .setMaxFileSize(1024 * 1024) // 1MB
                .setMaxFiles(2)
                .setEnableConsoleLogging(false)
                .setEnableFileLogging(false) // Disable file logging for tests
                .setEnableAuditLogging(false)
                .setEnablePerformanceLogging(false);
        return config;
    }

    // ============================================================================
    // GETTER AND SETTER METHODS WITH FLUENT API
    // ============================================================================

    /**
     * Gets the configured log directory path.
     *
     * @return The log directory path
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * Sets the log directory path.
     *
     * @param logDirectory The path to the log directory
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory != null ? logDirectory : DEFAULT_LOG_DIR;
        return this;
    }

    /**
     * Gets the maximum file size before rotation.
     *
     * @return The maximum file size in bytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Sets the maximum file size before rotation.
     *
     * @param maxFileSize The maximum file size in bytes
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        return this;
    }

    /**
     * Gets the maximum number of log files to keep.
     *
     * @return The maximum number of log files
     */
    public int getMaxFiles() {
        return maxFiles;
    }

    /**
     * Sets the maximum number of log files to keep in rotation.
     *
     * @param maxFiles The maximum number of files to keep
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles > 0 ? maxFiles : DEFAULT_MAX_FILES;
        return this;
    }

    /**
     * Gets the application log level.
     *
     * @return The current application log level
     */
    public Level getApplicationLogLevel() {
        return applicationLogLevel;
    }

    /**
     * Sets the application log level.
     *
     * @param applicationLogLevel The log level for general application logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setApplicationLogLevel(Level applicationLogLevel) {
        this.applicationLogLevel = applicationLogLevel != null ? applicationLogLevel : DEFAULT_LOG_LEVEL;
        return this;
    }

    /**
     * Gets the audit log level.
     *
     * @return The current audit log level
     */
    public Level getAuditLogLevel() {
        return auditLogLevel;
    }

    /**
     * Sets the audit log level for security-related events.
     *
     * @param auditLogLevel The log level for audit logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setAuditLogLevel(Level auditLogLevel) {
        this.auditLogLevel = auditLogLevel != null ? auditLogLevel : DEFAULT_AUDIT_LEVEL;
        return this;
    }

    /**
     * Gets the performance log level.
     *
     * @return The current performance log level
     */
    public Level getPerformanceLogLevel() {
        return performanceLogLevel;
    }

    /**
     * Sets the performance log level for performance monitoring.
     *
     * @param performanceLogLevel The log level for performance logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setPerformanceLogLevel(Level performanceLogLevel) {
        this.performanceLogLevel = performanceLogLevel != null ? performanceLogLevel : DEFAULT_PERFORMANCE_LEVEL;
        return this;
    }

    /**
     * Gets the system log level.
     *
     * @return The current system log level
     */
    public Level getSystemLogLevel() {
        return systemLogLevel;
    }

    /**
     * Sets the system log level for system-related events.
     *
     * @param systemLogLevel The log level for system logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setSystemLogLevel(Level systemLogLevel) {
        this.systemLogLevel = systemLogLevel != null ? systemLogLevel : DEFAULT_SYSTEM_LEVEL;
        return this;
    }

    /**
     * Checks if console logging is enabled.
     *
     * @return true if console logging is enabled
     */
    public boolean isEnableConsoleLogging() {
        return enableConsoleLogging;
    }

    /**
     * Enables or disables console logging.
     *
     * @param enableConsoleLogging true to enable console logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setEnableConsoleLogging(boolean enableConsoleLogging) {
        this.enableConsoleLogging = enableConsoleLogging;
        return this;
    }

    /**
     * Checks if file logging is enabled.
     *
     * @return true if file logging is enabled
     */
    public boolean isEnableFileLogging() {
        return enableFileLogging;
    }

    /**
     * Enables or disables file logging.
     *
     * @param enableFileLogging true to enable file logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setEnableFileLogging(boolean enableFileLogging) {
        this.enableFileLogging = enableFileLogging;
        return this;
    }

    /**
     * Checks if audit logging is enabled.
     *
     * @return true if audit logging is enabled
     */
    public boolean isEnableAuditLogging() {
        return enableAuditLogging;
    }

    /**
     * Enables or disables audit logging.
     *
     * @param enableAuditLogging true to enable audit logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setEnableAuditLogging(boolean enableAuditLogging) {
        this.enableAuditLogging = enableAuditLogging;
        return this;
    }

    /**
     * Checks if performance logging is enabled.
     *
     * @return true if performance logging is enabled
     */
    public boolean isEnablePerformanceLogging() {
        return enablePerformanceLogging;
    }

    /**
     * Enables or disables performance logging.
     *
     * @param enablePerformanceLogging true to enable performance logging
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setEnablePerformanceLogging(boolean enablePerformanceLogging) {
        this.enablePerformanceLogging = enablePerformanceLogging;
        return this;
    }

    /**
     * Gets the log file pattern for timestamp formatting.
     *
     * @return The current log file pattern
     */
    public String getLogFilePattern() {
        return logFilePattern;
    }

    /**
     * Sets the log file pattern for timestamp formatting.
     *
     * @param logFilePattern The pattern string for timestamp formatting
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setLogFilePattern(String logFilePattern) {
        this.logFilePattern = logFilePattern != null ? logFilePattern : "yyyy-MM-dd HH:mm:ss.SSS";
        return this;
    }

    /**
     * Gets a custom property value.
     *
     * @param key The property key
     * @return The property value, or null if not found
     */
    public String getCustomProperty(String key) {
        return customProperties.get(key);
    }

    /**
     * Sets a custom property value.
     *
     * @param key   The property key
     * @param value The property value
     * @return This LogConfiguration instance for method chaining
     */
    public LogConfiguration setCustomProperty(String key, String value) {
        if (key != null) {
            if (value != null) {
                customProperties.put(key, value);
            } else {
                customProperties.remove(key);
            }
        }
        return this;
    }

    /**
     * Gets all custom properties as a read-only map.
     *
     * @return A copy of the custom properties map
     */
    public Map<String, String> getCustomProperties() {
        return new HashMap<>(customProperties);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Creates a copy of this LogConfiguration with the same settings.
     *
     * @return A new LogConfiguration instance with identical settings
     */
    public LogConfiguration copy() {
        LogConfiguration copy = new LogConfiguration();
        copy.logDirectory = this.logDirectory;
        copy.maxFileSize = this.maxFileSize;
        copy.maxFiles = this.maxFiles;
        copy.applicationLogLevel = this.applicationLogLevel;
        copy.auditLogLevel = this.auditLogLevel;
        copy.performanceLogLevel = this.performanceLogLevel;
        copy.systemLogLevel = this.systemLogLevel;
        copy.enableConsoleLogging = this.enableConsoleLogging;
        copy.enableFileLogging = this.enableFileLogging;
        copy.enableAuditLogging = this.enableAuditLogging;
        copy.enablePerformanceLogging = this.enablePerformanceLogging;
        copy.logFilePattern = this.logFilePattern;
        copy.customProperties = new HashMap<>(this.customProperties);
        return copy;
    }

    /**
     * Validates the configuration settings and throws an exception if invalid.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void validate() throws IllegalArgumentException {
        if (logDirectory == null || logDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Log directory cannot be null or empty");
        }

        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Max file size must be positive");
        }

        if (maxFiles <= 0) {
            throw new IllegalArgumentException("Max files must be positive");
        }

        if (logFilePattern == null || logFilePattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Log file pattern cannot be null or empty");
        }

        // Validate that at least one logging output is enabled
        if (!enableConsoleLogging && !enableFileLogging) {
            throw new IllegalArgumentException("At least one logging output (console or file) must be enabled");
        }
    }

    /**
     * Returns a string representation of this configuration for debugging purposes.
     * Note: Does not include sensitive information.
     *
     * @return A string representation of the configuration
     */
    @Override
    public String toString() {
        return String.format("LogConfiguration{" +
                "logDirectory='%s', " +
                "maxFileSize=%d, " +
                "maxFiles=%d, " +
                "applicationLogLevel=%s, " +
                "auditLogLevel=%s, " +
                "performanceLogLevel=%s, " +
                "systemLogLevel=%s, " +
                "enableConsoleLogging=%b, " +
                "enableFileLogging=%b, " +
                "enableAuditLogging=%b, " +
                "enablePerformanceLogging=%b, " +
                "customPropertiesCount=%d" +
                '}',
                logDirectory, maxFileSize, maxFiles,
                applicationLogLevel, auditLogLevel, performanceLogLevel, systemLogLevel,
                enableConsoleLogging, enableFileLogging, enableAuditLogging, enablePerformanceLogging,
                customProperties.size());
    }
}
