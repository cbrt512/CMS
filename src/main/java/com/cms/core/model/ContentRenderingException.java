package com.cms.core.model;

/**
 * Exception thrown when content rendering fails.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing controlled error handling for content rendering operations. It
 * shields
 * technical rendering errors from end users while maintaining detailed
 * information
 * for debugging and logging purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * technical
 * rendering errors (template parsing failures, missing resources, format
 * conversion
 * issues) from propagating to end users as raw stack traces or technical error
 * messages.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by
 * providing user-friendly error messages for rendering failures while
 * maintaining
 * technical details for system administrators and developers.
 * </p>
 *
 * @see com.cms.core.model.Content#render(String, java.util.Map)
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentRenderingException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** The content type that failed to render */
    private final String contentType;

    /** The rendering format that was requested */
    private final String requestedFormat;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /**
     * Constructs a new ContentRenderingException with a general rendering message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public ContentRenderingException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content could not be displayed";
        this.contentType = null;
        this.requestedFormat = null;
    }

    /**
     * Constructs a new ContentRenderingException with specific rendering context.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures detailed technical context
     * for debugging while providing a sanitized user message.
     * </p>
     *
     * @param message         The detailed technical error message
     * @param userMessage     The user-friendly error message for display
     * @param contentType     The type of content that failed to render
     * @param requestedFormat The format that was requested
     */
    public ContentRenderingException(String message, String userMessage, String contentType, String requestedFormat) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Content could not be displayed";
        this.contentType = contentType;
        this.requestedFormat = requestedFormat;
    }

    /**
     * Constructs a new ContentRenderingException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the rendering failure
     */
    public ContentRenderingException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content could not be displayed";
        this.contentType = null;
        this.requestedFormat = null;
    }

    /**
     * Constructs a new ContentRenderingException with full context and cause.
     *
     * @param message         The detailed technical error message
     * @param userMessage     The user-friendly error message for display
     * @param contentType     The type of content that failed to render
     * @param requestedFormat The format that was requested
     * @param cause           The underlying cause of the rendering failure
     */
    public ContentRenderingException(String message, String userMessage, String contentType,
            String requestedFormat, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Content could not be displayed";
        this.contentType = contentType;
        this.requestedFormat = requestedFormat;
    }

    /**
     * Returns the type of content that failed to render.
     *
     * @return The content type, or null if not specified
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the format that was requested for rendering.
     *
     * @return The requested format, or null if not specified
     */
    public String getRequestedFormat() {
        return requestedFormat;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical rendering implementation details.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Creates a rendering exception for unsupported format errors.
     *
     * @param contentType       The content type being rendered
     * @param unsupportedFormat The unsupported format that was requested
     * @return A new ContentRenderingException for the unsupported format
     */
    public static ContentRenderingException unsupportedFormat(String contentType, String unsupportedFormat) {
        String technical = String.format("Content type '%s' does not support rendering format '%s'",
                contentType, unsupportedFormat);
        String user = String.format("The requested display format '%s' is not available for this content type",
                unsupportedFormat);
        return new ContentRenderingException(technical, user, contentType, unsupportedFormat);
    }

    /**
     * Creates a rendering exception for template processing errors.
     *
     * @param contentType   The content type being rendered
     * @param format        The rendering format being attempted
     * @param templateError The template processing error details
     * @return A new ContentRenderingException for the template error
     */
    public static ContentRenderingException templateError(String contentType, String format, String templateError) {
        String technical = String.format("Template processing failed for %s content in %s format: %s",
                contentType, format, templateError);
        String user = "Content could not be displayed due to a formatting issue. Please try again later.";
        return new ContentRenderingException(technical, user, contentType, format);
    }

    /**
     * Creates a rendering exception for missing resource errors.
     *
     * @param contentType     The content type being rendered
     * @param format          The rendering format being attempted
     * @param missingResource The missing resource identifier
     * @return A new ContentRenderingException for the missing resource
     */
    public static ContentRenderingException missingResource(String contentType, String format, String missingResource) {
        String technical = String.format("Required resource '%s' not found for rendering %s content in %s format",
                missingResource, contentType, format);
        String user = "Some content resources are currently unavailable. Please try again later.";
        return new ContentRenderingException(technical, user, contentType, format);
    }
}
