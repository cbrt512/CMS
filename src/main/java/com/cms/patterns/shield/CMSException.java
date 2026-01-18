package com.cms.patterns.shield;

/**
 * Abstract base class for all CMS exceptions implementing the Exception Shielding Pattern.
 *
 * <p>This class provides the foundation for the Exception Shielding design pattern,
 * ensuring that technical exceptions are caught and transformed into user-friendly
 * error messages while maintaining detailed logging for debugging purposes.</p>
 *
 * <p><strong>Design Pattern:</strong> Exception Shielding Pattern
 * - Shields technical exceptions from end users
 * - Provides user-friendly error messages
 * - Maintains detailed technical logging for debugging
 * - Ensures application stability by preventing crashes
 * - Implements controlled exception propagation</p>
 *
 * <p><strong>Implementation:</strong> Exception Shielding Pattern.
 * This implementation provides a comprehensive exception hierarchy that shields users from
 * technical implementation details.</p>
 *
 * <p><strong>Security Considerations:</strong>
 * - Prevents stack trace leakage to users
 * - Sanitizes error messages to avoid information disclosure
 * - Maintains audit trail for security monitoring
 * - Implements proper exception propagation control</p>
 *
 * @see com.cms.core.model.ContentManagementException
 * @see com.cms.core.model.UserManagementException
 * @see com.cms.patterns.shield.ExceptionShielder
 * @since 1.0
 * @author Otman Hmich S007924
 */
public abstract class CMSException extends Exception {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * User-friendly error message safe for display to end users.
     * This message is sanitized and does not contain technical details.
     */
    private final String userMessage;

    /**
     * Technical error message containing detailed debugging information.
     * This message is used for logging and should never be displayed to users.
     */
    private final String technicalMessage;

    /**
     * Error code for programmatic error handling and categorization.
     */
    private final ErrorCode errorCode;

    /**
     * Timestamp when the exception was created for audit purposes.
     */
    private final long timestamp;

    /**
     * Constructs a new CMS exception with user and technical messages.
     *
     * @param userMessage User-friendly error message safe for display
     * @param technicalMessage Technical details for logging and debugging
     * @param errorCode Categorization code for this type of error
     * @param cause The underlying cause of this exception (may be null)
     * @throws IllegalArgumentException if userMessage or errorCode is null
     */
    protected CMSException(String userMessage, String technicalMessage,
                          ErrorCode errorCode, Throwable cause) {
        super(technicalMessage, cause);

        if (userMessage == null || errorCode == null) {
            throw new IllegalArgumentException(
                "User message and error code cannot be null");
        }

        this.userMessage = userMessage;
        this.technicalMessage = technicalMessage != null ? technicalMessage : userMessage;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructs a new CMS exception with user message only.
     *
     * @param userMessage User-friendly error message safe for display
     * @param errorCode Categorization code for this type of error
     * @param cause The underlying cause of this exception (may be null)
     */
    protected CMSException(String userMessage, ErrorCode errorCode, Throwable cause) {
        this(userMessage, userMessage, errorCode, cause);
    }

    /**
     * Constructs a new CMS exception without an underlying cause.
     *
     * @param userMessage User-friendly error message safe for display
     * @param technicalMessage Technical details for logging and debugging
     * @param errorCode Categorization code for this type of error
     */
    protected CMSException(String userMessage, String technicalMessage, ErrorCode errorCode) {
        this(userMessage, technicalMessage, errorCode, null);
    }

    /**
     * Returns the user-friendly error message safe for display to end users.
     * This message does not contain technical details or stack traces.
     *
     * @return User-friendly error message
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the technical error message containing detailed debugging information.
     * This message should only be used for logging and never displayed to users.
     *
     * @return Technical error message with debugging details
     */
    public String getTechnicalMessage() {
        return technicalMessage;
    }

    /**
     * Returns the error code for programmatic error handling.
     *
     * @return Error code categorizing this exception type
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the timestamp when this exception was created.
     *
     * @return Exception creation timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns whether this exception should be logged at ERROR level.
     * Override this method in subclasses to customize logging behavior.
     *
     * @return true if this exception requires ERROR level logging
     */
    public boolean shouldLogAsError() {
        return errorCode.getSeverity() == ErrorSeverity.HIGH;
    }

    /**
     * Returns additional context information for error reporting.
     * Subclasses can override this to provide specific context details.
     *
     * @return Additional context information (may be null)
     */
    public String getContext() {
        return null;
    }

    /**
     * Enumeration of error codes for categorizing different types of CMS exceptions.
     * Each error code includes severity level and category information.
     */
    public enum ErrorCode {
        // Content Management Errors
        CONTENT_NOT_FOUND("Content item not found", ErrorSeverity.MEDIUM),
        CONTENT_VALIDATION_FAILED("Content validation failed", ErrorSeverity.MEDIUM),
        CONTENT_CREATION_FAILED("Content creation failed", ErrorSeverity.HIGH),
        CONTENT_UPDATE_FAILED("Content update failed", ErrorSeverity.HIGH),
        CONTENT_DELETE_FAILED("Content deletion failed", ErrorSeverity.HIGH),

        // User Management Errors
        USER_NOT_FOUND("User not found", ErrorSeverity.MEDIUM),
        USER_CREATION_FAILED("User creation failed", ErrorSeverity.HIGH),
        USER_AUTHENTICATION_FAILED("Authentication failed", ErrorSeverity.HIGH),
        USER_AUTHORIZATION_FAILED("Access denied", ErrorSeverity.HIGH),

        // File Operation Errors
        FILE_UPLOAD_FAILED("File upload failed", ErrorSeverity.MEDIUM),
        FILE_PROCESSING_FAILED("File processing failed", ErrorSeverity.MEDIUM),
        FILE_NOT_FOUND("File not found", ErrorSeverity.MEDIUM),

        // Template Processing Errors
        TEMPLATE_NOT_FOUND("Template not found", ErrorSeverity.MEDIUM),
        TEMPLATE_RENDERING_FAILED("Template rendering failed", ErrorSeverity.MEDIUM),

        // Repository Errors
        REPOSITORY_ERROR("Data access error", ErrorSeverity.HIGH),
        DATA_INTEGRITY_ERROR("Data integrity violation", ErrorSeverity.HIGH),

        // System Errors
        SYSTEM_ERROR("System error occurred", ErrorSeverity.HIGH),
        CONFIGURATION_ERROR("Configuration error", ErrorSeverity.HIGH);

        private final String description;
        private final ErrorSeverity severity;

        ErrorCode(String description, ErrorSeverity severity) {
            this.description = description;
            this.severity = severity;
        }

        /**
         * Returns the human-readable description of this error code.
         *
         * @return Error code description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the severity level of this error code.
         *
         * @return Error severity level
         */
        public ErrorSeverity getSeverity() {
            return severity;
        }
    }

    /**
     * Enumeration of error severity levels for exception categorization.
     */
    public enum ErrorSeverity {
        LOW("Low severity - informational"),
        MEDIUM("Medium severity - warning"),
        HIGH("High severity - error");

        private final String description;

        ErrorSeverity(String description) {
            this.description = description;
        }

        /**
         * Returns the description of this severity level.
         *
         * @return Severity level description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Returns a formatted string representation of this exception for logging.
     * Includes timestamp, error code, and technical details.
     *
     * @return Formatted exception string for logging
     */
    @Override
    public String toString() {
        return String.format(
            "[%s] %s - %s: %s (Timestamp: %d)",
            getClass().getSimpleName(),
            errorCode,
            errorCode.getDescription(),
            technicalMessage,
            timestamp
        );
    }
}
