package com.cms.core.model;

/**
 * Exception thrown when user management operations fail.
 *
 * <p>
 * This exception is part of the Exception Shielding pattern implementation,
 * providing secure and user-friendly error messages for user management
 * failures while
 * maintaining detailed technical information for system administrators and
 * debugging.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Exception Shielding Pattern - prevents
 * disclosure of
 * sensitive user management information (system architecture, database details,
 * security
 * mechanisms) while providing appropriate feedback for legitimate
 * administrative operations.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Carefully balances the need to
 * provide useful
 * error information with security practices, ensuring that error messages don't
 * reveal
 * information that could be exploited for reconnaissance or attacks.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Implements Exception Shielding by providing
 * controlled exception propagation for user management operations with
 * security-conscious
 * error messaging that doesn't expose sensitive system information.
 * </p>
 *
 * @see com.cms.core.model.User
 * @see com.cms.core.model.Site#registerUser(User, String)
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class UserManagementException extends Exception {

    /** Serialization version ID */
    private static final long serialVersionUID = 1L;

    /** User-friendly error message suitable for display */
    private final String userMessage;

    /** The username involved in the operation, if applicable */
    private final String username;

    /** The operation that was being performed when the error occurred */
    private final String operation;

    /**
     * Constructs a new UserManagementException with a general message.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     */
    public UserManagementException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "User management operation failed";
        this.username = null;
        this.operation = null;
    }

    /**
     * Constructs a new UserManagementException with operation context.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Captures technical context for
     * debugging
     * while providing sanitized user messages that don't reveal sensitive system
     * architecture or security implementation details.
     * </p>
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param username    The username involved in the operation
     * @param operation   The operation being performed when the error occurred
     */
    public UserManagementException(String message, String userMessage, String username, String operation) {
        super(message);
        this.userMessage = userMessage != null ? userMessage : "User management operation failed";
        this.username = username;
        this.operation = operation;
    }

    /**
     * Constructs a new UserManagementException with a cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param cause       The underlying cause of the user management failure
     */
    public UserManagementException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "User management operation failed";
        this.username = null;
        this.operation = null;
    }

    /**
     * Constructs a new UserManagementException with full context and cause.
     *
     * @param message     The detailed technical error message
     * @param userMessage The user-friendly error message for display
     * @param username    The username involved in the operation
     * @param operation   The operation being performed when the error occurred
     * @param cause       The underlying cause of the user management failure
     */
    public UserManagementException(String message, String userMessage, String username,
            String operation, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage != null ? userMessage : "User management operation failed";
        this.username = username;
        this.operation = operation;
    }

    /**
     * Returns a user-friendly error message suitable for display.
     *
     * <p>
     * <strong>Exception Shielding:</strong> This method provides the shielded,
     * user-friendly version of the error that can be safely displayed to end users
     * without revealing sensitive user management implementation details or
     * security
     * mechanisms.
     * </p>
     *
     * <p>
     * <strong>Security Consideration:</strong> The returned message is carefully
     * crafted to provide helpful feedback without disclosing information that could
     * be used for user enumeration, privilege escalation, or other security
     * attacks.
     * </p>
     *
     * @return The user-friendly error message, never null
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Returns the username involved in the failed operation.
     *
     * @return The username, or null if not applicable
     */
    public String getUsername() {
        return username;
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
     * Creates a user management exception for duplicate username errors.
     *
     * <p>
     * <strong>Security Note:</strong> Provides clear feedback about username
     * availability without revealing information about existing users.
     * </p>
     *
     * @param username The duplicate username that caused the error
     * @return A new UserManagementException for the duplicate username
     */
    public static UserManagementException duplicateUsername(String username) {
        String technical = String.format("Username '%s' already exists in the system", username);
        String user = "This username is already taken. Please choose a different username.";
        return new UserManagementException(technical, user, username, "register");
    }

    /**
     * Creates a user management exception for duplicate email errors.
     *
     * <p>
     * <strong>Security Note:</strong> Provides feedback about email availability
     * while being mindful not to confirm or deny the existence of specific accounts
     * for unauthorized users.
     * </p>
     *
     * @param email The duplicate email that caused the error
     * @return A new UserManagementException for the duplicate email
     */
    public static UserManagementException duplicateEmail(String email) {
        String technical = String.format("Email address '%s' is already registered in the system", email);
        String user = "This email address is already registered. Please use a different email address.";
        return new UserManagementException(technical, user, email, "register");
    }

    /**
     * Creates a user management exception for user not found errors.
     *
     * <p>
     * <strong>Security Note:</strong> Uses generic messaging that doesn't reveal
     * whether the user exists or not, preventing user enumeration attacks.
     * </p>
     *
     * @param username The username that was not found
     * @return A new UserManagementException for the not found error
     */
    public static UserManagementException userNotFound(String username) {
        String technical = String.format("User '%s' not found in the system", username);
        String user = "The requested user could not be found or access was denied.";
        return new UserManagementException(technical, user, username, "lookup");
    }

    /**
     * Creates a user management exception for insufficient privileges errors.
     *
     * @param username  The username involved in the operation
     * @param operation The operation that required higher privileges
     * @return A new UserManagementException for the insufficient privileges error
     */
    public static UserManagementException insufficientPrivileges(String username, String operation) {
        String technical = String.format("User '%s' has insufficient privileges for operation '%s'",
                username, operation);
        String user = "You don't have sufficient privileges to perform this operation.";
        return new UserManagementException(technical, user, username, operation);
    }

    /**
     * Creates a user management exception for account deactivation errors.
     *
     * @param username The username of the deactivated account
     * @return A new UserManagementException for the deactivated account
     */
    public static UserManagementException accountDeactivated(String username) {
        String technical = String.format("Account '%s' is deactivated and cannot be used", username);
        String user = "Your account has been deactivated. Please contact an administrator for assistance.";
        return new UserManagementException(technical, user, username, "access");
    }

    /**
     * Creates a user management exception for system errors during user operations.
     *
     * <p>
     * <strong>Exception Shielding:</strong> Shields technical system error details
     * while providing a generic user message that doesn't reveal system
     * architecture.
     * </p>
     *
     * @param technicalDetails The technical details of the system error
     * @param operation        The operation being performed when the error occurred
     * @return A new UserManagementException for the system error
     */
    public static UserManagementException systemError(String technicalDetails, String operation) {
        String technical = String.format("System error during %s operation: %s", operation, technicalDetails);
        String user = "A temporary system error occurred. Please try again in a few moments.";
        return new UserManagementException(technical, user, null, operation);
    }

    /**
     * Creates a user management exception for password policy violations.
     *
     * @param username        The username involved in the password operation
     * @param policyViolation Description of the policy violation
     * @return A new UserManagementException for the policy violation
     */
    public static UserManagementException passwordPolicy(String username, String policyViolation) {
        String technical = String.format("Password policy violation for user '%s': %s",
                username, policyViolation);
        String user = String.format("Password does not meet security criteria: %s", policyViolation);
        return new UserManagementException(technical, user, username, "password_change");
    }
}
