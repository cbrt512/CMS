package com.cms.patterns.shield;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.factory.ContentCreationException;
import com.cms.core.repository.Repository;
import com.cms.core.repository.RepositoryException;
import com.cms.io.*;
import com.cms.util.CMSLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Handler;

/**
 * Comprehensive JUnit test suite for Exception Shielding pattern implementation.
 *
 * <p>This test class validates the complete Exception Shielding pattern implementation,
 * ensuring that technical exceptions are properly shielded from end users while
 * maintaining detailed logging for developers and administrators.</p>
 *
 * <p><strong>Testing Focus:</strong> Exception Shielding validation
 * - Tests user-friendly error messages across all patterns
 * - Validates technical detail shielding from end users
 * - Tests proper logging of technical exceptions
 * - Verifies exception hierarchy and proper exception handling
 * - Tests error recovery and graceful degradation</p>
 *
 * <p><strong>Exception Shielding Pattern Implementation:</strong>
 * - Catches technical exceptions (NullPointerException, IOException, etc.)
 * - Logs detailed technical information for debugging
 * - Returns user-friendly error messages to end users
 * - Maintains application stability and security
 * - Provides consistent error handling across all components</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Exception Shielding Pattern Tests")
public class ExceptionShieldingTest {

    private ContentFactory contentFactory;
    private User testUser;
    private Site testSite;
    private CMSLogger cmsLogger;
    private TestLogHandler testLogHandler;

    @BeforeEach
    void setUp() {
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
        testSite = new Site("Test Site", "test-site", testUser);
        cmsLogger = CMSLogger.getInstance();

        // Set up test log handler to capture log messages
        testLogHandler = new TestLogHandler();
        java.util.logging.Logger.getLogger("com.cms").addHandler(testLogHandler);
        java.util.logging.Logger.getLogger("com.cms").setLevel(Level.ALL);
    }

    /**
     * Tests for Content Management Exception Shielding.
     */
    @Nested
    @DisplayName("Content Management Exception Shielding")
    class ContentManagementExceptionShieldingTests {

        @Test
        @DisplayName("Should shield technical exceptions in content validation")
        void shouldShieldTechnicalExceptionsInContentValidation() {
            // Arrange - Create content with null title to trigger validation exception
            String nullTitle = null;

            // Act & Assert
            ContentValidationException exception = assertThrows(ContentValidationException.class, () -> {
                // This should trigger a NullPointerException internally but shield it
                Content content = new ArticleContent(nullTitle, "Valid content", testUser);
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have user-friendly message");
            assertFalse(exception.getMessage().contains("NullPointerException"),
                "Should not expose technical exception type");
            assertFalse(exception.getMessage().contains("stack trace"),
                "Should not expose stack trace");
            assertTrue(exception.getMessage().contains("content"),
                "Should provide context about the error");
            assertTrue(exception.getMessage().toLowerCase().contains("validation"),
                "Should indicate validation error");

            // Verify technical details are logged
            assertTrue(testLogHandler.hasLoggedError(), "Should log technical error");
            assertTrue(testLogHandler.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("validation")),
                "Should log validation details");
        }

        @Test
        @DisplayName("Should shield exceptions in content rendering")
        void shouldShieldExceptionsInContentRendering() {
            // Arrange - Create content that will fail to render
            ArticleContent article = new ArticleContent("Test Article", "Content", testUser);

            // Act & Assert
            ContentRenderingException exception = assertThrows(ContentRenderingException.class, () -> {
                // Simulate rendering failure by passing invalid template data
                article.render(null); // This should trigger internal exceptions
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have user-friendly message");
            assertFalse(exception.getMessage().contains("NullPointerException"),
                "Should not expose technical details");
            assertTrue(exception.getMessage().contains("render"),
                "Should provide context about rendering");
            assertTrue(exception.getMessage().contains("temporarily"),
                "Should suggest temporary nature of problem");

            // Verify logging
            assertTrue(testLogHandler.hasLoggedError(), "Should log rendering error");
        }

        @Test
        @DisplayName("Should provide consistent error format across content types")
        void shouldProvideConsistentErrorFormatAcrossContentTypes() {
            // Arrange
            String[] contentTypes = {"ARTICLE", "PAGE", "IMAGE", "VIDEO"};
            String invalidTitle = ""; // Empty title should trigger validation

            for (String contentType : contentTypes) {
                // Act & Assert
                ContentCreationException exception = assertThrows(ContentCreationException.class, () -> {
                    contentFactory.createContent(contentType, invalidTitle, "Content", testUser);
                });

                // Verify consistent error message format
                assertNotNull(exception.getMessage(), "Should have error message");
                assertTrue(exception.getMessage().length() > 10, "Should have meaningful message");
                assertFalse(exception.getMessage().contains("Exception"),
                    "Should not contain technical exception names");
                assertTrue(exception.getMessage().matches(".*[a-zA-Z].*"),
                    "Should contain readable text");
            }
        }
    }

    /**
     * Tests for User Management Exception Shielding.
     */
    @Nested
    @DisplayName("User Management Exception Shielding")
    class UserManagementExceptionShieldingTests {

        @Test
        @DisplayName("Should shield authentication exceptions")
        void shouldShieldAuthenticationExceptions() {
            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
                // Simulate authentication failure
                User.authenticate(null, null); // Should trigger internal exceptions
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have user-friendly message");
            assertFalse(exception.getMessage().toLowerCase().contains("null"),
                "Should not expose null parameter details");
            assertTrue(exception.getMessage().contains("credentials") ||
                      exception.getMessage().contains("authentication"),
                "Should provide authentication context");
            assertFalse(exception.getMessage().contains("NullPointerException"),
                "Should not expose technical exception types");

            // Verify security logging
            assertTrue(testLogHandler.hasLoggedWarning() || testLogHandler.hasLoggedError(),
                "Should log authentication failures for security");
        }

        @Test
        @DisplayName("Should shield user creation exceptions")
        void shouldShieldUserCreationExceptions() {
            // Act & Assert
            UserCreationException exception = assertThrows(UserCreationException.class, () -> {
                // Try to create user with invalid data
                new User(null, "", "invalid-email", null);
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have user-friendly message");
            assertTrue(exception.getMessage().contains("user") ||
                      exception.getMessage().contains("account"),
                "Should provide user creation context");
            assertFalse(exception.getMessage().contains("IllegalArgumentException"),
                "Should not expose technical exception types");

            // Verify technical details are logged
            assertTrue(testLogHandler.hasLoggedError(), "Should log creation error");
        }

        @Test
        @DisplayName("Should handle session management exceptions gracefully")
        void shouldHandleSessionManagementExceptionsGracefully() {
            // Arrange
            UserSession session = testUser.createSession();

            // Act & Assert
            UserManagementException exception = assertThrows(UserManagementException.class, () -> {
                // Simulate session operation that fails
                session.validateSession(null); // Should trigger validation exception
            });

            // Verify shielded message
            assertNotNull(exception.getMessage(), "Should have error message");
            assertTrue(exception.getMessage().contains("session"),
                "Should provide session context");
            assertFalse(exception.getMessage().contains("Exception"),
                "Should not expose exception class names");
        }
    }

    /**
     * Tests for Repository Exception Shielding.
     */
    @Nested
    @DisplayName("Repository Exception Shielding")
    class RepositoryExceptionShieldingTests {

        @Test
        @DisplayName("Should shield data access exceptions")
        void shouldShieldDataAccessExceptions() {
            // Arrange
            Repository<Content, String> repository = new ContentRepository();

            // Act & Assert
            RepositoryException exception = assertThrows(RepositoryException.class, () -> {
                // Try to save null content
                repository.save(null);
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have user-friendly message");
            assertFalse(exception.getMessage().contains("NullPointerException"),
                "Should not expose technical exception");
            assertTrue(exception.getMessage().contains("save") ||
                      exception.getMessage().contains("store"),
                "Should provide operation context");

            // Verify error logging
            assertTrue(testLogHandler.hasLoggedError(), "Should log data access error");
        }

        @Test
        @DisplayName("Should handle concurrent access exceptions")
        void shouldHandleConcurrentAccessExceptions() throws InterruptedException {
            // Arrange
            Repository<Content, String> repository = new ContentRepository();
            Content testContent = contentFactory.createContent("ARTICLE", "Test", "Content", testUser);

            // Act - Simulate concurrent access
            Thread thread1 = new Thread(() -> {
                try {
                    repository.save(testContent);
                    repository.delete(testContent.getId());
                } catch (Exception e) {
                    // Expected - one thread should fail
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    repository.save(testContent);
                    repository.update(testContent);
                } catch (Exception e) {
                    // Expected - concurrent modification
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Assert - Should have logged concurrent access issues
            assertTrue(testLogHandler.hasLoggedWarning() || testLogHandler.hasLoggedError(),
                "Should log concurrent access issues");
        }
    }

    /**
     * Tests for I/O Exception Shielding.
     */
    @Nested
    @DisplayName("I/O Exception Shielding")
    class IOExceptionShieldingTests {

        @Test
        @DisplayName("Should shield file upload exceptions")
        void shouldShieldFileUploadExceptions() {
            // Arrange
            FileUploadService uploadService = new FileUploadService();

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                // Try to upload to invalid path
                uploadService.uploadFile(null, "/invalid/path/that/does/not/exist");
            });

            // Verify user-friendly message
            assertNotNull(exception.getMessage(), "Should have error message");
            assertFalse(exception.getMessage().contains("IOException"),
                "Should not expose technical IOException");
            assertFalse(exception.getMessage().contains("FileNotFoundException"),
                "Should not expose file system details");
            assertTrue(exception.getMessage().contains("upload") ||
                      exception.getMessage().contains("file"),
                "Should provide upload context");
        }

        @Test
        @DisplayName("Should handle template processing exceptions")
        void shouldHandleTemplateProcessingExceptions() {
            // Arrange
            TemplateProcessor templateProcessor = new TemplateProcessor();

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                // Try to process non-existent template
                templateProcessor.processTemplate("/non/existent/template.html", null);
            });

            // Verify shielded exception
            assertNotNull(exception.getMessage(), "Should have error message");
            assertFalse(exception.getMessage().contains("FileNotFoundException"),
                "Should not expose file system exceptions");
            assertTrue(exception.getMessage().contains("template") ||
                      exception.getMessage().contains("process"),
                "Should provide template context");
        }

        @Test
        @DisplayName("Should handle configuration loading exceptions")
        void shouldHandleConfigurationLoadingExceptions() {
            // Arrange
            ConfigurationManager configManager = new ConfigurationManager();

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                // Try to load invalid configuration
                configManager.loadConfiguration("/invalid/config.properties");
            });

            // Verify exception shielding
            assertNotNull(exception.getMessage(), "Should have error message");
            assertFalse(exception.getMessage().contains("IOException"),
                "Should not expose I/O exception details");
            assertTrue(exception.getMessage().contains("configuration") ||
                      exception.getMessage().contains("settings"),
                "Should provide configuration context");
        }
    }

    /**
     * Tests for Exception Hierarchy and Consistency.
     */
    @Nested
    @DisplayName("Exception Hierarchy and Consistency")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("Should maintain proper exception hierarchy")
        void shouldMaintainProperExceptionHierarchy() {
            // Act & Assert - Verify inheritance hierarchy
            assertTrue(ContentValidationException.class.getSuperclass() == Exception.class,
                "ContentValidationException should extend Exception");
            assertTrue(ContentRenderingException.class.getSuperclass() == Exception.class,
                "ContentRenderingException should extend Exception");
            assertTrue(UserCreationException.class.getSuperclass() == Exception.class,
                "UserCreationException should extend Exception");
            assertTrue(AuthenticationException.class.getSuperclass() == Exception.class,
                "AuthenticationException should extend Exception");
            assertTrue(RepositoryException.class.getSuperclass() == Exception.class,
                "RepositoryException should extend Exception");
            assertTrue(ContentCreationException.class.getSuperclass() == Exception.class,
                "ContentCreationException should extend Exception");
        }

        @Test
        @DisplayName("Should provide consistent exception message format")
        void shouldProvideConsistentExceptionMessageFormat() {
            // Arrange - Create various exceptions
            Exception[] exceptions = {
                new ContentValidationException("Test validation error"),
                new ContentRenderingException("Test rendering error"),
                new UserCreationException("Test user creation error"),
                new AuthenticationException("Test authentication error"),
                new RepositoryException("Test repository error"),
                new ContentCreationException("Test content creation error")
            };

            // Assert - All should have consistent format
            for (Exception exception : exceptions) {
                assertNotNull(exception.getMessage(), "Should have error message");
                assertTrue(exception.getMessage().length() > 5,
                    "Should have meaningful message length");
                assertFalse(exception.getMessage().toLowerCase().contains("exception"),
                    "Should not contain word 'exception' in user message");
                assertTrue(Character.isUpperCase(exception.getMessage().charAt(0)),
                    "Should start with capital letter");
            }
        }

        @ParameterizedTest
        @ValueSource(classes = {
            ContentValidationException.class,
            ContentRenderingException.class,
            UserCreationException.class,
            AuthenticationException.class,
            RepositoryException.class,
            ContentCreationException.class,
            UserManagementException.class,
            ContentManagementException.class
        })
        @DisplayName("Should support exception chaining for technical details")
        void shouldSupportExceptionChainingForTechnicalDetails(Class<? extends Exception> exceptionClass) throws Exception {
            // Arrange
            RuntimeException cause = new RuntimeException("Technical error details");

            // Act - Create exception with cause
            Exception exception = exceptionClass.getConstructor(String.class, Throwable.class)
                .newInstance("User-friendly message", cause);

            // Assert
            assertNotNull(exception.getCause(), "Should preserve technical cause");
            assertEquals(cause, exception.getCause(), "Should maintain cause chain");
            assertNotEquals(exception.getMessage(), cause.getMessage(),
                "User message should differ from technical message");
        }
    }

    /**
     * Tests for Logging Integration with Exception Shielding.
     */
    @Nested
    @DisplayName("Exception Logging Integration")
    class ExceptionLoggingIntegrationTests {

        @Test
        @DisplayName("Should log technical details while shielding user messages")
        void shouldLogTechnicalDetailsWhileShieldingUserMessages() {
            // Act
            try {
                // Trigger a technical exception that should be shielded
                contentFactory.createContent(null, null, null, null);
            } catch (ContentCreationException e) {
                // Assert user-friendly message
                assertFalse(e.getMessage().contains("NullPointerException"),
                    "User message should be shielded");
            }

            // Assert technical details are logged
            assertTrue(testLogHandler.hasLoggedError(), "Should log technical error");
            assertTrue(testLogHandler.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("createContent") || msg.contains("null")),
                "Should log technical method details");
        }

        @Test
        @DisplayName("Should categorize exception logs by severity")
        void shouldCategorizeExceptionLogsBySeverity() {
            // Act - Trigger different types of exceptions

            // Authentication failure (WARNING level)
            try {
                User.authenticate("invalid", "credentials");
            } catch (AuthenticationException e) {
                // Expected
            }

            // Content creation error (ERROR level)
            try {
                contentFactory.createContent("INVALID", "Title", "Content", testUser);
            } catch (ContentCreationException e) {
                // Expected
            }

            // Assert appropriate log levels
            assertTrue(testLogHandler.hasLoggedWarning(),
                "Should log authentication failures as warnings");
            assertTrue(testLogHandler.hasLoggedError(),
                "Should log creation errors as errors");
        }

        @Test
        @DisplayName("Should include exception context in log messages")
        void shouldIncludeExceptionContextInLogMessages() {
            // Act
            try {
                testSite.addContent(null); // Should trigger ContentManagementException
            } catch (ContentManagementException e) {
                // Expected
            }

            // Assert
            assertTrue(testLogHandler.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("addContent") || msg.contains("Site")),
                "Should log operation context");
            assertTrue(testLogHandler.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains(testSite.getName())),
                "Should log site context");
        }
    }

    /**
     * Tests for Error Recovery and Graceful Degradation.
     */
    @Nested
    @DisplayName("Error Recovery and Graceful Degradation")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should attempt error recovery when possible")
        void shouldAttemptErrorRecoveryWhenPossible() {
            // Arrange
            ConfigurationManager configManager = new ConfigurationManager();

            // Act - Try to load invalid config, should fall back to defaults
            assertDoesNotThrow(() -> {
                configManager.loadConfigurationWithDefaults("/invalid/path/config.properties");
            }, "Should not throw exception when defaults available");

            // Assert - Should have fallback configuration
            assertNotNull(configManager.getProperty("default.timeout"),
                "Should have default configuration values");
        }

        @Test
        @DisplayName("Should maintain application stability during exceptions")
        void shouldMaintainApplicationStabilityDuringExceptions() {
            // Act - Multiple operations that should fail gracefully
            assertDoesNotThrow(() -> {
                try {
                    contentFactory.createContent("INVALID", "Title", "Content", testUser);
                } catch (ContentCreationException e) {
                    // Expected - should be caught and handled
                }

                try {
                    testSite.addContent(null);
                } catch (ContentManagementException e) {
                    // Expected - should be caught and handled
                }

                // Site should still be functional
                assertNotNull(testSite.getName(), "Site should remain functional");
                assertTrue(testSite.getContents().isEmpty(), "Site content should be consistent");
            });
        }

        @Test
        @DisplayName("Should provide helpful error resolution suggestions")
        void shouldProvideHelpfulErrorResolutionSuggestions() {
            // Act
            ContentCreationException exception = assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("ARTICLE", "", "Content", testUser); // Empty title
            });

            // Assert - Should provide helpful suggestions
            String message = exception.getMessage().toLowerCase();
            assertTrue(message.contains("title") || message.contains("name"),
                "Should identify what field is problematic");
            assertTrue(message.contains("required") || message.contains("empty") || message.contains("provide"),
                "Should suggest what user needs to do");
        }
    }

    /**
     * Custom log handler for testing log output.
     */
    private static class TestLogHandler extends Handler {
        private java.util.List<LogRecord> logRecords = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            logRecords.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}

        public boolean hasLoggedError() {
            return logRecords.stream().anyMatch(r -> r.getLevel().intValue() >= Level.SEVERE.intValue());
        }

        public boolean hasLoggedWarning() {
            return logRecords.stream().anyMatch(r -> r.getLevel().intValue() >= Level.WARNING.intValue());
        }

        public java.util.List<String> getErrorMessages() {
            return logRecords.stream()
                .filter(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }

        public java.util.List<String> getWarningMessages() {
            return logRecords.stream()
                .filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue())
                .map(LogRecord::getMessage)
                .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Mock ContentRepository for testing repository exceptions.
     */
    private static class ContentRepository implements Repository<Content, String> {
        private java.util.Map<String, Content> storage = new java.util.HashMap<>();

        @Override
        public Content save(Content entity) throws RepositoryException {
            if (entity == null) {
                throw new RepositoryException("Unable to save content. Please verify your data and try again.");
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Content findById(String id) throws RepositoryException {
            if (id == null) {
                throw new RepositoryException("Unable to find content. Please provide a valid identifier.");
            }
            return storage.get(id);
        }

        @Override
        public java.util.List<Content> findAll() throws RepositoryException {
            return new java.util.ArrayList<>(storage.values());
        }

        @Override
        public Content update(Content entity) throws RepositoryException {
            if (entity == null || entity.getId() == null) {
                throw new RepositoryException("Unable to update content. Please verify your data and try again.");
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public void delete(String id) throws RepositoryException {
            if (id == null) {
                throw new RepositoryException("Unable to delete content. Please provide a valid identifier.");
            }
            storage.remove(id);
        }

        @Override
        public boolean existsById(String id) throws RepositoryException {
            if (id == null) {
                throw new RepositoryException("Unable to check content existence. Please provide a valid identifier.");
            }
            return storage.containsKey(id);
        }

        @Override
        public long count() throws RepositoryException {
            return storage.size();
        }
    }
}
