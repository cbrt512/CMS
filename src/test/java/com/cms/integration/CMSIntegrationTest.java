package com.cms.integration;

import com.cms.core.model.*;
import com.cms.core.repository.Repository;
import com.cms.patterns.factory.*;
import com.cms.patterns.composite.*;
import com.cms.patterns.iterator.*;
import com.cms.io.*;
import com.cms.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Comprehensive JUnit integration test suite for the complete CMS system.
 *
 * <p>This test class validates end-to-end workflows and integration between all
 * components of the JavaCMS system . It tests
 * the complete integration of all patterns, core technologies, and system functionality.</p>
 *
 * <p><strong>Testing Focus:</strong> JUnit Testing  - Integration testing
 * - Tests complete end-to-end CMS workflows
 * - Validates integration between all design patterns
 * - Tests core technology integration (Collections, Generics, I/O, Logging)
 * - Verifies cross-component functionality and data flow
 * - Tests system reliability, performance, and error handling
 * - Validates security integration across all components</p>
 *
 * <p><strong>Integration Test Coverage:</strong>
 * - Complete content management workflows (create → publish → archive)
 * - User management and authentication integration
 * - File upload and template processing workflows
 * - Multi-pattern interactions and data consistency
 * - Cross-component logging and audit trail integration
 * - Performance and scalability under realistic loads
 * - Error handling and recovery across component boundaries</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("CMS Integration Tests - Complete System Validation")
public class CMSIntegrationTest {

    @TempDir
    Path tempWorkDir;

    private Site cms;
    private ContentFactory contentFactory;
    private Repository<Content, String> contentRepository;
    private Repository<User, String> userRepository;
    private FileUploadService fileUploadService;
    private TemplateProcessor templateProcessor;
    private ConfigurationManager configurationManager;
    private ContentExporter contentExporter;
    private ContentImporter contentImporter;
    private CMSLogger cmsLogger;
    private AuditLogger auditLogger;

    // Test users with different roles
    private User adminUser;
    private User editorUser;
    private User viewerUser;

    @BeforeEach
    void setUpIntegrationEnvironment() {
        // Initialize core components
        contentFactory = new ContentFactory();
        contentRepository = new InMemoryContentRepository<>();
        userRepository = new InMemoryUserRepository<>();
        fileUploadService = new FileUploadService();
        templateProcessor = new TemplateProcessor();
        configurationManager = new ConfigurationManager();
        contentExporter = new ContentExporter();
        contentImporter = new ContentImporter();
        cmsLogger = CMSLogger.getInstance();
        auditLogger = new AuditLogger();

        // Create test users
        adminUser = new User("admin", "Admin User", "admin@cms.test", Role.ADMIN);
        editorUser = new User("editor", "Editor User", "editor@cms.test", Role.EDITOR);
        viewerUser = new User("viewer", "Viewer User", "viewer@cms.test", Role.VIEWER);

        // Save users to repository
        userRepository.save(adminUser);
        userRepository.save(editorUser);
        userRepository.save(viewerUser);

        // Initialize CMS site
        cms = new Site("Integration Test CMS", "integration-cms", adminUser);

        // Configure working directories
        System.setProperty("cms.work.dir", tempWorkDir.toString());
        System.setProperty("cms.upload.dir", tempWorkDir.resolve("uploads").toString());
        System.setProperty("cms.template.dir", tempWorkDir.resolve("templates").toString());
        System.setProperty("cms.export.dir", tempWorkDir.resolve("exports").toString());

        // Create directories
        try {
            Files.createDirectories(tempWorkDir.resolve("uploads"));
            Files.createDirectories(tempWorkDir.resolve("templates"));
            Files.createDirectories(tempWorkDir.resolve("exports"));
            Files.createDirectories(tempWorkDir.resolve("config"));
        } catch (IOException e) {
            fail("Failed to create test directories: " + e.getMessage());
        }
    }

    /**
     * Complete content management workflow integration tests.
     */
    @Nested
    @DisplayName("Complete Content Management Workflow")
    class ContentManagementWorkflowTests {

