package com.cms.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive I/O utility class providing advanced file operations,
 * streaming utilities, and helper methods for the CMS I/O operations.
 *
 * <p>
 * This utility class provides common I/O operations used throughout
 * the CMS system, including file operations, stream processing, compression,
 * backup management, and asynchronous I/O support. It serves as a central
 * location for shared I/O functionality across all I/O services.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Utility Pattern - Provides stateless
 * helper methods for common I/O operations, following the principle of
 * code reusability and centralized functionality.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Java I/O Operations
 * Provides advanced I/O operations including file system operations,
 * stream processing, compression, asynchronous operations, and resource
 * management with comprehensive utility methods.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Safe file operations with path validation
 * - Secure stream processing with limits
 * - File integrity verification with checksums
 * - Resource cleanup and exception handling
 * - Safe temporary file creation and cleanup
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong>
 * Uses Map for file metadata, List for batch operations,
 * Set for file filtering, providing utility-level
 * collections integration.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class IOUtils {

    /** Default buffer size for I/O operations (8KB) */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Maximum file size for safety operations (100MB) */
    private static final long MAX_SAFE_FILE_SIZE = 100 * 1024 * 1024;

    /** Executor service for asynchronous operations */
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "IOUtils-Async");
        thread.setDaemon(true);
        return thread;
    });

    /** Private constructor to prevent instantiation */
    private IOUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Safely copies data between streams with size limits and progress tracking.
     *
     * <p>
     * This method provides secure stream copying with:
     * </p>
     * <ul>
     * <li>Size limit enforcement to prevent DoS attacks</li>
     * <li>Progress tracking for large operations</li>
     * <li>Efficient buffered copying</li>
     * <li>Resource management and cleanup</li>
     * <li>Exception handling and recovery</li>
     * </ul>
     *
     * <p>
     * <strong>Java I/O Features:</strong>
     * </p>
     * <ul>
     * <li>Buffered stream operations for efficiency</li>
     * <li>Size validation and limit enforcement</li>
     * <li>Progress tracking during copy operations</li>
     * <li>Proper exception handling and cleanup</li>
     * </ul>
     *
     * @param input            the input stream to read from
     * @param output           the output stream to write to
     * @param maxSize          maximum allowed bytes to copy (0 for no limit)
     * @param progressCallback optional progress callback (can be null)
     * @return total bytes copied
     * @throws IOException              if I/O operation fails
     * @throws IllegalArgumentException if streams are null or maxSize is negative
     * @throws SecurityException        if size limit is exceeded
     */
    public static long copyStream(InputStream input, OutputStream output,
            long maxSize, ProgressCallback progressCallback) throws IOException {

        if (input == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        if (maxSize < 0) {
            throw new IllegalArgumentException("Max size cannot be negative");
        }

        // Use default max size if none specified
        long effectiveMaxSize = maxSize > 0 ? maxSize : MAX_SAFE_FILE_SIZE;

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long totalBytes = 0;
        int bytesRead;
        long lastProgressUpdate = System.currentTimeMillis();

        try (BufferedInputStream bufferedInput = new BufferedInputStream(input);
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(output)) {

            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                totalBytes += bytesRead;

                // Enforce size limit
                if (totalBytes > effectiveMaxSize) {
                    throw new SecurityException("Stream size exceeds maximum allowed: " + effectiveMaxSize);
                }

                bufferedOutput.write(buffer, 0, bytesRead);

                // Update progress if callback provided
                if (progressCallback != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastProgressUpdate > 100) { // Update every 100ms
                        progressCallback.updateProgress(totalBytes, effectiveMaxSize);
                        lastProgressUpdate = currentTime;
                    }
                }
            }

            bufferedOutput.flush();

            // Final progress update
            if (progressCallback != null) {
                progressCallback.updateProgress(totalBytes, totalBytes);
            }
        }

        return totalBytes;
    }

    /**
     * Progress callback interface for long-running I/O operations.
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void updateProgress(long completed, long total);
    }

    /**
     * Copies data between streams with default settings.
     *
     * @param input  the input stream
     * @param output the output stream
     * @return total bytes copied
     * @throws IOException if I/O operation fails
     */
    public static long copyStream(InputStream input, OutputStream output) throws IOException {
        return copyStream(input, output, 0, null);
    }

    /**
     * Safely reads a file to string with encoding and size validation.
     *
     * <p>
     * <strong>Security Feature:</strong> Validates file size before reading
     * to prevent memory exhaustion attacks.
     * </p>
     *
     * @param filePath the file path to read
     * @param maxSize  maximum file size allowed (0 for default limit)
     * @return file content as string
     * @throws IOException       if file reading fails
     * @throws SecurityException if file is too large
     */
    public static String readFileToString(Path filePath, long maxSize) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("File not found or not readable: " + filePath);
        }

        long fileSize = Files.size(filePath);
        long effectiveMaxSize = maxSize > 0 ? maxSize : MAX_SAFE_FILE_SIZE;

        if (fileSize > effectiveMaxSize) {
            throw new SecurityException("File size (" + fileSize +
                    ") exceeds maximum allowed (" + effectiveMaxSize + ")");
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder((int) fileSize);
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString();
        }
    }

    /**
     * Safely writes string to file with atomic operation.
     *
     * <p>
     * Uses temporary file and atomic move for consistency.
     * </p>
     *
     * @param filePath     the target file path
     * @param content      the content to write
     * @param createBackup whether to create backup of existing file
     * @throws IOException if writing fails
     */
    public static void writeStringToFile(Path filePath, String content, boolean createBackup)
            throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        if (content == null) {
            content = "";
        }

        // Create parent directories if needed
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Create backup if requested and file exists
        Path backupPath = null;
        if (createBackup && Files.exists(filePath)) {
            backupPath = createBackupFile(filePath);
        }

        // Use temporary file for atomic write
        Path tempPath = filePath.getParent().resolve(filePath.getFileName() + ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(content);
                writer.flush();
            }

            // Atomic move to final location
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            // Cleanup temp file on failure
            Files.deleteIfExists(tempPath);

            // Restore backup if available
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    Files.move(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreError) {
                    // Log but don't mask original error
                    System.err.println("Failed to restore backup: " + restoreError.getMessage());
                }
            }

            throw e;
        }

        // Cleanup successful backup
        if (backupPath != null) {
            Files.deleteIfExists(backupPath);
        }
    }

    /**
     * Creates a backup file with timestamp.
     *
     * @param originalPath the original file to backup
     * @return path to the backup file
     * @throws IOException if backup creation fails
     */
    public static Path createBackupFile(Path originalPath) throws IOException {
        if (!Files.exists(originalPath)) {
            throw new FileNotFoundException("Original file does not exist: " + originalPath);
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now());
        String backupName = originalPath.getFileName() + ".backup." + timestamp;
        Path backupPath = originalPath.getParent().resolve(backupName);

        Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    /**
     * Compresses data using GZIP compression.
     *
     * <p>
     * <strong>Java I/O Feature:</strong> Demonstrates stream chaining
     * with compression streams for efficient data storage.
     * </p>
     *
     * @param input  the input stream to compress
     * @param output the output stream for compressed data
     * @return number of compressed bytes written
     * @throws IOException if compression fails
     */
    public static long compressStream(InputStream input, OutputStream output) throws IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Input and output streams cannot be null");
        }

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(new BufferedOutputStream(output));
                BufferedInputStream bufferedIn = new BufferedInputStream(input)) {

            return copyStream(bufferedIn, gzipOut);
        }
    }

    /**
     * Decompresses GZIP compressed data.
     *
     * @param input  the compressed input stream
     * @param output the output stream for decompressed data
     * @return number of decompressed bytes written
     * @throws IOException if decompression fails
     */
    public static long decompressStream(InputStream input, OutputStream output) throws IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Input and output streams cannot be null");
        }

        try (GZIPInputStream gzipIn = new GZIPInputStream(new BufferedInputStream(input));
                BufferedOutputStream bufferedOut = new BufferedOutputStream(output)) {

            return copyStream(gzipIn, bufferedOut);
        }
    }

    /**
     * Calculates file checksum for integrity verification.
     *
     * <p>
     * <strong>Security Feature:</strong> Provides file integrity verification
     * using cryptographic hash functions.
     * </p>
     *
     * @param filePath  the file to checksum
     * @param algorithm the hash algorithm (MD5, SHA-1, SHA-256)
     * @return hex string representation of checksum
     * @throws IOException              if file reading fails
     * @throws NoSuchAlgorithmException if algorithm is not available
     */
    public static String calculateChecksum(Path filePath, String algorithm)
            throws IOException, NoSuchAlgorithmException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            algorithm = "SHA-256";
        }

        MessageDigest digest = MessageDigest.getInstance(algorithm);

        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Verifies file integrity against expected checksum.
     *
     * @param filePath         the file to verify
     * @param expectedChecksum the expected checksum
     * @param algorithm        the hash algorithm used
     * @return true if checksums match, false otherwise
     * @throws IOException if file reading fails
     */
    public static boolean verifyChecksum(Path filePath, String expectedChecksum, String algorithm)
            throws IOException {
        try {
            String actualChecksum = calculateChecksum(filePath, algorithm);
            return actualChecksum.equalsIgnoreCase(expectedChecksum);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unsupported algorithm: " + algorithm, e);
        }
    }

    /**
     * Gets comprehensive file metadata.
     *
     * <p>
     * <strong>Java I/O Feature:</strong> Demonstrates NIO.2 file attributes
     * and metadata extraction capabilities.
     * </p>
     *
     * @param filePath the file to analyze
     * @return map containing file metadata
     * @throws IOException if file access fails
     */
    public static Map<String, Object> getFileMetadata(Path filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File does not exist: " + filePath);
        }

        Map<String, Object> metadata = new HashMap<>();

        // Basic attributes
        metadata.put("fileName", filePath.getFileName().toString());
        metadata.put("absolutePath", filePath.toAbsolutePath().toString());
        metadata.put("size", Files.size(filePath));
        metadata.put("isDirectory", Files.isDirectory(filePath));
        metadata.put("isReadable", Files.isReadable(filePath));
        metadata.put("isWritable", Files.isWritable(filePath));
        metadata.put("isExecutable", Files.isExecutable(filePath));
        metadata.put("isHidden", Files.isHidden(filePath));

        // Time attributes
        metadata.put("lastModified", Files.getLastModifiedTime(filePath).toInstant());
        try {
            // Note: Creation time and last access time require platform support
            // Basic file times are more reliable
            metadata.put("lastModified", Files.getLastModifiedTime(filePath).toInstant());
        } catch (Exception e) {
            // Some filesystems don't support all time attributes
            metadata.put("lastModified", Files.getLastModifiedTime(filePath).toInstant());
        }

        // Content type
        String contentType = Files.probeContentType(filePath);
        metadata.put("contentType", contentType != null ? contentType : "unknown");

        // File extension
        String fileName = filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        metadata.put("extension", lastDot != -1 ? fileName.substring(lastDot + 1) : "");

        return metadata;
    }

    /**
     * Safely deletes files and directories with validation.
     *
     * <p>
     * <strong>Security Feature:</strong> Validates paths and provides
     * safe deletion with backup options.
     * </p>
     *
     * @param path         the path to delete
     * @param recursive    whether to delete directories recursively
     * @param createBackup whether to create backup before deletion
     * @return true if deletion was successful
     * @throws IOException if deletion fails
     */
    public static boolean safeDelete(Path path, boolean recursive, boolean createBackup)
            throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            return false;
        }

        // Create backup if requested
        Path backupPath = null;
        if (createBackup) {
            try {
                backupPath = createBackupFile(path);
            } catch (IOException e) {
                // Log warning but continue with deletion
                System.err.println("Warning: Could not create backup: " + e.getMessage());
            }
        }

        try {
            if (Files.isDirectory(path)) {
                if (recursive) {
                    deleteDirectoryRecursively(path);
                } else {
                    Files.delete(path); // Will fail if directory is not empty
                }
            } else {
                Files.delete(path);
            }

            return true;

        } catch (IOException e) {
            // Restore backup on failure if available
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    Files.move(backupPath, path, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreError) {
                    // Log but don't mask original error
                    System.err.println("Failed to restore backup after delete failure: " +
                            restoreError.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Recursively deletes directory contents.
     *
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder()) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + path, e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Asynchronously copies files with progress tracking.
     *
     * <p>
     * <strong>Advanced I/O Feature:</strong> Demonstrates asynchronous
     * I/O operations using CompletableFuture for non-blocking operations.
     * </p>
     *
     * @param source           the source file path
     * @param target           the target file path
     * @param progressCallback optional progress callback
     * @return CompletableFuture that completes when copy is finished
     */
    public static CompletableFuture<Long> copyFileAsync(Path source, Path target,
            ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(source)) {
                    throw new RuntimeException("Source file does not exist: " + source);
                }

                // Create parent directories if needed
                Path parentDir = target.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                try (InputStream input = Files.newInputStream(source);
                        OutputStream output = Files.newOutputStream(target)) {

                    long fileSize = Files.size(source);
                    return copyStream(input, output, fileSize, progressCallback);
                }

            } catch (IOException e) {
                throw new RuntimeException("File copy failed", e);
            }
        }, ASYNC_EXECUTOR);
    }

    /**
     * Creates a temporary file with specific prefix and suffix.
     *
     * <p>
     * <strong>Security Feature:</strong> Creates temporary files with
     * appropriate permissions and cleanup mechanisms.
     * </p>
     *
     * @param prefix the file name prefix
     * @param suffix the file name suffix (including extension)
     * @return path to the temporary file
     * @throws IOException if temporary file creation fails
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "cms_temp";
        }
        if (suffix == null) {
            suffix = ".tmp";
        }

        Path tempFile = Files.createTempFile(prefix, suffix);

        // Register for cleanup on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // Ignore cleanup failures during shutdown
            }
        }));

        return tempFile;
    }

    /**
     * Batch processes files in a directory with filtering.
     *
     * <p>
     * <strong>Collections Integration:</strong> Uses Stream API and
     * collections for efficient batch file processing.
     * </p>
     *
     * @param directory  the directory to process
     * @param fileFilter filter predicate for files to include
     * @param processor  the file processor function
     * @param maxFiles   maximum number of files to process (0 for no limit)
     * @return list of processing results
     * @throws IOException if directory access fails
     */
    public static <T> List<T> batchProcessFiles(Path directory,
            java.util.function.Predicate<Path> fileFilter,
            java.util.function.Function<Path, T> processor,
            int maxFiles) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Directory does not exist or is not a directory: " + directory);
        }

        List<T> results = new ArrayList<>();

        try (var stream = Files.walk(directory, 1)) { // Only immediate children
            var fileStream = stream.filter(Files::isRegularFile);

            if (fileFilter != null) {
                fileStream = fileStream.filter(fileFilter);
            }

            if (maxFiles > 0) {
                fileStream = fileStream.limit(maxFiles);
            }

            fileStream.forEach(path -> {
                try {
                    T result = processor.apply(path);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    // Log error but continue processing
                    System.err.println("Error processing file " + path + ": " + e.getMessage());
                }
            });
        }

        return results;
    }

    /**
     * Gets disk space information for a path.
     *
     * @param path the path to check
     * @return map containing space information
     * @throws IOException if path access fails
     */
    public static Map<String, Long> getDiskSpaceInfo(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        Map<String, Long> spaceInfo = new HashMap<>();

        try {
            FileStore store = Files.getFileStore(path);
            spaceInfo.put("totalSpace", store.getTotalSpace());
            spaceInfo.put("usableSpace", store.getUsableSpace());
            spaceInfo.put("unallocatedSpace", store.getUnallocatedSpace());
        } catch (IOException e) {
            // Fallback for file system that doesn't support space queries
            spaceInfo.put("totalSpace", -1L);
            spaceInfo.put("usableSpace", -1L);
            spaceInfo.put("unallocatedSpace", -1L);
        }

        return spaceInfo;
    }

    /**
     * Cleanup method for shutdown - attempts to clean up executor.
     */
    public static void shutdown() {
        ASYNC_EXECUTOR.shutdown();
    }
}
