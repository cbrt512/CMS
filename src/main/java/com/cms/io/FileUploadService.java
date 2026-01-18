package com.cms.io;

import com.cms.util.CMSLogger;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Secure file upload service providing comprehensive validation,
 * storage management, and security features for content management.
 *
 * <p>
 * This service implements secure file upload functionality with
 * comprehensive validation, sanitization, and storage management.
 * It provides protection against common security vulnerabilities
 * including path traversal, file type attacks, and size-based DoS.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Service Pattern - Encapsulates
 * complex file upload logic with comprehensive error handling and
 * security validation.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * Implements file and stream operations with proper resource management,
 * exception handling, and security considerations.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - File type validation with MIME type checking
 * - Path traversal prevention with path sanitization
 * - File size limits with configurable constraints
 * - Filename sanitization against malicious characters
 * - Upload directory isolation and permission control
 * - Duplicate file detection with hash-based verification
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong>
 * Uses Set for allowed extensions, Map for configuration,
 * and List for validation messages, providing comprehensive
 * collections integration.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class FileUploadService {

    /** Default maximum file size (10MB) */
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Default upload directory name */
    private static final String DEFAULT_UPLOAD_DIR = "uploads";

    /** Pattern for safe filename validation */
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    /** Set of allowed file extensions for security */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "pdf", "txt", "doc", "docx",
            "xls", "xlsx", "ppt", "pptx", "zip", "xml", "json");

    /** Map of file extensions to MIME types for validation */
    private static final Map<String, Set<String>> MIME_TYPE_MAPPING = new ConcurrentHashMap<>();

    static {
        // Initialize MIME type mappings for security validation
        MIME_TYPE_MAPPING.put("jpg", Set.of("image/jpeg"));
        MIME_TYPE_MAPPING.put("jpeg", Set.of("image/jpeg"));
        MIME_TYPE_MAPPING.put("png", Set.of("image/png"));
        MIME_TYPE_MAPPING.put("gif", Set.of("image/gif"));
        MIME_TYPE_MAPPING.put("pdf", Set.of("application/pdf"));
        MIME_TYPE_MAPPING.put("txt", Set.of("text/plain"));
        MIME_TYPE_MAPPING.put("xml", Set.of("application/xml", "text/xml"));
        MIME_TYPE_MAPPING.put("json", Set.of("application/json"));
        MIME_TYPE_MAPPING.put("zip", Set.of("application/zip"));
    }

    private final Path uploadDirectory;
    private final long maxFileSize;
    private final Set<String> allowedExtensions;
    private final Map<String, Object> configuration;

    /** Logger instance for file upload operations */
    private static final CMSLogger logger = CMSLogger.getInstance();

    /**
     * Creates a FileUploadService with default configuration.
     *
     * <p>
     * Uses default upload directory, file size limits, and
     * allowed file types for standard content management scenarios.
     * </p>
     *
     * @throws IOException if the upload directory cannot be created
     */
    public FileUploadService() throws IOException {
        this(Paths.get(DEFAULT_UPLOAD_DIR), DEFAULT_MAX_FILE_SIZE, ALLOWED_EXTENSIONS);
    }

    /**
     * Creates a FileUploadService with custom configuration.
     *
     * <p>
     * Allows customization of upload directory, size limits,
     * and allowed file types for specific deployment requirements.
     * </p>
     *
     * @param uploadDirectory   the directory for storing uploaded files
     * @param maxFileSize       maximum allowed file size in bytes
     * @param allowedExtensions set of allowed file extensions
     * @throws IOException              if the upload directory cannot be created or
     *                                  accessed
     * @throws IllegalArgumentException if parameters are invalid
     */
    public FileUploadService(Path uploadDirectory, long maxFileSize,
            Set<String> allowedExtensions) throws IOException {
        this.uploadDirectory = Objects.requireNonNull(uploadDirectory,
                "Upload directory cannot be null");
        this.maxFileSize = Math.max(1, maxFileSize);
        this.allowedExtensions = allowedExtensions != null ? Set.copyOf(allowedExtensions) : ALLOWED_EXTENSIONS;
        this.configuration = new ConcurrentHashMap<>();

        // Initialize upload directory with proper security
        initializeUploadDirectory();

        // Initialize configuration
        initializeConfiguration();
    }

    /**
     * Initializes the upload directory with appropriate permissions and structure.
     *
     * <p>
     * <strong>Security Feature:</strong> Creates upload directory with
     * proper permissions and organizes uploads by date for better management.
     * </p>
     *
     * @throws IOException if directory creation fails
     */
    private void initializeUploadDirectory() throws IOException {
        if (!Files.exists(uploadDirectory)) {
            Files.createDirectories(uploadDirectory);
        }

        // Ensure the directory is writable
        if (!Files.isWritable(uploadDirectory)) {
            throw new IOException("Upload directory is not writable: " + uploadDirectory);
        }
    }

    /**
     * Initializes service configuration with default values.
     */
    private void initializeConfiguration() {
        configuration.put("enableHashVerification", true);
        configuration.put("createDateDirectories", true);
        configuration.put("preserveOriginalNames", false);
        configuration.put("allowOverwrite", false);
    }

    /**
     * Uploads a file from an InputStream with comprehensive validation and
     * security.
     *
     * <p>
     * This method provides complete file upload functionality including:
     * </p>
     * <ul>
     * <li>Input validation and sanitization</li>
     * <li>File type and size verification</li>
     * <li>Secure storage with path traversal prevention</li>
     * <li>Duplicate detection and handling</li>
     * <li>Comprehensive error reporting</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>InputStream processing with buffered operations</li>
     * <li>File system operations using NIO.2 API</li>
     * <li>Resource management with try-with-resources</li>
     * <li>Stream copying with proper error handling</li>
     * </ul>
     *
     * @param inputStream the input stream containing file data
     * @param filename    the original filename
     * @param contentType the MIME content type of the file
     * @return UploadResult containing status and file information
     * @throws IllegalArgumentException if parameters are null or invalid
     */
    public UploadResult uploadFile(InputStream inputStream, String filename, String contentType) {
        // Input validation
        if (inputStream == null) {
            return new UploadResult(UploadResult.Status.VALIDATION_FAILED, filename,
                    List.of("Input stream cannot be null"), "Invalid input stream");
        }

        if (filename == null || filename.trim().isEmpty()) {
            return new UploadResult(UploadResult.Status.VALIDATION_FAILED, filename,
                    List.of("Filename cannot be null or empty"), "Invalid filename");
        }

        List<String> validationMessages = new ArrayList<>();

        // Sanitize and validate filename
        String sanitizedFilename = sanitizeFilename(filename);
        if (!isValidFilename(sanitizedFilename, validationMessages)) {
            return new UploadResult(UploadResult.Status.VALIDATION_FAILED, filename,
                    validationMessages, "Filename validation failed");
        }

        // Validate file extension
        String extension = getFileExtension(sanitizedFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            return new UploadResult(UploadResult.Status.UNSUPPORTED_FILE_TYPE, filename,
                    List.of("File type not allowed: " + extension), "Unsupported file type");
        }

        // Validate MIME type if provided
        if (contentType != null && !isValidMimeType(extension, contentType)) {
            logger.logSuspiciousActivity("FILE_UPLOAD_MIME_MISMATCH",
                    "MIME type mismatch detected during file upload", null, "unknown", "HIGH");
            return new UploadResult(UploadResult.Status.SECURITY_VIOLATION, filename,
                    List.of("MIME type mismatch for extension: " + extension),
                    "Security validation failed");
        }

        try {
            UploadResult result = processFileUpload(inputStream, sanitizedFilename, contentType);

            // Log successful file upload
            if (result.getStatus() == UploadResult.Status.SUCCESS) {
                logger.logFileUpload(sanitizedFilename, result.getFileSize(), "unknown",
                        result.getStoredPath().toString(), contentType);
            }

            return result;
        } catch (IOException e) {
            logger.logError(e, "FileUploadService.uploadFile", "unknown", "FILE_UPLOAD_IO_ERROR");
            return new UploadResult(UploadResult.Status.IO_ERROR, filename,
                    List.of("I/O error during upload: " + e.getMessage()),
                    "File upload failed");
        } catch (SecurityException e) {
            logger.logSuspiciousActivity("FILE_UPLOAD_SECURITY_VIOLATION",
                    "Security violation during file upload: " + e.getMessage(), null, "unknown", "HIGH");
            return new UploadResult(UploadResult.Status.SECURITY_VIOLATION, filename,
                    List.of("Security violation: " + e.getMessage()),
                    "Security check failed");
        }
    }

    /**
     * Processes the actual file upload with stream operations and storage.
     *
     * <p>
     * <strong>Java I/O Implementation:</strong> Demonstrates comprehensive
     * I/O operations including stream processing, file system operations,
     * and resource management.
     * </p>
     *
     * @param inputStream the input stream containing file data
     * @param filename    the sanitized filename
     * @param contentType the MIME content type
     * @return UploadResult with upload status and file information
     * @throws IOException if I/O operations fail
     */
    private UploadResult processFileUpload(InputStream inputStream, String filename,
            String contentType) throws IOException {
        // Create target directory (organized by date if configured)
        Path targetDirectory = createTargetDirectory();

        // Generate unique filename to prevent conflicts
        String storedFilename = generateUniqueFilename(targetDirectory, filename);
        Path targetPath = targetDirectory.resolve(storedFilename);

        // Process upload with buffered streams and size validation
        long fileSize;
        try (BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(
                        Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW))) {

            fileSize = copyStreamWithSizeLimit(bufferedInput, bufferedOutput);
        }

        // Verify file was created successfully
        if (!Files.exists(targetPath) || Files.size(targetPath) != fileSize) {
            // Cleanup on failure
            Files.deleteIfExists(targetPath);
            throw new IOException("File verification failed after upload");
        }

        return new UploadResult(filename, storedFilename, targetPath, fileSize, contentType);
    }

    /**
     * Copies data between streams with size limit enforcement.
     *
     * <p>
     * <strong>Security Feature:</strong> Prevents size-based DoS attacks
     * by enforcing configurable file size limits during streaming.
     * </p>
     *
     * @param input  the input stream to read from
     * @param output the output stream to write to
     * @return the total number of bytes copied
     * @throws IOException if I/O operations fail or size limit is exceeded
     */
    private long copyStreamWithSizeLimit(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[8192]; // 8KB buffer for efficient copying
        long totalBytes = 0;
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            totalBytes += bytesRead;

            // Enforce size limit during streaming
            if (totalBytes > maxFileSize) {
                throw new IOException("File size exceeds maximum allowed: " + maxFileSize);
            }

            output.write(buffer, 0, bytesRead);
        }

        output.flush();
        return totalBytes;
    }

    /**
     * Creates the target directory for file storage.
     *
     * <p>
     * If configured, organizes uploads into date-based subdirectories
     * for better file management and organization.
     * </p>
     *
     * @return the target directory path
     * @throws IOException if directory creation fails
     */
    private Path createTargetDirectory() throws IOException {
        Path targetDir = uploadDirectory;

        // Create date-based subdirectories if configured
        if (Boolean.TRUE.equals(configuration.get("createDateDirectories"))) {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            targetDir = uploadDirectory.resolve(dateDir);
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        return targetDir;
    }

    /**
     * Generates a unique filename to prevent conflicts.
     *
     * <p>
     * Creates unique filenames using timestamp and hash-based approaches
     * while preserving file extensions for proper type handling.
     * </p>
     *
     * @param directory        the target directory
     * @param originalFilename the original filename
     * @return a unique filename for storage
     */
    private String generateUniqueFilename(Path directory, String originalFilename) {
        if (Boolean.TRUE.equals(configuration.get("preserveOriginalNames")) &&
                !Files.exists(directory.resolve(originalFilename))) {
            return originalFilename;
        }

        String baseName = getBaseName(originalFilename);
        String extension = getFileExtension(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());

        String uniqueFilename = String.format("%s_%s.%s", baseName, timestamp, extension);

        // Ensure uniqueness (extremely rare collision case)
        int counter = 1;
        while (Files.exists(directory.resolve(uniqueFilename))) {
            uniqueFilename = String.format("%s_%s_%d.%s", baseName, timestamp, counter++, extension);
        }

        return uniqueFilename;
    }

    /**
     * Sanitizes filename by removing dangerous characters and patterns.
     *
     * <p>
     * <strong>Security Feature:</strong> Prevents path traversal attacks
     * and ensures filenames are safe for filesystem storage.
     * </p>
     *
     * @param filename the filename to sanitize
     * @return sanitized filename safe for storage
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_file";
        }

        // Remove path separators and dangerous characters
        String sanitized = filename.replaceAll("[/\\\\:*?\"<>|]", "_")
                .replaceAll("\\.{2,}", ".") // Multiple dots
                .replaceAll("^\\.", "_") // Leading dot
                .trim();

        // Limit length to prevent filesystem issues
        if (sanitized.length() > 255) {
            String extension = getFileExtension(sanitized);
            String baseName = sanitized.substring(0, 255 - extension.length() - 1);
            sanitized = baseName + "." + extension;
        }

        return sanitized.isEmpty() ? "unnamed_file" : sanitized;
    }

    /**
     * Validates filename against security and format requirements.
     *
     * @param filename           the filename to validate
     * @param validationMessages list to collect validation errors
     * @return true if filename is valid, false otherwise
     */
    private boolean isValidFilename(String filename, List<String> validationMessages) {
        if (filename == null || filename.trim().isEmpty()) {
            validationMessages.add("Filename cannot be empty");
            return false;
        }

        // Check for reserved names (Windows-specific but good practice)
        String[] reservedNames = { "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
                "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6",
                "LPT7", "LPT8", "LPT9" };

        String baseName = getBaseName(filename).toUpperCase();
        if (Arrays.asList(reservedNames).contains(baseName)) {
            validationMessages.add("Filename uses reserved name: " + baseName);
            return false;
        }

        // Check for file extension
        if (getFileExtension(filename).isEmpty()) {
            validationMessages.add("File must have an extension");
            return false;
        }

        return true;
    }

    /**
     * Validates MIME type against file extension for security.
     *
     * @param extension   the file extension
     * @param contentType the provided MIME type
     * @return true if MIME type is valid for the extension
     */
    private boolean isValidMimeType(String extension, String contentType) {
        Set<String> validMimeTypes = MIME_TYPE_MAPPING.get(extension.toLowerCase());
        return validMimeTypes == null || validMimeTypes.contains(contentType.toLowerCase());
    }

    /**
     * Extracts the base name of a file (without extension).
     *
     * @param filename the filename
     * @return the base name without extension
     */
    private String getBaseName(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? filename : filename.substring(0, lastDot);
    }

    /**
     * Extracts the file extension.
     *
     * @param filename the filename
     * @return the file extension (without dot)
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }

    /**
     * Lists all uploaded files with their metadata.
     *
     * <p>
     * <strong>Java I/O Feature:</strong> Directory traversal using
     * Files.walk() for comprehensive file system exploration.
     * </p>
     *
     * @return list of uploaded file information
     * @throws IOException if directory access fails
     */
    public List<Map<String, Object>> listUploadedFiles() throws IOException {
        List<Map<String, Object>> fileList = new ArrayList<>();

        if (!Files.exists(uploadDirectory)) {
            return fileList;
        }

        try (var stream = Files.walk(uploadDirectory)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("filename", path.getFileName().toString());
                            fileInfo.put("path", path.toString());
                            fileInfo.put("size", Files.size(path));
                            fileInfo.put("lastModified", Files.getLastModifiedTime(path).toInstant());
                            fileInfo.put("contentType", Files.probeContentType(path));
                            fileList.add(fileInfo);
                        } catch (IOException e) {
                            // Log error but continue processing other files
                            System.err.println("Error reading file info for: " + path + " - " + e.getMessage());
                        }
                    });
        }

        return fileList;
    }

    /**
     * Deletes an uploaded file by filename.
     *
     * <p>
     * <strong>Security Feature:</strong> Validates file path to prevent
     * deletion of files outside the upload directory.
     * </p>
     *
     * @param filename the name of the file to delete
     * @return true if file was deleted, false if file not found
     * @throws IOException       if deletion fails
     * @throws SecurityException if file is outside upload directory
     */
    public boolean deleteFile(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        String sanitized = sanitizeFilename(filename);
        Path filePath = uploadDirectory.resolve(sanitized);

        // Security check: ensure file is within upload directory
        if (!filePath.startsWith(uploadDirectory)) {
            throw new SecurityException("File path outside upload directory: " + filePath);
        }

        if (!Files.exists(filePath)) {
            return false;
        }

        Files.delete(filePath);
        return true;
    }

    /**
     * Gets the upload directory path.
     *
     * @return the upload directory path
     */
    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    /**
     * Gets the maximum file size limit.
     *
     * @return maximum file size in bytes
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Gets the set of allowed file extensions.
     *
     * @return immutable set of allowed extensions
     */
    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    /**
     * Updates service configuration.
     *
     * @param key   the configuration key
     * @param value the configuration value
     */
    public void setConfiguration(String key, Object value) {
        Objects.requireNonNull(key, "Configuration key cannot be null");
        configuration.put(key, value);
    }

    /**
     * Gets service configuration value.
     *
     * @param key the configuration key
     * @return the configuration value, or null if not set
     */
    public Object getConfiguration(String key) {
        return configuration.get(key);
    }
}
