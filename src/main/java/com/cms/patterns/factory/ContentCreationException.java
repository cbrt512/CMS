package com.cms.patterns.factory;

/**
 * Exception thrown when content creation through the ContentFactory fails.
 *
 * <p>
 * This exception implements the Exception Shielding pattern,
 * providing user-friendly error messages for content creation failures while
 * maintaining
 * detailed technical information for debugging. It specifically handles errors
 * that occur
 * during the Factory Pattern content creation process.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - shields
 * technical
 * content creation errors (instantiation failures, property validation errors,
 * unsupported
 * types) from end users while providing meaningful error messages that help
 * users understand
 * and correct content creation issues.
 * </p>
 *
 * <p>
 * <strong>Factory Pattern Integration:</strong> This exception is specifically
 * designed
 * to handle errors in the Factory Pattern implementation, providing
 * factory-specific error
 * information while maintaining the Exception Shielding pattern's user-friendly
 * approach.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Supports both Factory Pattern and Exception
 * Shielding patterns by providing controlled error handling for factory
 * operations
 * with appropriate user messaging and technical detail preservation.
 * </p>
 *
 * @see com.cms.patterns.factory.ContentFactory
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentCreationException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /** The content type that failed to be created */
    private final String contentType;

    /** The specific creation step that failed */
    private final String creationStep;

    /**
     * Constructs a new ContentCreationException with a general creation message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public ContentCreationException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content could not be created";
        this.contentType = null;
        this.creationStep = null;
    }

    /**
     * Constructs a new ContentCreationException with specific factory context.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures detailed technical context
     * for debugging while providing sanitized user messages that don't reveal
     * factory implementation details or system architecture.
     * </p>
     *
     * @param message      The detailed technical error message
     * @param userMessage  The user-friendly error message for display
     * @param contentType  The type of content that failed to be created
     * @param creationStep The specific step in the creation process that failed
     */
    public ContentCreationException(String message, String userMessage, String contentType, String creationStep) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content could not be created";
        this.contentType = contentType;
        this.creationStep = creationStep;
    }

    /**
     * Constructs a new ContentCreationException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the content creation failure
     */
    public ContentCreationException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content could not be created";
        this.contentType = null;
        this.creationStep = null;
    }

    /**
     * Constructs a new ContentCreationException with full context and cause.
     *
     * @param message      The detailed technical error message
     * @param userMessage  The user-friendly error message for display
     * @param contentType  The type of content that failed to be created
     * @param creationStep The specific step in the creation process that failed
     * @param cause        The underlying cause of the content creation failure
     */
    public ContentCreationException(String message, String userMessage, String contentType,
            String creationStep, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content could not be created";
        this.contentType = contentType;
        this.creationStep = creationStep;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical factory implementation details or system
     * architecture.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the content type that failed to be created.
     *
     * @return The content type, or null if not specified
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the creation step that failed.
     *
     * @return The creation step, or null if not specified
     */
    public String getCreationStep() {
        return creationStep;
    }

    /**
     * Creates a content creation exception for unsupported content types.
     *
     * <p>
     * <strong>Factory Pattern Error Handling:</strong> Provides specific error
     * handling
     * for cases where the factory is asked to create content types it doesn't
     * support.
     * </p>
     *
     * @param unsupportedType The unsupported content type that was requested
     * @return A new ContentCreationException for the unsupported type
     */
    public static ContentCreationException unsupportedContentType(String unsupportedType) {
        String technical = String.format("Unsupported content type '%s' requested from ContentFactory",
                unsupportedType);
        String user = String.format("The content type '%s' is not supported. " +
                "Supported types are: article, page, image, video.", unsupportedType);
        return new ContentCreationException(technical, user, unsupportedType, "type_validation");
    }

    /**
     * Creates a content creation exception for content validation failures.
     *
     * @param contentType     The content type being created
     * @param validationError The validation error details
     * @return A new ContentCreationException for the validation failure
     */
    public static ContentCreationException validationFailed(String contentType, String validationError) {
        String technical = String.format("Content validation failed for type '%s': %s",
                contentType, validationError);
        String user = "The content could not be created because some needed information is missing or invalid. " +
                "Please check your input and try again.";
        return new ContentCreationException(technical, user, contentType, "validation");
    }

    /**
     * Creates a content creation exception for content validation failures with
     * cause.
     *
     * @param contentType     The content type being created
     * @param validationError The validation error details
     * @param cause           The underlying validation exception
     * @return A new ContentCreationException for the validation failure
     */
    public static ContentCreationException validationFailed(String contentType, String validationError,
            Throwable cause) {
        String technical = String.format("Content validation failed for type '%s': %s",
                contentType, validationError);
        String user = "The content could not be created because some needed information is missing or invalid. " +
                "Please check your input and try again.";
        return new ContentCreationException(technical, user, contentType, "validation", cause);
    }

    /**
     * Creates a content creation exception for instantiation failures.
     *
     * @param contentType        The content type that failed to be instantiated
     * @param instantiationError The instantiation error details
     * @return A new ContentCreationException for the instantiation failure
     */
    public static ContentCreationException creationFailed(String contentType, String instantiationError) {
        String technical = String.format("Content instantiation failed for type '%s': %s",
                contentType, instantiationError);
        String user = "The content could not be created due to a system error. Please try again.";
        return new ContentCreationException(technical, user, contentType, "instantiation");
    }

    /**
     * Creates a content creation exception for instantiation failures with cause.
     *
     * @param contentType        The content type that failed to be instantiated
     * @param instantiationError The instantiation error details
     * @param cause              The underlying exception that caused the
     *                           instantiation failure
     * @return A new ContentCreationException for the instantiation failure
     */
    public static ContentCreationException creationFailed(String contentType, String instantiationError,
            Throwable cause) {
        String technical = String.format("Content instantiation failed for type '%s': %s",
                contentType, instantiationError);
        String user = "The content could not be created due to a system error. Please try again.";
        return new ContentCreationException(technical, user, contentType, "instantiation", cause);
    }

    /**
     * Creates a content creation exception for template not found errors.
     *
     * @param templateName The template that was not found
     * @return A new ContentCreationException for the missing template
     */
    public static ContentCreationException templateNotFound(String templateName) {
        String technical = String.format("Template '%s' not found in template repository", templateName);
        String user = String.format(
                "The template '%s' could not be found. Please check the template name and try again.",
                templateName);
        return new ContentCreationException(technical, user, null, "template_lookup");
    }

    /**
     * Creates a content creation exception for invalid template errors.
     *
     * @param templateName  The invalid template name
     * @param templateError The template error details
     * @return A new ContentCreationException for the invalid template
     */
    public static ContentCreationException invalidTemplate(String templateName, String templateError) {
        String technical = String.format("Invalid template '%s': %s", templateName, templateError);
        String user = String.format("The template '%s' is not valid and cannot be used for content creation.",
                templateName);
        return new ContentCreationException(technical, user, null, "template_validation");
    }

    /**
     * Creates a content creation exception for property-related errors.
     *
     * @param contentType   The content type being created
     * @param propertyName  The property that caused the error
     * @param propertyError The property error details
     * @return A new ContentCreationException for the property error
     */
    public static ContentCreationException propertyError(String contentType, String propertyName,
            String propertyError) {
        String technical = String.format("Property error in %s content creation - %s: %s",
                contentType, propertyName, propertyError);
        String user = String.format("There was an issue with the %s field. Please check the value and try again.",
                propertyName);
        return new ContentCreationException(technical, user, contentType, "property_validation");
    }
}
