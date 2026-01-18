package com.cms.core.model;

/**
 * Exception thrown when content management operations fail.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing user-friendly error messages for content management failures while
 * maintaining
 * detailed technical information for debugging and system administration
 * purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * technical
 * content management errors (database failures, validation errors, file system
 * issues)
 * from propagating to end users as raw stack traces or technical error
 * messages.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by providing
 * controlled exception propagation for content management operations with
 * user-friendly
 * error messages and proper error handling without exposing technical
 * implementation details.
 * </p>
 *
 * @see com.cms.core.model.Site
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentManagementException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /** The content ID involved in the operation, if applicable */
    private final String contentId;

    /** The operation that was being performed when the error occurred */
    private final String operation;

    /**
     * Constructs a new ContentManagementException with a general message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public ContentManagementException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content operation failed";
        this.contentId = null;
        this.operation = null;
    }

    /**
     * Constructs a new ContentManagementException with operation context.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures detailed technical context
     * for debugging while providing sanitized user messages that don't reveal
     * implementation details.
     * </p>
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param contentId   The ID of the content involved in the operation
     * @param operation   The operation being performed when the error occurred
     */
    public ContentManagementException(String message, String userMessage, String contentId, String operation) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content operation failed";
        this.contentId = contentId;
        this.operation = operation;
    }

    /**
     * Constructs a new ContentManagementException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the content management failure
     */
    public ContentManagementException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content operation failed";
        this.contentId = null;
        this.operation = null;
    }

    /**
     * Constructs a new ContentManagementException with full context and cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param contentId   The ID of the content involved in the operation
     * @param operation   The operation being performed when the error occurred
     * @param cause       The underlying cause of the content management failure
     */
    public ContentManagementException(String message, String userMessage, String contentId,
            String operation, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content operation failed";
        this.contentId = contentId;
        this.operation = operation;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical content management implementation details.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the content ID involved in the failed operation.
     *
     * @return The content ID, or null if not applicable
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Returns the operation that was being performed when the error occurred.
     *
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Creates a content management exception for content not found errors.
     *
     * @param contentId The ID of the content that was not found
     * @return A new ContentManagementException for the not found error
     */
    public static ContentManagementException contentNotFound(String contentId) {
        String technical = String.format("Content with ID '%s' not found in the system", contentId);
        String user = "The requested content could not be found. It may have been deleted or moved.";
        return new ContentManagementException(technical, user, contentId, "retrieve");
    }

    /**
     * Creates a content management exception for access denied errors.
     *
     * @param contentId The ID of the content that access was denied to
     * @param operation The operation that was denied
     * @return A new ContentManagementException for the access denied error
     */
    public static ContentManagementException accessDenied(String contentId, String operation) {
        String technical = String.format("Access denied for content '%s' during operation '%s'",
                contentId, operation);
        String user = "You don't have permission to perform this action on the selected content.";
        return new ContentManagementException(technical, user, contentId, operation);
    }

    /**
     * Creates a content management exception for content validation errors.
     *
     * @param contentId         The ID of the content that failed validation
     * @param validationDetails The validation error details
     * @return A new ContentManagementException for the validation error
     */
    public static ContentManagementException validationError(String contentId, String validationDetails) {
        String technical = String.format("Content validation failed for ID '%s': %s",
                contentId, validationDetails);
        String user = "The content could not be saved because it contains invalid information. " +
                "Please check your input and try again.";
        return new ContentManagementException(technical, user, contentId, "validate");
    }

    /**
     * Creates a content management exception for storage errors.
     *
     * @param contentId    The ID of the content that couldn't be stored
     * @param storageError The storage error details
     * @return A new ContentManagementException for the storage error
     */
    public static ContentManagementException storageError(String contentId, String storageError) {
        String technical = String.format("Storage operation failed for content '%s': %s",
                contentId, storageError);
        String user = "The content could not be saved due to a temporary system issue. " +
                "Please try again in a moment.";
        return new ContentManagementException(technical, user, contentId, "save");
    }

    /**
     * Creates a content management exception for concurrent modification errors.
     *
     * @param contentId The ID of the content that was concurrently modified
     * @return A new ContentManagementException for the concurrent modification
     *         error
     */
    public static ContentManagementException concurrentModification(String contentId) {
        String technical = String.format("Content '%s' was modified by another user during this operation",
                contentId);
        String user = "This content was modified by another user while you were editing it. " +
                "Please refresh and try your changes again.";
        return new ContentManagementException(technical, user, contentId, "update");
    }
}
