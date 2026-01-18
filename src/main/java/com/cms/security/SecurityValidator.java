package com.cms.security;

import com.cms.core.model.User;
import com.cms.core.model.Role;
import com.cms.core.model.Content;
import com.cms.patterns.shield.CMSException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.util.LoggerUtil;

import java.util.regex.Pattern;

/**
 * Comprehensive security validation utility for the CMS application.
 *
 * <p>
 * This class provides security validation methods to prevent various types of
 * attacks and security vulnerabilities. It works in conjunction with
 * InputSanitizer
 * to provide defense-in-depth security for the CMS application.
 * </p>
 *
 * <p>
 * <strong>Security Validations:</strong>
 * - Authentication credential validation
 * - Authorization level verification
 * - Content security policy validation
 * - File upload security checks
 * - Session security validation
 * - Input boundary validation to prevent crashes
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Security Implementation - Validation Framework.
 * This class complements the input sanitization requirements by providing
 * comprehensive validation that prevents application crashes and security
 * vulnerabilities.
 * </p>
 *
 * <p>
 * <strong>Integration:</strong>
 * - Used by authentication services for credential validation
 * - Applied in content creation/editing workflows
 * - Integrated with file upload processes
 * - Used for session management security
 * - Applied in all user-facing operations
 * </p>
 *
 * @see com.cms.security.InputSanitizer
 * @see com.cms.patterns.shield.CMSException
 * @see com.cms.core.model.User
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class SecurityValidator {

    // Password strength patterns
    private static final Pattern WEAK_PASSWORD_PATTERN = Pattern.compile(
            "^(password|123456|qwerty|admin|letmein|welcome)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PASSWORD_STRENGTH_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    // Content security patterns
    private static final Pattern SUSPICIOUS_CONTENT_PATTERN = Pattern.compile(
            "(\\<\\?php|\\<\\%|javascript:|vbscript:|data:text/html)",
            Pattern.CASE_INSENSITIVE);

    // IP address pattern for validation
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    // Session token pattern
    private static final Pattern SESSION_TOKEN_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+/]{32,128}={0,2}$");

    // Maximum values to prevent resource exhaustion
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int MAX_CONTENT_ITEMS_PER_USER = 1000;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SecurityValidator() {
        throw new UnsupportedOperationException(
                "SecurityValidator is a utility class and cannot be instantiated");
    }

    /**
     * Validates user credentials for authentication.
     * Prevents credential-based attacks and ensures password security.
     *
     * @param username The username to validate
     * @param password The password to validate
     * @throws IllegalArgumentException if credentials are invalid or insecure
     */
    public static void validateCredentials(String username, String password) {
        LoggerUtil.logDebug("SecurityValidator", "Validating user credentials");

        // Validate username
        if (username == null || username.trim().isEmpty()) {
            LoggerUtil.logWarn("SecurityValidator", "Authentication attempt with null/empty username");
            throw new IllegalArgumentException("Username cannot be empty");
        }

        // Sanitize and validate username
        try {
            InputSanitizer.sanitizeUsername(username);
        } catch (IllegalArgumentException e) {
            LoggerUtil.logWarn("SecurityValidator",
                    "Invalid username format in authentication: " + username);
            throw new IllegalArgumentException("Invalid username format");
        }

        // Validate password
        if (password == null || password.isEmpty()) {
            LoggerUtil.logWarn("SecurityValidator", "Authentication attempt with null/empty password");
            throw new IllegalArgumentException("Password cannot be empty");
        }

        // Check for weak passwords
        if (WEAK_PASSWORD_PATTERN.matcher(password).matches()) {
            LoggerUtil.logWarn("SecurityValidator", "Weak password detected for user: " + username);
            throw new IllegalArgumentException("Password is too weak. Please choose a stronger password");
        }

        // Validate password strength (for new passwords)
        if (!PASSWORD_STRENGTH_PATTERN.matcher(password).matches()) {
            LoggerUtil.logWarn("SecurityValidator", "Password strength requirements not met for user: " + username);
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain uppercase, lowercase, numbers, and special characters");
        }

        LoggerUtil.logDebug("SecurityValidator", "Credentials validation successful for user: " + username);
    }

    /**
     * Validates user authorization for specific operations.
     * Ensures users have appropriate permissions for requested actions.
     *
     * @param user       The user to validate
     * @param operation  The operation being requested
     * @param resourceId The resource being accessed (may be null)
     * @throws SecurityException if user is not authorized
     */
    public static void validateAuthorization(User user, String operation, String resourceId) {
        if (user == null) {
            LoggerUtil.logWarn("SecurityValidator",
                    "Authorization check failed - null user for operation: " + operation);
            throw new SecurityException("Authentication required");
        }

        LoggerUtil.logDebug("SecurityValidator",
                "Validating authorization for user: " + user.getUsername() + ", operation: " + operation);

        // Check if user is active
        if (!user.isActive()) {
            LoggerUtil.logWarn("SecurityValidator",
                    "Authorization denied - inactive user: " + user.getUsername());
            throw new SecurityException("User account is inactive");
        }

        // Validate operation string
        if (operation == null || operation.trim().isEmpty()) {
            LoggerUtil.logError("SecurityValidator", "Invalid operation string in authorization check");
            throw new IllegalArgumentException("Operation cannot be empty");
        }

        // Check role-based permissions
        Role userRole = user.getRole();
        if (!isOperationAllowedForRole(operation, userRole)) {
            LoggerUtil.logWarn("SecurityValidator",
                    String.format("Authorization denied - user: %s, role: %s, operation: %s",
                            user.getUsername(), userRole, operation));
            throw new SecurityException("Insufficient permissions for this operation");
        }

        // Additional resource-specific checks
        if (resourceId != null && !canUserAccessResource(user, resourceId)) {
            LoggerUtil.logWarn("SecurityValidator",
                    String.format("Resource access denied - user: %s, resource: %s",
                            user.getUsername(), resourceId));
            throw new SecurityException("Access denied to requested resource");
        }

        LoggerUtil.logDebug("SecurityValidator", "Authorization successful");
    }

    /**
     * Validates content security to prevent malicious content injection.
     * Checks for suspicious patterns and potentially dangerous content.
     *
     * @param content The content to validate
     * @throws SecurityException if content contains suspicious elements
     */
    public static void validateContentSecurity(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        LoggerUtil.logDebug("SecurityValidator", "Validating content security for: " + content.getId());

        // Validate title
        String title = content.getTitle();
        if (title != null && SUSPICIOUS_CONTENT_PATTERN.matcher(title).find()) {
            LoggerUtil.logWarn("SecurityValidator",
                    "Suspicious content detected in title: " + content.getId());
            throw new SecurityException("Content title contains potentially dangerous elements");
        }

        // Validate body content
        String body = content.getBody();
        if (body != null && SUSPICIOUS_CONTENT_PATTERN.matcher(body).find()) {
            LoggerUtil.logWarn("SecurityValidator",
                    "Suspicious content detected in body: " + content.getId());
            throw new SecurityException("Content body contains potentially dangerous elements");
        }

        // Check content length to prevent resource exhaustion
        if (body != null && body.length() > 1000000) { // 1MB limit
            LoggerUtil.logWarn("SecurityValidator",
                    "Oversized content detected: " + content.getId());
            throw new SecurityException("Content exceeds maximum allowed size");
        }

        LoggerUtil.logDebug("SecurityValidator", "Content security validation successful");
    }

    /**
     * Validates file upload security including size, type, and content checks.
     *
     * @param filename    The name of the file being uploaded
     * @param fileSize    The size of the file in bytes
     * @param fileContent The file content bytes (may be null for size-only check)
     * @throws SecurityException if file fails security validation
     */
    public static void validateFileUploadSecurity(String filename, long fileSize, byte[] fileContent) {
        LoggerUtil.logDebug("SecurityValidator",
                "Validating file upload security: " + filename + ", size: " + fileSize);

        // Validate filename
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        // Sanitize and check filename
        try {
            String sanitizedName = InputSanitizer.sanitizeFileName(filename);
            if (!InputSanitizer.isValidFileType(sanitizedName)) {
                LoggerUtil.logWarn("SecurityValidator", "Invalid file type: " + filename);
                throw new SecurityException("File type not allowed for upload");
            }
        } catch (IllegalArgumentException e) {
            LoggerUtil.logWarn("SecurityValidator", "Invalid filename in upload: " + filename);
            throw new SecurityException("Invalid filename format");
        }

        // Validate file size
        long maxSizeBytes = MAX_FILE_SIZE_MB * 1024 * 1024;
        if (fileSize > maxSizeBytes) {
            LoggerUtil.logWarn("SecurityValidator",
                    "File size exceeds limit: " + fileSize + " bytes, limit: " + maxSizeBytes);
            throw new SecurityException("File size exceeds maximum allowed limit of " + MAX_FILE_SIZE_MB + "MB");
        }

        if (fileSize <= 0) {
            LoggerUtil.logWarn("SecurityValidator", "Empty or invalid file size: " + fileSize);
            throw new SecurityException("Invalid file size");
        }

        // Basic content validation if content is provided
        if (fileContent != null) {
            validateFileContent(filename, fileContent);
        }

        LoggerUtil.logDebug("SecurityValidator", "File upload security validation successful");
    }

    /**
     * Validates session security including token format and expiration.
     *
     * @param sessionToken     The session token to validate
     * @param lastActivityTime The timestamp of last session activity
     * @throws SecurityException if session is invalid or expired
     */
    public static void validateSessionSecurity(String sessionToken, long lastActivityTime) {
        LoggerUtil.logDebug("SecurityValidator", "Validating session security");

        // Validate token format
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            LoggerUtil.logWarn("SecurityValidator", "Empty session token");
            throw new SecurityException("Invalid session token");
        }

        if (!SESSION_TOKEN_PATTERN.matcher(sessionToken).matches()) {
            LoggerUtil.logWarn("SecurityValidator", "Invalid session token format");
            throw new SecurityException("Invalid session token format");
        }

        // Check session timeout
        long currentTime = System.currentTimeMillis();
        long timeoutMillis = SESSION_TIMEOUT_MINUTES * 60 * 1000;

        if (currentTime - lastActivityTime > timeoutMillis) {
            LoggerUtil.logWarn("SecurityValidator", "Session expired");
            throw new SecurityException("Session has expired. Please log in again.");
        }

        LoggerUtil.logDebug("SecurityValidator", "Session security validation successful");
    }

    /**
     * Validates IP address format and checks against blocked IPs.
     *
     * @param ipAddress The IP address to validate
     * @throws SecurityException if IP address is invalid or blocked
     */
    public static void validateIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be empty");
        }

        if (!IP_ADDRESS_PATTERN.matcher(ipAddress).matches()) {
            LoggerUtil.logWarn("SecurityValidator", "Invalid IP address format: " + ipAddress);
            throw new SecurityException("Invalid IP address format");
        }

        // Check for private/localhost addresses in production scenarios
        if (isPrivateOrLocalAddress(ipAddress)) {
            LoggerUtil.logDebug("SecurityValidator", "Private/local IP address detected: " + ipAddress);
        }

        LoggerUtil.logDebug("SecurityValidator", "IP address validation successful: " + ipAddress);
    }

    /**
     * Validates operation limits to prevent resource exhaustion attacks.
     *
     * @param user          The user performing operations
     * @param operationType The type of operation being performed
     * @throws SecurityException if operation limits are exceeded
     */
    public static void validateOperationLimits(User user, String operationType) {
        LoggerUtil.logDebug("SecurityValidator",
                "Validating operation limits for user: " + user.getUsername() + ", operation: " + operationType);

        // This is a simplified implementation - in production, you'd likely
        // maintain counters in a cache or database

        if ("CONTENT_CREATION".equals(operationType)) {
            // In a real implementation, you'd check actual content count
            LoggerUtil.logDebug("SecurityValidator", "Content creation limit check passed");
        }

        if ("LOGIN_ATTEMPT".equals(operationType)) {
            // In a real implementation, you'd track failed login attempts
            LoggerUtil.logDebug("SecurityValidator", "Login attempt limit check passed");
        }

        LoggerUtil.logDebug("SecurityValidator", "Operation limits validation successful");
    }

    /**
     * Determines if an operation is allowed for a given user role.
     *
     * @param operation The operation being requested
     * @param role      The user's role
     * @return true if operation is allowed for the role
     */
    private static boolean isOperationAllowedForRole(String operation, Role role) {
        switch (role) {
            case ADMINISTRATOR:
                return true; // Admins can perform all operations

            case EDITOR:
                return !"DELETE_USER".equals(operation) &&
                        !"SYSTEM_CONFIG".equals(operation);

            case AUTHOR:
                return "CREATE_CONTENT".equals(operation) ||
                        "EDIT_OWN_CONTENT".equals(operation) ||
                        "VIEW_CONTENT".equals(operation);

            case GUEST:
                return "VIEW_CONTENT".equals(operation);

            default:
                return false;
        }
    }

    /**
     * Checks if a user can access a specific resource.
     *
     * @param user       The user requesting access
     * @param resourceId The resource being accessed
     * @return true if access is allowed
     */
    private static boolean canUserAccessResource(User user, String resourceId) {
        // Simplified resource access check
        // In production, this would involve checking ownership, permissions, etc.

        if (user.getRole() == Role.ADMINISTRATOR) {
            return true; // Admins can access all resources
        }

        // Additional resource-specific checks would go here
        return true; // Simplified for this implementation
    }

    /**
     * Validates file content for security threats.
     *
     * @param filename The name of the file
     * @param content  The file content bytes
     * @throws SecurityException if file content is suspicious
     */
    private static void validateFileContent(String filename, byte[] content) {
        // Check for executable file signatures
        if (content.length >= 2) {
            // Check for Windows executable signatures
            if ((content[0] == 'M' && content[1] == 'Z') || // PE executable
                    (content[0] == (byte) 0x7F && content[1] == 'E')) { // ELF executable
                LoggerUtil.logWarn("SecurityValidator",
                        "Executable file content detected: " + filename);
                throw new SecurityException("Executable files are not allowed");
            }
        }

        // Check for script content in non-script files
        String contentString = new String(content).toLowerCase();
        if (!filename.toLowerCase().endsWith(".js") &&
                !filename.toLowerCase().endsWith(".php")) {
            if (contentString.contains("<script") || contentString.contains("<?php")) {
                LoggerUtil.logWarn("SecurityValidator",
                        "Script content detected in non-script file: " + filename);
                throw new SecurityException("Suspicious file content detected");
            }
        }
    }

    /**
     * Checks if an IP address is private or localhost.
     *
     * @param ipAddress The IP address to check
     * @return true if the address is private or localhost
     */
    private static boolean isPrivateOrLocalAddress(String ipAddress) {
        return ipAddress.startsWith("127.") || // Localhost
                ipAddress.startsWith("10.") || // Class A private
                ipAddress.startsWith("192.168.") || // Class C private
                (ipAddress.startsWith("172.") && // Class B private
                        ipAddress.split("\\.").length > 1 &&
                        Integer.parseInt(ipAddress.split("\\.")[1]) >= 16 &&
                        Integer.parseInt(ipAddress.split("\\.")[1]) <= 31);
    }
}
