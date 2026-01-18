package com.cms.util;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.io.FileUploadService;
import com.cms.io.TemplateProcessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Stream;

/**
 * Comprehensive JUnit test suite for Logging Framework implementation.
 *
 * <p>This test class validates the complete Logging implementation as required
 * , testing all logging operations, configuration management,
 * audit trails, and security logging throughout the CMS application.</p>
 *
 * <p><strong>Testing Focus:</strong> JUnit Testing  - Logging validation
 * - Tests comprehensive logging operations across all CMS components
 * - Validates audit trail functionality and security event logging
 * - Tests logging configuration and runtime management
 * - Verifies performance logging and error tracking
 * - Tests concurrent logging operations and thread safety
 * - Validates log format consistency and structured logging</p>
 *
 * <p><strong>Logging Framework Implementation Coverage:</strong>
 * - CMSLogger with content, user, security, and system operations logging
 * - AuditLogger with immutable security audit trails and integrity verification
 * - LogConfiguration with flexible runtime configuration and environment support
 * - Integration throughout all patterns and components
 * - Security-first design with sensitive data filtering
 * - Performance optimization and asynchronous logging</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Logging Framework Tests")
public class LoggingTest {

    @TempDir
    Path tempLogDir;

    private CMSLogger cmsLogger;
    private AuditLogger auditLogger;
    private LogConfiguration logConfiguration;
    private ContentFactory contentFactory;
    private User testUser;
    private Site testSite;
    private TestLogCapture logCapture;

    @BeforeEach
    void setUp() {
        cmsLogger = CMSLogger.getInstance();
        auditLogger = new AuditLogger();
        logConfiguration = new LogConfiguration();
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
        testSite = new Site("Test Site", "test-site", testUser);

        // Set up log capture for testing
        logCapture = new TestLogCapture();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(logCapture);
        rootLogger.setLevel(Level.ALL);

        // Configure logging to use temp directory
        System.setProperty("cms.log.dir", tempLogDir.toString());
        logConfiguration.setLogDirectory(tempLogDir.toString());
    }

    @AfterEach
    void tearDown() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.removeHandler(logCapture);
        logCapture.close();
    }

    /**
     * Tests for CMSLogger content management logging.
     */
    @Nested
    @DisplayName("CMS Logger Content Management Tests")
    class CMSLoggerContentManagementTests {

        @Test
        @DisplayName("Should log content creation operations")
        void shouldLogContentCreationOperations() {
            // Act
            Content article = contentFactory.createContent("ARTICLE", "Test Article", "Content", testUser);
            cmsLogger.logContentCreated(article, testUser);

            // Assert
            assertTrue(logCapture.hasInfoMessage(), "Should log content creation at INFO level");
            List<String> infoMessages = logCapture.getInfoMessages();

            boolean hasContentLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("content created") &&
                                msg.contains("Test Article") &&
                                msg.contains(testUser.getUsername()));
            assertTrue(hasContentLog, "Should log content creation details");

            // Verify structured logging
            LogRecord lastRecord = logCapture.getLastRecord();
            assertNotNull(lastRecord, "Should have log record");
            assertEquals(Level.INFO, lastRecord.getLevel(), "Should use INFO level for content creation");
        }

        @Test
        @DisplayName("Should log content modification operations")
        void shouldLogContentModificationOperations() {
            // Arrange
            Content article = contentFactory.createContent("ARTICLE", "Original Title", "Original Content", testUser);
            String originalTitle = article.getTitle();

            // Act - Modify content
            article.setTitle("Updated Title");
            article.setContent("Updated Content");
            cmsLogger.logContentModified(article, testUser, "title, content");

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            boolean hasModificationLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("content modified") &&
                                msg.contains("Updated Title") &&
                                msg.contains("title, content") &&
                                msg.contains(testUser.getUsername()));
            assertTrue(hasModificationLog, "Should log content modification with changed fields");
        }

        @Test
        @DisplayName("Should log content publication workflow")
        void shouldLogContentPublicationWorkflow() {
            // Arrange
            Content article = contentFactory.createContent("ARTICLE", "Publication Test", "Content", testUser);

            // Act - Publication workflow
            cmsLogger.logContentStatusChange(article, ContentStatus.DRAFT, ContentStatus.UNDER_REVIEW, testUser);
            article.setStatus(ContentStatus.UNDER_REVIEW);

            cmsLogger.logContentStatusChange(article, ContentStatus.UNDER_REVIEW, ContentStatus.PUBLISHED, testUser);
            article.setStatus(ContentStatus.PUBLISHED);

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();

            boolean hasDraftToReviewLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("status changed") &&
                                msg.contains("DRAFT") &&
                                msg.contains("UNDER_REVIEW"));
            assertTrue(hasDraftToReviewLog, "Should log draft to review transition");

            boolean hasReviewToPublishedLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("status changed") &&
                                msg.contains("UNDER_REVIEW") &&
                                msg.contains("PUBLISHED"));
            assertTrue(hasReviewToPublishedLog, "Should log review to published transition");
        }

        @ParameterizedTest
        @EnumSource(value = ContentStatus.class, names = {"DRAFT", "PUBLISHED", "ARCHIVED", "UNDER_REVIEW"})
        @DisplayName("Should log all content status transitions")
        void shouldLogAllContentStatusTransitions(ContentStatus targetStatus) {
            // Arrange
            Content content = contentFactory.createContent("ARTICLE", "Status Test", "Content", testUser);
            ContentStatus originalStatus = content.getStatus();

            // Act
            cmsLogger.logContentStatusChange(content, originalStatus, targetStatus, testUser);

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            boolean hasStatusLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("status changed") &&
                                msg.contains(originalStatus.toString()) &&
                                msg.contains(targetStatus.toString()));
            assertTrue(hasStatusLog, "Should log status change to " + targetStatus);
        }

        @Test
        @DisplayName("Should log content deletion with security audit")
        void shouldLogContentDeletionWithSecurityAudit() {
            // Arrange
            Content article = contentFactory.createContent("ARTICLE", "Delete Test", "Content", testUser);
            String contentId = article.getId();
            String contentTitle = article.getTitle();

            // Act
            cmsLogger.logContentDeleted(contentId, contentTitle, testUser, "User requested deletion");

            // Assert
            List<String> warningMessages = logCapture.getWarningMessages();
            boolean hasDeletionLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("content deleted") &&
                                msg.contains(contentTitle) &&
                                msg.contains(testUser.getUsername()) &&
                                msg.contains("User requested deletion"));
            assertTrue(hasDeletionLog, "Should log content deletion with reason");

            // Verify elevated logging level for deletion
            LogRecord lastRecord = logCapture.getLastRecord();
            assertTrue(lastRecord.getLevel().intValue() >= Level.WARNING.intValue(),
                "Content deletion should be logged at WARNING level or higher");
        }
    }

    /**
     * Tests for CMSLogger user management logging.
     */
    @Nested
    @DisplayName("CMS Logger User Management Tests")
    class CMSLoggerUserManagementTests {

        @Test
        @DisplayName("Should log user authentication events")
        void shouldLogUserAuthenticationEvents() {
            // Act - Successful authentication
            cmsLogger.logUserLogin(testUser, "192.168.1.100", true);

            // Failed authentication
            cmsLogger.logUserLoginAttempt("invalidUser", "192.168.1.101", false, "Invalid credentials");

            // User logout
            cmsLogger.logUserLogout(testUser, "192.168.1.100", "Normal logout");

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            List<String> warningMessages = logCapture.getWarningMessages();

            boolean hasLoginLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("user login") &&
                                msg.contains(testUser.getUsername()) &&
                                msg.contains("192.168.1.100"));
            assertTrue(hasLoginLog, "Should log successful user login");

            boolean hasFailedLoginLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("login attempt failed") &&
                                msg.contains("invalidUser") &&
                                msg.contains("Invalid credentials"));
            assertTrue(hasFailedLoginLog, "Should log failed login attempts");

            boolean hasLogoutLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("user logout") &&
                                msg.contains(testUser.getUsername()));
            assertTrue(hasLogoutLog, "Should log user logout");
        }

        @Test
        @DisplayName("Should log user account management operations")
        void shouldLogUserAccountManagementOperations() {
            // Arrange
            User newUser = new User("newUser", "New User", "new@example.com", Role.VIEWER);
            User adminUser = new User("admin", "Admin User", "admin@example.com", Role.ADMIN);

            // Act
            cmsLogger.logUserCreated(newUser, adminUser);
            cmsLogger.logUserRoleChanged(testUser, Role.EDITOR, Role.ADMIN, adminUser);
            cmsLogger.logUserDeactivated(testUser, adminUser, "Account violation");

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            List<String> warningMessages = logCapture.getWarningMessages();

            boolean hasUserCreationLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("user created") &&
                                msg.contains("newUser") &&
                                msg.contains(adminUser.getUsername()));
            assertTrue(hasUserCreationLog, "Should log user creation");

            boolean hasRoleChangeLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("role changed") &&
                                msg.contains("EDITOR") &&
                                msg.contains("ADMIN"));
            assertTrue(hasRoleChangeLog, "Should log role changes");

            boolean hasDeactivationLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("user deactivated") &&
                                msg.contains("Account violation"));
            assertTrue(hasDeactivationLog, "Should log user deactivation with reason");
        }

        @Test
        @DisplayName("Should log session management events")
        void shouldLogSessionManagementEvents() {
            // Arrange
            UserSession session = testUser.createSession();

            // Act
            cmsLogger.logSessionCreated(session, "192.168.1.100");
            cmsLogger.logSessionActivity(session, "content_view", "Viewed article: Test Article");
            cmsLogger.logSessionExpired(session, "Timeout after 30 minutes");

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            List<String> warningMessages = logCapture.getWarningMessages();

            boolean hasSessionCreationLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("session created") &&
                                msg.contains(session.getSessionId()) &&
                                msg.contains("192.168.1.100"));
            assertTrue(hasSessionCreationLog, "Should log session creation");

            boolean hasSessionActivityLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("session activity") &&
                                msg.contains("content_view") &&
                                msg.contains("Test Article"));
            assertTrue(hasSessionActivityLog, "Should log session activities");

            boolean hasSessionExpirationLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("session expired") &&
                                msg.contains("Timeout"));
            assertTrue(hasSessionExpirationLog, "Should log session expiration");
        }
    }

    /**
     * Tests for CMSLogger security event logging.
     */
    @Nested
    @DisplayName("CMS Logger Security Event Tests")
    class CMSLoggerSecurityEventTests {

        @Test
        @DisplayName("Should log security violations and unauthorized access")
        void shouldLogSecurityViolationsAndUnauthorizedAccess() {
            // Act
            cmsLogger.logSecurityViolation("Unauthorized access attempt", testUser, "192.168.1.100",
                "Attempted to access admin panel without ADMIN role");

            cmsLogger.logSuspiciousActivity("Multiple failed login attempts", "192.168.1.101",
                "5 failed login attempts in 2 minutes");

            cmsLogger.logAccessDenied(testUser, "admin/users", Role.ADMIN,
                "User lacks required permissions");

            // Assert
            List<String> severeMessages = logCapture.getSevereMessages();
            List<String> warningMessages = logCapture.getWarningMessages();

            boolean hasSecurityViolationLog = severeMessages.stream()
                .anyMatch(msg -> msg.contains("SECURITY VIOLATION") &&
                                msg.contains("admin panel") &&
                                msg.contains("192.168.1.100"));
            assertTrue(hasSecurityViolationLog, "Should log security violations at SEVERE level");

            boolean hasSuspiciousActivityLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("suspicious activity") &&
                                msg.contains("192.168.1.101") &&
                                msg.contains("5 failed login"));
            assertTrue(hasSuspiciousActivityLog, "Should log suspicious activities");

            boolean hasAccessDeniedLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("access denied") &&
                                msg.contains("admin/users") &&
                                msg.contains("required permissions"));
            assertTrue(hasAccessDeniedLog, "Should log access denied events");
        }

        @Test
        @DisplayName("Should log file upload security events")
        void shouldLogFileUploadSecurityEvents() {
            // Act
            cmsLogger.logFileUploadAttempt(testUser, "malicious.exe", "192.168.1.100", false,
                "Executable file type not allowed");

            cmsLogger.logFileUploadSuccess(testUser, "safe-image.jpg", "192.168.1.100",
                "/uploads/2024/safe-image.jpg", 204800L);

            cmsLogger.logFileUploadSecurityViolation("../../../etc/passwd", "192.168.1.101",
                "Path traversal attempt detected");

            // Assert
            List<String> infoMessages = logCapture.getInfoMessages();
            List<String> warningMessages = logCapture.getWarningMessages();
            List<String> severeMessages = logCapture.getSevereMessages();

            boolean hasBlockedUploadLog = warningMessages.stream()
                .anyMatch(msg -> msg.contains("file upload blocked") &&
                                msg.contains("malicious.exe") &&
                                msg.contains("Executable file"));
            assertTrue(hasBlockedUploadLog, "Should log blocked file uploads");

            boolean hasSuccessfulUploadLog = infoMessages.stream()
                .anyMatch(msg -> msg.contains("file upload success") &&
                                msg.contains("safe-image.jpg") &&
                                msg.contains("204800"));
            assertTrue(hasSuccessfulUploadLog, "Should log successful uploads");

            boolean hasSecurityViolationLog = severeMessages.stream()
                .anyMatch(msg -> msg.contains("SECURITY VIOLATION") &&
                                msg.contains("path traversal") &&
                                msg.contains("../../../etc/passwd"));
            assertTrue(hasSecurityViolationLog, "Should log security violations at SEVERE level");
        }

        @Test
        @DisplayName("Should sanitize sensitive data in security logs")
        void shouldSanitizeSensitiveDataInSecurityLogs() {
            // Act - Log events with potentially sensitive data
            cmsLogger.logPasswordChangeAttempt(testUser, true, "192.168.1.100");
            cmsLogger.logApiKeyAccess("api-key-12345", testUser, "GET /api/content", true);
            cmsLogger.logDatabaseQuery(testUser, "SELECT * FROM users WHERE email = ?",
                Arrays.asList("test@example.com"));

            // Assert - Sensitive data should be sanitized
            List<String> allMessages = logCapture.getAllMessages();

            // Password should not appear in logs
            boolean hasPasswordInLog = allMessages.stream()
                .anyMatch(msg -> msg.toLowerCase().contains("password") &&
                                !msg.contains("[REDACTED]"));
            assertFalse(hasPasswordInLog, "Password values should not appear in logs");

            // API keys should be masked
            boolean hasFullApiKey = allMessages.stream()
                .anyMatch(msg -> msg.contains("api-key-12345"));
            assertFalse(hasFullApiKey, "Full API keys should not appear in logs");

            // Email addresses in database queries should be parameterized
            boolean hasEmailParameter = allMessages.stream()
                .anyMatch(msg -> msg.contains("database query") &&
                                msg.contains("?") &&
                                !msg.contains("test@example.com"));
            assertTrue(hasEmailParameter, "Database queries should use parameterized logging");
        }
    }

    /**
     * Tests for AuditLogger functionality.
     */
    @Nested
    @DisplayName("Audit Logger Tests")
    class AuditLoggerTests {

        @Test
        @DisplayName("Should create immutable audit records")
        void shouldCreateImmutableAuditRecords() {
            // Act
            auditLogger.logAuditEvent("USER_LOGIN", testUser.getUsername(), "192.168.1.100",
                "User logged in successfully", Map.of("session_id", "sess-123", "role", "EDITOR"));

            auditLogger.logAuditEvent("CONTENT_MODIFIED", testUser.getUsername(), "192.168.1.100",
                "Article content updated", Map.of("content_id", "art-456", "changes", "title,body"));

            // Assert
            List<AuditRecord> auditRecords = auditLogger.getAuditRecords();
            assertEquals(2, auditRecords.size(), "Should have 2 audit records");

            AuditRecord loginRecord = auditRecords.get(0);
            assertEquals("USER_LOGIN", loginRecord.getEventType(), "Should record event type");
            assertEquals(testUser.getUsername(), loginRecord.getUsername(), "Should record username");
            assertEquals("192.168.1.100", loginRecord.getIpAddress(), "Should record IP address");
            assertNotNull(loginRecord.getTimestamp(), "Should have timestamp");
            assertNotNull(loginRecord.getSequenceNumber(), "Should have sequence number");

            // Test immutability - these should throw exceptions or have no effect
            assertThrows(UnsupportedOperationException.class, () -> {
                loginRecord.getEventDetails().put("new_key", "new_value");
            }, "Audit record details should be immutable");
        }

        @Test
        @DisplayName("Should maintain audit trail integrity")
        void shouldMaintainAuditTrailIntegrity() {
            // Arrange - Create multiple audit events
            String[] eventTypes = {"USER_LOGIN", "CONTENT_CREATED", "ROLE_CHANGED", "FILE_UPLOADED", "USER_LOGOUT"};

            // Act
            for (int i = 0; i < eventTypes.length; i++) {
                auditLogger.logAuditEvent(eventTypes[i], testUser.getUsername(), "192.168.1.100",
                    "Event " + i, Map.of("event_number", String.valueOf(i)));
            }

            // Assert
            List<AuditRecord> auditRecords = auditLogger.getAuditRecords();
            assertEquals(eventTypes.length, auditRecords.size(), "Should have all audit records");

            // Verify sequence numbers are sequential
            for (int i = 0; i < auditRecords.size(); i++) {
                AuditRecord record = auditRecords.get(i);
                assertTrue(record.getSequenceNumber() > 0, "Sequence number should be positive");

                if (i > 0) {
                    long prevSequence = auditRecords.get(i - 1).getSequenceNumber();
                    long currentSequence = record.getSequenceNumber();
                    assertTrue(currentSequence > prevSequence, "Sequence numbers should be increasing");
                }
            }

            // Verify timestamps are chronological
            for (int i = 1; i < auditRecords.size(); i++) {
                LocalDateTime prevTime = auditRecords.get(i - 1).getTimestamp();
                LocalDateTime currentTime = auditRecords.get(i).getTimestamp();
                assertTrue(currentTime.isEqual(prevTime) || currentTime.isAfter(prevTime),
                    "Timestamps should be chronological");
            }
        }

        @Test
        @DisplayName("Should support audit trail search and filtering")
        void shouldSupportAuditTrailSearchAndFiltering() {
            // Arrange - Create varied audit events
            auditLogger.logAuditEvent("USER_LOGIN", "user1", "192.168.1.100", "Login", Map.of("role", "EDITOR"));
            auditLogger.logAuditEvent("USER_LOGIN", "user2", "192.168.1.101", "Login", Map.of("role", "ADMIN"));
            auditLogger.logAuditEvent("CONTENT_CREATED", "user1", "192.168.1.100", "Content", Map.of("type", "ARTICLE"));
            auditLogger.logAuditEvent("FILE_UPLOADED", "user2", "192.168.1.101", "Upload", Map.of("type", "IMAGE"));
            auditLogger.logAuditEvent("USER_LOGOUT", "user1", "192.168.1.100", "Logout", Map.of());

            // Act & Assert - Filter by event type
            List<AuditRecord> loginRecords = auditLogger.findAuditRecords("USER_LOGIN");
            assertEquals(2, loginRecords.size(), "Should find 2 login records");
            assertTrue(loginRecords.stream().allMatch(r -> r.getEventType().equals("USER_LOGIN")),
                "All records should be login events");

            // Filter by username
            List<AuditRecord> user1Records = auditLogger.findAuditRecordsByUser("user1");
            assertEquals(3, user1Records.size(), "Should find 3 records for user1");
            assertTrue(user1Records.stream().allMatch(r -> r.getUsername().equals("user1")),
                "All records should be for user1");

            // Filter by IP address
            List<AuditRecord> ipRecords = auditLogger.findAuditRecordsByIp("192.168.1.101");
            assertEquals(2, ipRecords.size(), "Should find 2 records for IP");
            assertTrue(ipRecords.stream().allMatch(r -> r.getIpAddress().equals("192.168.1.101")),
                "All records should be from the IP");
        }

        @Test
        @DisplayName("Should handle concurrent audit logging safely")
        void shouldHandleConcurrentAuditLoggingSafely() throws InterruptedException {
            // Arrange
            int threadCount = 10;
            int recordsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // Act - Concurrent audit logging
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < recordsPerThread; j++) {
                            auditLogger.logAuditEvent("CONCURRENT_TEST", "thread-" + threadId,
                                "192.168.1." + threadId, "Test event " + j,
                                Map.of("thread_id", String.valueOf(threadId), "record_id", String.valueOf(j)));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            // Assert
            List<AuditRecord> allRecords = auditLogger.getAuditRecords();
            assertEquals(threadCount * recordsPerThread, allRecords.size(),
                "Should have all audit records from all threads");

            // Verify sequence number integrity
            Set<Long> sequenceNumbers = new HashSet<>();
            for (AuditRecord record : allRecords) {
                assertTrue(sequenceNumbers.add(record.getSequenceNumber()),
                    "All sequence numbers should be unique");
            }

            // Verify all threads contributed records
            Set<String> threadIds = new HashSet<>();
            for (AuditRecord record : allRecords) {
                String threadId = record.getEventDetails().get("thread_id");
                threadIds.add(threadId);
            }
            assertEquals(threadCount, threadIds.size(), "All threads should have contributed records");
        }
    }

    /**
     * Tests for LogConfiguration management.
     */
    @Nested
    @DisplayName("Log Configuration Tests")
    class LogConfigurationTests {

        @Test
        @DisplayName("Should provide fluent configuration API")
        void shouldProvideFluentConfigurationAPI() {
            // Act - Fluent configuration
            LogConfiguration config = LogConfiguration.builder()
                .setLogLevel(Level.INFO)
                .setLogDirectory(tempLogDir.toString())
                .setMaxFileSize(10 * 1024 * 1024) // 10MB
                .setMaxFiles(5)
                .setRotateDaily(true)
                .setAsyncLogging(true)
                .setStructuredLogging(true)
                .build();

            // Assert
            assertEquals(Level.INFO, config.getLogLevel(), "Should set log level");
            assertEquals(tempLogDir.toString(), config.getLogDirectory(), "Should set log directory");
            assertEquals(10 * 1024 * 1024, config.getMaxFileSize(), "Should set max file size");
            assertEquals(5, config.getMaxFiles(), "Should set max files");
            assertTrue(config.isRotateDaily(), "Should enable daily rotation");
            assertTrue(config.isAsyncLogging(), "Should enable async logging");
            assertTrue(config.isStructuredLogging(), "Should enable structured logging");
        }

        @ParameterizedTest
        @ValueSource(strings = {"DEVELOPMENT", "TESTING", "PRODUCTION"})
        @DisplayName("Should provide environment-specific defaults")
        void shouldProvideEnvironmentSpecificDefaults(String environment) {
            // Act
            LogConfiguration config = LogConfiguration.forEnvironment(environment);

            // Assert
            assertNotNull(config, "Should create configuration for environment: " + environment);
            assertNotNull(config.getLogLevel(), "Should have default log level");
            assertNotNull(config.getLogDirectory(), "Should have default log directory");

            // Verify environment-specific settings
            switch (environment) {
                case "DEVELOPMENT":
                    assertEquals(Level.ALL, config.getLogLevel(), "Development should log everything");
                    assertTrue(config.isConsoleOutput(), "Development should output to console");
                    break;
                case "TESTING":
                    assertEquals(Level.INFO, config.getLogLevel(), "Testing should log INFO and above");
                    assertFalse(config.isAsyncLogging(), "Testing should use synchronous logging");
                    break;
                case "PRODUCTION":
                    assertTrue(config.getLogLevel().intValue() >= Level.INFO.intValue(),
                        "Production should limit log verbosity");
                    assertTrue(config.isAsyncLogging(), "Production should use async logging");
                    assertTrue(config.isRotateDaily(), "Production should rotate logs daily");
                    break;
            }
        }

        @Test
        @DisplayName("Should support runtime configuration updates")
        void shouldSupportRuntimeConfigurationUpdates() {
            // Arrange
            LogConfiguration config = new LogConfiguration();
            config.setLogLevel(Level.INFO);

            // Apply configuration to logger
            Logger testLogger = Logger.getLogger("test.runtime.config");
            testLogger.setLevel(config.getLogLevel());

            // Verify initial configuration
            assertEquals(Level.INFO, testLogger.getLevel(), "Should set initial log level");

            // Act - Runtime update
            config.updateLogLevel(Level.WARNING);
            testLogger.setLevel(config.getLogLevel());

            // Assert
            assertEquals(Level.WARNING, testLogger.getLevel(), "Should update log level at runtime");
            assertEquals(Level.WARNING, config.getLogLevel(), "Config should reflect new log level");
        }

        @Test
        @DisplayName("Should validate configuration parameters")
        void shouldValidateConfigurationParameters() {
            // Test invalid log directory
            assertThrows(IllegalArgumentException.class, () -> {
                LogConfiguration.builder()
                    .setLogDirectory("/invalid/path/that/does/not/exist")
                    .build();
            }, "Should reject invalid log directory");

            // Test invalid file size
            assertThrows(IllegalArgumentException.class, () -> {
                LogConfiguration.builder()
                    .setMaxFileSize(-1)
                    .build();
            }, "Should reject negative file size");

            // Test invalid number of files
            assertThrows(IllegalArgumentException.class, () -> {
                LogConfiguration.builder()
                    .setMaxFiles(0)
                    .build();
            }, "Should reject zero max files");

            // Test null log level
            assertThrows(IllegalArgumentException.class, () -> {
                LogConfiguration.builder()
                    .setLogLevel(null)
                    .build();
            }, "Should reject null log level");
        }

        @Test
        @DisplayName("Should support custom properties and extensions")
        void shouldSupportCustomPropertiesAndExtensions() {
            // Act
            LogConfiguration config = LogConfiguration.builder()
                .setCustomProperty("app.name", "JavaCMS")
                .setCustomProperty("app.version", "1.0.0")
                .setCustomProperty("log.format", "json")
                .addLogHandler("database", "com.cms.logging.DatabaseHandler")
                .addLogHandler("elasticsearch", "com.cms.logging.ElasticsearchHandler")
                .build();

            // Assert
            assertEquals("JavaCMS", config.getCustomProperty("app.name"), "Should store custom property");
            assertEquals("1.0.0", config.getCustomProperty("app.version"), "Should store version");
            assertEquals("json", config.getCustomProperty("log.format"), "Should store format");

            Map<String, String> handlers = config.getCustomHandlers();
            assertEquals("com.cms.logging.DatabaseHandler", handlers.get("database"),
                "Should register database handler");
            assertEquals("com.cms.logging.ElasticsearchHandler", handlers.get("elasticsearch"),
                "Should register elasticsearch handler");
        }
    }

    /**
     * Tests for logging integration across the application.
     */
    @Nested
    @DisplayName("Logging Integration Tests")
    class LoggingIntegrationTests {

        @Test
        @DisplayName("Should integrate with all CMS components")
        void shouldIntegrateWithAllCMSComponents() {
            // Act - Exercise various CMS operations that should generate logs

            // Content operations
            Content article = contentFactory.createContent("ARTICLE", "Integration Article", "Content", testUser);
            cmsLogger.logContentCreated(article, testUser);

            // User operations
            cmsLogger.logUserLogin(testUser, "192.168.1.100", true);

            // File operations (simulated)
            FileUploadService uploadService = new FileUploadService();
            cmsLogger.logFileUploadAttempt(testUser, "test.jpg", "192.168.1.100", true, "Success");

            // Template operations (simulated)
            TemplateProcessor templateProcessor = new TemplateProcessor();
            cmsLogger.logTemplateProcessed("article-template.html", testUser, 250L, true);

            // Security operations
            cmsLogger.logAccessDenied(testUser, "/admin/settings", Role.ADMIN, "Insufficient privileges");

            // Assert
            List<String> allMessages = logCapture.getAllMessages();
            assertTrue(allMessages.size() >= 5, "Should have logs from all integrated components");

            boolean hasContentLog = allMessages.stream()
                .anyMatch(msg -> msg.contains("content created"));
            assertTrue(hasContentLog, "Should have content operation logs");

            boolean hasUserLog = allMessages.stream()
                .anyMatch(msg -> msg.contains("user login"));
            assertTrue(hasUserLog, "Should have user operation logs");

            boolean hasFileLog = allMessages.stream()
                .anyMatch(msg -> msg.contains("file upload"));
            assertTrue(hasFileLog, "Should have file operation logs");

            boolean hasTemplateLog = allMessages.stream()
                .anyMatch(msg -> msg.contains("template processed"));
            assertTrue(hasTemplateLog, "Should have template operation logs");

            boolean hasSecurityLog = allMessages.stream()
                .anyMatch(msg -> msg.contains("access denied"));
            assertTrue(hasSecurityLog, "Should have security operation logs");
        }

        @Test
        @DisplayName("Should maintain consistent log format across components")
        void shouldMaintainConsistentLogFormatAcrossComponents() {
            // Act - Generate logs from different components
            cmsLogger.logContentCreated(
                contentFactory.createContent("ARTICLE", "Format Test", "Content", testUser), testUser);
            cmsLogger.logUserLogin(testUser, "192.168.1.100", true);
            cmsLogger.logSecurityViolation("Test violation", testUser, "192.168.1.100", "Test reason");

            // Assert - Check log format consistency
            List<LogRecord> logRecords = logCapture.getLogRecords();
            assertTrue(logRecords.size() >= 3, "Should have multiple log records");

            for (LogRecord record : logRecords) {
                assertNotNull(record.getLoggerName(), "Should have logger name");
                assertNotNull(record.getMessage(), "Should have message");
                assertNotNull(record.getLevel(), "Should have log level");

                // Check for consistent timestamp format (implicit in LogRecord)
                assertTrue(record.getMillis() > 0, "Should have valid timestamp");

                // Check message structure (should contain relevant information)
                String message = record.getMessage();
                assertFalse(message.isEmpty(), "Message should not be empty");
                assertTrue(message.length() > 10, "Message should have substance");
            }
        }

        @Test
        @DisplayName("Should handle logging errors gracefully")
        void shouldHandleLoggingErrorsGracefully() {
            // Arrange - Configure logger with problematic settings
            LogConfiguration problemConfig = LogConfiguration.builder()
                .setLogDirectory("/read-only/directory") // Should cause permission issues
                .setMaxFileSize(1) // Extremely small file size
                .build();

            // Act & Assert - Logging should not crash the application
            assertDoesNotThrow(() -> {
                cmsLogger.logContentCreated(
                    contentFactory.createContent("ARTICLE", "Error Test", "Content", testUser), testUser);
            }, "Logging errors should not crash the application");

            // Verify fallback logging mechanism
            assertTrue(logCapture.getLogRecords().size() > 0,
                "Should have some log records despite configuration issues");
        }

        @Test
        @DisplayName("Should support log aggregation and analysis")
        void shouldSupportLogAggregationAndAnalysis() throws InterruptedException {
            // Arrange - Generate a variety of log events
            for (int i = 0; i < 100; i++) {
                if (i % 4 == 0) {
                    cmsLogger.logUserLogin(testUser, "192.168.1.100", true);
                } else if (i % 4 == 1) {
                    cmsLogger.logContentCreated(
                        contentFactory.createContent("ARTICLE", "Article " + i, "Content", testUser), testUser);
                } else if (i % 4 == 2) {
                    cmsLogger.logFileUploadAttempt(testUser, "file" + i + ".jpg", "192.168.1.100", true, "Success");
                } else {
                    cmsLogger.logSecurityViolation("Violation " + i, testUser, "192.168.1.100", "Test");
                }

                // Small delay to ensure different timestamps
                Thread.sleep(1);
            }

            // Act - Analyze logs
            List<LogRecord> allRecords = logCapture.getLogRecords();

            // Assert - Log analysis
            assertTrue(allRecords.size() >= 100, "Should have generated expected number of logs");

            // Count by log level
            Map<Level, Long> logsByLevel = allRecords.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    LogRecord::getLevel, java.util.stream.Collectors.counting()));

            assertTrue(logsByLevel.containsKey(Level.INFO), "Should have INFO level logs");
            assertTrue(logsByLevel.containsKey(Level.SEVERE), "Should have SEVERE level logs (security violations)");

            // Time-based analysis
            long startTime = allRecords.get(0).getMillis();
            long endTime = allRecords.get(allRecords.size() - 1).getMillis();
            long duration = endTime - startTime;

            assertTrue(duration > 0, "Logs should span some time duration");
            assertTrue(duration < 10000, "All logs should be generated within reasonable time");

            // Message pattern analysis
            long userLoginCount = allRecords.stream()
                .mapToLong(record -> record.getMessage().contains("user login") ? 1 : 0)
                .sum();
            long contentCreationCount = allRecords.stream()
                .mapToLong(record -> record.getMessage().contains("content created") ? 1 : 0)
                .sum();

            assertEquals(25, userLoginCount, "Should have expected number of user login logs");
            assertEquals(25, contentCreationCount, "Should have expected number of content creation logs");
        }
    }

    /**
     * Helper class for capturing log output during tests.
     */
    private static class TestLogCapture extends Handler {
        private final List<LogRecord> logRecords = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean closed = false;

        @Override
        public void publish(LogRecord record) {
            if (!closed && record != null) {
                logRecords.add(record);
            }
        }

        @Override
        public void flush() {
            // No-op for test handler
        }

        @Override
        public void close() throws SecurityException {
            closed = true;
        }

        public List<LogRecord> getLogRecords() {
            return new ArrayList<>(logRecords);
        }

        public LogRecord getLastRecord() {
            return logRecords.isEmpty() ? null : logRecords.get(logRecords.size() - 1);
        }

        public boolean hasInfoMessage() {
            return logRecords.stream().anyMatch(r -> r.getLevel() == Level.INFO);
        }

        public boolean hasWarningMessage() {
            return logRecords.stream().anyMatch(r -> r.getLevel() == Level.WARNING);
        }

        public boolean hasSevereMessage() {
            return logRecords.stream().anyMatch(r -> r.getLevel() == Level.SEVERE);
        }

        public List<String> getInfoMessages() {
            return logRecords.stream()
                .filter(r -> r.getLevel() == Level.INFO)
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }

        public List<String> getWarningMessages() {
            return logRecords.stream()
                .filter(r -> r.getLevel() == Level.WARNING)
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }

        public List<String> getSevereMessages() {
            return logRecords.stream()
                .filter(r -> r.getLevel() == Level.SEVERE)
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }

        public List<String> getAllMessages() {
            return logRecords.stream()
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Mock audit record for testing.
     */
    private static class AuditRecord {
        private final long sequenceNumber;
        private final LocalDateTime timestamp;
        private final String eventType;
        private final String username;
        private final String ipAddress;
        private final String description;
        private final Map<String, String> eventDetails;

        public AuditRecord(long sequenceNumber, String eventType, String username, String ipAddress,
                          String description, Map<String, String> eventDetails) {
            this.sequenceNumber = sequenceNumber;
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
            this.username = username;
            this.ipAddress = ipAddress;
            this.description = description;
            this.eventDetails = Collections.unmodifiableMap(new HashMap<>(eventDetails));
        }

        public long getSequenceNumber() { return sequenceNumber; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getUsername() { return username; }
        public String getIpAddress() { return ipAddress; }
        public String getDescription() { return description; }
        public Map<String, String> getEventDetails() { return eventDetails; }
    }
}
