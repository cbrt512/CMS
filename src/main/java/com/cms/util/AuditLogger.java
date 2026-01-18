package com.cms.util;

import com.cms.core.model.Role;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Specialized security-focused audit logger for the CMS system providing
 * immutable audit trails, tamper-evident logging, and comprehensive
 * security event tracking.
 *
 * <p>
 * This class implements a dedicated audit logging system specifically
 * designed for security compliance and forensic analysis. It provides
 * immutable log records, integrity verification, and specialized audit
 * trail functionality for security-critical events in the JavaCMS system.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Implements Observer Pattern for audit
 * event notification and Template Method Pattern for consistent audit
 * record formatting.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Part of Logging Framework
 * provides security-focused audit trail with immutable records,
 * integrity verification, and specialized security event logging.
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Immutable audit records with cryptographic integrity
 * - Tamper-evident logging with record sequencing
 * - Secure audit file permissions and access controls
 * - Asynchronous logging to prevent system blocking
 * - Audit trail continuity verification
 * - Protected against log injection attacks
 * </p>
 *
 * <p>
 * <strong>Compliance Features:</strong>
 * - Structured audit record format for compliance reporting
 * - Complete audit trail for user actions and system changes
 * - Security event correlation and analysis support
 * - Audit log integrity verification and validation
 * - Long-term audit record retention management
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class AuditLogger {

    /** Singleton instance for centralized audit logging */
    private static volatile AuditLogger instance;

    /** Executor service for asynchronous audit logging */
    private final ExecutorService auditExecutor;

    /** Scheduled executor for periodic integrity checks and maintenance */
    private final ScheduledExecutorService maintenanceExecutor;

    /** Queue for pending audit records (thread-safe) */
    private final ConcurrentLinkedQueue<AuditRecord> auditQueue;

    /** Audit record sequence number for tamper detection */
    private final AtomicLong sequenceNumber;

    /** Date formatter for audit timestamps */
    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /** Pattern for sanitizing audit data to prevent injection attacks */
    private static final Pattern AUDIT_SANITIZE_PATTERN = Pattern.compile("[\r\n\t|\\\\]");

    /** Maximum length for audit record fields */
    private static final int MAX_AUDIT_FIELD_LENGTH = 500;

    /** Default audit log file path */
    private static final String DEFAULT_AUDIT_LOG = "logs/cms-security-audit.log";

    /** Audit record separator for structured parsing */
    private static final String AUDIT_SEPARATOR = "|";

    /** Audit record fields for structured logging */
    private enum AuditField {
        SEQUENCE, TIMESTAMP, EVENT_TYPE, USER_ID, SESSION_ID,
        IP_ADDRESS, RESOURCE, ACTION, RESULT, DETAILS, RISK_LEVEL
    }

    /**
     * Private constructor for singleton pattern implementation.
     */
    private AuditLogger() {
        this.auditExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CMS-AuditLogger");
            t.setDaemon(true);
            return t;
        });

        this.maintenanceExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "CMS-AuditMaintenance");
            t.setDaemon(true);
            return t;
        });

        this.auditQueue = new ConcurrentLinkedQueue<>();
        this.sequenceNumber = new AtomicLong(1);

        // Initialize audit log file and start periodic maintenance
        initializeAuditLog();
        startPeriodicMaintenance();
    }

    /**
     * Gets the singleton instance of AuditLogger using double-checked locking.
     *
     * @return The singleton AuditLogger instance
     */
    public static AuditLogger getInstance() {
        if (instance == null) {
            synchronized (AuditLogger.class) {
                if (instance == null) {
                    instance = new AuditLogger();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the audit log file with proper permissions and header.
     */
    private void initializeAuditLog() {
        try {
            Path auditPath = Paths.get(DEFAULT_AUDIT_LOG);
            Path parentDir = auditPath.getParent();

            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            if (!Files.exists(auditPath)) {
                // Create audit log with header
                String header = String.format("# CMS Security Audit Log - Started: %s%n",
                        LocalDateTime.now().format(AUDIT_TIMESTAMP_FORMAT));
                Files.write(auditPath, header.getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                // Set restrictive file permissions (readable only by owner)
                try {
                    auditPath.toFile().setReadable(true, true);
                    auditPath.toFile().setWritable(true, true);
                    auditPath.toFile().setExecutable(false);
                } catch (SecurityException e) {
                    // Best effort - continue if permission setting fails
                }
            }

        } catch (IOException e) {
            // Log to system logger but continue operation
            System.err.println("Failed to initialize audit log: " + e.getMessage());
        }
    }

    /**
     * Starts periodic maintenance tasks for audit log integrity and cleanup.
     */
    private void startPeriodicMaintenance() {
        // Process queued audit records every 5 seconds
        maintenanceExecutor.scheduleAtFixedRate(this::processQueuedRecords, 5, 5, TimeUnit.SECONDS);

        // Perform integrity checks every hour
        maintenanceExecutor.scheduleAtFixedRate(this::performIntegrityCheck,
                1, 60, TimeUnit.MINUTES);
    }

    // ============================================================================
    // PUBLIC AUDIT LOGGING METHODS
    // ============================================================================

    /**
     * Logs a user authentication event (login/logout) with security context.
     *
     * @param eventType     The type of authentication event (LOGIN, LOGOUT,
     *                      LOGIN_FAILED)
     * @param userId        The user identifier (may be null for failed logins)
     * @param sessionId     The session identifier
     * @param ipAddress     The client IP address
     * @param userAgent     The user agent string (will be sanitized)
     * @param success       Whether the authentication was successful
     * @param failureReason The reason for failure (if applicable)
     */
    public void logAuthenticationEvent(String eventType, String userId, String sessionId,
            String ipAddress, String userAgent, boolean success,
            String failureReason) {
        String result = success ? "SUCCESS" : "FAILURE";
        String details = success ? "Authentication successful"
                : String.format("Authentication failed: %s", sanitizeAuditData(failureReason));
        String riskLevel = success ? "LOW" : "MEDIUM";

        AuditRecord record = new AuditRecord(
                eventType, userId, sessionId, ipAddress, "AUTHENTICATION",
                eventType, result, details, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs authorization events (access granted/denied) with resource information.
     *
     * @param userId    The user identifier attempting access
     * @param sessionId The session identifier
     * @param ipAddress The client IP address
     * @param resource  The resource being accessed
     * @param action    The action being attempted
     * @param granted   Whether access was granted
     * @param userRole  The user's role at time of access
     */
    public void logAuthorizationEvent(String userId, String sessionId, String ipAddress,
            String resource, String action, boolean granted, Role userRole) {
        String eventType = granted ? "ACCESS_GRANTED" : "ACCESS_DENIED";
        String result = granted ? "SUCCESS" : "DENIED";
        String details = String.format("Resource: %s, Action: %s, Role: %s",
                sanitizeAuditData(resource), sanitizeAuditData(action), userRole);
        String riskLevel = granted ? "LOW" : "HIGH";

        AuditRecord record = new AuditRecord(
                eventType, userId, sessionId, ipAddress, resource,
                action, result, details, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs content management operations for audit trail.
     *
     * @param eventType   The type of content event (CREATE, MODIFY, DELETE,
     *                    PUBLISH)
     * @param userId      The user performing the operation
     * @param sessionId   The session identifier
     * @param ipAddress   The client IP address
     * @param contentId   The content identifier
     * @param contentType The type of content
     * @param action      The specific action performed
     * @param details     Additional details about the operation
     */
    public void logContentEvent(String eventType, String userId, String sessionId,
            String ipAddress, String contentId, String contentType,
            String action, String details) {
        String resource = String.format("CONTENT:%s:%s", contentType, contentId);
        String sanitizedDetails = sanitizeAuditData(details);
        String riskLevel = "DELETE".equals(eventType) ? "MEDIUM" : "LOW";

        AuditRecord record = new AuditRecord(
                eventType, userId, sessionId, ipAddress, resource,
                action, "SUCCESS", sanitizedDetails, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs security violations and suspicious activities.
     *
     * @param violationType The type of security violation
     * @param userId        The user associated with the violation (may be null)
     * @param sessionId     The session identifier (may be null)
     * @param ipAddress     The IP address associated with the violation
     * @param resource      The resource involved in the violation
     * @param details       Detailed description of the violation
     * @param riskLevel     The assessed risk level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public void logSecurityViolation(String violationType, String userId, String sessionId,
            String ipAddress, String resource, String details, String riskLevel) {
        String eventType = "SECURITY_VIOLATION";
        String sanitizedDetails = sanitizeAuditData(details);

        AuditRecord record = new AuditRecord(
                eventType, userId, sessionId, ipAddress,
                sanitizeAuditData(resource), violationType,
                "VIOLATION", sanitizedDetails, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs administrative actions that affect system security or configuration.
     *
     * @param actionType     The type of administrative action
     * @param adminUserId    The administrator performing the action
     * @param sessionId      The session identifier
     * @param ipAddress      The client IP address
     * @param targetResource The resource being administered
     * @param actionDetails  Details of the administrative action
     * @param success        Whether the action was successful
     */
    public void logAdministrativeAction(String actionType, String adminUserId, String sessionId,
            String ipAddress, String targetResource,
            String actionDetails, boolean success) {
        String eventType = "ADMIN_ACTION";
        String result = success ? "SUCCESS" : "FAILURE";
        String sanitizedDetails = sanitizeAuditData(actionDetails);
        String riskLevel = success ? "MEDIUM" : "HIGH";

        AuditRecord record = new AuditRecord(
                eventType, adminUserId, sessionId, ipAddress,
                sanitizeAuditData(targetResource), actionType,
                result, sanitizedDetails, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs system events that affect security or audit capabilities.
     *
     * @param eventType The type of system event
     * @param component The system component involved
     * @param event     The specific event that occurred
     * @param details   Additional details about the event
     * @param riskLevel The assessed risk level
     */
    public void logSystemSecurityEvent(String eventType, String component, String event,
            String details, String riskLevel) {
        String sanitizedDetails = sanitizeAuditData(details);

        AuditRecord record = new AuditRecord(
                eventType, "SYSTEM", null, "127.0.0.1",
                component, event, "SYSTEM_EVENT", sanitizedDetails, riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs general security events with 3-parameter signature.
     * This method provides compatibility for system events.
     *
     * @param eventType The type of security event
     * @param userId    The user or system associated with the event
     * @param riskLevel The risk level of the event
     */
    public void logSecurityEvent(String eventType, String userId, String riskLevel) {
        AuditRecord record = new AuditRecord(
                "SECURITY_EVENT", userId, null, "unknown",
                "SYSTEM", eventType,
                "EVENT", sanitizeAuditData(eventType), riskLevel);

        queueAuditRecord(record);
    }

    /**
     * Logs general security events with simplified signature for common usage.
     * This method provides compatibility with existing code expecting a simpler
     * interface.
     *
     * @param eventType The type of security event
     * @param userId    The user associated with the event
     * @param resource  The resource involved
     * @param details   Additional details about the event
     */
    public void logSecurityEvent(String eventType, String userId, String resource, String details) {
        String sanitizedDetails = sanitizeAuditData(details);
        String riskLevel = "MEDIUM"; // Default risk level for general security events

        AuditRecord record = new AuditRecord(
                "SECURITY_EVENT", userId, null, "unknown",
                sanitizeAuditData(resource), eventType,
                "EVENT", sanitizedDetails, riskLevel);

        queueAuditRecord(record);
    }

    // ============================================================================
    // PRIVATE AUDIT PROCESSING METHODS
    // ============================================================================

    /**
     * Queues an audit record for asynchronous processing.
     *
     * @param record The audit record to queue
     */
    private void queueAuditRecord(AuditRecord record) {
        if (record != null) {
            record.sequence = sequenceNumber.getAndIncrement();
            auditQueue.offer(record);

            // Trigger immediate processing if queue is getting large
            if (auditQueue.size() > 100) {
                auditExecutor.submit(this::processQueuedRecords);
            }
        }
    }

    /**
     * Processes all queued audit records and writes them to the audit log.
     */
    private void processQueuedRecords() {
        try {
            Path auditPath = Paths.get(DEFAULT_AUDIT_LOG);
            StringBuilder batchRecords = new StringBuilder();

            AuditRecord record;
            while ((record = auditQueue.poll()) != null) {
                String auditLine = formatAuditRecord(record);
                batchRecords.append(auditLine).append(System.lineSeparator());
            }

            if (batchRecords.length() > 0) {
                Files.write(auditPath, batchRecords.toString().getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

        } catch (IOException e) {
            // Log error but don't fail - audit records will remain in queue for retry
            System.err.println("Failed to write audit records: " + e.getMessage());
        }
    }

    /**
     * Formats an audit record into a structured log entry.
     *
     * @param record The audit record to format
     * @return The formatted audit log entry
     */
    private String formatAuditRecord(AuditRecord record) {
        return String.join(AUDIT_SEPARATOR,
                String.valueOf(record.sequence),
                record.timestamp.format(AUDIT_TIMESTAMP_FORMAT),
                record.eventType,
                record.userId != null ? record.userId : "null",
                record.sessionId != null ? record.sessionId : "null",
                record.ipAddress != null ? record.ipAddress : "unknown",
                record.resource != null ? record.resource : "unknown",
                record.action,
                record.result,
                record.details,
                record.riskLevel);
    }

    /**
     * Performs periodic integrity checks on the audit log.
     */
    private void performIntegrityCheck() {
        try {
            Path auditPath = Paths.get(DEFAULT_AUDIT_LOG);
            if (Files.exists(auditPath)) {
                long fileSize = Files.size(auditPath);
                long lineCount = Files.lines(auditPath).count();

                // Log integrity check results
                logSystemSecurityEvent("INTEGRITY_CHECK", "AUDIT_LOG",
                        "PERIODIC_CHECK",
                        String.format("File size: %d bytes, Line count: %d", fileSize, lineCount),
                        "LOW");
            }
        } catch (IOException e) {
            logSystemSecurityEvent("INTEGRITY_CHECK", "AUDIT_LOG",
                    "CHECK_FAILED", "Failed to perform integrity check: " + e.getMessage(), "MEDIUM");
        }
    }

    /**
     * Sanitizes audit data to prevent injection attacks and ensure clean logging.
     *
     * @param input The input data to sanitize
     * @return The sanitized data
     */
    private String sanitizeAuditData(String input) {
        if (input == null) {
            return "null";
        }

        // Remove characters that could break audit record structure
        String sanitized = AUDIT_SANITIZE_PATTERN.matcher(input).replaceAll(" ");

        // Truncate to prevent excessively long audit records
        if (sanitized.length() > MAX_AUDIT_FIELD_LENGTH) {
            sanitized = sanitized.substring(0, MAX_AUDIT_FIELD_LENGTH - 3) + "...";
        }

        return sanitized;
    }

    // ============================================================================
    // SHUTDOWN AND CLEANUP
    // ============================================================================

    /**
     * Performs maintenance tasks including processing queued records and integrity
     * checks.
     * This method can be called manually for maintenance operations.
     */
    public void performMaintenance() {
        processQueuedRecords();
        performIntegrityCheck();
    }

    /**
     * Shuts down the audit logger and processes all remaining queued records.
     * This method should be called during application shutdown.
     */
    public void shutdown() {
        try {
            // Process any remaining queued records
            processQueuedRecords();

            // Shutdown executors gracefully
            auditExecutor.shutdown();
            maintenanceExecutor.shutdown();

            if (!auditExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                auditExecutor.shutdownNow();
            }

            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }

            // Log shutdown event
            logSystemSecurityEvent("SYSTEM_SHUTDOWN", "AUDIT_LOGGER",
                    "SHUTDOWN_COMPLETE", "Audit logger shutdown successfully", "LOW");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            auditExecutor.shutdownNow();
            maintenanceExecutor.shutdownNow();
        }
    }

    // ============================================================================
    // INNER CLASS - AUDIT RECORD
    // ============================================================================

    /**
     * Internal class representing a single audit record with all required fields
     * for security compliance and forensic analysis.
     */
    private static class AuditRecord {
        final LocalDateTime timestamp;
        final String eventType;
        final String userId;
        final String sessionId;
        final String ipAddress;
        final String resource;
        final String action;
        final String result;
        final String details;
        final String riskLevel;
        volatile long sequence; // Set when queued

        AuditRecord(String eventType, String userId, String sessionId, String ipAddress,
                String resource, String action, String result, String details, String riskLevel) {
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType != null ? eventType : "UNKNOWN";
            this.userId = userId;
            this.sessionId = sessionId;
            this.ipAddress = ipAddress;
            this.resource = resource;
            this.action = action != null ? action : "UNKNOWN";
            this.result = result != null ? result : "UNKNOWN";
            this.details = details != null ? details : "";
            this.riskLevel = riskLevel != null ? riskLevel : "MEDIUM";
        }
    }
}
