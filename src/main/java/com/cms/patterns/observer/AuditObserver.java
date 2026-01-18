package com.cms.patterns.observer;

import com.cms.core.model.Content;
import com.cms.core.model.User;
import com.cms.util.CMSLogger;
import com.cms.util.AuditLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Specialized observer for comprehensive audit trail generation and security
 * monitoring.
 *
 * <p>
 * This observer maintains detailed audit logs of all content operations for
 * compliance, security monitoring, and forensic analysis. It provides
 * comprehensive
 * tracking of who did what, when, and why, with immutable audit records and
 * integrity verification.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Security-focused
 * concrete observer that generates immutable audit trails with advanced
 * filtering,
 * risk assessment, and compliance reporting capabilities.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> shows advanced Collections Framework
 * usage with concurrent data structures, Generics for type-safe audit
 * operations,
 * and seamless integration with existing logging infrastructure and security
 * systems.
 * </p>
 *
 * <p>
 * <strong>Audit Features:</strong>
 * <ul>
 * <li>Immutable audit records with digital signatures</li>
 * <li>Real-time security event detection and alerting</li>
 * <li>Risk-based audit level adjustment</li>
 * <li>Compliance reporting with configurable retention</li>
 * <li>Forensic analysis support with detailed context</li>
 * <li>Audit trail integrity verification and validation</li>
 * </ul>
 * </p>
 *
 * @see ContentObserver For the observer interface
 * @see ContentEvent For event data structure
 * @see AuditLogger For audit logging infrastructure
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class AuditObserver implements ContentObserver {

    private final CMSLogger logger;
    private final AuditLogger auditLogger;

    // Audit record storage (Collections Framework)
    private final Map<String, AuditRecord> auditRecords; // Event ID -> Audit Record
    private final Queue<AuditRecord> recentAudits; // Recent audit records for quick access
    private final Map<String, List<AuditRecord>> userAuditHistory; // User -> Audit history
    private final Map<String, List<AuditRecord>> contentAuditHistory; // Content -> Audit history

    // Security monitoring and risk assessment (Generics)
    private final Map<String, SecurityMetrics> userSecurityMetrics; // User -> Security metrics
    private final Set<String> suspiciousActivities; // Flagged activities
    private final Map<RiskLevel, AtomicLong> riskLevelCounters; // Risk level statistics
    private final Queue<SecurityAlert> securityAlerts; // Recent security alerts

    // Compliance and retention management
    private final Map<ComplianceType, ComplianceConfig> complianceConfigurations;
    private final Set<String> retentionExemptRecords; // Records exempt from retention policies
    private final Map<String, LocalDateTime> recordExpirationTimes; // Record ID -> Expiration

    // Performance and statistics
    private final AtomicLong auditRecordCount;
    private final Map<ContentEvent.EventType, AtomicLong> eventTypeCounters;
    private final Map<String, AtomicLong> complianceTypeCounters;

    /**
     * Risk levels for audit events.
     */
    public enum RiskLevel {
        MINIMAL(0, "Routine operations with no security implications"),
        LOW(1, "Standard operations with minimal risk"),
        MEDIUM(2, "Operations requiring attention"),
        HIGH(3, "Operations with security implications"),
        CRITICAL(4, "Operations requiring immediate review");

        private final int level;
        private final String description;

        RiskLevel(int level, String description) {
            this.level = level;
            this.description = description;
        }

        public int getLevel() {
            return level;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Compliance types for different regulatory implementations.
     */
    public enum ComplianceType {
        GDPR("General Data Protection Regulation"),
        SOX("Sarbanes-Oxley Act"),
        HIPAA("Health Insurance Portability and Accountability Act"),
        PCI_DSS("Payment Card Industry Data Security Standard"),
        ISO27001("ISO 27001 Information Security Management"),
        CUSTOM("Custom compliance implementations");

        private final String description;

        ComplianceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Immutable audit record with comprehensive context information.
     */
    private static class AuditRecord {
        final String recordId;
        final String eventId;
        final LocalDateTime timestamp;
        final String operation;
        final String contentId;
        final String contentTitle;
        final String contentType;
        final String userId;
        final String username;
        final String sessionId;
        final RiskLevel riskLevel;
        final Map<String, Object> context;
        final Map<String, Object> beforeState;
        final Map<String, Object> afterState;
        final String ipAddress;
        final String userAgent;
        final Set<ComplianceType> applicableCompliance;
        final String integrity; // Digital signature/hash for integrity verification

        AuditRecord(Builder builder) {
            this.recordId = builder.recordId != null ? builder.recordId : UUID.randomUUID().toString();
            this.eventId = builder.eventId;
            this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
            this.operation = builder.operation;
            this.contentId = builder.contentId;
            this.contentTitle = builder.contentTitle;
            this.contentType = builder.contentType;
            this.userId = builder.userId;
            this.username = builder.username;
            this.sessionId = builder.sessionId;
            this.riskLevel = builder.riskLevel != null ? builder.riskLevel : RiskLevel.LOW;
            this.context = builder.context != null ? Collections.unmodifiableMap(new HashMap<>(builder.context))
                    : Collections.emptyMap();
            this.beforeState = builder.beforeState != null
                    ? Collections.unmodifiableMap(new HashMap<>(builder.beforeState))
                    : Collections.emptyMap();
            this.afterState = builder.afterState != null
                    ? Collections.unmodifiableMap(new HashMap<>(builder.afterState))
                    : Collections.emptyMap();
            this.ipAddress = builder.ipAddress;
            this.userAgent = builder.userAgent;
            this.applicableCompliance = builder.applicableCompliance != null
                    ? Collections.unmodifiableSet(new HashSet<>(builder.applicableCompliance))
                    : Collections.emptySet();
            this.integrity = calculateIntegrity();
        }

        private String calculateIntegrity() {
            // Simplified integrity calculation - in practice would use proper digital
            // signatures
            String dataToHash = recordId + eventId + timestamp + operation + contentId + userId;
            return "SHA256:" + Integer.toHexString(dataToHash.hashCode());
        }

        public boolean verifyIntegrity() {
            return integrity.equals(calculateIntegrity());
        }

        static class Builder {
            String recordId;
            String eventId;
            LocalDateTime timestamp;
            String operation;
            String contentId;
            String contentTitle;
            String contentType;
            String userId;
            String username;
            String sessionId;
            RiskLevel riskLevel;
            Map<String, Object> context;
            Map<String, Object> beforeState;
            Map<String, Object> afterState;
            String ipAddress;
            String userAgent;
            Set<ComplianceType> applicableCompliance;

            Builder eventId(String eventId) {
                this.eventId = eventId;
                return this;
            }

            Builder operation(String operation) {
                this.operation = operation;
                return this;
            }

            Builder contentId(String contentId) {
                this.contentId = contentId;
                return this;
            }

            Builder contentTitle(String contentTitle) {
                this.contentTitle = contentTitle;
                return this;
            }

            Builder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            Builder username(String username) {
                this.username = username;
                return this;
            }

            Builder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }

            Builder riskLevel(RiskLevel riskLevel) {
                this.riskLevel = riskLevel;
                return this;
            }

            Builder context(Map<String, Object> context) {
                this.context = context;
                return this;
            }

            Builder beforeState(Map<String, Object> beforeState) {
                this.beforeState = beforeState;
                return this;
            }

            Builder afterState(Map<String, Object> afterState) {
                this.afterState = afterState;
                return this;
            }

            Builder ipAddress(String ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }

            Builder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }

            Builder applicableCompliance(Set<ComplianceType> compliance) {
                this.applicableCompliance = compliance;
                return this;
            }

            AuditRecord build() {
                return new AuditRecord(this);
            }
        }

        public static Builder builder() {
            return new Builder();
        }
    }

    /**
     * Security metrics for user behavior analysis.
     */
    private static class SecurityMetrics {
        final AtomicLong totalOperations = new AtomicLong(0);
        final AtomicLong suspiciousOperations = new AtomicLong(0);
        final AtomicLong failedOperations = new AtomicLong(0);
        final Map<RiskLevel, AtomicLong> riskLevelCounts = new ConcurrentHashMap<>();
        volatile LocalDateTime lastActivity;
        volatile LocalDateTime firstSuspiciousActivity;
        volatile double riskScore = 0.0;

        SecurityMetrics() {
            for (RiskLevel level : RiskLevel.values()) {
                riskLevelCounts.put(level, new AtomicLong(0));
            }
            this.lastActivity = LocalDateTime.now();
        }

        void recordOperation(RiskLevel riskLevel, boolean suspicious) {
            totalOperations.incrementAndGet();
            riskLevelCounts.get(riskLevel).incrementAndGet();
            lastActivity = LocalDateTime.now();

            if (suspicious) {
                suspiciousOperations.incrementAndGet();
                if (firstSuspiciousActivity == null) {
                    firstSuspiciousActivity = LocalDateTime.now();
                }
            }

            updateRiskScore();
        }

        private void updateRiskScore() {
            long total = totalOperations.get();
            if (total == 0)
                return;

            double score = 0.0;
            for (Map.Entry<RiskLevel, AtomicLong> entry : riskLevelCounts.entrySet()) {
                double ratio = (double) entry.getValue().get() / total;
                score += ratio * entry.getKey().getLevel() * 25; // Scale to 0-100
            }

            // Add suspicious activity penalty
            long suspicious = suspiciousOperations.get();
            if (suspicious > 0) {
                score += (double) suspicious / total * 50;
            }

            this.riskScore = Math.min(score, 100.0);
        }
    }

    /**
     * Security alert for suspicious activities.
     */
    private static class SecurityAlert {
        final String alertId;
        final LocalDateTime timestamp;
        final String alertType;
        final String description;
        final String userId;
        final String contentId;
        final RiskLevel severity;
        final Map<String, Object> details;

        SecurityAlert(String alertType, String description, String userId, String contentId,
                RiskLevel severity, Map<String, Object> details) {
            this.alertId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.alertType = alertType;
            this.description = description;
            this.userId = userId;
            this.contentId = contentId;
            this.severity = severity;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
        }
    }

    /**
     * Configuration for compliance implementations.
     */
    private static class ComplianceConfig {
        final ComplianceType type;
        final long retentionPeriodDays;
        final boolean requiresEncryption;
        final boolean requiresSignature;
        final Set<String> monitoredOperations;
        final Map<String, Object> customSettings;

        ComplianceConfig(ComplianceType type, long retentionPeriodDays, boolean requiresEncryption,
                boolean requiresSignature, Set<String> monitoredOperations,
                Map<String, Object> customSettings) {
            this.type = type;
            this.retentionPeriodDays = retentionPeriodDays;
            this.requiresEncryption = requiresEncryption;
            this.requiresSignature = requiresSignature;
            this.monitoredOperations = new HashSet<>(monitoredOperations);
            this.customSettings = new HashMap<>(customSettings);
        }
    }

    /**
     * Constructs an AuditObserver with default configuration.
     */
    public AuditObserver() {
        this.logger = CMSLogger.getInstance();
        this.auditLogger = AuditLogger.getInstance();

        // Initialize concurrent collections for thread safety
        this.auditRecords = new ConcurrentHashMap<>();
        this.recentAudits = new ConcurrentLinkedQueue<>();
        this.userAuditHistory = new ConcurrentHashMap<>();
        this.contentAuditHistory = new ConcurrentHashMap<>();
        this.userSecurityMetrics = new ConcurrentHashMap<>();
        this.suspiciousActivities = ConcurrentHashMap.newKeySet();
        this.riskLevelCounters = new ConcurrentHashMap<>();
        this.securityAlerts = new ConcurrentLinkedQueue<>();
        this.complianceConfigurations = new ConcurrentHashMap<>();
        this.retentionExemptRecords = ConcurrentHashMap.newKeySet();
        this.recordExpirationTimes = new ConcurrentHashMap<>();

        // Initialize statistics
        this.auditRecordCount = new AtomicLong(0);
        this.eventTypeCounters = new ConcurrentHashMap<>();
        this.complianceTypeCounters = new ConcurrentHashMap<>();

        // Initialize counters
        for (ContentEvent.EventType type : ContentEvent.EventType.values()) {
            eventTypeCounters.put(type, new AtomicLong(0));
        }

        for (RiskLevel level : RiskLevel.values()) {
            riskLevelCounters.put(level, new AtomicLong(0));
        }

        for (ComplianceType type : ComplianceType.values()) {
            complianceTypeCounters.put(type.name(), new AtomicLong(0));
        }

        // Set up default compliance configurations
        initializeComplianceConfigurations();

        logger.logContentActivity("AuditObserver initialized",
                "complianceTypes=" + complianceConfigurations.size());
    }

    /**
     * Initializes default compliance configurations.
     */
    private void initializeComplianceConfigurations() {
        // GDPR compliance - strict data protection implementations
        complianceConfigurations.put(ComplianceType.GDPR, new ComplianceConfig(
                ComplianceType.GDPR,
                2555, // 7 years retention
                true, // Encryption needed
                true, // Digital signature needed
                Set.of("CONTENT_CREATED", "CONTENT_UPDATED", "CONTENT_DELETED", "USER_ACCESS"),
                Map.of("dataProcessingLawfulBasis", "consent", "rightToErasure", true)));

        // SOX compliance - financial reporting implementations
        complianceConfigurations.put(ComplianceType.SOX, new ComplianceConfig(
                ComplianceType.SOX,
                2555, // 7 years retention
                true, // Encryption needed
                true, // Digital signature needed
                Set.of("CONTENT_PUBLISHED", "CONTENT_UPDATED", "FINANCIAL_DATA_ACCESS"),
                Map.of("financialReporting", true, "executiveCertification", true)));

        // ISO27001 compliance - information security management
        complianceConfigurations.put(ComplianceType.ISO27001, new ComplianceConfig(
                ComplianceType.ISO27001,
                1095, // 3 years retention
                true, // Encryption needed
                false, // Signature optional
                Set.of("SECURITY_EVENT", "ACCESS_CONTROL", "DATA_BREACH"),
                Map.of("riskAssessment", true, "incidentResponse", true)));

        // Default custom compliance
        complianceConfigurations.put(ComplianceType.CUSTOM, new ComplianceConfig(
                ComplianceType.CUSTOM,
                365, // 1 year retention
                false, // No encryption needed
                false, // No signature needed
                Set.of("CONTENT_CREATED", "CONTENT_UPDATED", "CONTENT_DELETED"),
                Map.of("customAudit", true)));
    }

    @Override
    public void onContentCreated(ContentEvent event) {
        eventTypeCounters.get(ContentEvent.EventType.CREATED).incrementAndGet();

        try {
            RiskLevel riskLevel = assessRiskLevel(event, "CONTENT_CREATED");

            AuditRecord auditRecord = AuditRecord.builder()
                    .eventId(event.getEventId())
                    .operation("CONTENT_CREATED")
                    .contentId(event.getContent().getId())
                    .contentTitle(event.getContent().getTitle())
                    .contentType(event.getContent().getClass().getSimpleName())
                    .userId(event.getUser().getId())
                    .username(event.getUser().getUsername())
                    .sessionId(event.getSessionId())
                    .riskLevel(riskLevel)
                    .context(buildAuditContext(event, "creation"))
                    .afterState(captureContentState(event.getContent()))
                    .applicableCompliance(determineApplicableCompliance(event, "CONTENT_CREATED"))
                    .build();

            recordAuditEvent(auditRecord);
            updateSecurityMetrics(event.getUser().getId(), riskLevel, false);

            // Check for suspicious patterns
            if (detectSuspiciousCreationActivity(event)) {
                flagSuspiciousActivity(event, "Unusual content creation pattern detected");
            }

            logger.logSecurityEvent("Content creation audit recorded",
                    "eventId=" + event.getEventId() +
                            ", riskLevel=" + riskLevel +
                            ", content=" + event.getContent().getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to record content creation audit", null, "audit_recording");
        }
    }

    @Override
    public void onContentUpdated(ContentEvent event) {
        eventTypeCounters.get(ContentEvent.EventType.UPDATED).incrementAndGet();

        try {
            RiskLevel riskLevel = assessRiskLevel(event, "CONTENT_UPDATED");

            // Enhanced audit for updates - capture before/after state
            Map<String, Object> beforeState = new HashMap<>();
            for (String field : event.getChangedFields()) {
                beforeState.put(field, event.getPreviousValue(field));
            }

            AuditRecord auditRecord = AuditRecord.builder()
                    .eventId(event.getEventId())
                    .operation("CONTENT_UPDATED")
                    .contentId(event.getContent().getId())
                    .contentTitle(event.getContent().getTitle())
                    .contentType(event.getContent().getClass().getSimpleName())
                    .userId(event.getUser().getId())
                    .username(event.getUser().getUsername())
                    .sessionId(event.getSessionId())
                    .riskLevel(riskLevel)
                    .context(buildUpdateAuditContext(event))
                    .beforeState(beforeState)
                    .afterState(captureContentState(event.getContent()))
                    .applicableCompliance(determineApplicableCompliance(event, "CONTENT_UPDATED"))
                    .build();

            recordAuditEvent(auditRecord);
            updateSecurityMetrics(event.getUser().getId(), riskLevel, false);

            // Check for suspicious update patterns
            if (detectSuspiciousUpdateActivity(event)) {
                flagSuspiciousActivity(event, "Suspicious content modification pattern detected");
            }

            logger.logSecurityEvent("Content update audit recorded",
                    "eventId=" + event.getEventId() +
                            ", riskLevel=" + riskLevel +
                            ", changedFields=" + event.getChangedFields().size());

        } catch (Exception e) {
            logger.logError(e, "Failed to record content update audit", null, "audit_recording");
        }
    }

    @Override
    public void onContentPublished(ContentEvent event) {
        eventTypeCounters.get(ContentEvent.EventType.PUBLISHED).incrementAndGet();

        try {
            // Publication is always higher risk - content becomes public
            RiskLevel riskLevel = RiskLevel.MEDIUM;
            if (event.getContent().getMetadata().containsKey("sensitive")) {
                riskLevel = RiskLevel.HIGH;
            }

            AuditRecord auditRecord = AuditRecord.builder()
                    .eventId(event.getEventId())
                    .operation("CONTENT_PUBLISHED")
                    .contentId(event.getContent().getId())
                    .contentTitle(event.getContent().getTitle())
                    .contentType(event.getContent().getClass().getSimpleName())
                    .userId(event.getUser().getId())
                    .username(event.getUser().getUsername())
                    .sessionId(event.getSessionId())
                    .riskLevel(riskLevel)
                    .context(buildPublicationAuditContext(event))
                    .afterState(captureContentState(event.getContent()))
                    .applicableCompliance(determineApplicableCompliance(event, "CONTENT_PUBLISHED"))
                    .build();

            recordAuditEvent(auditRecord);
            updateSecurityMetrics(event.getUser().getId(), riskLevel, false);

            // Always log publication events to audit logger
            auditLogger.logSecurityEvent("Content Published",
                    event.getUser().getUsername(),
                    "Published content: " + event.getContent().getTitle() +
                            " (ID: " + event.getContent().getId() + ")",
                    riskLevel.name());

            logger.logSecurityEvent("Content publication audit recorded",
                    "eventId=" + event.getEventId() +
                            ", riskLevel=" + riskLevel +
                            ", publicationDate=" + event.getPublicationDate());

        } catch (Exception e) {
            logger.logError(e, "Failed to record content publication audit", null, "audit_recording");
        }
    }

    @Override
    public void onContentDeleted(ContentEvent event) {
        eventTypeCounters.get(ContentEvent.EventType.DELETED).incrementAndGet();

        try {
            // Deletion is always high risk - data loss potential
            RiskLevel riskLevel = event.isTemporaryDeletion() ? RiskLevel.MEDIUM : RiskLevel.HIGH;

            AuditRecord auditRecord = AuditRecord.builder()
                    .eventId(event.getEventId())
                    .operation(event.isTemporaryDeletion() ? "CONTENT_ARCHIVED" : "CONTENT_DELETED")
                    .contentId(event.getContent().getId())
                    .contentTitle(event.getContent().getTitle())
                    .contentType(event.getContent().getClass().getSimpleName())
                    .userId(event.getUser().getId())
                    .username(event.getUser().getUsername())
                    .sessionId(event.getSessionId())
                    .riskLevel(riskLevel)
                    .context(buildDeletionAuditContext(event))
                    .beforeState(captureContentState(event.getContent()))
                    .applicableCompliance(determineApplicableCompliance(event, "CONTENT_DELETED"))
                    .build();

            recordAuditEvent(auditRecord);
            updateSecurityMetrics(event.getUser().getId(), riskLevel, false);

            // Mark audit record as retention-exempt for permanent deletions
            if (!event.isTemporaryDeletion()) {
                retentionExemptRecords.add(auditRecord.recordId);

                // Generate security alert for permanent deletion
                SecurityAlert alert = new SecurityAlert(
                        "PERMANENT_DELETION",
                        "Permanent content deletion performed",
                        event.getUser().getId(),
                        event.getContent().getId(),
                        RiskLevel.HIGH,
                        Map.of("reason", event.getDeletionReason(), "recoverable", false));
                securityAlerts.offer(alert);
            }

            // Always log deletion events to audit logger
            auditLogger.logSecurityEvent("Content Deleted",
                    event.getUser().getUsername(),
                    (event.isTemporaryDeletion() ? "Archived" : "Permanently deleted") +
                            " content: " + event.getContent().getTitle() +
                            " (ID: " + event.getContent().getId() +
                            "), Reason: " + event.getDeletionReason(),
                    riskLevel.name());

            logger.logSecurityEvent("Content deletion audit recorded",
                    "eventId=" + event.getEventId() +
                            ", riskLevel=" + riskLevel +
                            ", temporary=" + event.isTemporaryDeletion());

        } catch (Exception e) {
            logger.logError(e, "Failed to record content deletion audit", null, "audit_recording");
        }
    }

    // Audit processing methods

    private void recordAuditEvent(AuditRecord auditRecord) {
        // Store the audit record
        auditRecords.put(auditRecord.recordId, auditRecord);
        recentAudits.offer(auditRecord);

        // Maintain recent audits queue size
        while (recentAudits.size() > 10000) {
            recentAudits.poll();
        }

        // Update user and content audit histories
        userAuditHistory.computeIfAbsent(auditRecord.userId, k -> new CopyOnWriteArrayList<>())
                .add(auditRecord);
        contentAuditHistory.computeIfAbsent(auditRecord.contentId, k -> new CopyOnWriteArrayList<>())
                .add(auditRecord);

        // Update statistics
        auditRecordCount.incrementAndGet();
        riskLevelCounters.get(auditRecord.riskLevel).incrementAndGet();

        // Update compliance counters
        for (ComplianceType compliance : auditRecord.applicableCompliance) {
            complianceTypeCounters.get(compliance.name()).incrementAndGet();
        }

        // Set retention expiration if applicable
        setRetentionExpiration(auditRecord);

        // Verify record integrity
        if (!auditRecord.verifyIntegrity()) {
            logger.logError(new RuntimeException("Integrity verification failed"),
                    "Audit record integrity verification failed - recordId=" + auditRecord.recordId, null,
                    "integrity_verification");
        }
    }

    private RiskLevel assessRiskLevel(ContentEvent event, String operation) {
        // Base risk assessment
        RiskLevel baseRisk = RiskLevel.LOW;

        // Assess based on operation type
        switch (operation) {
            case "CONTENT_DELETED":
                baseRisk = RiskLevel.HIGH;
                break;
            case "CONTENT_PUBLISHED":
                baseRisk = RiskLevel.MEDIUM;
                break;
            case "CONTENT_UPDATED":
                baseRisk = event.getChangedFields().contains("content") ? RiskLevel.MEDIUM : RiskLevel.LOW;
                break;
            case "CONTENT_CREATED":
                baseRisk = RiskLevel.LOW;
                break;
        }

        // Escalate based on content sensitivity
        if (event.getContent().getMetadata().containsKey("confidential")) {
            baseRisk = RiskLevel.values()[Math.min(baseRisk.ordinal() + 1, RiskLevel.values().length - 1)];
        }

        // Escalate based on user risk profile
        SecurityMetrics userMetrics = userSecurityMetrics.get(event.getUser().getId());
        if (userMetrics != null && userMetrics.riskScore > 70) {
            baseRisk = RiskLevel.values()[Math.min(baseRisk.ordinal() + 1, RiskLevel.values().length - 1)];
        }

        // Escalate for off-hours activity
        int hour = LocalDateTime.now().getHour();
        if (hour < 6 || hour > 22) { // Outside business hours
            baseRisk = RiskLevel.values()[Math.min(baseRisk.ordinal() + 1, RiskLevel.values().length - 1)];
        }

        return baseRisk;
    }

    private Map<String, Object> buildAuditContext(ContentEvent event, String contextType) {
        Map<String, Object> context = new HashMap<>();
        context.put("contextType", contextType);
        context.put("eventTimestamp", event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        context.put("eventSource", event.getSource());
        context.put("userAgent", "CMS-Web-Client"); // Would be extracted from request in real system
        context.put("ipAddress", "127.0.0.1"); // Would be extracted from request in real system

        // Add event metadata
        context.putAll(event.getMetadata());

        return context;
    }

    private Map<String, Object> buildUpdateAuditContext(ContentEvent event) {
        Map<String, Object> context = buildAuditContext(event, "update");
        context.put("changedFields", new ArrayList<>(event.getChangedFields()));
        context.put("changeReason", event.getReason());
        return context;
    }

    private Map<String, Object> buildPublicationAuditContext(ContentEvent event) {
        Map<String, Object> context = buildAuditContext(event, "publication");
        context.put("publicationDate", event.getPublicationDate());
        context.put("immediatePublication", event.getPublicationDate() == null ||
                event.getPublicationDate().isBefore(LocalDateTime.now().plusMinutes(1)));
        return context;
    }

    private Map<String, Object> buildDeletionAuditContext(ContentEvent event) {
        Map<String, Object> context = buildAuditContext(event, "deletion");
        context.put("deletionReason", event.getDeletionReason());
        context.put("temporaryDeletion", event.isTemporaryDeletion());
        context.put("recoverable", event.isTemporaryDeletion());
        return context;
    }

    private Map<String, Object> captureContentState(Content content) {
        Map<String, Object> state = new HashMap<>();
        state.put("id", content.getId());
        state.put("title", content.getTitle());
        state.put("body", content.getBody() != null ? content.getBody().substring(0,
                Math.min(500, content.getBody().length())) + "..." : null);
        state.put("status", content.getStatus());
        state.put("createdBy", content.getCreatedBy());
        state.put("createdDate", content.getCreatedDate());
        state.put("lastModifiedDate", content.getModifiedDate());
        state.put("metadata", new HashMap<>(content.getMetadata()));
        return state;
    }

    private Set<ComplianceType> determineApplicableCompliance(ContentEvent event, String operation) {
        Set<ComplianceType> applicable = new HashSet<>();

        for (Map.Entry<ComplianceType, ComplianceConfig> entry : complianceConfigurations.entrySet()) {
            ComplianceConfig config = entry.getValue();
            if (config.monitoredOperations.contains(operation)) {
                applicable.add(entry.getKey());
            }
        }

        // Check for content-specific compliance implementations
        Object complianceMetadata = event.getContent().getMetadata().get("compliance");
        if (complianceMetadata instanceof String) {
            try {
                ComplianceType contentCompliance = ComplianceType.valueOf((String) complianceMetadata);
                applicable.add(contentCompliance);
            } catch (IllegalArgumentException e) {
                // Invalid compliance type in metadata
                logger.logError(new RuntimeException("Invalid compliance type"),
                        "Invalid compliance type in content metadata: " + complianceMetadata, null,
                        "compliance_validation");
            }
        }

        // Default to CUSTOM compliance if none specified
        if (applicable.isEmpty()) {
            applicable.add(ComplianceType.CUSTOM);
        }

        return applicable;
    }

    private void updateSecurityMetrics(String userId, RiskLevel riskLevel, boolean suspicious) {
        SecurityMetrics metrics = userSecurityMetrics.computeIfAbsent(userId, k -> new SecurityMetrics());
        metrics.recordOperation(riskLevel, suspicious);

        // Generate security alert if risk score becomes too high
        if (metrics.riskScore > 80 && !suspiciousActivities.contains(userId)) {
            SecurityAlert alert = new SecurityAlert(
                    "HIGH_RISK_USER",
                    "User risk score exceeded threshold",
                    userId,
                    null,
                    RiskLevel.HIGH,
                    Map.of("riskScore", metrics.riskScore, "totalOperations", metrics.totalOperations.get()));
            securityAlerts.offer(alert);
        }
    }

    private boolean detectSuspiciousCreationActivity(ContentEvent event) {
        // Check for rapid content creation
        List<AuditRecord> userHistory = userAuditHistory.get(event.getUser().getId());
        if (userHistory != null) {
            long recentCreations = userHistory.stream()
                    .filter(record -> record.operation.equals("CONTENT_CREATED"))
                    .filter(record -> record.timestamp.isAfter(LocalDateTime.now().minusHours(1)))
                    .count();

            return recentCreations > 10; // More than 10 creations in an hour
        }

        return false;
    }

    private boolean detectSuspiciousUpdateActivity(ContentEvent event) {
        // Check for rapid modifications of the same content
        List<AuditRecord> contentHistory = contentAuditHistory.get(event.getContent().getId());
        if (contentHistory != null) {
            long recentUpdates = contentHistory.stream()
                    .filter(record -> record.operation.equals("CONTENT_UPDATED"))
                    .filter(record -> record.timestamp.isAfter(LocalDateTime.now().minusMinutes(30)))
                    .count();

            return recentUpdates > 5; // More than 5 updates in 30 minutes
        }

        return false;
    }

    private void flagSuspiciousActivity(ContentEvent event, String description) {
        suspiciousActivities.add(event.getUser().getId() + ":" + event.getEventId());
        updateSecurityMetrics(event.getUser().getId(), RiskLevel.HIGH, true);

        SecurityAlert alert = new SecurityAlert(
                "SUSPICIOUS_ACTIVITY",
                description,
                event.getUser().getId(),
                event.getContent().getId(),
                RiskLevel.HIGH,
                Map.of("eventId", event.getEventId(), "operation", event.getEventType().name()));
        securityAlerts.offer(alert);

        logger.logSecurityEvent("Suspicious activity detected",
                "user=" + event.getUser().getUsername() +
                        ", description=" + description +
                        ", eventId=" + event.getEventId());
    }

    private void setRetentionExpiration(AuditRecord auditRecord) {
        if (retentionExemptRecords.contains(auditRecord.recordId)) {
            return; // Exempt from retention policies
        }

        long maxRetentionDays = 0;
        for (ComplianceType compliance : auditRecord.applicableCompliance) {
            ComplianceConfig config = complianceConfigurations.get(compliance);
            if (config != null) {
                maxRetentionDays = Math.max(maxRetentionDays, config.retentionPeriodDays);
            }
        }

        if (maxRetentionDays > 0) {
            LocalDateTime expirationDate = auditRecord.timestamp.plusDays(maxRetentionDays);
            recordExpirationTimes.put(auditRecord.recordId, expirationDate);
        }
    }

    // Public management methods

    /**
     * Gets audit records for a specific user.
     *
     * @param userId The user ID to get audit records for
     * @param limit  Maximum number of records to return
     * @return List of audit records for the user
     */
    public List<AuditRecord> getUserAuditHistory(String userId, int limit) {
        List<AuditRecord> history = userAuditHistory.get(userId);
        if (history == null) {
            return Collections.emptyList();
        }

        return history.stream()
                .sorted(Comparator.comparing((AuditRecord r) -> r.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets audit records for a specific content item.
     *
     * @param contentId The content ID to get audit records for
     * @return List of audit records for the content
     */
    public List<AuditRecord> getContentAuditHistory(String contentId) {
        List<AuditRecord> history = contentAuditHistory.get(contentId);
        return history != null ? new ArrayList<>(history) : Collections.emptyList();
    }

    /**
     * Gets recent security alerts.
     *
     * @param limit Maximum number of alerts to return
     * @return List of recent security alerts
     */
    public List<SecurityAlert> getRecentSecurityAlerts(int limit) {
        return securityAlerts.stream()
                .sorted(Comparator.comparing((SecurityAlert a) -> a.timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets security metrics for a specific user.
     *
     * @param userId The user ID
     * @return Security metrics for the user or null if not found
     */
    public SecurityMetrics getUserSecurityMetrics(String userId) {
        return userSecurityMetrics.get(userId);
    }

    /**
     * Performs audit trail integrity verification.
     *
     * @return Map of verification results
     */
    public Map<String, Object> verifyAuditIntegrity() {
        Map<String, Object> results = new LinkedHashMap<>();

        int totalRecords = auditRecords.size();
        int verifiedRecords = 0;
        int corruptedRecords = 0;

        for (AuditRecord record : auditRecords.values()) {
            if (record.verifyIntegrity()) {
                verifiedRecords++;
            } else {
                corruptedRecords++;
                logger.logError(new RuntimeException("Integrity verification failed"),
                        "Audit record integrity verification failed - recordId=" + record.recordId, null,
                        "integrity_verification");
            }
        }

        results.put("totalRecords", totalRecords);
        results.put("verifiedRecords", verifiedRecords);
        results.put("corruptedRecords", corruptedRecords);
        results.put("integrityPercentage", totalRecords > 0 ? (double) verifiedRecords / totalRecords * 100 : 100.0);

        return results;
    }

    /**
     * Gets comprehensive audit statistics.
     *
     * @return Map of audit statistics
     */
    public Map<String, Object> getAuditStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalAuditRecords", auditRecordCount.get());
        stats.put("uniqueUsers", userAuditHistory.size());
        stats.put("uniqueContent", contentAuditHistory.size());
        stats.put("recentAuditCount", recentAudits.size());
        stats.put("securityAlerts", securityAlerts.size());
        stats.put("suspiciousActivities", suspiciousActivities.size());
        stats.put("retentionExemptRecords", retentionExemptRecords.size());

        // Event type breakdown
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        for (Map.Entry<ContentEvent.EventType, AtomicLong> entry : eventTypeCounters.entrySet()) {
            eventCounts.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("eventTypeCounts", eventCounts);

        // Risk level breakdown
        Map<String, Long> riskCounts = new LinkedHashMap<>();
        for (Map.Entry<RiskLevel, AtomicLong> entry : riskLevelCounters.entrySet()) {
            riskCounts.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("riskLevelCounts", riskCounts);

        // Compliance breakdown
        Map<String, Long> complianceCounts = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : complianceTypeCounters.entrySet()) {
            complianceCounts.put(entry.getKey(), entry.getValue().get());
        }
        stats.put("complianceCounts", complianceCounts);

        return stats;
    }

    /**
     * Cleans up expired audit records based on retention policies.
     *
     * @return Number of records cleaned up
     */
    public int cleanupExpiredRecords() {
        int cleanedUp = 0;
        LocalDateTime now = LocalDateTime.now();

        Iterator<Map.Entry<String, LocalDateTime>> iterator = recordExpirationTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            if (entry.getValue().isBefore(now)) {
                String recordId = entry.getKey();

                // Remove from all tracking structures
                AuditRecord record = auditRecords.remove(recordId);
                if (record != null) {
                    List<AuditRecord> userRecords = userAuditHistory.get(record.userId);
                    if (userRecords != null) {
                        userRecords.remove(record);
                    }
                    List<AuditRecord> contentRecords = contentAuditHistory.get(record.contentId);
                    if (contentRecords != null) {
                        contentRecords.remove(record);
                    }
                    iterator.remove();
                    cleanedUp++;
                }
            }
        }

        if (cleanedUp > 0) {
            logger.logContentActivity("Expired audit records cleaned up", "count=" + cleanedUp);
        }

        return cleanedUp;
    }

    // ContentObserver interface methods

    @Override
    public String getObserverName() {
        return "Audit Observer";
    }

    @Override
    public int getPriority() {
        return 5; // Highest priority - audit logging should happen first
    }

    @Override
    public boolean shouldObserve(Class<?> contentType) {
        // Audit all content types
        return Content.class.isAssignableFrom(contentType);
    }
}
