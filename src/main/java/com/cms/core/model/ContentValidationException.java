package com.cms.core.model;

/**
 * Exception thrown when content validation fails.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing user-friendly error messages for content validation failures while
 * maintaining detailed technical information for debugging purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - shields
 * technical
 * validation details from end users while providing meaningful error messages
 * that
 * help users understand and correct validation issues.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by
 * providing controlled exception propagation with user-friendly messages and
 * proper error handling without exposing technical stack traces.
 * </p>
 *
 * @see com.cms.core.model.Content#validate()
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentValidationException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** The field that failed validation, if applicable */
    private final String fieldName;

    /** The invalid value that caused the validation failure */
    private final Object invalidValue;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /**
     * Constructs a new ContentValidationException with a general validation
     * message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public ContentValidationException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content validation failed";
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Constructs a new ContentValidationException with field-specific information.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Provides detailed technical information
     * for logging while maintaining a user-friendly message for display.
     * </p>
     *
     * @param message      The detailed technical error message
     * @param userMessage  The user-friendly error message for display
     * @param fieldName    The name of the field that failed validation
     * @param invalidValue The value that caused the validation failure
     */
    public ContentValidationException(String message, String userMessage, String fieldName, Object invalidValue) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content validation failed";
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    /**
     * Constructs a new ContentValidationException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the validation failure
     */
    public ContentValidationException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content validation failed";
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Returns the name of the field that failed validation.
     *
     * @return The field name, or null if not field-specific
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the invalid value that caused the validation failure.
     *
     * @return The invalid value, or null if not applicable
     */
    public Object getInvalidValue() {
        return invalidValue;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical implementation details.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Creates a validation exception for required field violations.
     *
     * @param fieldName The name of the required field
     * @return A new ContentValidationException for the missing required field
     */
    public static ContentValidationException requiredField(String fieldName) {
        String technical = String.format("Required field '%s' is missing or empty", fieldName);
        String user = String.format("The %s field is required and cannot be empty", fieldName);
        return new ContentValidationException(technical, user, fieldName, null);
    }

    /**
     * Creates a validation exception for field length violations.
     *
     * @param fieldName     The name of the field with length issues
     * @param currentLength The actual length of the field
     * @param maxLength     The maximum allowed length
     * @return A new ContentValidationException for the length violation
     */
    public static ContentValidationException fieldTooLong(String fieldName, int currentLength, int maxLength) {
        String technical = String.format("Field '%s' length %d exceeds maximum of %d",
                fieldName, currentLength, maxLength);
        String user = String.format("The %s must be %d characters or less (currently %d characters)",
                fieldName, maxLength, currentLength);
        return new ContentValidationException(technical, user, fieldName, currentLength);
    }

    /**
     * Creates a validation exception for invalid format violations.
     *
     * @param fieldName      The name of the field with format issues
     * @param value          The invalid value
     * @param expectedFormat Description of the expected format
     * @return A new ContentValidationException for the format violation
     */
    public static ContentValidationException invalidFormat(String fieldName, Object value, String expectedFormat) {
        String technical = String.format("Field '%s' has invalid format. Value: %s, Expected: %s",
                fieldName, value, expectedFormat);
        String user = String.format("The %s format is invalid. Expected format: %s",
                fieldName, expectedFormat);
        return new ContentValidationException(technical, user, fieldName, value);
    }
}
