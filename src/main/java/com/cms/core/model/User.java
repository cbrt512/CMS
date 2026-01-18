package com.cms.core.model;

import com.cms.util.CMSLogger;
import com.cms.util.AuditLogger;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Represents a user in the JavaCMS system with authentication and authorization
 * capabilities.
 *
 * <p>
 * This class encapsulates user account information, authentication credentials,
 * and
 * authorization roles. It implements secure password handling with salt-based
 * hashing
 * and provides role-based access control for the content management system.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Implements secure password storage
 * using
 * SHA-256 hashing with salt, input sanitization for user data, and controlled
 * access
 * to sensitive information. No credentials are stored in plain text, providing
 * secure credential management.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Uses HashSet&lt;Role&gt; for role
 * management,
 * providing proper Collections Framework usage with type-safe role operations.
 * </p>
 *
 * <p>
 * <strong>Generics:</strong> Implements type-safe collections and operations
 * throughout,
 * providing Generics implementation with parameterized collections and methods.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Supports authentication and authorization
 * functionality
 * required for content management security, integrates with Exception Shielding
 * pattern
 * for secure error handling.
 * </p>
 *
 * @see com.cms.core.model.Role
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class User implements Comparable<User> {

    /** Unique identifier for this user */
    private final String id;

    /** Username for login - must be unique across the system */
    private String username;

    /** User's email address - must be unique and valid */
    private String email;

    /** User's display name */
    private String displayName;

    /** Salted hash of the user's password - never store plain text */
    private String passwordHash;

    /** Salt used for password hashing */
    private String passwordSalt;

    /** Set of roles assigned to this user for authorization */
    private Set<Role> roles;

    /** Indicates if the user account is currently active */
    private boolean active;

    /** Timestamp when the user was created */
    private LocalDateTime createdDate;

    /** Timestamp of the user's last login */
    private LocalDateTime lastLoginDate;

    /** Number of failed login attempts (for security monitoring) */
    private int failedLoginAttempts;

    /** Timestamp when the account was locked (if applicable) */
    private LocalDateTime accountLockedUntil;

    /** Maximum allowed failed login attempts before account lockout */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** Account lockout duration in minutes */
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    /** Logger instance for user operations */
    private static final CMSLogger logger = CMSLogger.getInstance();

    /** Audit logger instance for security events */
    private static final AuditLogger auditLogger = AuditLogger.getInstance();

    /**
     * Constructs a new User with the specified credentials.
     *
     * <p>
     * <strong>Security Implementation:</strong> Immediately hashes the provided
     * password
     * with a generated salt and discards the plain text password. Validates input
     * parameters
     * to prevent security vulnerabilities.
     * </p>
     *
     * <p>
     * <strong>Collections Framework:</strong> Initializes a HashSet for role
     * management,
     * demonstrating proper Collections Framework instantiation and usage.
     * </p>
     *
     * @param username    The unique username for login, must not be null or empty
     * @param email       The user's email address, must be valid format
     * @param displayName The user's display name, must not be null or empty
     * @param password    The plain text password, will be hashed immediately
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws UserCreationException    if password hashing fails
     */
    public User(String username, String email, String displayName, String password)
            throws UserCreationException {
        // Validate required parameters
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Validate email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        this.id = java.util.UUID.randomUUID().toString();
        this.username = sanitizeInput(username.trim());
        this.email = sanitizeInput(email.trim().toLowerCase());
        this.displayName = sanitizeInput(displayName.trim());

        // Generate salt and hash password securely
        try {
            this.passwordSalt = generateSalt();
            this.passwordHash = hashPassword(password, this.passwordSalt);
        } catch (NoSuchAlgorithmException e) {
            throw new UserCreationException("Failed to create secure password hash",
                    "User account could not be created due to a system error", e);
        }

        this.roles = new HashSet<>();
        this.active = true;
        this.createdDate = LocalDateTime.now();
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    /**
     * Authenticates the user with the provided password.
     *
     * <p>
     * <strong>Security Implementation:</strong> Uses secure password comparison
     * with
     * salted hashing. Implements account lockout protection against brute force
     * attacks.
     * Tracks failed login attempts and enforces temporary account lockouts.
     * </p>
     *
     * @param password The plain text password to verify
     * @return true if authentication succeeds, false otherwise
     * @throws AuthenticationException if account is locked or authentication system
     *                                 fails
     */
    public boolean authenticate(String password) throws AuthenticationException {
        if (password == null) {
            recordFailedLogin();
            return false;
        }

        // Check if account is locked
        if (isAccountLocked()) {
            throw new AuthenticationException(
                    String.format("Account '%s' is temporarily locked until %s", username, accountLockedUntil),
                    "Your account has been temporarily locked due to multiple failed login attempts. Please try again later.");
        }

        try {
            String hashedInput = hashPassword(password, passwordSalt);
            boolean authenticated = MessageDigest.isEqual(
                    passwordHash.getBytes(),
                    hashedInput.getBytes());

            if (authenticated) {
                // Reset failed attempts and update last login
                this.failedLoginAttempts = 0;
                this.lastLoginDate = LocalDateTime.now();

                // Log successful authentication
                logger.logUserLogin(this.id, "unknown", "unknown", true);
                auditLogger.logAuthenticationEvent("LOGIN", this.id, null, "unknown",
                        "unknown", true, null);

                return true;
            } else {
                recordFailedLogin();

                // Log failed authentication attempt
                logger.logUserLogin(this.username, "unknown", "unknown", false);
                auditLogger.logAuthenticationEvent("LOGIN_FAILED", this.username, null,
                        "unknown", "unknown", false, "Invalid credentials");

                return false;
            }

        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(
                    "Password verification failed due to system error: " + e.getMessage(),
                    "Authentication temporarily unavailable. Please try again later.",
                    e);
        }
    }

    /**
     * Changes the user's password.
     *
     * <p>
     * <strong>Security Implementation:</strong> Requires current password
     * verification
     * before allowing password change. Generates new salt and hash for the new
     * password.
     * </p>
     *
     * @param currentPassword The current password for verification
     * @param newPassword     The new password to set
     * @param modifiedBy      The ID of the user making this change
     * @throws AuthenticationException  if current password is incorrect
     * @throws IllegalArgumentException if new password doesn't meet security
     *                                  criteria
     */
    public void changePassword(String currentPassword, String newPassword, String modifiedBy)
            throws AuthenticationException {
        if (!authenticate(currentPassword)) {
            throw new AuthenticationException(
                    String.format("Current password verification failed for user %s", username),
                    "Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        try {
            this.passwordSalt = generateSalt();
            this.passwordHash = hashPassword(newPassword, this.passwordSalt);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(
                    "Password change failed due to system error: " + e.getMessage(),
                    "Password could not be changed due to a system error. Please try again.",
                    e);
        }
    }

    /**
     * Adds a role to this user's authorization set.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Set operations for
     * role management, ensuring no duplicate roles and efficient role checking.
     * </p>
     *
     * @param role    The role to add, must not be null
     * @param grantor The ID of the user granting this role
     * @throws IllegalArgumentException if role is null
     */
    public void addRole(Role role, String grantor) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (grantor == null || grantor.trim().isEmpty()) {
            throw new IllegalArgumentException("Grantor ID cannot be null or empty");
        }

        roles.add(role);
    }

    /**
     * Removes a role from this user's authorization set.
     *
     * @param role    The role to remove
     * @param revoker The ID of the user revoking this role
     * @return true if the role was present and removed, false otherwise
     */
    public boolean removeRole(Role role, String revoker) {
        if (role == null) {
            return false;
        }
        if (revoker == null || revoker.trim().isEmpty()) {
            throw new IllegalArgumentException("Revoker ID cannot be null or empty");
        }

        return roles.remove(role);
    }

    /**
     * Checks if this user has the specified role.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Set.contains() for efficient
     * role checking operations.
     * </p>
     *
     * @param role The role to check for
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    /**
     * Checks if this user has any of the specified roles.
     *
     * <p>
     * Useful for authorization checks where multiple roles might grant access
     * to a particular resource or operation.
     * </p>
     *
     * @param requiredRoles The roles to check for
     * @return true if the user has at least one of the specified roles
     */
    public boolean hasAnyRole(Role... requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return false;
        }

        for (Role role : requiredRoles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deactivates the user account.
     *
     * @param deactivatedBy The ID of the user performing the deactivation
     */
    public void deactivate(String deactivatedBy) {
        if (deactivatedBy == null || deactivatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Deactivated by user ID cannot be null or empty");
        }
        this.active = false;
    }

    /**
     * Reactivates the user account.
     *
     * @param reactivatedBy The ID of the user performing the reactivation
     */
    public void reactivate(String reactivatedBy) {
        if (reactivatedBy == null || reactivatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Reactivated by user ID cannot be null or empty");
        }
        this.active = true;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    // Getter methods

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns an unmodifiable view of the user's roles.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses defensive copying
     * and unmodifiable collections to prevent external modification of user roles.
     * </p>
     *
     * @return An unmodifiable Set of the user's roles
     */
    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Returns the primary role of this user.
     *
     * <p>
     * Returns the highest priority role assigned to this user. If multiple roles
     * are assigned, returns the role with the highest privileges (ADMIN > EDITOR >
     * VIEWER).
     * This is a convenience method for code that needs a single role reference.
     * </p>
     *
     * @return The primary role of the user, or VIEWER if no roles are assigned
     */
    public Role getRole() {
        if (roles.isEmpty()) {
            return Role.GUEST; // Default role
        }

        // Return highest priority role
        if (roles.contains(Role.ADMINISTRATOR)) {
            return Role.ADMINISTRATOR;
        } else if (roles.contains(Role.PUBLISHER)) {
            return Role.PUBLISHER;
        } else if (roles.contains(Role.EDITOR)) {
            return Role.EDITOR;
        } else if (roles.contains(Role.AUTHOR)) {
            return Role.AUTHOR;
        } else {
            return Role.GUEST;
        }
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Checks if the account is currently locked due to failed login attempts.
     *
     * @return true if the account is locked and the lockout period hasn't expired
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }

    // Private helper methods

    /**
     * Records a failed login attempt and implements account lockout logic.
     */
    private void recordFailedLogin() {
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
        }
    }

    /**
     * Generates a secure random salt for password hashing.
     *
     * @return Base64-encoded salt string
     * @throws NoSuchAlgorithmException if secure random generation fails
     */
    private String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password with the provided salt using SHA-256.
     *
     * @param password The plain text password
     * @param salt     The salt to use for hashing
     * @return The hashed password as a Base64-encoded string
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hashedBytes = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    /**
     * Sanitizes input strings to prevent XSS attacks.
     *
     * @param input The input string to sanitize
     * @return The sanitized string
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        return input.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<.*?>", "")
                .trim();
    }

    /**
     * Validates email format using a basic regex pattern.
     *
     * @param email The email to validate
     * @return true if the email format is valid
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    /**
     * Compares users by username for sorting purposes.
     *
     * @param other The other user to compare with
     * @return negative if this username comes before other, positive if after, zero
     *         if equal
     */
    @Override
    public int compareTo(User other) {
        if (other == null) {
            return 1;
        }
        return this.username.compareTo(other.username);
    }

    /**
     * Checks equality based on user ID.
     *
     * @param obj The object to compare with
     * @return true if the users have the same ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        User user = (User) obj;
        return Objects.equals(id, user.id);
    }

    /**
     * Returns hash code based on user ID.
     *
     * @return The hash code for this user
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of this user.
     *
     * @return A string representation excluding sensitive information
     */
    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', email='%s', active=%s, roles=%d}",
                id, username, email, active, roles.size());
    }
}