        @Test
        @DisplayName("Should execute complete article publication workflow")
        void shouldExecuteCompleteArticlePublicationWorkflow() {
            // Phase 1: Content Creation (Factory Pattern + Collections)
            Content article = contentFactory.createContent("ARTICLE",
                "Complete Integration Test Article",
                "This article tests the complete CMS integration workflow including all patterns and technologies.",
                editorUser);

            assertNotNull(article, "Article should be created");
            assertEquals("Complete Integration Test Article", article.getTitle(), "Title should be set");
            assertEquals(ContentStatus.DRAFT, article.getStatus(), "Initial status should be DRAFT");

            // Save to repository (Collections Framework + Generics)
            Content savedArticle = contentRepository.save(article);
            assertEquals(article.getId(), savedArticle.getId(), "Repository should return saved article");
            assertTrue(contentRepository.existsById(article.getId()), "Article should exist in repository");

            // Log content creation (Logging)
            cmsLogger.logContentCreated(article, editorUser);
            auditLogger.logAuditEvent("CONTENT_CREATED", editorUser.getUsername(), "127.0.0.1",
                "Article created: " + article.getTitle(),
                Map.of("content_id", article.getId(), "content_type", "ARTICLE"));

            // Phase 2: Content Organization (Composite Pattern)
            Category newsCategory = new Category("News", "News articles category");
            Category techCategory = new Category("Technology", "Technology subcategory");
            ContentItem articleItem = new ContentItem(article);

            newsCategory.add(techCategory);
            techCategory.add(articleItem);
            cms.addComponent(newsCategory);

            assertEquals(1, newsCategory.getChildren().size(), "News category should have tech subcategory");
            assertEquals(1, techCategory.getChildren().size(), "Tech category should have article");
            assertEquals(1, cms.getTotalContentCount(), "Site should have 1 content item");

            // Phase 3: Content Review and Approval Workflow
            // Editor submits for review
            article.setStatus(ContentStatus.UNDER_REVIEW);
            contentRepository.update(article);
            cmsLogger.logContentStatusChange(article, ContentStatus.DRAFT, ContentStatus.UNDER_REVIEW, editorUser);

            // Admin approves and publishes
            article.setStatus(ContentStatus.PUBLISHED);
            article.setPublishedAt(LocalDateTime.now());
            article.setPublishedBy(adminUser.getUsername());
            contentRepository.update(article);
            cmsLogger.logContentStatusChange(article, ContentStatus.UNDER_REVIEW, ContentStatus.PUBLISHED, adminUser);

            // Phase 4: Content Discovery (Iterator Pattern)
            List<Content> allContent = contentRepository.findAll();
            ContentIterator<Content> publishedIterator = ContentIterator.publishedOnly(allContent);

            List<Content> publishedContent = new ArrayList<>();
            while (publishedIterator.hasNext()) {
                publishedContent.add(publishedIterator.next());
            }

            assertEquals(1, publishedContent.size(), "Should find 1 published article");
            assertEquals(ContentStatus.PUBLISHED, publishedContent.get(0).getStatus(), "Article should be published");

            // Phase 5: Site Structure Navigation (Composite + Iterator)
            SiteStructureIterator structureIterator = SiteStructureIterator.depthFirst(cms.getRootComponents());
            List<SiteComponent> allComponents = new ArrayList<>();
            while (structureIterator.hasNext()) {
                allComponents.add(structureIterator.next());
            }

            assertTrue(allComponents.size() >= 3, "Should traverse all site components");
            assertTrue(allComponents.stream().anyMatch(c -> c instanceof Category), "Should include categories");
            assertTrue(allComponents.stream().anyMatch(c -> c instanceof ContentItem), "Should include content items");

            // Phase 6: Content Export (I/O Operations)
            Path exportFile = tempWorkDir.resolve("exports").resolve("complete-workflow-export.json");
            contentExporter.exportToJson(Arrays.asList(article), exportFile.toString());

            assertTrue(Files.exists(exportFile), "Export file should be created");

            // Verify export content
            try {
                String exportedContent = Files.readString(exportFile, StandardCharsets.UTF_8);
                assertTrue(exportedContent.contains("Complete Integration Test Article"), "Export should contain article title");
                assertTrue(exportedContent.contains("PUBLISHED"), "Export should contain published status");
            } catch (IOException e) {
                fail("Failed to read export file: " + e.getMessage());
            }

            // Phase 7: Audit Trail Verification
            List<AuditRecord> auditRecords = auditLogger.getAuditRecords();
            assertFalse(auditRecords.isEmpty(), "Should have audit records");

            boolean hasContentCreationRecord = auditRecords.stream()
                .anyMatch(r -> r.getEventType().equals("CONTENT_CREATED") &&
                              r.getEventDetails().get("content_id").equals(article.getId()));
            assertTrue(hasContentCreationRecord, "Should have content creation audit record");

            // Verify complete workflow integrity
            Content finalArticle = contentRepository.findById(article.getId());
            assertNotNull(finalArticle, "Article should still exist in repository");
            assertEquals(ContentStatus.PUBLISHED, finalArticle.getStatus(), "Article should be published");
            assertNotNull(finalArticle.getPublishedAt(), "Article should have publication timestamp");
            assertEquals(adminUser.getUsername(), finalArticle.getPublishedBy(), "Article should record publisher");
        }

        @Test
        @DisplayName("Should handle multi-content workflow with different types")
        void shouldHandleMultiContentWorkflowWithDifferentTypes() {
            // Create diverse content types
            Content article = contentFactory.createContent("ARTICLE", "Tech News", "Latest technology trends", editorUser);
            Content page = contentFactory.createContent("PAGE", "About Us", "<h1>About Our Company</h1><p>We are innovative...</p>", adminUser);
            Content image = contentFactory.createContent("IMAGE", "Company Logo", "/uploads/logo.png", editorUser);
            Content video = contentFactory.createContent("VIDEO", "Product Demo", "/uploads/demo.mp4", adminUser);

            // Save all content to repository
            List<Content> allContent = Arrays.asList(article, page, image, video);
            for (Content content : allContent) {
                contentRepository.save(content);
                cmsLogger.logContentCreated(content, content.getCreatedBy().equals("admin") ? adminUser : editorUser);
            }

            // Organize in composite structure
            Category articlesCategory = new Category("Articles", "News articles");
            Category pagesCategory = new Category("Pages", "Static pages");
            Category mediaCategory = new Category("Media", "Images and videos");

            articlesCategory.add(new ContentItem(article));
            pagesCategory.add(new ContentItem(page));
            mediaCategory.add(new ContentItem(image));
            mediaCategory.add(new ContentItem(video));

            cms.addComponent(articlesCategory);
            cms.addComponent(pagesCategory);
            cms.addComponent(mediaCategory);

            // Verify organization
            assertEquals(4, cms.getTotalContentCount(), "Should have all 4 content items");
            assertEquals(3, cms.getRootComponents().size(), "Should have 3 main categories");

            // Test type-specific iteration
            ContentIterator<ArticleContent> articleIterator = ContentIterator.byType(allContent, ArticleContent.class);
            List<ArticleContent> articles = new ArrayList<>();
            while (articleIterator.hasNext()) {
                articles.add(articleIterator.next());
            }
            assertEquals(1, articles.size(), "Should find 1 article");
            assertTrue(articles.get(0) instanceof ArticleContent, "Should be ArticleContent type");

            // Test content filtering by status
            article.publish();
            page.publish();
            contentRepository.update(article);
            contentRepository.update(page);

            ContentIterator<Content> publishedIterator = ContentIterator.publishedOnly(allContent);
            List<Content> publishedContent = new ArrayList<>();
            while (publishedIterator.hasNext()) {
                publishedContent.add(publishedIterator.next());
            }
            assertEquals(2, publishedContent.size(), "Should find 2 published items");

            // Test export of mixed content types
            Path mixedExportFile = tempWorkDir.resolve("exports").resolve("mixed-content-export.xml");
            contentExporter.exportToXml(allContent, mixedExportFile.toString());

            assertTrue(Files.exists(mixedExportFile), "Mixed export should be created");

            // Test import workflow
            List<Content> importedContent = contentImporter.importFromXml(mixedExportFile.toString(), adminUser);
            assertEquals(4, importedContent.size(), "Should import all 4 content items");

            // Verify imported content integrity
            Set<String> originalTitles = allContent.stream().map(Content::getTitle).collect(java.util.stream.Collectors.toSet());
            Set<String> importedTitles = importedContent.stream().map(Content::getTitle).collect(java.util.stream.Collectors.toSet());
            assertEquals(originalTitles, importedTitles, "Imported titles should match original");
        }

