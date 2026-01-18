package com.cms.core.model;

/**
 * Exception thrown when user creation fails.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing user-friendly error messages for user creation failures while
 * maintaining
 * detailed technical information for system administrators and debugging
 * purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * technical
 * user creation errors (password hashing failures, database connection issues,
 * validation
 * errors) from propagating to end users as raw stack traces or technical error
 * messages.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by providing
 * controlled exception propagation with user-friendly messages for account
 * creation issues.
 * </p>
 *
 * @see com.cms.core.model.User
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class UserCreationException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /**
     * Constructs a new UserCreationException with a general creation message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public UserCreationException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "User account could not be created";
    }

    /**
     * Constructs a new UserCreationException with a cause.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures the underlying technical cause
     * while providing a sanitized user message that doesn't reveal implementation
     * details.
     * </p>
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the user creation failure
     */
    public UserCreationException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "User account could not be created";
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing technical implementation details about user creation
     * processes.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Creates a user creation exception for duplicate username errors.
     *
     * @param username The duplicate username that caused the error
     * @return A new UserCreationException for the duplicate username
     */
    public static UserCreationException duplicateUsername(String username) {
        String technical = String.format("Username '%s' already exists in the system", username);
        String user = "This username is already taken. Please choose a different username.";
        return new UserCreationException(technical, user);
    }

    /**
     * Creates a user creation exception for duplicate email errors.
     *
     * @param email The duplicate email that caused the error
     * @return A new UserCreationException for the duplicate email
     */
    public static UserCreationException duplicateEmail(String email) {
        String technical = String.format("Email address '%s' is already registered in the system", email);
        String user = "This email address is already registered. Please use a different email address or try logging in.";
        return new UserCreationException(technical, user);
    }

    /**
     * Creates a user creation exception for password security criteria failures.
     *
     * @param criteria The security criteria that was not met
     * @return A new UserCreationException for the password criteria
     */
    public static UserCreationException passwordCriteria(String criteria) {
        String technical = String.format("Password does not meet security criteria: %s", criteria);
        String user = String.format("Password must meet the following criteria: %s", criteria);
        return new UserCreationException(technical, user);
    }

    /**
     * Creates a user creation exception for system errors during account creation.
     *
     * @param technicalDetails The technical details of the system error
     * @return A new UserCreationException for the system error
     */
    public static UserCreationException systemError(String technicalDetails) {
        String technical = String.format("System error during user creation: %s", technicalDetails);
        String user = "Your account could not be created due to a temporary system issue. Please try again later.";
        return new UserCreationException(technical, user);
    }
}
