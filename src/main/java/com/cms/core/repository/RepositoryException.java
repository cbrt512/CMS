package com.cms.core.repository;

/**
 * Exception thrown when repository operations fail.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing user-friendly error messages for data access failures while
 * maintaining
 * detailed technical information for system administrators and debugging
 * purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * technical
 * data access errors (database connection failures, query syntax errors,
 * constraint
 * violations) from propagating to end users as raw stack traces or technical
 * messages.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Supports Exception Shielding by providing
 * controlled exception propagation for repository operations with user-friendly
 * error
 * messages and proper error handling without exposing data layer implementation
 * details.
 * </p>
 *
 * @see com.cms.core.repository.Repository
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class RepositoryException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /** The entity type involved in the operation, if applicable */
    private final String entityType;

    /** The repository operation that failed */
    private final String operation;

    /**
     * Constructs a new RepositoryException with a general message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public RepositoryException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Data operation failed";
        this.entityType = null;
        this.operation = null;
    }

    /**
     * Constructs a new RepositoryException with operation context.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures technical context for
     * debugging
     * while providing sanitized user messages that don't reveal data layer
     * architecture
     * or database implementation details.
     * </p>
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param entityType  The type of entity involved in the operation
     * @param operation   The repository operation that failed
     */
    public RepositoryException(String message, String userMessage, String entityType, String operation) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Data operation failed";
        this.entityType = entityType;
        this.operation = operation;
    }

    /**
     * Constructs a new RepositoryException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the repository failure
     */
    public RepositoryException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Data operation failed";
        this.entityType = null;
        this.operation = null;
    }

    /**
     * Constructs a new RepositoryException with full context and cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param entityType  The type of entity involved in the operation
     * @param operation   The repository operation that failed
     * @param cause       The underlying cause of the repository failure
     */
    public RepositoryException(String message, String userMessage, String entityType,
            String operation, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Data operation failed";
        this.entityType = entityType;
        this.operation = operation;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical data access implementation details.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the entity type involved in the failed operation.
     *
     * @return The entity type, or null if not specified
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the operation that failed.
     *
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Creates a repository exception for entity not found errors.
     *
     * @param entityType The type of entity that was not found
     * @param entityId   The ID of the entity that was not found
     * @return A new RepositoryException for the not found error
     */
    public static RepositoryException entityNotFound(String entityType, Object entityId) {
        String technical = String.format("%s with ID '%s' not found in repository", entityType, entityId);
        String user = "The requested item could not be found. It may have been deleted or moved.";
        return new RepositoryException(technical, user, entityType, "findById");
    }

    /**
     * Creates a repository exception for data access errors.
     *
     * @param entityType       The type of entity involved in the operation
     * @param operation        The operation that failed
     * @param technicalDetails The technical details of the error
     * @return A new RepositoryException for the data access error
     */
    public static RepositoryException dataAccessError(String entityType, String operation, String technicalDetails) {
        String technical = String.format("Data access error during %s operation on %s: %s",
                operation, entityType, technicalDetails);
        String user = "A temporary data access issue occurred. Please try again in a moment.";
        return new RepositoryException(technical, user, entityType, operation);
    }

    /**
     * Creates a repository exception for constraint violation errors.
     *
     * @param entityType        The type of entity that violated constraints
     * @param constraintDetails The constraint violation details
     * @return A new RepositoryException for the constraint violation
     */
    public static RepositoryException constraintViolation(String entityType, String constraintDetails) {
        String technical = String.format("Constraint violation for %s: %s", entityType, constraintDetails);
        String user = "The operation could not be completed because it would violate data integrity rules.";
        return new RepositoryException(technical, user, entityType, "constraint_check");
    }
}