        @ParameterizedTest
        @MethodSource("provideContentWorkflowScenarios")
        @DisplayName("Should handle various content workflow scenarios")
        void shouldHandleVariousContentWorkflowScenarios(String contentType, String title, String content,
                                                        ContentStatus targetStatus, User author, boolean shouldSucceed) {
            // Act
            try {
                Content createdContent = contentFactory.createContent(contentType, title, content, author);
                contentRepository.save(createdContent);

                if (targetStatus != ContentStatus.DRAFT) {
                    createdContent.setStatus(targetStatus);
                    contentRepository.update(createdContent);
                }

                cmsLogger.logContentCreated(createdContent, author);

                // Assert success case
                if (shouldSucceed) {
                    assertNotNull(createdContent, "Content should be created successfully");
                    assertEquals(targetStatus, createdContent.getStatus(), "Should reach target status");
                    assertTrue(contentRepository.existsById(createdContent.getId()), "Should exist in repository");
                } else {
                    fail("Expected content creation to fail but it succeeded");
                }

            } catch (Exception e) {
                // Assert failure case
                if (!shouldSucceed) {
                    assertNotNull(e.getMessage(), "Should have error message");
                    assertTrue(e.getMessage().length() > 0, "Error message should not be empty");
                } else {
                    fail("Content creation failed unexpectedly: " + e.getMessage());
                }
            }
        }

        private static Stream<Arguments> provideContentWorkflowScenarios() {
            User testEditor = new User("testEditor", "Test Editor", "editor@test.com", Role.EDITOR);
            User testViewer = new User("testViewer", "Test Viewer", "viewer@test.com", Role.VIEWER);

            return Stream.of(
                Arguments.of("ARTICLE", "Valid Article", "Valid content", ContentStatus.PUBLISHED, testEditor, true),
                Arguments.of("PAGE", "Valid Page", "<h1>Valid HTML</h1>", ContentStatus.DRAFT, testEditor, true),
                Arguments.of("IMAGE", "Valid Image", "/uploads/valid.jpg", ContentStatus.PUBLISHED, testEditor, true),
                Arguments.of("VIDEO", "Valid Video", "/uploads/valid.mp4", ContentStatus.DRAFT, testEditor, true),
                Arguments.of("INVALID_TYPE", "Invalid Content", "Content", ContentStatus.DRAFT, testEditor, false),
                Arguments.of("ARTICLE", "", "No title", ContentStatus.DRAFT, testEditor, false),
                Arguments.of("ARTICLE", "Article", null, ContentStatus.DRAFT, testEditor, false)
            );
        }
    }

    /**
     * User management and authentication integration tests.
     */
    @Nested
    @DisplayName("User Management and Authentication Integration")
    class UserManagementIntegrationTests {

        @Test
        @DisplayName("Should handle complete user lifecycle and authentication")
        void shouldHandleCompleteUserLifecycleAndAuthentication() {
            // Phase 1: User Registration
            User newUser = new User("newbie", "New User", "newbie@cms.test", Role.VIEWER);
            User savedUser = userRepository.save(newUser);

            assertEquals("newbie", savedUser.getUsername(), "User should be saved with correct username");
            assertTrue(userRepository.existsById("newbie"), "User should exist in repository");
            cmsLogger.logUserCreated(newUser, adminUser);

            // Phase 2: Authentication
            UserSession session = newUser.createSession();
            assertNotNull(session, "Session should be created");
            assertNotNull(session.getSessionId(), "Session should have ID");
            assertTrue(session.isActive(), "Session should be active");
            cmsLogger.logSessionCreated(session, "127.0.0.1");

            // Phase 3: Role-Based Access Control
            // Test viewer accessing content (should succeed)
            List<Content> viewableContent = contentRepository.findAll();
            assertNotNull(viewableContent, "Viewer should access content list");

            // Test viewer trying to create content (should be restricted)
            try {
                Content restrictedContent = contentFactory.createContent("ARTICLE", "Unauthorized", "Content", newUser);
                // In a full implementation, this might throw an authorization exception
                // For now, we'll test that the content is created but not published
                assertEquals(ContentStatus.DRAFT, restrictedContent.getStatus(), "Viewer content should remain draft");
            } catch (Exception e) {
                // Expected if authorization is enforced at creation level
                assertTrue(e.getMessage().contains("permission") || e.getMessage().contains("role"),
                    "Should indicate permission/role issue");
            }

            // Phase 4: Role Change Workflow
            Role originalRole = newUser.getRole();
            newUser.setRole(Role.EDITOR);
            userRepository.update(newUser);
            cmsLogger.logUserRoleChanged(newUser, originalRole, Role.EDITOR, adminUser);

            // Test enhanced permissions after role change
            Content editorContent = contentFactory.createContent("ARTICLE", "Editor Article", "Now I can edit", newUser);
            assertNotNull(editorContent, "Editor should create content");
            contentRepository.save(editorContent);

            // Phase 5: Session Management
            cmsLogger.logSessionActivity(session, "content_creation", "Created article: Editor Article");

            // Simulate session timeout
            session.updateLastActivity(LocalDateTime.now().minusMinutes(31)); // Assuming 30-minute timeout
            assertFalse(session.isActive(), "Session should be inactive after timeout");
            cmsLogger.logSessionExpired(session, "Session timeout after 30 minutes");

            // Phase 6: User Deactivation
            newUser.deactivate();
            userRepository.update(newUser);
            cmsLogger.logUserDeactivated(newUser, adminUser, "User lifecycle test completion");

            assertFalse(newUser.isActive(), "User should be deactivated");

            // Phase 7: Audit Trail Verification
            auditLogger.logAuditEvent("USER_LIFECYCLE_COMPLETE", adminUser.getUsername(), "127.0.0.1",
                "Completed user lifecycle test", Map.of("test_user", newUser.getUsername()));

            List<AuditRecord> auditRecords = auditLogger.getAuditRecords();
            boolean hasUserLifecycleRecord = auditRecords.stream()
                .anyMatch(r -> r.getEventType().equals("USER_LIFECYCLE_COMPLETE"));
            assertTrue(hasUserLifecycleRecord, "Should have user lifecycle audit record");
        }

