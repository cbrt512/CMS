package com.cms.patterns.shield;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentCreationException;
import com.cms.core.repository.RepositoryException;
import com.cms.util.LoggerUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for exception shielding operations in the CMS application.
 *
 * <p>
 * This class provides static methods to shield technical exceptions and convert
 * them
 * into user-friendly error messages. It implements the Exception Shielding
 * pattern by
 * catching low-level technical exceptions and transforming them into
 * appropriate
 * CMS-specific exceptions with sanitized error messages.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern
 * - Shields technical exceptions from end users
 * - Provides centralized exception transformation logic
 * - Maintains comprehensive logging of technical details
 * - Ensures consistent error handling across the application
 * - Prevents sensitive information disclosure
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Exception Shielding Pattern.
 * This utility class complements the CMSException hierarchy by providing
 * centralized shielding operations that transform technical exceptions
 * into user-friendly errors.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong>
 * - Prevents stack trace exposure to users
 * - Sanitizes error messages to prevent information disclosure
 * - Implements controlled exception propagation
 * - Maintains audit logging for security monitoring
 * - Protects against exception-based information leakage
 * </p>
 *
 * @see com.cms.patterns.shield.CMSException
 * @see com.cms.core.model.ContentManagementException
 * @see com.cms.core.model.UserManagementException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class ExceptionShielder {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ExceptionShielder() {
        throw new UnsupportedOperationException(
                "ExceptionShielder is a utility class and cannot be instantiated");
    }

    /**
     * Shields a generic technical exception and converts it to
     * ContentManagementException.
     * This is the primary shielding method used throughout the application.
     *
     * @param technicalException  The technical exception to shield
     * @param userFriendlyMessage User-friendly message to display
     * @param context             Additional context information for logging
     * @return Shielded ContentManagementException safe for user display
     * @throws IllegalArgumentException if technicalException is null
     */
    public static ContentManagementException shield(Exception technicalException,
            String userFriendlyMessage,
            String context) {
        if (technicalException == null) {
            throw new IllegalArgumentException("Technical exception cannot be null");
        }

        // Log the technical details for debugging
        String technicalDetails = String.format(
                "Technical exception shielded: %s - %s. Context: %s",
                technicalException.getClass().getSimpleName(),
                technicalException.getMessage(),
                context != null ? context : "No context provided");

        LoggerUtil.logError("ExceptionShielder", technicalDetails, technicalException);

        // Create appropriate error code based on exception type
        CMSException.ErrorCode errorCode = determineErrorCode(technicalException);

        // Return shielded exception with user-friendly message
        return new ContentManagementException(
                technicalDetails,
                userFriendlyMessage != null ? userFriendlyMessage : "An error occurred",
                errorCode != null ? errorCode.toString() : null,
                "Unknown Operation",
                technicalException);
    }

    /**
     * Shields a technical exception with default user message.
     *
     * @param technicalException The technical exception to shield
     * @param context            Additional context information for logging
     * @return Shielded ContentManagementException
     */
    public static ContentManagementException shield(Exception technicalException, String context) {
        return shield(technicalException, "An unexpected error occurred. Please try again.", context);
    }

    /**
     * Shields a technical exception with minimal context.
     *
     * @param technicalException The technical exception to shield
     * @return Shielded ContentManagementException
     */
    public static ContentManagementException shield(Exception technicalException) {
        return shield(technicalException, null, null);
    }

    /**
     * Shields database-related exceptions with appropriate user messages.
     *
     * @param dbException The database exception to shield
     * @param operation   The database operation that failed
     * @return Shielded RepositoryException
     */
    public static RepositoryException shieldDatabaseException(Exception dbException, String operation) {
        String userMessage = "Unable to complete the requested operation. Please try again later.";
        String technicalMessage = String.format(
                "Database operation failed: %s - %s",
                operation != null ? operation : "Unknown operation",
                dbException.getMessage());

        LoggerUtil.logError("ExceptionShielder",
                "Database exception shielded: " + technicalMessage, dbException);

        return new RepositoryException(
                technicalMessage,
                userMessage,
                "Database",
                "Repository Operation",
                dbException);
    }

    /**
     * Shields file I/O exceptions with appropriate user messages.
     *
     * @param ioException The I/O exception to shield
     * @param fileName    The name of the file being processed (may be null)
     * @return Shielded ContentManagementException
     */
    public static ContentManagementException shieldIOException(IOException ioException, String fileName) {
        String userMessage = fileName != null
                ? String.format("Unable to process file '%s'. Please check the file and try again.",
                        sanitizeFileName(fileName))
                : "File processing failed. Please try again.";

        String technicalMessage = String.format(
                "I/O operation failed for file '%s': %s",
                fileName != null ? fileName : "unknown",
                ioException.getMessage());

        LoggerUtil.logError("ExceptionShielder",
                "I/O exception shielded: " + technicalMessage, ioException);

        return new ContentManagementException(
                technicalMessage,
                userMessage,
                null,
                "File Processing",
                ioException);
    }

    /**
     * Shields authentication and authorization exceptions.
     *
     * @param authException The authentication/authorization exception to shield
     * @param operation     The operation that was being attempted
     * @return Shielded AuthenticationException
     */
    public static AuthenticationException shieldAuthException(Exception authException, String operation) {
        String userMessage = "Access denied. Please check your credentials and try again.";
        String technicalMessage = String.format(
                "Authentication/Authorization failed for operation '%s': %s",
                operation != null ? operation : "unknown",
                authException.getMessage());

        // Use different log level for security-related exceptions
        LoggerUtil.logWarn("ExceptionShielder",
                "Auth exception shielded: " + technicalMessage);

        return new AuthenticationException(
                technicalMessage,
                userMessage,
                authException);
    }

    /**
     * Shields validation exceptions with specific field information.
     *
     * @param validationException The validation exception to shield
     * @param fieldName           The field that failed validation (may be null)
     * @return Shielded ContentValidationException
     */
    public static ContentValidationException shieldValidationException(Exception validationException,
            String fieldName) {
        String userMessage = fieldName != null
                ? String.format("Invalid value provided for %s. Please correct and try again.", fieldName)
                : "Validation failed. Please check your input and try again.";

        String technicalMessage = String.format(
                "Validation failed for field '%s': %s",
                fieldName != null ? fieldName : "unknown",
                validationException.getMessage());

        LoggerUtil.logWarn("ExceptionShielder",
                "Validation exception shielded: " + technicalMessage);

        return new ContentValidationException(
                technicalMessage,
                userMessage,
                validationException);
    }

    /**
     * Shields concurrent operation exceptions (timeouts, execution failures).
     *
     * @param concurrentException The concurrent operation exception to shield
     * @param operation           The concurrent operation that failed
     * @return Shielded ContentManagementException
     */
    public static ContentManagementException shieldConcurrentException(Exception concurrentException,
            String operation) {
        String userMessage = "The system is currently busy. Please try again in a moment.";
        String technicalMessage = String.format(
                "Concurrent operation failed: %s - %s",
                operation != null ? operation : "unknown",
                concurrentException.getMessage());

        LoggerUtil.logError("ExceptionShielder",
                "Concurrent exception shielded: " + technicalMessage, concurrentException);

        CMSException.ErrorCode errorCode = concurrentException instanceof TimeoutException
                ? CMSException.ErrorCode.SYSTEM_ERROR
                : CMSException.ErrorCode.SYSTEM_ERROR;

        return new ContentManagementException(
                technicalMessage,
                userMessage,
                null,
                "Concurrent Operation",
                concurrentException);
    }

    /**
     * Shields operation using functional interface approach.
     * Executes the provided operation and shields any exceptions that occur.
     *
     * @param <T>       The return type of the operation
     * @param operation The operation to execute safely
     * @return The result of the operation
     * @throws ContentManagementException if the operation throws an exception
     */
    public static <T> T shield(java.util.function.Supplier<T> operation) throws ContentManagementException {
        try {
            return operation.get();
        } catch (Exception e) {
            throw shield(e, "Operation failed", "Functional shielding");
        }
    }

    /**
     * Shields operation using functional interface approach with custom user
     * message.
     * Executes the provided operation and shields any exceptions with a custom
     * error message.
     *
     * @param <T>                 The return type of the operation
     * @param operation           The operation to execute safely
     * @param userFriendlyMessage Custom message to display to users on failure
     * @return The result of the operation
     * @throws ContentManagementException if the operation throws an exception
     */
    public static <T> T shield(java.util.function.Supplier<T> operation, String userFriendlyMessage)
            throws ContentManagementException {
        try {
            return operation.get();
        } catch (Exception e) {
            throw shield(e, userFriendlyMessage, "Functional shielding with custom message");
        }
    }

    /**
     * Determines the appropriate error code based on the type of technical
     * exception.
     *
     * @param exception The technical exception to categorize
     * @return Appropriate error code for the exception type
     */
    private static CMSException.ErrorCode determineErrorCode(Exception exception) {
        if (exception instanceof IOException) {
            return CMSException.ErrorCode.FILE_PROCESSING_FAILED;
        } else if (exception instanceof SQLException) {
            return CMSException.ErrorCode.REPOSITORY_ERROR;
        } else if (exception instanceof SecurityException) {
            return CMSException.ErrorCode.USER_AUTHORIZATION_FAILED;
        } else if (exception instanceof IllegalArgumentException ||
                exception instanceof IllegalStateException) {
            return CMSException.ErrorCode.CONTENT_VALIDATION_FAILED;
        } else if (exception instanceof ExecutionException ||
                exception instanceof TimeoutException) {
            return CMSException.ErrorCode.SYSTEM_ERROR;
        } else {
            return CMSException.ErrorCode.SYSTEM_ERROR;
        }
    }

    /**
     * Sanitizes a file name for safe display in user messages.
     * Removes potentially dangerous characters and limits length.
     *
     * @param fileName The file name to sanitize
     * @return Sanitized file name safe for display
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }

        // Remove path separators and limit length
        String sanitized = fileName.replaceAll("[/\\\\:]", "");
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 47) + "...";
        }

        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    /**
     * Provides a safe way to extract error information from any exception.
     * This method never throws exceptions itself.
     *
     * @param exception The exception to extract information from
     * @return Safe error information suitable for logging
     */
    public static String safeGetErrorInfo(Throwable exception) {
        if (exception == null) {
            return "No exception information available";
        }

        try {
            StringBuilder info = new StringBuilder();
            info.append("Exception: ").append(exception.getClass().getSimpleName());

            String message = exception.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                info.append(" - Message: ").append(message);
            }

            Throwable cause = exception.getCause();
            if (cause != null && cause != exception) {
                info.append(" - Cause: ").append(cause.getClass().getSimpleName());
                String causeMessage = cause.getMessage();
                if (causeMessage != null && !causeMessage.trim().isEmpty()) {
                    info.append(" (").append(causeMessage).append(")");
                }
            }

            return info.toString();
        } catch (Exception e) {
            // Fallback if anything goes wrong during error info extraction
            return "Error occurred while extracting exception information: " +
                    exception.getClass().getSimpleName();
        }
    }
}
