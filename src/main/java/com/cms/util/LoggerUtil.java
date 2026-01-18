package com.cms.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging operations throughout the CMS application.
 *
 * <p>
 * This class provides centralized logging functionality with different log
 * levels
 * (DEBUG, INFO, WARN, ERROR) and consistent formatting. It implements the
 * logging
 * framework for the CMS application.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Logging Framework
 * - Centralized logging mechanism
 * - Multiple log levels (DEBUG, INFO, WARN, ERROR)
 * - Timestamp formatting and structured output
 * - Exception logging with stack traces
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Utility/Helper pattern with static methods
 * for easy access from anywhere in the application.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class LoggerUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_FORMAT = "[%s] %s - %s: %s";

    // Log levels
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private LoggerUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Logs a debug message.
     *
     * @param component The component/class name generating the log
     * @param message   The debug message
     */
    public static void logDebug(String component, String message) {
        log(DEBUG, component, message);
    }

    /**
     * Logs an informational message.
     *
     * @param component The component/class name generating the log
     * @param message   The informational message
     */
    public static void logInfo(String component, String message) {
        log(INFO, component, message);
    }

    /**
     * Logs a warning message.
     *
     * @param component The component/class name generating the log
     * @param message   The warning message
     */
    public static void logWarn(String component, String message) {
        log(WARN, component, message);
    }

    /**
     * Logs an error message.
     *
     * @param component The component/class name generating the log
     * @param message   The error message
     */
    public static void logError(String component, String message) {
        log(ERROR, component, message);
    }

    /**
     * Logs an error message with exception details.
     *
     * @param component The component/class name generating the log
     * @param message   The error message
     * @param throwable The exception that caused the error
     */
    public static void logError(String component, String message, Throwable throwable) {
        log(ERROR, component, message + " - Exception: " + throwable.getMessage());
        if (throwable.getCause() != null) {
            log(ERROR, component, "Caused by: " + throwable.getCause().getMessage());
        }
    }

    /**
     * Core logging method that formats and outputs log messages.
     *
     * @param level     The log level (DEBUG, INFO, WARN, ERROR)
     * @param component The component/class name generating the log
     * @param message   The log message
     */
    private static void log(String level, String component, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String formattedMessage = String.format(LOG_FORMAT, timestamp, level, component, message);

        // For this simple implementation, we'll just print to System.out
        // In a real application, this might write to files, databases, etc.
        if (ERROR.equals(level) || WARN.equals(level)) {
            System.err.println(formattedMessage);
        } else {
            System.out.println(formattedMessage);
        }
    }

    /**
     * Checks if debug logging is enabled.
     * For this simple implementation, always returns true.
     *
     * @return true if debug logging is enabled
     */
    public static boolean isDebugEnabled() {
        return true;
    }

    /**
     * Logs method entry for debugging purposes.
     *
     * @param component  The component/class name
     * @param methodName The method being entered
     */
    public static void logMethodEntry(String component, String methodName) {
        if (isDebugEnabled()) {
            logDebug(component, "Entering method: " + methodName);
        }
    }

    /**
     * Logs method exit for debugging purposes.
     *
     * @param component  The component/class name
     * @param methodName The method being exited
     */
    public static void logMethodExit(String component, String methodName) {
        if (isDebugEnabled()) {
            logDebug(component, "Exiting method: " + methodName);
        }
    }
}