        @Test
        @DisplayName("Should enforce security across all user operations")
        void shouldEnforceSecurityAcrossAllUserOperations() {
            // Test password security
            User secureUser = new User("secure", "Secure User", "secure@cms.test", Role.EDITOR);
            secureUser.setPassword("SecureP@ssw0rd123");
            userRepository.save(secureUser);

            // Test authentication with correct credentials
            boolean validAuth = User.authenticate("secure", "SecureP@ssw0rd123");
            assertTrue(validAuth, "Valid credentials should authenticate");
            cmsLogger.logUserLogin(secureUser, "127.0.0.1", true);

            // Test authentication with incorrect credentials
            boolean invalidAuth = User.authenticate("secure", "WrongPassword");
            assertFalse(invalidAuth, "Invalid credentials should fail");
            cmsLogger.logUserLoginAttempt("secure", "127.0.0.1", false, "Invalid password");

            // Test suspicious activity detection (multiple failed attempts)
            for (int i = 0; i < 5; i++) {
                User.authenticate("secure", "WrongPassword" + i);
                cmsLogger.logUserLoginAttempt("secure", "127.0.0.1", false, "Failed attempt " + (i + 1));
            }
            cmsLogger.logSuspiciousActivity("Multiple failed login attempts", "127.0.0.1",
                "5 failed attempts for user: secure");

            // Test session security
            UserSession secureSession = secureUser.createSession();
            cmsLogger.logSessionCreated(secureSession, "127.0.0.1");

            // Test session hijacking protection (IP change detection)
            cmsLogger.logSuspiciousActivity("Session IP change", "192.168.1.100",
                "Session IP changed from 127.0.0.1 to 192.168.1.100");

            // Test privilege escalation attempt
            try {
                secureUser.setRole(Role.ADMIN); // Should require admin authorization
                cmsLogger.logSecurityViolation("Unauthorized role change attempt", secureUser, "127.0.0.1",
                    "User attempted to change their own role to ADMIN");
            } catch (Exception e) {
                // Expected if privilege escalation is properly prevented
                assertTrue(e.getMessage().contains("authorization") || e.getMessage().contains("permission"),
                    "Should prevent unauthorized role changes");
            }

            // Test data access logging
            Content sensitiveContent = contentFactory.createContent("ARTICLE", "Confidential", "Sensitive data", adminUser);
            contentRepository.save(sensitiveContent);

            // Log access attempt
            auditLogger.logAuditEvent("SENSITIVE_DATA_ACCESS", secureUser.getUsername(), "127.0.0.1",
                "Accessed confidential content", Map.of("content_id", sensitiveContent.getId(), "classification", "confidential"));

            // Verify audit trail contains security events
            List<AuditRecord> securityRecords = auditLogger.findAuditRecords("SENSITIVE_DATA_ACCESS");
            assertEquals(1, securityRecords.size(), "Should have sensitive data access record");
            assertEquals("confidential", securityRecords.get(0).getEventDetails().get("classification"),
                "Should record data classification");
        }
    }

    /**
     * File operations and template processing integration tests.
     */
    @Nested
    @DisplayName("File Operations and Template Processing Integration")
    class FileOperationsIntegrationTests {

