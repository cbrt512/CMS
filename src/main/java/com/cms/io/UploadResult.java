package com.cms.io;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result object for file upload operations containing upload status,
 * file information, and any validation messages.
 *
 * <p>
 * This class represents the outcome of a file upload operation,
 * providing detailed information about the success or failure of the upload,
 * along with comprehensive metadata about the uploaded file.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * This class supports secure file upload functionality by providing
 * structured results with validation feedback and security information.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - File type validation results
 * - Size limit compliance checking
 * - Path security validation
 * - Upload timestamp tracking
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class UploadResult {

    /**
     * Enumeration of possible upload outcomes.
     */
    public enum Status {
        /** Upload completed successfully */
        SUCCESS,
        /** Upload failed due to validation errors */
        VALIDATION_FAILED,
        /** Upload failed due to I/O errors */
        IO_ERROR,
        /** Upload failed due to security restrictions */
        SECURITY_VIOLATION,
        /** Upload failed due to size constraints */
        SIZE_LIMIT_EXCEEDED,
        /** Upload failed due to unsupported file type */
        UNSUPPORTED_FILE_TYPE
    }

    private final Status status;
    private final String originalFilename;
    private final String storedFilename;
    private final Path storedPath;
    private final long fileSize;
    private final String contentType;
    private final Instant uploadTimestamp;
    private final List<String> validationMessages;
    private final String errorMessage;

    /**
     * Creates a successful upload result.
     *
     * @param originalFilename the original filename provided by the user
     * @param storedFilename   the filename used for storage (may be different for
     *                         security)
     * @param storedPath       the path where the file was stored
     * @param fileSize         the size of the uploaded file in bytes
     * @param contentType      the MIME type of the uploaded file
     */
    public UploadResult(String originalFilename, String storedFilename, Path storedPath,
            long fileSize, String contentType) {
        this.status = Status.SUCCESS;
        this.originalFilename = sanitizeFilename(originalFilename);
        this.storedFilename = sanitizeFilename(storedFilename);
        this.storedPath = Objects.requireNonNull(storedPath, "Stored path cannot be null");
        this.fileSize = Math.max(0, fileSize);
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.uploadTimestamp = Instant.now();
        this.validationMessages = Collections.emptyList();
        this.errorMessage = null;
    }

    /**
     * Creates a failed upload result.
     *
     * @param status             the failure status (must not be SUCCESS)
     * @param originalFilename   the original filename provided by the user
     * @param validationMessages list of validation error messages
     * @param errorMessage       the primary error message
     * @throws IllegalArgumentException if status is SUCCESS
     */
    public UploadResult(Status status, String originalFilename,
            List<String> validationMessages, String errorMessage) {
        if (status == Status.SUCCESS) {
            throw new IllegalArgumentException("Use success constructor for successful uploads");
        }

        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.originalFilename = sanitizeFilename(originalFilename);
        this.storedFilename = null;
        this.storedPath = null;
        this.fileSize = 0;
        this.contentType = null;
        this.uploadTimestamp = Instant.now();
        this.validationMessages = validationMessages != null ? List.copyOf(validationMessages)
                : Collections.emptyList();
        this.errorMessage = errorMessage;
    }

    /**
     * Sanitizes a filename by removing potentially dangerous characters.
     *
     * <p>
     * <strong>Security Feature:</strong> Prevents path traversal attacks
     * and ensures filenames are safe for storage and display.
     * </p>
     *
     * @param filename the filename to sanitize
     * @return sanitized filename safe for storage and display
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unknown";
        }

        // Remove path separators and potentially dangerous characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_")
                .replaceAll("\\.{2,}", ".") // Remove multiple dots
                .trim();
    }

    /**
     * Returns whether the upload was successful.
     *
     * @return true if the upload completed successfully, false otherwise
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns the upload status.
     *
     * @return the status of the upload operation
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the original filename provided by the user.
     *
     * @return the original filename (sanitized for security)
     */
    public String getOriginalFilename() {
        return originalFilename;
    }

    /**
     * Returns the filename used for storage.
     *
     * <p>
     * This may be different from the original filename for security
     * reasons or to prevent naming conflicts.
     * </p>
     *
     * @return the stored filename, or null if upload failed
     */
    public String getStoredFilename() {
        return storedFilename;
    }

    /**
     * Returns the path where the file was stored.
     *
     * @return the storage path, or null if upload failed
     */
    public Path getStoredPath() {
        return storedPath;
    }

    /**
     * Returns the size of the uploaded file.
     *
     * @return the file size in bytes, or 0 if upload failed
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Returns the MIME content type of the uploaded file.
     *
     * @return the content type, or null if upload failed
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the timestamp when the upload was processed.
     *
     * @return the upload processing timestamp
     */
    public Instant getUploadTimestamp() {
        return uploadTimestamp;
    }

    /**
     * Returns the list of validation messages.
     *
     * <p>
     * For successful uploads, this list is typically empty.
     * For failed uploads, it contains detailed validation errors.
     * </p>
     *
     * @return immutable list of validation messages
     */
    public List<String> getValidationMessages() {
        return validationMessages;
    }

    /**
     * Returns the primary error message for failed uploads.
     *
     * @return the error message, or null for successful uploads
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns a human-readable summary of the upload result.
     *
     * @return formatted summary string
     */
    public String getSummary() {
        if (isSuccess()) {
            return String.format("Successfully uploaded '%s' (%d bytes) as '%s'",
                    originalFilename, fileSize, storedFilename);
        } else {
            return String.format("Failed to upload '%s': %s (%s)",
                    originalFilename, errorMessage, status);
        }
    }

    @Override
    public String toString() {
        return String.format("UploadResult{status=%s, originalFilename='%s', " +
                "storedFilename='%s', fileSize=%d, contentType='%s', timestamp=%s}",
                status, originalFilename, storedFilename, fileSize, contentType, uploadTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        UploadResult that = (UploadResult) obj;
        return fileSize == that.fileSize &&
                status == that.status &&
                Objects.equals(originalFilename, that.originalFilename) &&
                Objects.equals(storedFilename, that.storedFilename) &&
                Objects.equals(storedPath, that.storedPath) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(uploadTimestamp, that.uploadTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, originalFilename, storedFilename,
                storedPath, fileSize, contentType, uploadTimestamp);
    }
}
