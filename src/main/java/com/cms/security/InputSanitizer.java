package com.cms.security;

import com.cms.patterns.shield.CMSException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.util.LoggerUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Comprehensive input sanitization utility for the CMS application.
 *
 * <p>This class provides essential input validation and sanitization methods to prevent
 * security vulnerabilities including XSS attacks, SQL injection, path traversal,
 * and other malicious input-based attacks. It implements the security requirements
 * mandated by the exam specifications.</p>
 *
 * <p><strong>Security Requirements:</strong>
 * - Input Sanitization: Validates and sanitizes all user inputs
 * - XSS Prevention: Removes dangerous HTML/JavaScript content
 * - Path Traversal Protection: Prevents directory traversal attacks
 * - SQL Injection Prevention: Sanitizes database input parameters
 * - File Upload Security: Validates file types and names
 * - Application Crash Prevention: Validates inputs to prevent crashes</p>
 *
 * <p><strong>Purpose:</strong> Security Implementation - Input Sanitization.
 * This class addresses the security requirement to validate
 * all user inputs, preventing various security vulnerabilities.</p>
 *
 * <p><strong>Integration:</strong>
 * - Used by Content.java for title and body sanitization
 * - Integrated with FileUploadService for secure file handling
 * - Used by User.java for username and email validation
 * - Applied in all user-facing input processing</p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.io.FileUploadService
 * @see com.cms.security.SecurityValidator
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class InputSanitizer {

    // HTML/XSS patterns for removal
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
        "on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IFRAME_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "<object[^>]*>.*?</object>",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern EMBED_PATTERN = Pattern.compile(
        "<embed[^>]*>.*?</embed>",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "\\.\\.[\\\\/]|[\\\\/]\\.\\.[\\\\/]|[\\\\/]\\.\\.$"
    );

    // SQL injection patterns (basic detection)
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+",
        Pattern.CASE_INSENSITIVE
    );

    // File name validation patterns
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile(
        "[<>:\"|?*\\x00-\\x1f]"
    );

    private static final Pattern RESERVED_NAMES = Pattern.compile(
        "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$",
        Pattern.CASE_INSENSITIVE
    );

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Username validation pattern
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,30}$"
    );

    // Allowed file extensions for uploads
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt", "csv", "xml", "json"
    ));

    // Maximum lengths for various input types
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_FILENAME_LENGTH = 255;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private InputSanitizer() {
        throw new UnsupportedOperationException(
            "InputSanitizer is a utility class and cannot be instantiated");
    }

    /**
     * Sanitizes HTML content to prevent XSS attacks while preserving safe formatting.
     * Removes dangerous HTML tags and JavaScript while allowing basic formatting.
     *
     * @param input The HTML content to sanitize
     * @return Sanitized HTML content safe for display
     * @throws IllegalArgumentException if input is null
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        try {
            LoggerUtil.logDebug("InputSanitizer", "Sanitizing HTML content, length: " + input.length());

            // Remove dangerous script tags
            String sanitized = SCRIPT_PATTERN.matcher(input).replaceAll("");

            // Remove javascript: protocols
            sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

            // Remove event handlers (onClick, onLoad, etc.)
            sanitized = ON_EVENT_PATTERN.matcher(sanitized).replaceAll("");

            // Remove potentially dangerous tags
            sanitized = IFRAME_PATTERN.matcher(sanitized).replaceAll("");
            sanitized = OBJECT_PATTERN.matcher(sanitized).replaceAll("");
            sanitized = EMBED_PATTERN.matcher(sanitized).replaceAll("");

            // Additional sanitization for common XSS vectors
            sanitized = sanitized.replace("vbscript:", "");
            sanitized = sanitized.replace("data:", "");

            LoggerUtil.logDebug("InputSanitizer",
                "HTML sanitization complete, final length: " + sanitized.length());

            return sanitized;

        } catch (Exception e) {
            LoggerUtil.logError("InputSanitizer", "HTML sanitization failed", e);
            // Return empty string on sanitization failure for security
            return "";
        }
    }

    /**
     * Sanitizes plain text content by removing control characters and limiting length.
     * Used for content that should not contain HTML markup.
     *
     * @param input The text content to sanitize
     * @param maxLength Maximum allowed length
     * @return Sanitized text content
     */
    public static String sanitizeText(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        try {
            // Remove control characters except tabs, line feeds, and carriage returns
            String sanitized = input.replaceAll("\\p{Cntrl}&&[^\t\n\r]", "");

            // Trim whitespace
            sanitized = sanitized.trim();

            // Enforce length limit
            if (sanitized.length() > maxLength) {
                sanitized = sanitized.substring(0, maxLength);
                LoggerUtil.logWarn("InputSanitizer",
                    "Text truncated to maximum length: " + maxLength);
            }

            return sanitized;

        } catch (Exception e) {
            LoggerUtil.logError("InputSanitizer", "Text sanitization failed", e);
            return "";
        }
    }

    /**
     * Sanitizes file names to prevent path traversal and invalid characters.
     * Ensures file names are safe for file system operations.
     *
     * @param filename The filename to sanitize
     * @return Sanitized filename safe for file operations
     * @throws IllegalArgumentException if filename is null or empty after sanitization
     */
    public static String sanitizeFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        try {
            // Remove path components
            String sanitized = filename.replaceAll(".*[\\\\/]", "");

            // Remove invalid characters
            sanitized = INVALID_FILENAME_CHARS.matcher(sanitized).replaceAll("");

            // Check for reserved names
            if (RESERVED_NAMES.matcher(sanitized).matches()) {
                sanitized = "file_" + sanitized;
            }

            // Enforce length limit
            if (sanitized.length() > MAX_FILENAME_LENGTH) {
                String extension = getFileExtension(sanitized);
                int nameLength = MAX_FILENAME_LENGTH - extension.length() - 1;
                sanitized = sanitized.substring(0, nameLength) + "." + extension;
            }

            // Ensure we still have a valid filename
            if (sanitized.trim().isEmpty() || sanitized.equals(".")) {
                throw new IllegalArgumentException("Filename becomes invalid after sanitization");
            }

            LoggerUtil.logDebug("InputSanitizer",
                "Filename sanitized: " + filename + " -> " + sanitized);

            return sanitized;

        } catch (Exception e) {
            LoggerUtil.logError("InputSanitizer", "Filename sanitization failed for: " + filename, e);
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
    }

    /**
     * Validates that a file type is allowed for upload.
     * Prevents upload of potentially dangerous file types.
     *
     * @param filename The filename to check
     * @return true if file type is allowed, false otherwise
     */
    public static boolean isValidFileType(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename.toLowerCase());
        return ALLOWED_FILE_EXTENSIONS.contains(extension);
    }

    /**
     * Validates and sanitizes email addresses.
     * Ensures email addresses conform to basic format requirements.
     *
     * @param email The email address to validate
     * @return Sanitized email address
     * @throws IllegalArgumentException if email is invalid
     */
    public static String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String sanitized = email.trim().toLowerCase();

        if (sanitized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email address too long");
        }

        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        return sanitized;
    }

    /**
     * Validates and sanitizes usernames.
     * Ensures usernames contain only safe characters and appropriate length.
     *
     * @param username The username to validate
     * @return Sanitized username
     * @throws IllegalArgumentException if username is invalid
     */
    public static String sanitizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String sanitized = username.trim();

        if (sanitized.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username too long");
        }

        if (!USERNAME_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                "Username must contain only letters, numbers, underscores, and hyphens (3-30 characters)");
        }

        return sanitized;
    }

    /**
     * Sanitizes search queries to prevent injection attacks.
     * Removes potentially dangerous SQL patterns while preserving search functionality.
     *
     * @param query The search query to sanitize
     * @return Sanitized search query
     */
    public static String sanitizeSearchQuery(String query) {
        if (query == null) {
            return "";
        }

        try {
            // Basic text sanitization
            String sanitized = sanitizeText(query, 100);

            // Remove potential SQL injection patterns
            if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
                LoggerUtil.logWarn("InputSanitizer",
                    "Potential SQL injection attempt in search query: " + query);
                // Return empty query for suspicious content
                return "";
            }

            // Remove path traversal attempts
            if (PATH_TRAVERSAL_PATTERN.matcher(sanitized).find()) {
                LoggerUtil.logWarn("InputSanitizer",
                    "Path traversal attempt in search query: " + query);
                return "";
            }

            return sanitized;

        } catch (Exception e) {
            LoggerUtil.logError("InputSanitizer", "Search query sanitization failed", e);
            return "";
        }
    }

    /**
     * Sanitizes content titles with appropriate length limits.
     *
     * @param title The content title to sanitize
     * @return Sanitized title
     */
    public static String sanitizeTitle(String title) {
        return sanitizeText(title, MAX_TITLE_LENGTH);
    }

    /**
     * Sanitizes content body with HTML sanitization.
     *
     * @param content The content body to sanitize
     * @return Sanitized content body
     */
    public static String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }

        // First apply HTML sanitization
        String sanitized = sanitizeHtml(content);

        // Then apply length limits
        if (sanitized.length() > MAX_CONTENT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_CONTENT_LENGTH);
            LoggerUtil.logWarn("InputSanitizer",
                "Content truncated to maximum length: " + MAX_CONTENT_LENGTH);
        }

        return sanitized;
    }

    /**
     * Validates input to prevent application crashes.
     * Checks for null values, excessive lengths, and invalid characters.
     *
     * @param input The input to validate
     * @param fieldName Name of the field for error reporting
     * @param maxLength Maximum allowed length
     * @throws IllegalArgumentException if input is invalid and could cause crashes
     */
    public static void validateInput(String input, String fieldName, int maxLength) {
        if (input == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }

        if (input.length() > maxLength) {
            throw new IllegalArgumentException(
                fieldName + " exceeds maximum length of " + maxLength + " characters");
        }

        // Check for null bytes that could cause issues
        if (input.contains("\0")) {
            throw new IllegalArgumentException(
                fieldName + " contains invalid null characters");
        }
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename The filename to extract extension from
     * @return File extension without the dot, or empty string if no extension
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }

        return "";
    }
}