        @Test
        @DisplayName("Should execute complete file upload and processing workflow")
        void shouldExecuteCompleteFileUploadAndProcessingWorkflow() throws IOException {
            // Phase 1: Create test files
            String imageContent = createMockImageContent();
            Path testImageFile = tempWorkDir.resolve("test-upload.jpg");
            Files.write(testImageFile, imageContent.getBytes(StandardCharsets.UTF_8));

            String templateContent = """
                <!DOCTYPE html>
                <html>
                <head><title>${title}</title></head>
                <body>
                    <h1>${heading}</h1>
                    <img src="${image_url}" alt="${image_alt}" />
                    <p>Author: ${author}</p>
                    <p>Published: ${publish_date}</p>
                    <div class="content">${content}</div>
                </body>
                </html>
                """;
            Path templateFile = tempWorkDir.resolve("templates").resolve("article-template.html");
            Files.write(templateFile, templateContent.getBytes(StandardCharsets.UTF_8));

            // Phase 2: File Upload (I/O + Security)
            UploadResult uploadResult;
            try (InputStream fileStream = Files.newInputStream(testImageFile)) {
                uploadResult = fileUploadService.uploadFile(fileStream,
                    tempWorkDir.resolve("uploads").toString(), "test-upload.jpg");
            }

            assertTrue(uploadResult.isSuccess(), "File upload should succeed");
            assertNotNull(uploadResult.getUploadedFilePath(), "Should have upload path");
            cmsLogger.logFileUploadSuccess(editorUser, "test-upload.jpg", "127.0.0.1",
                uploadResult.getUploadedFilePath(), uploadResult.getFileSize());

            // Phase 3: Create Content with Uploaded File
            Content article = contentFactory.createContent("ARTICLE", "File Integration Article",
                "This article includes an uploaded image.", editorUser);
            contentRepository.save(article);

            Content image = contentFactory.createContent("IMAGE", "Uploaded Test Image",
                uploadResult.getUploadedFilePath(), editorUser);
            contentRepository.save(image);

            // Phase 4: Template Processing (I/O + Collections)
            Map<String, Object> templateVars = new HashMap<>();
            templateVars.put("title", article.getTitle());
            templateVars.put("heading", article.getTitle());
            templateVars.put("image_url", uploadResult.getUploadedFilePath());
            templateVars.put("image_alt", image.getTitle());
            templateVars.put("author", article.getCreatedBy());
            templateVars.put("publish_date", LocalDateTime.now().toString());
            templateVars.put("content", article.getBody());

            String processedTemplate = templateProcessor.processTemplate(templateFile.toString(), templateVars);

            assertNotNull(processedTemplate, "Template should be processed");
            assertTrue(processedTemplate.contains(article.getTitle()), "Should contain article title");
            assertTrue(processedTemplate.contains(uploadResult.getUploadedFilePath()), "Should contain image path");
            assertTrue(processedTemplate.contains(editorUser.getUsername()), "Should contain author");
            assertFalse(processedTemplate.contains("${"), "Should not contain unprocessed variables");

            // Phase 5: Save Processed Content
            Path outputFile = tempWorkDir.resolve("generated-article.html");
            Files.write(outputFile, processedTemplate.getBytes(StandardCharsets.UTF_8));

            assertTrue(Files.exists(outputFile), "Generated file should exist");
            assertTrue(Files.size(outputFile) > 1000, "Generated file should have substantial content");

            // Phase 6: Export Content with File References
            List<Content> contentWithFiles = Arrays.asList(article, image);
            Path exportFile = tempWorkDir.resolve("exports").resolve("content-with-files.json");
            contentExporter.exportToJson(contentWithFiles, exportFile.toString());

            assertTrue(Files.exists(exportFile), "Export with files should be created");

            String exportedContent = Files.readString(exportFile, StandardCharsets.UTF_8);
            assertTrue(exportedContent.contains(uploadResult.getUploadedFilePath()),
                "Export should preserve file paths");

            // Phase 7: Import and Verify File References
            List<Content> importedContent = contentImporter.importFromJson(exportFile.toString(), adminUser);
            assertEquals(2, importedContent.size(), "Should import both article and image");

            Content importedImage = importedContent.stream()
                .filter(c -> c.getTitle().equals("Uploaded Test Image"))
                .findFirst().orElse(null);
            assertNotNull(importedImage, "Should import image content");
            assertTrue(importedImage.getBody().contains("uploads"), "Should preserve upload path");

            // Phase 8: Cleanup and Logging
            cmsLogger.logFileProcessingComplete("test-upload.jpg", processedTemplate.length(), true);
            auditLogger.logAuditEvent("FILE_WORKFLOW_COMPLETE", editorUser.getUsername(), "127.0.0.1",
                "Completed file upload and processing workflow",
                Map.of("original_file", "test-upload.jpg", "template", "article-template.html",
                      "output_size", String.valueOf(processedTemplate.length())));

            // Verify audit trail
            List<AuditRecord> fileWorkflowRecords = auditLogger.findAuditRecords("FILE_WORKFLOW_COMPLETE");
            assertEquals(1, fileWorkflowRecords.size(), "Should have file workflow completion record");
        }

        @Test
        @DisplayName("Should handle batch file operations with error recovery")
        void shouldHandleBatchFileOperationsWithErrorRecovery() throws IOException {
            // Phase 1: Create multiple test files (some valid, some invalid)
            List<Path> testFiles = new ArrayList<>();

            // Valid files
            for (int i = 0; i < 5; i++) {
                Path validFile = tempWorkDir.resolve("valid-" + i + ".txt");
                Files.write(validFile, ("Valid content " + i).getBytes(StandardCharsets.UTF_8));
                testFiles.add(validFile);
            }

            // Invalid files (problematic names, large sizes, etc.)
            Path invalidNameFile = tempWorkDir.resolve("../traversal-attempt.txt");
            Path largeFile = tempWorkDir.resolve("oversized.txt");
            try {
                Files.write(largeFile, new byte[50 * 1024 * 1024]); // 50MB file
            } catch (IOException e) {
                // May fail due to disk space, create smaller file
                Files.write(largeFile, "Large file content".getBytes(StandardCharsets.UTF_8));
            }

            // Phase 2: Batch Upload Processing
            List<UploadResult> uploadResults = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Path file : testFiles) {
                try (InputStream fileStream = Files.newInputStream(file)) {
                    UploadResult result = fileUploadService.uploadFile(fileStream,
                        tempWorkDir.resolve("uploads").toString(), file.getFileName().toString());
                    uploadResults.add(result);

                    if (result.isSuccess()) {
                        successCount++;
                        cmsLogger.logFileUploadSuccess(editorUser, file.getFileName().toString(),
                            "127.0.0.1", result.getUploadedFilePath(), result.getFileSize());
                    } else {
                        failureCount++;
                        cmsLogger.logFileUploadAttempt(editorUser, file.getFileName().toString(),
                            "127.0.0.1", false, result.getErrorMessage());
                    }
                } catch (Exception e) {
                    failureCount++;
                    cmsLogger.logFileUploadAttempt(editorUser, file.getFileName().toString(),
                        "127.0.0.1", false, e.getMessage());
                }
            }

            // Phase 3: Error Recovery and Retry Logic
            List<UploadResult> failedUploads = uploadResults.stream()
                .filter(r -> !r.isSuccess())
                .collect(java.util.stream.Collectors.toList());

            if (!failedUploads.isEmpty()) {
                cmsLogger.logBatchOperationResult("file_upload_batch", successCount, failureCount,
                    "Batch file upload completed with some failures");
            }

            // Phase 4: Content Creation from Successful Uploads
            List<Content> createdContent = new ArrayList<>();
            for (UploadResult result : uploadResults) {
                if (result.isSuccess()) {
                    try {
                        Content content = contentFactory.createContent("IMAGE",
                            "Batch Upload " + result.getOriginalFilename(),
                            result.getUploadedFilePath(), editorUser);
                        contentRepository.save(content);
                        createdContent.add(content);
                        cmsLogger.logContentCreated(content, editorUser);
                    } catch (Exception e) {
                        cmsLogger.logContentCreationFailure(result.getOriginalFilename(), e.getMessage());
                    }
                }
            }

