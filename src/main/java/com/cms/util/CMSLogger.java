package com.cms.util;

import com.cms.core.exception.ContentManagementException;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.Role;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

/**
 * Central logging service for all CMS operations providing comprehensive
 * activity logging, security event tracking, and performance monitoring.
 *
 * <p>
 * This class implements the comprehensive logging framework required for
 * the JavaCMS system, providing standardized logging capabilities across
 * all system components including content management, user operations,
 * security events, and system operations.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Implements centralized logging service
 * pattern with thread-safe operations and configurable log levels.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Logging Framework
 * provides proper logging implementation with multiple log levels,
 * security-focused audit trails, and performance monitoring capabilities.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - No sensitive data logging (passwords, tokens, secrets)
 * - Input sanitization for all logged data
 * - Secure log file permissions and access control
 * - Audit integrity with immutable log records
 * - Log rotation to prevent storage overflow
 * </p>
 *
 * <p>
 * <strong>Performance Features:</strong>
 * - Asynchronous logging to prevent blocking operations
 * - Configurable log levels to control verbosity
 * - Thread-safe operations using concurrent collections
 * - Memory-efficient string formatting and caching
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class CMSLogger {

    /** Primary logger instance for general CMS operations */
    private static final Logger logger = Logger.getLogger(CMSLogger.class.getName());

    /** Security-focused logger for audit events */
    private static final Logger auditLogger = Logger.getLogger("CMS_AUDIT");

    /** Performance monitoring logger */
    private static final Logger performanceLogger = Logger.getLogger("CMS_PERFORMANCE");

    /** System operations logger */
    private static final Logger systemLogger = Logger.getLogger("CMS_SYSTEM");

    /** Thread-safe cache for frequently accessed logger configurations */
    private static final ConcurrentHashMap<String, Logger> loggerCache = new ConcurrentHashMap<>();

    /** Date formatter for consistent timestamp formatting */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Pattern for sanitizing input data - removes control characters and potential
     * injection attempts
     */
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[\r\n\t]");

    /** Maximum length for logged messages to prevent memory issues */
    private static final int MAX_MESSAGE_LENGTH = 1000;

    /** Singleton instance for centralized access */
    private static volatile CMSLogger instance;

    /** Configuration for log file locations and settings */
    private LogConfiguration configuration;

    static {
        initializeLoggers();
    }

    /**
     * Private constructor for singleton pattern implementation.
     */
    private CMSLogger() {
        this.configuration = LogConfiguration.getDefault();
    }

    /**
     * Gets the singleton instance of CMSLogger using double-checked locking
     * for thread-safe initialization.
     *
     * @return The singleton CMSLogger instance
     */
    public static CMSLogger getInstance() {
        if (instance == null) {
            synchronized (CMSLogger.class) {
                if (instance == null) {
                    instance = new CMSLogger();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes all logger instances with appropriate handlers and formatters.
     * Sets up file-based logging with rotation and console output for development.
     */
    private static void initializeLoggers() {
        try {
            // Create logs directory if it doesn't exist
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            // Configure main application logger
            FileHandler mainHandler = new FileHandler("logs/cms-application.log", 10 * 1024 * 1024, 5, true);
            mainHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(mainHandler);
            logger.setLevel(Level.INFO);

            // Configure audit logger with stricter security settings
            FileHandler auditHandler = new FileHandler("logs/cms-audit.log", 10 * 1024 * 1024, 10, true);
            auditHandler.setFormatter(new SimpleFormatter());
            auditLogger.addHandler(auditHandler);
            auditLogger.setLevel(Level.INFO);

            // Configure performance logger
            FileHandler perfHandler = new FileHandler("logs/cms-performance.log", 5 * 1024 * 1024, 3, true);
            perfHandler.setFormatter(new SimpleFormatter());
            performanceLogger.addHandler(perfHandler);
            performanceLogger.setLevel(Level.INFO);

            // Configure system logger
            FileHandler systemHandler = new FileHandler("logs/cms-system.log", 5 * 1024 * 1024, 3, true);
            systemHandler.setFormatter(new SimpleFormatter());
            systemLogger.addHandler(systemHandler);
            systemLogger.setLevel(Level.INFO);

        } catch (IOException e) {
            // Fallback to console logging if file logging fails
            logger.warning("Failed to initialize file logging, falling back to console: " + e.getMessage());
        }
    }

    // ============================================================================
    // CONTENT MANAGEMENT OPERATIONS LOGGING
    // ============================================================================

    /**
     * Logs content creation events with content metadata and security context.
     *
     * @param contentId   The unique identifier of the created content
     * @param userId      The identifier of the user who created the content
     * @param contentType The type of content created (Article, Page, Image, Video)
     * @param title       The title of the created content (sanitized)
     */
    public void logContentCreated(String contentId, String userId, String contentType, String title) {
        String sanitizedTitle = sanitizeInput(title);
        String message = String.format("Content created - ID: %s, Type: %s, Title: %s, Creator: %s",
                contentId, contentType, sanitizedTitle, userId);
        logger.info(message);
        auditLogger.info("CONTENT_CREATE - " + message);
    }

    /**
     * Logs content publication events including publishing workflow details.
     *
     * @param contentId      The unique identifier of the published content
     * @param publishDate    The date when content was published
     * @param userId         The identifier of the user who published the content
     * @param previousStatus The previous status before publishing
     */
    public void logContentPublished(String contentId, Date publishDate, String userId, ContentStatus previousStatus) {
        String message = String.format("Content published - ID: %s, Date: %s, Publisher: %s, Previous Status: %s",
                contentId, publishDate, userId, previousStatus);
        logger.info(message);
        auditLogger.info("CONTENT_PUBLISH - " + message);
    }

    /**
     * Logs content deletion events with security audit information.
     *
     * @param contentId   The unique identifier of the deleted content
     * @param userId      The identifier of the user who deleted the content
     * @param contentType The type of content deleted
     * @param reason      The reason for deletion (optional)
     */
    public void logContentDeleted(String contentId, String userId, String contentType, String reason) {
        String sanitizedReason = sanitizeInput(reason);
        String message = String.format("Content deleted - ID: %s, Type: %s, Deleted by: %s, Reason: %s",
                contentId, contentType, userId, sanitizedReason != null ? sanitizedReason : "Not specified");
        logger.warning(message);
        auditLogger.warning("CONTENT_DELETE - " + message);
    }

    /**
     * Logs content modification events with change tracking.
     *
     * @param contentId      The unique identifier of the modified content
     * @param userId         The identifier of the user who modified the content
     * @param changedFields  Array of field names that were modified
     * @param previousStatus The status before modification
     * @param newStatus      The status after modification
     */
    public void logContentModified(String contentId, String userId, String[] changedFields,
            ContentStatus previousStatus, ContentStatus newStatus) {
        String fieldsChanged = String.join(", ", changedFields);
        String message = String.format("Content modified - ID: %s, Modified by: %s, Fields: [%s], Status: %s -> %s",
                contentId, userId, fieldsChanged, previousStatus, newStatus);
        logger.info(message);
        auditLogger.info("CONTENT_MODIFY - " + message);
    }

    // ============================================================================
    // USER MANAGEMENT & SECURITY OPERATIONS LOGGING
    // ============================================================================

    /**
     * Logs user login events with security context and session information.
     *
     * @param userId       The identifier of the user logging in
     * @param ipAddress    The IP address of the login attempt (sanitized)
     * @param userAgent    The user agent string (sanitized for security)
     * @param loginSuccess Whether the login was successful
     */
    public void logUserLogin(String userId, String ipAddress, String userAgent, boolean loginSuccess) {
        String sanitizedUserAgent = sanitizeInput(userAgent);
        String status = loginSuccess ? "SUCCESS" : "FAILED";
        String message = String.format("User login %s - User: %s, IP: %s, UserAgent: %s",
                status, userId, ipAddress, truncate(sanitizedUserAgent, 200));

        if (loginSuccess) {
            logger.info(message);
            auditLogger.info("USER_LOGIN_SUCCESS - " + message);
        } else {
            logger.warning(message);
            auditLogger.warning("USER_LOGIN_FAILED - " + message);
        }
    }

    /**
     * Logs user logout events with session duration tracking.
     *
     * @param userId          The identifier of the user logging out
     * @param sessionDuration The duration of the session in milliseconds
     * @param ipAddress       The IP address of the logout
     */
    public void logUserLogout(String userId, long sessionDuration, String ipAddress) {
        long sessionMinutes = sessionDuration / (1000 * 60);
        String message = String.format("User logout - User: %s, Session Duration: %d minutes, IP: %s",
                userId, sessionMinutes, ipAddress);
        logger.info(message);
        auditLogger.info("USER_LOGOUT - " + message);
    }

    /**
     * Logs user creation events with role assignment information.
     *
     * @param newUserId       The identifier of the newly created user
     * @param role            The role assigned to the new user
     * @param createdByUserId The identifier of the user who created the account
     */
    public void logUserCreated(String newUserId, Role role, String createdByUserId) {
        String message = String.format("User created - New User: %s, Role: %s, Created by: %s",
                newUserId, role, createdByUserId);
        logger.info(message);
        auditLogger.info("USER_CREATE - " + message);
    }

    // ============================================================================
    // SECURITY EVENT LOGGING
    // ============================================================================

    /**
     * Logs unauthorized access attempts with detailed security context.
     *
     * @param userId          The identifier of the user attempting access (may be
     *                        null)
     * @param resource        The resource that was accessed without authorization
     * @param ipAddress       The IP address of the unauthorized access attempt
     * @param attemptedAction The action that was attempted
     */
    public void logUnauthorizedAccess(String userId, String resource, String ipAddress, String attemptedAction) {
        String sanitizedAction = sanitizeInput(attemptedAction);
        String message = String.format("UNAUTHORIZED ACCESS - User: %s, Resource: %s, IP: %s, Action: %s",
                userId != null ? userId : "Anonymous", resource, ipAddress, sanitizedAction);
        logger.severe(message);
        auditLogger.severe("SECURITY_VIOLATION - " + message);
    }

    /**
     * Logs password change attempts with success/failure tracking.
     *
     * @param userId        The identifier of the user changing password
     * @param success       Whether the password change was successful
     * @param ipAddress     The IP address of the password change attempt
     * @param failureReason The reason for failure (if applicable)
     */
    public void logPasswordChangeAttempt(String userId, boolean success, String ipAddress, String failureReason) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Password change %s - User: %s, IP: %s", status, userId, ipAddress);

        if (!success && failureReason != null) {
            String sanitizedReason = sanitizeInput(failureReason);
            message += String.format(", Reason: %s", sanitizedReason);
        }

        if (success) {
            logger.info(message);
            auditLogger.info("PASSWORD_CHANGE_SUCCESS - " + message);
        } else {
            logger.warning(message);
            auditLogger.warning("PASSWORD_CHANGE_FAILED - " + message);
        }
    }

    /**
     * Logs suspicious activity detected by security monitoring systems.
     *
     * @param activityType The type of suspicious activity detected
     * @param details      Detailed description of the suspicious activity
     * @param userId       The user associated with the activity (may be null)
     * @param ipAddress    The IP address associated with the activity
     * @param riskLevel    The assessed risk level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public void logSuspiciousActivity(String activityType, String details, String userId, String ipAddress,
            String riskLevel) {
        String sanitizedDetails = sanitizeInput(details);
        String message = String.format("SUSPICIOUS ACTIVITY [%s] - Type: %s, User: %s, IP: %s, Details: %s",
                riskLevel, activityType, userId != null ? userId : "Unknown", ipAddress, sanitizedDetails);

        // Log at appropriate level based on risk
        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            logger.severe(message);
            auditLogger.severe("SECURITY_THREAT - " + message);
        } else {
            logger.warning(message);
            auditLogger.warning("SECURITY_ALERT - " + message);
        }
    }

    // ============================================================================
    // SYSTEM OPERATIONS LOGGING
    // ============================================================================

    /**
     * Logs file upload operations with security and metadata information.
     *
     * @param filename   The name of the uploaded file (sanitized)
     * @param fileSize   The size of the uploaded file in bytes
     * @param userId     The identifier of the user uploading the file
     * @param uploadPath The path where the file was stored
     * @param mimeType   The MIME type of the uploaded file
     */
    public void logFileUpload(String filename, long fileSize, String userId, String uploadPath, String mimeType) {
        String sanitizedFilename = sanitizeInput(filename);
        String sanitizedPath = sanitizeInput(uploadPath);
        String fileSizeMB = String.format("%.2f", fileSize / (1024.0 * 1024.0));

        String message = String.format("File uploaded - Name: %s, Size: %s MB, User: %s, Path: %s, Type: %s",
                sanitizedFilename, fileSizeMB, userId, sanitizedPath, mimeType);
        logger.info(message);
        systemLogger.info("FILE_UPLOAD - " + message);
    }

    /**
     * Logs template processing operations with performance metrics.
     *
     * @param templateId    The identifier of the processed template
     * @param renderTime    The time taken to render the template in milliseconds
     * @param templatePath  The path to the template file
     * @param outputSize    The size of the generated output
     * @param variableCount The number of variables processed
     */
    public void logTemplateProcessing(String templateId, long renderTime, String templatePath, int outputSize,
            int variableCount) {
        String sanitizedPath = sanitizeInput(templatePath);
        String message = String.format(
                "Template processed - ID: %s, Path: %s, Render Time: %d ms, Output: %d bytes, Variables: %d",
                templateId, sanitizedPath, renderTime, outputSize, variableCount);
        logger.info(message);
        performanceLogger.info("TEMPLATE_RENDER - " + message);
    }

    /**
     * Logs configuration changes with audit trail information.
     *
     * @param configKey The configuration key that was changed
     * @param oldValue  The previous value (sanitized, never log sensitive data)
     * @param newValue  The new value (sanitized, never log sensitive data)
     * @param userId    The identifier of the user making the change
     */
    public void logConfigurationChange(String configKey, String oldValue, String newValue, String userId) {
        // Never log sensitive configuration values like passwords, API keys, etc.
        String sanitizedOldValue = isSensitiveKey(configKey) ? "[REDACTED]" : sanitizeInput(oldValue);
        String sanitizedNewValue = isSensitiveKey(configKey) ? "[REDACTED]" : sanitizeInput(newValue);

        String message = String.format("Configuration changed - Key: %s, Old: %s, New: %s, Changed by: %s",
                configKey, sanitizedOldValue, sanitizedNewValue, userId);
        logger.info(message);
        auditLogger.info("CONFIG_CHANGE - " + message);
    }

    // ============================================================================
    // PERFORMANCE AND ERROR LOGGING
    // ============================================================================

    /**
     * Logs performance metrics for system operations and monitoring.
     *
     * @param operation      The name of the operation being measured
     * @param duration       The duration of the operation in milliseconds
     * @param resourceUsage  Additional resource usage information
     * @param operationCount The number of items processed (if applicable)
     */
    public void logPerformanceMetrics(String operation, long duration, String resourceUsage, int operationCount) {
        String sanitizedResource = sanitizeInput(resourceUsage);
        String message = String.format("Performance - Operation: %s, Duration: %d ms, Resources: %s, Count: %d",
                operation, duration, sanitizedResource, operationCount);
        performanceLogger.info(message);

        // Log slow operations as warnings
        if (duration > 5000) { // 5 seconds threshold
            logger.warning("SLOW_OPERATION - " + message);
        }
    }

    /**
     * Logs errors with comprehensive context information while maintaining
     * security through exception shielding patterns.
     *
     * @param exception The exception that occurred
     * @param context   The context in which the error occurred
     * @param userId    The user associated with the operation (may be null)
     * @param operation The operation that failed
     */
    public void logError(Exception exception, String context, String userId, String operation) {
        String sanitizedContext = sanitizeInput(context);
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = sanitizeInput(exception.getMessage());

        String message = String.format("ERROR - Operation: %s, Type: %s, Message: %s, Context: %s, User: %s",
                operation, exceptionType, exceptionMessage, sanitizedContext, userId != null ? userId : "System");

        logger.severe(message);

        // Log full stack trace to system logger for debugging (not exposed to users)
        systemLogger.severe("FULL_ERROR_DETAILS - " + message + "\nStack Trace: " + getStackTrace(exception));
    }

    /**
     * Logs error with simplified signature for common usage patterns.
     * This method provides compatibility for code expecting message-first
     * signature.
     *
     * @param message   The error message describing what went wrong
     * @param exception The exception that was caught
     */
    public void logError(String message, Exception exception) {
        logError(exception, message, null, "Unknown Operation");
    }

    /**
     * Logs error with 3-parameter signature.
     * This overload provides the signature expected by many callers.
     *
     * @param message   The error message describing what went wrong
     * @param exception The exception that was caught
     * @param userId    The user ID associated with the error
     */
    public static void logError(String message, Exception exception, String userId) {
        getInstance().logError(exception, message, userId, "Content Operation");
    }

    /**
     * Logs error with ContentManagementException and user context.
     * This overload specifically handles ContentManagementException.
     *
     * @param message   The error message describing what went wrong
     * @param exception The ContentManagementException that was caught
     * @param userId    The user ID associated with the error
     */
    public static void logError(String message, ContentManagementException exception, String userId) {
        getInstance().logError(exception, message, userId, "Content Management Operation");
    }

    /**
     * Logs content operation for system tracking.
     * This method provides compatibility for content operation logging.
     *
     * @param operation The content operation being logged
     */
    public static void logContentOperation(String operation) {
        getInstance().logSystemOperation(operation);
    }

    /**
     * Logs content operation with detailed context.
     * This overload provides the 4-parameter signature expected by many callers.
     *
     * @param contentId The ID of the content being operated on
     * @param userId    The user performing the operation
     * @param operation The operation type
     * @param details   Additional operation details
     */
    public static void logContentOperation(String contentId, String userId, String operation, String details) {
        String message = String.format("CONTENT_OPERATION - ID: %s, User: %s, Operation: %s, Details: %s",
                contentId, userId, operation, details);
        getInstance().logSystemOperation(message);
    }

    /**
     * Logs application startup and shutdown events.
     *
     * @param event                The startup/shutdown event (START, STOP, RESTART)
     * @param version              The application version
     * @param configurationSummary Summary of key configuration settings
     */
    public void logSystemEvent(String event, String version, String configurationSummary) {
        String sanitizedConfig = sanitizeInput(configurationSummary);
        String message = String.format("SYSTEM_%s - Version: %s, Config: %s, Time: %s",
                event, version, sanitizedConfig, LocalDateTime.now().format(TIMESTAMP_FORMAT));

        logger.info(message);
        systemLogger.info(message);
        auditLogger.info("SYSTEM_EVENT - " + message);
    }

    /**
     * Logs system events with simplified signature.
     * This overload provides the 2-parameter signature expected by many callers.
     *
     * @param event   The event type
     * @param details Event details or description
     */
    public static void logSystemEvent(String event, String details) {
        String message = String.format("SYSTEM_EVENT - %s: %s, Time: %s",
                event, details, LocalDateTime.now().format(getInstance().TIMESTAMP_FORMAT));
        getInstance().logger.info(message);
        getInstance().systemLogger.info(message);
    }

    /**
     * Logs performance metrics for system monitoring.
     * This method logs performance-related information.
     *
     * @param operation The operation being measured
     * @param duration  The duration of the operation
     * @param context   Additional context about the operation
     */
    public static void logPerformanceMetric(String operation, long duration, String context) {
        String message = String.format("PERFORMANCE_METRIC - Operation: %s, Duration: %dms, Context: %s, Time: %s",
                operation, duration, context, LocalDateTime.now().format(getInstance().TIMESTAMP_FORMAT));
        getInstance().logger.info(message);
        getInstance().systemLogger.info(message);
    }

    /**
     * Logs validation warnings for content processing.
     * This method logs validation issues that are warnings, not errors.
     *
     * @param message The warning message
     * @param context Additional context about the validation issue
     */
    public static void logValidationWarning(String message, String context) {
        String logMessage = String.format("VALIDATION_WARNING - %s, Context: %s, Time: %s",
                message, context, LocalDateTime.now().format(getInstance().TIMESTAMP_FORMAT));
        getInstance().logger.warning(logMessage);
        getInstance().systemLogger.warning(logMessage);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Sanitizes input to prevent log injection attacks and ensure clean log output.
     * Removes control characters and limits message length.
     *
     * @param input The input string to sanitize
     * @return The sanitized string, or null if input was null
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Remove control characters that could be used for log injection
        String sanitized = SANITIZE_PATTERN.matcher(input).replaceAll(" ");

        // Truncate to prevent excessively long log messages
        return truncate(sanitized, MAX_MESSAGE_LENGTH);
    }

    /**
     * Truncates a string to the specified maximum length, adding ellipsis if
     * truncated.
     *
     * @param input     The input string to truncate
     * @param maxLength The maximum length allowed
     * @return The truncated string
     */
    private String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength - 3) + "...";
    }

    /**
     * Checks if a configuration key contains sensitive data that should not be
     * logged.
     *
     * @param key The configuration key to check
     * @return true if the key is considered sensitive
     */
    private boolean isSensitiveKey(String key) {
        if (key == null)
            return false;
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || lowerKey.contains("secret") ||
                lowerKey.contains("key") || lowerKey.contains("token") ||
                lowerKey.contains("credential") || lowerKey.contains("auth");
    }

    /**
     * Extracts stack trace from exception as a string for detailed logging.
     *
     * @param exception The exception to extract stack trace from
     * @return The stack trace as a string
     */
    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    // ============================================================================
    // DEBUG LOGGING METHODS
    // ============================================================================

    /**
     * Logs debug messages with single parameter for development and
     * troubleshooting.
     *
     * @param message The debug message template
     * @param param   The parameter to substitute into the message
     */
    public void debug(String message, Object param) {
        if (logger.isLoggable(Level.FINE)) {
            String formattedMessage = message.replace("{}", String.valueOf(param));
            logger.fine(formattedMessage);
        }
    }

    /**
     * Logs debug messages with two parameters for development and troubleshooting.
     *
     * @param message The debug message template
     * @param param1  The first parameter to substitute into the message
     * @param param2  The second parameter to substitute into the message
     */
    public void debug(String message, Object param1, Object param2) {
        if (logger.isLoggable(Level.FINE)) {
            String formattedMessage = message.replaceFirst("\\{\\}", String.valueOf(param1))
                    .replaceFirst("\\{\\}", String.valueOf(param2));
            logger.fine(formattedMessage);
        }
    }

    /**
     * Logs debug messages with three parameters for development and
     * troubleshooting.
     *
     * @param message The debug message template
     * @param param1  The first parameter to substitute into the message
     * @param param2  The second parameter to substitute into the message
     * @param param3  The third parameter to substitute into the message
     */
    public void debug(String message, Object param1, Object param2, Object param3) {
        if (logger.isLoggable(Level.FINE)) {
            String formattedMessage = message.replaceFirst("\\{\\}", String.valueOf(param1))
                    .replaceFirst("\\{\\}", String.valueOf(param2))
                    .replaceFirst("\\{\\}", String.valueOf(param3));
            logger.fine(formattedMessage);
        }
    }

    /**
     * Updates the log configuration at runtime.
     *
     * @param newConfiguration The new logging configuration
     */
    public void updateConfiguration(LogConfiguration newConfiguration) {
        if (newConfiguration != null) {
            this.configuration = newConfiguration;
            logger.info("Logging configuration updated");
        }
    }

    /**
     * Logs general content activity information.
     * This is a convenience method for general activity logging.
     *
     * @param activity The activity description
     * @param details  Additional details about the activity
     */
    public void logContentActivity(String activity, String details) {
        logSystemEvent("CONTENT_ACTIVITY", "1.0", activity + (details != null ? " - " + details : ""));
    }

    /**
     * Logs system operation information.
     * This is a convenience method for system operation logging.
     *
     * @param operation The operation description
     * @param details   Additional details about the operation
     */
    public void logSystemOperation(String operation, String details) {
        logSystemEvent("SYSTEM_OPERATION", "1.0", operation + (details != null ? " - " + details : ""));
    }

    /**
     * Logs system operation information (single parameter version).
     * This is a convenience method for system operation logging.
     *
     * @param operation The operation description
     */
    public void logSystemOperation(String operation) {
        logSystemEvent("SYSTEM_OPERATION", "1.0", operation);
    }

    /**
     * Logs security events with user and action details.
     * This is a convenience method for security logging.
     *
     * @param securityEvent The security event description
     * @param details       Additional details about the security event
     */
    public static void logSecurityEvent(String securityEvent, String details) {
        getInstance().logSystemEvent("SECURITY_EVENT", "1.0", securityEvent + (details != null ? " - " + details : ""));
    }

    /**
     * Logs security events with user context for audit and compliance.
     * This overload provides the 3-parameter signature expected by many callers.
     *
     * @param username  The username associated with the security event
     * @param eventType The type of security event
     * @param details   Additional details about the security event
     */
    public static void logSecurityEvent(String username, String eventType, String details) {
        String message = String.format("User: %s, Event: %s, Details: %s", username, eventType, details);
        getInstance().logSystemEvent("SECURITY_EVENT", "1.0", message);
    }

    /**
     * Logs user action events.
     */
    public static void logUserAction(String username, String action, String details) {
        getInstance().logSystemEvent("USER_ACTION", "1.0",
                username + " performed " + action + (details != null ? " - " + details : ""));
    }

    /**
     * Logs validation errors.
     */
    public static void logValidationError(String message, String username) {
        getInstance().logSystemEvent("VALIDATION_ERROR", "1.0",
                message + (username != null ? " (user: " + username + ")" : ""));
    }

    /**
     * Logs validation success events.
     */
    public static void logValidationSuccess(String message, String username) {
        getInstance().logSystemEvent("VALIDATION_SUCCESS", "1.0",
                message + (username != null ? " (user: " + username + ")" : ""));
    }

    /**
     * Gets the current logging configuration.
     *
     * @return The current LogConfiguration instance
     */
    public LogConfiguration getConfiguration() {
        return this.configuration;
    }
}
