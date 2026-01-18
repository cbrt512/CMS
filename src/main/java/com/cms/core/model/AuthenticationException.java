package com.cms.core.model;

/**
 * Exception thrown when user authentication fails.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing secure error handling for authentication operations. It shields
 * sensitive
 * authentication details from potential attackers while providing meaningful
 * feedback
 * to legitimate users.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * disclosure of
 * sensitive authentication information (account existence, password criteria,
 * system
 * architecture details) while still providing appropriate feedback for
 * legitimate use cases.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Carefully balances security and
 * usability by
 * providing enough information for legitimate users to resolve authentication
 * issues without
 * revealing information that could be exploited by attackers.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by providing
 * controlled exception propagation for authentication failures with
 * security-conscious
 * error messaging.
 * </p>
 *
 * @see com.cms.core.model.User#authenticate(String)
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class AuthenticationException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /**
     * Constructs a new AuthenticationException with a general authentication
     * message.
     *
     * @param message     The detailed technical error message for logging
     * @param userMessage The user-friendly error message for display
     */
    public AuthenticationException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "Authentication failed";
    }

    /**
     * Constructs a new AuthenticationException with a cause.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures technical authentication
     * failures
     * (system errors, password hashing failures, etc.) while providing sanitized
     * user messages
     * that don't reveal system architecture or implementation details.
     * </p>
     *
     * @param message     The detailed technical error message for logging
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the authentication failure
     */
    public AuthenticationException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "Authentication failed";
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing sensitive authentication implementation details.
     * </p>
     *
     * <p>
     * <strong>Security Consideration:</strong> The returned message is carefully
     * crafted to provide helpful feedback without disclosing information that could
     * be used for reconnaissance or attacks.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Creates an authentication exception for invalid credentials.
     *
     * <p>
     * <strong>Security Note:</strong> Uses generic messaging to avoid revealing
     * whether the username exists or if only the password was incorrect, preventing
     * username enumeration attacks.
     * </p>
     *
     * @return A new AuthenticationException for invalid credentials
     */
    public static AuthenticationException invalidCredentials() {
        String technical = "Authentication failed - invalid username or password";
        String user = "Invalid username or password. Please check your credentials and try again.";
        return new AuthenticationException(technical, user);
    }

    /**
     * Creates an authentication exception for account lockout situations.
     *
     * @param lockoutEndTime Description of when the lockout will end
     * @return A new AuthenticationException for account lockout
     */
    public static AuthenticationException accountLocked(String lockoutEndTime) {
        String technical = String.format("Authentication blocked - account locked until %s", lockoutEndTime);
        String user = String.format("Your account has been temporarily locked due to multiple failed login attempts. " +
                "Please try again after %s.", lockoutEndTime);
        return new AuthenticationException(technical, user);
    }

    /**
     * Creates an authentication exception for disabled accounts.
     *
     * @return A new AuthenticationException for disabled accounts
     */
    public static AuthenticationException accountDisabled() {
        String technical = "Authentication failed - account is disabled";
        String user = "Your account has been disabled. Please contact an administrator for assistance.";
        return new AuthenticationException(technical, user);
    }

    /**
     * Creates an authentication exception for system errors.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Shields technical system error details
     * while providing a generic user message that doesn't reveal system
     * architecture.
     * </p>
     *
     * @param technicalDetails The technical details of the system error
     * @return A new AuthenticationException for system errors
     */
    public static AuthenticationException systemError(String technicalDetails) {
        String technical = String.format("Authentication system error: %s", technicalDetails);
        String user = "Authentication is temporarily unavailable. Please try again in a few moments.";
        return new AuthenticationException(technical, user);
    }

    /**
     * Creates an authentication exception for password expiration.
     *
     * @return A new AuthenticationException for expired passwords
     */
    public static AuthenticationException passwordExpired() {
        String technical = "Authentication failed - password has expired";
        String user = "Your password has expired. Please reset your password to continue.";
        return new AuthenticationException(technical, user);
    }
}