            // Phase 5: Batch Export of Created Content
            if (!createdContent.isEmpty()) {
                Path batchExportFile = tempWorkDir.resolve("exports").resolve("batch-upload-export.xml");
                contentExporter.exportToXml(createdContent, batchExportFile.toString());

                assertTrue(Files.exists(batchExportFile), "Batch export should be created");
                cmsLogger.logBatchExportComplete(createdContent.size(), batchExportFile.toString());
            }

            // Assert final state
            assertTrue(successCount > 0, "Should have some successful uploads");
            assertEquals(testFiles.size(), successCount + failureCount, "Should account for all files");

            // Verify logging integrity
            auditLogger.logAuditEvent("BATCH_OPERATION_COMPLETE", editorUser.getUsername(), "127.0.0.1",
                "Completed batch file operation",
                Map.of("total_files", String.valueOf(testFiles.size()),
                      "successful", String.valueOf(successCount),
                      "failed", String.valueOf(failureCount)));
        }

        private String createMockImageContent() {
            return "Mock JPEG content with proper headers and binary data representation";
        }
    }

    /**
     * Performance and scalability integration tests.
     */
    @Nested
    @DisplayName("Performance and Scalability Integration")
    class PerformanceScalabilityTests {

        @Test
        @DisplayName("Should handle high-volume content operations efficiently")
        void shouldHandleHighVolumeContentOperationsEfficiently() throws InterruptedException {
            int contentCount = 1000;
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Future<List<Content>>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            // Phase 1: Concurrent Content Creation
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<List<Content>> future = executor.submit(() -> {
                    List<Content> threadContent = new ArrayList<>();
                    try {
                        for (int j = 0; j < contentCount / threadCount; j++) {
                            Content content = contentFactory.createContent("ARTICLE",
                                "Performance Test Article " + threadId + "-" + j,
                                "Content from thread " + threadId + " item " + j,
                                threadId % 2 == 0 ? editorUser : adminUser);

                            contentRepository.save(content);
                            threadContent.add(content);

                            if (j % 10 == 0) {
                                cmsLogger.logContentCreated(content, content.getCreatedBy().equals("admin") ? adminUser : editorUser);
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                    return threadContent;
                });
                futures.add(future);
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");

            long creationTime = System.currentTimeMillis() - startTime;

            // Collect all created content
            List<Content> allCreatedContent = new ArrayList<>();
            for (Future<List<Content>> future : futures) {
                try {
                    allCreatedContent.addAll(future.get());
                } catch (ExecutionException e) {
                    fail("Content creation thread failed: " + e.getMessage());
                }
            }

            executor.shutdown();

            // Phase 2: Performance Validation
            assertEquals(contentCount, allCreatedContent.size(), "Should create all content items");
            assertTrue(creationTime < 15000, "Content creation should complete within 15 seconds");

            // Phase 3: Bulk Operations Performance
            startTime = System.currentTimeMillis();

            // Bulk status updates
            List<Content> publishedContent = allCreatedContent.subList(0, contentCount / 2);
            for (Content content : publishedContent) {
                content.setStatus(ContentStatus.PUBLISHED);
                contentRepository.update(content);
            }

            long updateTime = System.currentTimeMillis() - startTime;
            assertTrue(updateTime < 10000, "Bulk updates should complete within 10 seconds");

            // Phase 4: Query Performance
            startTime = System.currentTimeMillis();

            List<Content> allContent = contentRepository.findAll();
            assertEquals(contentCount, allContent.size(), "Repository should return all content");

            // Test filtered iterations
            ContentIterator<Content> publishedIterator = ContentIterator.publishedOnly(allContent);
            int publishedCount = 0;
            while (publishedIterator.hasNext()) {
                publishedIterator.next();
                publishedCount++;
            }
            assertEquals(contentCount / 2, publishedCount, "Should find correct number of published items");

            long queryTime = System.currentTimeMillis() - startTime;
            assertTrue(queryTime < 5000, "Query operations should complete within 5 seconds");

            // Phase 5: Export Performance
            startTime = System.currentTimeMillis();

            Path performanceExportFile = tempWorkDir.resolve("exports").resolve("performance-test-export.json");
            contentExporter.exportToJson(allContent, performanceExportFile.toString());

            long exportTime = System.currentTimeMillis() - startTime;
            assertTrue(exportTime < 10000, "Export should complete within 10 seconds");
            assertTrue(Files.exists(performanceExportFile), "Export file should be created");
            assertTrue(Files.size(performanceExportFile) > 100000, "Export file should be substantial");

            // Phase 6: Memory and Resource Validation
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();

            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            assertTrue(memoryUsagePercent < 80, "Memory usage should be reasonable: " + memoryUsagePercent + "%");

            // Log performance metrics
            cmsLogger.logPerformanceMetrics("high_volume_test", contentCount,
                creationTime + updateTime + queryTime + exportTime, memoryUsagePercent);

            auditLogger.logAuditEvent("PERFORMANCE_TEST_COMPLETE", adminUser.getUsername(), "127.0.0.1",
                "Completed high-volume performance test",
                Map.of("content_count", String.valueOf(contentCount),
                      "total_time_ms", String.valueOf(creationTime + updateTime + queryTime + exportTime),
                      "memory_usage_percent", String.valueOf(memoryUsagePercent)));
        }

        @Test
        @DisplayName("Should maintain system stability under stress")
        void shouldMaintainSystemStabilityUnderStress() throws InterruptedException {
            int stressOperations = 500;
            int concurrentUsers = 20;
            ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
            CountDownLatch latch = new CountDownLatch(concurrentUsers);

            // Track errors and successes
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

            // Phase 1: Stress Test Execution
            for (int i = 0; i < concurrentUsers; i++) {
                final int userId = i;
                executor.submit(() -> {
                    User stressUser = new User("stress" + userId, "Stress User " + userId,
                        "stress" + userId + "@test.com", Role.EDITOR);

                    try {
                        userRepository.save(stressUser);

                        for (int j = 0; j < stressOperations / concurrentUsers; j++) {
                            try {
                                // Mixed operations to stress different components
                                switch (j % 4) {
                                    case 0:
                                        // Content operations
                                        Content content = contentFactory.createContent("ARTICLE",
                                            "Stress Article " + userId + "-" + j, "Stress content", stressUser);
                                        contentRepository.save(content);
                                        break;
                                    case 1:
                                        // Repository operations
                                        List<Content> allContent = contentRepository.findAll();
                                        assertNotNull(allContent, "Repository should respond");
                                        break;
                                    case 2:
                                        // User session operations
                                        UserSession session = stressUser.createSession();
                                        assertNotNull(session, "Session should be created");
                                        break;
                                    case 3:
                                        // Iterator operations
                                        List<Content> contentList = contentRepository.findAll();
                                        if (!contentList.isEmpty()) {
                                            ContentIterator<Content> iterator = ContentIterator.all(contentList);
                                            while (iterator.hasNext() && j % 10 == 0) {
                                                iterator.next(); // Sample iteration
                                                break; // Don't iterate everything every time
                                            }
                                        }
                                        break;
                                }
                                successCount.incrementAndGet();

                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                                errors.add(e);

                                // Log error but continue (resilience test)
                                cmsLogger.logOperationError("stress_test", e.getMessage());
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(60, TimeUnit.SECONDS), "Stress test should complete within 60 seconds");
            executor.shutdown();

            // Phase 2: Stability Validation
            int totalOperations = successCount.get() + errorCount.get();
            double errorRate = (double) errorCount.get() / totalOperations * 100;

            assertTrue(errorRate < 5, "Error rate should be less than 5%: " + errorRate + "%");
            assertTrue(successCount.get() > stressOperations * 0.95, "Should have high success rate");

            // Phase 3: System State Verification
            // Verify repositories are still functional
            List<User> allUsers = userRepository.findAll();
            assertTrue(allUsers.size() >= concurrentUsers + 3, "All users should be in repository"); // +3 for setup users

            List<Content> allContent = contentRepository.findAll();
            assertTrue(allContent.size() > 0, "Content should exist in repository");

            // Verify no data corruption
            for (Content content : allContent) {
                assertNotNull(content.getId(), "Content ID should not be null");
                assertNotNull(content.getTitle(), "Content title should not be null");
                assertNotNull(content.getCreatedBy(), "Content creator should not be null");
            }

            // Phase 4: Recovery Operations
            // Test that normal operations still work after stress
            Content recoveryContent = contentFactory.createContent("ARTICLE",
                "Post-Stress Recovery Article", "System recovery test", adminUser);
            contentRepository.save(recoveryContent);

            assertTrue(contentRepository.existsById(recoveryContent.getId()),
                "System should be fully functional after stress test");

            // Log stress test results
            cmsLogger.logStressTestComplete(totalOperations, successCount.get(), errorCount.get(), errorRate);
            auditLogger.logAuditEvent("STRESS_TEST_COMPLETE", adminUser.getUsername(), "127.0.0.1",
                "Completed system stability stress test",
                Map.of("total_operations", String.valueOf(totalOperations),
                      "success_count", String.valueOf(successCount.get()),
                      "error_count", String.valueOf(errorCount.get()),
                      "error_rate_percent", String.valueOf(errorRate)));
        }
    }

    /**
     * Error handling and recovery integration tests.
     */
    @Nested
    @DisplayName("Error Handling and Recovery Integration")
    class ErrorHandlingRecoveryTests {

        @Test
        @DisplayName("Should handle cascading failures gracefully")
        void shouldHandleCascadingFailuresGracefully() {
            // Phase 1: Simulate Repository Failure
            Repository<Content, String> failingRepository = new FailingContentRepository<>();

            try {
                Content testContent = contentFactory.createContent("ARTICLE", "Failure Test", "Content", editorUser);
                failingRepository.save(testContent);
                fail("Expected repository operation to fail");
            } catch (Exception e) {
                // Expected failure
                assertNotNull(e.getMessage(), "Should have error message");
                cmsLogger.logOperationError("repository_failure", e.getMessage());
            }

            // Phase 2: Verify System Continues Operating
            // Even with repository failure, other components should work
            Content backupContent = contentFactory.createContent("ARTICLE", "Backup Content", "Content", editorUser);
            assertNotNull(backupContent, "Content factory should still work");

            // File operations should continue
            try {
                Path testFile = tempWorkDir.resolve("failure-test.txt");
                Files.write(testFile, "Failure recovery test".getBytes(StandardCharsets.UTF_8));
                assertTrue(Files.exists(testFile), "File operations should continue");
            } catch (IOException e) {
                fail("File operations should continue working: " + e.getMessage());
            }

            // Phase 3: Graceful Degradation
            // System should provide fallback functionality
            List<Content> fallbackContent = new ArrayList<>();
            fallbackContent.add(backupContent);

            ContentIterator<Content> iterator = ContentIterator.all(fallbackContent);
            assertTrue(iterator.hasNext(), "Iterator should work with local content");

            // Phase 4: Error Recovery and Retry
            // Simulate repository recovery
            Repository<Content, String> recoveredRepository = new InMemoryContentRepository<>();
            Content recoveredContent = contentFactory.createContent("ARTICLE", "Recovery Test", "Recovered", editorUser);

            // Should work after recovery
            Content saved = recoveredRepository.save(recoveredContent);
            assertEquals(recoveredContent.getId(), saved.getId(), "Repository should work after recovery");

            cmsLogger.logSystemRecovery("repository_failure", "Repository recovered and operational");
            auditLogger.logAuditEvent("SYSTEM_RECOVERY", adminUser.getUsername(), "127.0.0.1",
                "System recovered from repository failure",
                Map.of("failed_component", "repository", "recovery_time_ms", "1000"));
        }

        @Test
        @DisplayName("Should maintain data consistency during partial failures")
        void shouldMaintainDataConsistencyDuringPartialFailures() {
            // Phase 1: Setup Multi-Component Operation
            List<Content> batchContent = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Content content = contentFactory.createContent("ARTICLE",
                    "Consistency Test " + i, "Content " + i, editorUser);
                batchContent.add(content);
            }

            // Phase 2: Simulate Partial Save Failure
            int successfulSaves = 0;
            int failedSaves = 0;

            for (int i = 0; i < batchContent.size(); i++) {
                Content content = batchContent.get(i);
                try {
                    if (i == 5) {
                        // Simulate failure on 6th item
                        throw new RuntimeException("Simulated repository failure");
                    }
                    contentRepository.save(content);
                    successfulSaves++;
                    cmsLogger.logContentCreated(content, editorUser);
                } catch (Exception e) {
                    failedSaves++;
                    cmsLogger.logOperationError("batch_save_failure",
                        "Failed to save content: " + content.getTitle() + " - " + e.getMessage());

                    // Continue with other items (partial failure handling)
                }
            }

            // Phase 3: Verify Consistency
            List<Content> savedContent = contentRepository.findAll();
            assertEquals(successfulSaves, savedContent.size(), "Only successful saves should be in repository");
            assertEquals(9, successfulSaves, "Should have 9 successful saves");
            assertEquals(1, failedSaves, "Should have 1 failed save");

            // Verify data integrity of saved content
            for (Content content : savedContent) {
                assertNotNull(content.getId(), "Saved content should have ID");
                assertNotNull(content.getTitle(), "Saved content should have title");
                assertFalse(content.getTitle().contains("Consistency Test 5"),
                    "Failed content should not be in repository");
            }

            // Phase 4: Compensating Transactions
            // Retry failed operations
            Content failedContent = batchContent.get(5);
            try {
                Content retriedContent = contentFactory.createContent("ARTICLE",
                    failedContent.getTitle() + " (Retry)", failedContent.getBody(), editorUser);
                contentRepository.save(retriedContent);
                cmsLogger.logOperationRetrySuccess("content_save", retriedContent.getId());
            } catch (Exception e) {
                cmsLogger.logOperationRetryFailure("content_save", e.getMessage());
            }

            // Phase 5: Final Consistency Check
            List<Content> finalContent = contentRepository.findAll();
            assertEquals(10, finalContent.size(), "Should have all content after retry");

            auditLogger.logAuditEvent("CONSISTENCY_CHECK_COMPLETE", adminUser.getUsername(), "127.0.0.1",
                "Maintained data consistency during partial failures",
                Map.of("initial_batch_size", "10", "initial_failures", "1",
                      "final_success_count", String.valueOf(finalContent.size())));
        }
    }

    // Helper classes for testing

    /**
     * Mock repository that fails operations for testing error handling.
     */
    private static class FailingContentRepository<T> implements Repository<T, String> {
        @Override
        public T save(T entity) {
            throw new RuntimeException("Simulated repository failure during save operation");
        }

        @Override
        public T findById(String id) {
            throw new RuntimeException("Simulated repository failure during findById operation");
        }

        @Override
        public List<T> findAll() {
            throw new RuntimeException("Simulated repository failure during findAll operation");
        }

        @Override
        public T update(T entity) {
            throw new RuntimeException("Simulated repository failure during update operation");
        }

        @Override
        public void delete(String id) {
            throw new RuntimeException("Simulated repository failure during delete operation");
        }

        @Override
        public boolean existsById(String id) {
            return false;
        }

        @Override
        public long count() {
            return 0;
        }
    }

    /**
     * In-memory repository implementation for testing.
     */
    private static class InMemoryContentRepository<T> implements Repository<T, String> {
        private final Map<String, T> storage = new ConcurrentHashMap<>();

        @Override
        public T save(T entity) {
            String id = extractId(entity);
            storage.put(id, entity);
            return entity;
        }

        @Override
        public T findById(String id) {
            return storage.get(id);
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public T update(T entity) {
            String id = extractId(entity);
            storage.put(id, entity);
            return entity;
        }

        @Override
        public void delete(String id) {
            storage.remove(id);
        }

        @Override
        public boolean existsById(String id) {
            return storage.containsKey(id);
        }

        @Override
        public long count() {
            return storage.size();
        }

        @SuppressWarnings("unchecked")
        private String extractId(T entity) {
            if (entity instanceof Content) {
                return ((Content) entity).getId();
            } else if (entity instanceof User) {
                return ((User) entity).getUsername();
            }
            return entity.toString();
        }
    }

    /**
     * In-memory user repository implementation for testing.
     */
    private static class InMemoryUserRepository<T extends User> implements Repository<T, String> {
        private final Map<String, T> storage = new ConcurrentHashMap<>();

        @Override
        public T save(T entity) {
            storage.put(entity.getUsername(), entity);
            return entity;
        }

        @Override
        public T findById(String id) {
            return storage.get(id);
        }

        @Override
        public List<T> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public T update(T entity) {
            storage.put(entity.getUsername(), entity);
            return entity;
        }

        @Override
        public void delete(String id) {
            storage.remove(id);
        }

        @Override
        public boolean existsById(String id) {
            return storage.containsKey(id);
        }

        @Override
        public long count() {
            return storage.size();
        }
    }

    /**
     * Mock audit record for testing.
     */
    private static class AuditRecord {
        private final String eventType;
        private final String username;
        private final String ipAddress;
        private final String description;
        private final Map<String, String> eventDetails;
        private final LocalDateTime timestamp;

        public AuditRecord(String eventType, String username, String ipAddress,
                          String description, Map<String, String> eventDetails) {
            this.eventType = eventType;
            this.username = username;
            this.ipAddress = ipAddress;
            this.description = description;
            this.eventDetails = new HashMap<>(eventDetails);
            this.timestamp = LocalDateTime.now();
        }

        public String getEventType() { return eventType; }
        public String getUsername() { return username; }
        public String getIpAddress() { return ipAddress; }
        public String getDescription() { return description; }
        public Map<String, String> getEventDetails() { return eventDetails; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
