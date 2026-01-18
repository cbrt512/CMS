package com.cms.io;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.util.CMSLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive JUnit test suite for Java I/O Operations implementation.
 *
 * <p>This test class validates the complete Java I/O implementation as required
 * , testing file operations, stream processing, character encoding,
 * resource management, and all I/O-related functionality throughout the CMS.</p>
 *
 * <p><strong>Testing Focus:</strong> JUnit Testing  - Java I/O validation
 * - Tests comprehensive file upload and validation functionality
 * - Validates template processing with dynamic content generation
 * - Tests configuration management with multiple formats
 * - Verifies content export/import in various formats
 * - Tests advanced I/O utilities and stream operations
 * - Validates security features and resource management</p>
 *
 * <p><strong>Java I/O Implementation Coverage:</strong>
 * - File and stream operations with proper encoding
 * - Resource management with try-with-resources
 * - Character encoding handling (UTF-8)
 * - Buffered I/O for performance optimization
 * - Exception handling and error recovery
 * - Asynchronous I/O operations
 * - File compression and integrity checking
 * - Path traversal prevention and security validation</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Java I/O Operations Tests")
public class IOOperationsTest {

    @TempDir
    Path tempDir;

    private FileUploadService fileUploadService;
    private TemplateProcessor templateProcessor;
    private ConfigurationManager configurationManager;
    private ContentExporter contentExporter;
    private ContentImporter contentImporter;
    private IOUtils ioUtils;
    private ContentFactory contentFactory;
    private User testUser;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        templateProcessor = new TemplateProcessor();
        configurationManager = new ConfigurationManager();
        contentExporter = new ContentExporter();
        contentImporter = new ContentImporter();
        ioUtils = new IOUtils();
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
    }

    /**
     * Tests for FileUploadService I/O operations.
     */
    @Nested
    @DisplayName("File Upload Service I/O Tests")
    class FileUploadServiceIOTests {

        @Test
        @DisplayName("Should handle file upload with proper I/O operations")
        void shouldHandleFileUploadWithProperIOOperations() throws IOException {
            // Arrange
            String fileName = "test-image.jpg";
            String fileContent = "Mock JPEG image content with binary data: \u00FF\u00D8\u00FF\u00E0";
            Path sourceFile = tempDir.resolve(fileName);
            Files.write(sourceFile, fileContent.getBytes(StandardCharsets.UTF_8));

            Path uploadDir = tempDir.resolve("uploads");
            Files.createDirectories(uploadDir);

            // Act
            UploadResult result;
            try (InputStream inputStream = Files.newInputStream(sourceFile)) {
                result = fileUploadService.uploadFile(inputStream, uploadDir.toString(), fileName);
            }

            // Assert
            assertNotNull(result, "Upload result should not be null");
            assertTrue(result.isSuccess(), "Upload should be successful");
            assertNotNull(result.getUploadedFilePath(), "Should have uploaded file path");
            assertTrue(Files.exists(Paths.get(result.getUploadedFilePath())), "Uploaded file should exist");

            // Verify file content integrity
            String uploadedContent = Files.readString(Paths.get(result.getUploadedFilePath()), StandardCharsets.UTF_8);
            assertEquals(fileContent, uploadedContent, "File content should be preserved");

            // Verify file metadata
            assertTrue(result.getFileSize() > 0, "File size should be recorded");
            assertNotNull(result.getMimeType(), "MIME type should be detected");
            assertTrue(result.getMimeType().startsWith("image/"), "Should detect image MIME type");
        }

        @Test
        @DisplayName("Should handle large file uploads with streaming I/O")
        void shouldHandleLargeFileUploadsWithStreamingIO() throws IOException {
            // Arrange - Create a large file (1MB)
            String fileName = "large-file.txt";
            Path largeFile = tempDir.resolve(fileName);
            int fileSize = 1024 * 1024; // 1MB

            // Create large file with specific content pattern
            try (BufferedWriter writer = Files.newBufferedWriter(largeFile, StandardCharsets.UTF_8)) {
                for (int i = 0; i < fileSize / 50; i++) {
                    writer.write("This is line " + i + " of the large test file.\n");
                }
            }

            Path uploadDir = tempDir.resolve("large-uploads");
            Files.createDirectories(uploadDir);

            // Act - Upload with streaming
            UploadResult result;
            long startTime = System.currentTimeMillis();

            try (InputStream inputStream = Files.newInputStream(largeFile);
                 BufferedInputStream bufferedInput = new BufferedInputStream(inputStream, 8192)) {
                result = fileUploadService.uploadFile(bufferedInput, uploadDir.toString(), fileName);
            }

            long uploadTime = System.currentTimeMillis() - startTime;

            // Assert
            assertTrue(result.isSuccess(), "Large file upload should succeed");
            assertTrue(result.getFileSize() > 500000, "Should handle large file size");
            assertTrue(uploadTime < 5000, "Upload should complete in reasonable time");

            // Verify file integrity
            assertTrue(Files.exists(Paths.get(result.getUploadedFilePath())), "Large file should exist");
            long uploadedSize = Files.size(Paths.get(result.getUploadedFilePath()));
            assertEquals(Files.size(largeFile), uploadedSize, "File sizes should match");
        }

        @ParameterizedTest
        @ValueSource(strings = {"image.jpg", "document.pdf", "archive.zip", "text.txt", "video.mp4"})
        @DisplayName("Should validate different file types during upload")
        void shouldValidateDifferentFileTypesDuringUpload(String fileName) throws IOException {
            // Arrange
            String fileContent = createMockFileContent(fileName);
            Path sourceFile = tempDir.resolve(fileName);
            Files.write(sourceFile, fileContent.getBytes(StandardCharsets.UTF_8));

            Path uploadDir = tempDir.resolve("typed-uploads");
            Files.createDirectories(uploadDir);

            // Act
            UploadResult result;
            try (InputStream inputStream = Files.newInputStream(sourceFile)) {
                result = fileUploadService.uploadFile(inputStream, uploadDir.toString(), fileName);
            }

            // Assert
            if (isAllowedFileType(fileName)) {
                assertTrue(result.isSuccess(), "Allowed file type should upload successfully: " + fileName);
                assertNotNull(result.getMimeType(), "MIME type should be detected");
                assertTrue(result.getFileSize() > 0, "File size should be recorded");
            } else {
                assertFalse(result.isSuccess(), "Disallowed file type should be rejected: " + fileName);
                assertNotNull(result.getErrorMessage(), "Should have error message");
                assertTrue(result.getErrorMessage().contains("type"), "Error should mention file type");
            }
        }

        @Test
        @DisplayName("Should prevent path traversal attacks in file upload")
        void shouldPreventPathTraversalAttacksInFileUpload() throws IOException {
            // Arrange - Malicious file names
            String[] maliciousNames = {
                "../../../etc/passwd",
                "..\\..\\windows\\system32\\config\\sam",
                "legitimate-file.jpg/../../../sensitive-file.txt",
                "normal.txt\u0000malicious.exe",
                "file with spaces/../traverse.txt"
            };

            String fileContent = "Mock file content";
            Path uploadDir = tempDir.resolve("secure-uploads");
            Files.createDirectories(uploadDir);

            for (String maliciousName : maliciousNames) {
                // Act & Assert
                Path sourceFile = tempDir.resolve("temp-file.txt");
                Files.write(sourceFile, fileContent.getBytes(StandardCharsets.UTF_8));

                try (InputStream inputStream = Files.newInputStream(sourceFile)) {
                    UploadResult result = fileUploadService.uploadFile(inputStream, uploadDir.toString(), maliciousName);

                    // Should either reject the file or sanitize the path
                    if (result.isSuccess()) {
                        String uploadedPath = result.getUploadedFilePath();
                        assertTrue(uploadedPath.contains(uploadDir.toString()),
                            "Uploaded file should stay within upload directory");
                        assertFalse(uploadedPath.contains(".."), "Path should not contain traversal sequences");
                    } else {
                        assertNotNull(result.getErrorMessage(), "Should provide security error message");
                    }
                }
            }
        }

        private String createMockFileContent(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "jpg":
                case "jpeg":
                    return "\u00FF\u00D8\u00FF\u00E0"; // JPEG header
                case "png":
                    return "\u0089PNG\r\n\u001A\n"; // PNG header
                case "pdf":
                    return "%PDF-1.4"; // PDF header
                case "zip":
                    return "PK\u0003\u0004"; // ZIP header
                case "mp4":
                    return "\u0000\u0000\u0000\u0020ftypmp42"; // MP4 header
                default:
                    return "Mock content for " + fileName;
            }
        }

        private boolean isAllowedFileType(String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            return Arrays.asList("jpg", "jpeg", "png", "gif", "pdf", "txt", "zip", "mp4", "avi").contains(extension);
        }
    }

    /**
     * Tests for TemplateProcessor I/O operations.
     */
    @Nested
    @DisplayName("Template Processor I/O Tests")
    class TemplateProcessorIOTests {

        @Test
        @DisplayName("Should process templates with proper I/O operations")
        void shouldProcessTemplatesWithProperIOOperations() throws IOException {
            // Arrange
            String templateContent = """
                <!DOCTYPE html>
                <html>
                <head><title>${title}</title></head>
                <body>
                    <h1>${heading}</h1>
                    <p>${content}</p>
                    <footer>Created by: ${author}</footer>
                </body>
                </html>
                """;

            Path templateFile = tempDir.resolve("template.html");
            Files.write(templateFile, templateContent.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> variables = new HashMap<>();
            variables.put("title", "Test Page");
            variables.put("heading", "Welcome to Our Site");
            variables.put("content", "This is the main content of the page.");
            variables.put("author", "Test Author");

            // Act
            String processedContent = templateProcessor.processTemplate(templateFile.toString(), variables);

            // Assert
            assertNotNull(processedContent, "Processed content should not be null");
            assertTrue(processedContent.contains("Test Page"), "Should replace title variable");
            assertTrue(processedContent.contains("Welcome to Our Site"), "Should replace heading variable");
            assertTrue(processedContent.contains("This is the main content"), "Should replace content variable");
            assertTrue(processedContent.contains("Test Author"), "Should replace author variable");
            assertFalse(processedContent.contains("${"), "Should not contain unprocessed variables");
        }

        @Test
        @DisplayName("Should handle template caching and I/O optimization")
        void shouldHandleTemplateCachingAndIOOptimization() throws IOException {
            // Arrange
            String templateContent = "Cached template: ${message} - ${timestamp}";
            Path templateFile = tempDir.resolve("cached-template.txt");
            Files.write(templateFile, templateContent.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> variables1 = Map.of("message", "First", "timestamp", "2024-01-01");
            Map<String, Object> variables2 = Map.of("message", "Second", "timestamp", "2024-01-02");

            // Act - Process same template multiple times
            long startTime = System.currentTimeMillis();
            String result1 = templateProcessor.processTemplate(templateFile.toString(), variables1);
            long firstProcessTime = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            String result2 = templateProcessor.processTemplate(templateFile.toString(), variables2);
            long secondProcessTime = System.currentTimeMillis() - startTime;

            // Assert
            assertTrue(result1.contains("First"), "First processing should work");
            assertTrue(result2.contains("Second"), "Second processing should work");

            // Second processing should be faster due to caching (template parsing cached)
            assertTrue(secondProcessTime <= firstProcessTime,
                "Cached processing should not be slower than initial processing");
        }

        @Test
        @DisplayName("Should handle different character encodings in templates")
        void shouldHandleDifferentCharacterEncodingsInTemplates() throws IOException {
            // Arrange - Template with various Unicode characters
            String templateContent = """
                Multi-language template:
                English: ${english}
                Français: ${french}
                Español: ${spanish}
                中文: ${chinese}
                العربية: ${arabic}
                Emoji: ${emoji}
                """;

            Path templateFile = tempDir.resolve("unicode-template.txt");
            Files.write(templateFile, templateContent.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> variables = new HashMap<>();
            variables.put("english", "Hello World");
            variables.put("french", "Bonjour le monde");
            variables.put("spanish", "Hola Mundo");
            variables.put("chinese", "你好世界");
            variables.put("arabic", "مرحبا بالعالم");
            variables.put("emoji", "🌍🌎🌏");

            // Act
            String processedContent = templateProcessor.processTemplate(templateFile.toString(), variables);

            // Assert
            assertNotNull(processedContent, "Should process Unicode template");
            assertTrue(processedContent.contains("Hello World"), "Should handle English");
            assertTrue(processedContent.contains("Bonjour le monde"), "Should handle French");
            assertTrue(processedContent.contains("Hola Mundo"), "Should handle Spanish");
            assertTrue(processedContent.contains("你好世界"), "Should handle Chinese");
            assertTrue(processedContent.contains("مرحبا بالعالم"), "Should handle Arabic");
            assertTrue(processedContent.contains("🌍🌎🌏"), "Should handle emoji");

            // Verify content can be written back with proper encoding
            Path outputFile = tempDir.resolve("unicode-output.txt");
            Files.write(outputFile, processedContent.getBytes(StandardCharsets.UTF_8));
            String readBack = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertEquals(processedContent, readBack, "Unicode content should round-trip correctly");
        }

        @Test
        @DisplayName("Should handle template I/O errors gracefully")
        void shouldHandleTemplateIOErrorsGracefully() {
            // Test non-existent template file
            assertThrows(Exception.class, () -> {
                templateProcessor.processTemplate("/non/existent/template.html", Map.of());
            }, "Should throw exception for non-existent template");

            // Test template with unreadable permissions (simulated)
            assertDoesNotThrow(() -> {
                // This should be handled gracefully with appropriate error message
                try {
                    templateProcessor.processTemplate("", Map.of());
                } catch (Exception e) {
                    assertTrue(e.getMessage().contains("template") || e.getMessage().contains("file"),
                        "Error message should be user-friendly");
                }
            });
        }
    }

    /**
     * Tests for ConfigurationManager I/O operations.
     */
    @Nested
    @DisplayName("Configuration Manager I/O Tests")
    class ConfigurationManagerIOTests {

        @Test
        @DisplayName("Should load and save properties configuration files")
        void shouldLoadAndSavePropertiesConfigurationFiles() throws IOException {
            // Arrange - Properties format
            String propertiesContent = """
                # Application Configuration
                app.name=JavaCMS
                app.version=1.0.0
                app.timeout=30000
                app.debug=true

                # Database Settings
                db.host=localhost
                db.port=5432
                db.name=cms_db
                """;

            Path configFile = tempDir.resolve("app.properties");
            Files.write(configFile, propertiesContent.getBytes(StandardCharsets.UTF_8));

            // Act - Load configuration
            configurationManager.loadConfiguration(configFile.toString());

            // Assert - Verify loaded properties
            assertEquals("JavaCMS", configurationManager.getProperty("app.name"), "Should load app name");
            assertEquals("1.0.0", configurationManager.getProperty("app.version"), "Should load version");
            assertEquals("30000", configurationManager.getProperty("app.timeout"), "Should load timeout");
            assertEquals("true", configurationManager.getProperty("app.debug"), "Should load debug flag");
            assertEquals("localhost", configurationManager.getProperty("db.host"), "Should load DB host");

            // Test configuration updates
            configurationManager.setProperty("app.version", "1.1.0");
            configurationManager.setProperty("new.setting", "test-value");

            // Act - Save updated configuration
            Path updatedConfigFile = tempDir.resolve("updated-app.properties");
            configurationManager.saveConfiguration(updatedConfigFile.toString());

            // Assert - Verify saved content
            String savedContent = Files.readString(updatedConfigFile, StandardCharsets.UTF_8);
            assertTrue(savedContent.contains("app.version=1.1.0"), "Should save updated version");
            assertTrue(savedContent.contains("new.setting=test-value"), "Should save new setting");
        }

        @Test
        @DisplayName("Should handle JSON configuration files")
        void shouldHandleJsonConfigurationFiles() throws IOException {
            // Arrange - JSON format
            String jsonContent = """
                {
                    "application": {
                        "name": "JavaCMS",
                        "version": "1.0.0",
                        "features": ["content-management", "user-auth", "file-upload"]
                    },
                    "database": {
                        "host": "localhost",
                        "port": 5432,
                        "credentials": {
                            "username": "cms_user",
                            "encrypted": true
                        }
                    },
                    "logging": {
                        "level": "INFO",
                        "file": "/var/log/cms.log",
                        "rotate": true
                    }
                }
                """;

            Path jsonConfigFile = tempDir.resolve("config.json");
            Files.write(jsonConfigFile, jsonContent.getBytes(StandardCharsets.UTF_8));

            // Act
            configurationManager.loadConfiguration(jsonConfigFile.toString());

            // Assert - Verify nested property access
            assertEquals("JavaCMS", configurationManager.getProperty("application.name"), "Should load nested app name");
            assertEquals("localhost", configurationManager.getProperty("database.host"), "Should load nested DB host");
            assertEquals("INFO", configurationManager.getProperty("logging.level"), "Should load logging level");

            // Test array handling
            String features = configurationManager.getProperty("application.features");
            assertNotNull(features, "Should load array property");
            assertTrue(features.contains("content-management"), "Should contain feature");
        }

        @Test
        @DisplayName("Should handle XML configuration files")
        void shouldHandleXmlConfigurationFiles() throws IOException {
            // Arrange - XML format
            String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                    <application>
                        <name>JavaCMS</name>
                        <version>1.0.0</version>
                        <description>Content Management System for Java SE</description>
                    </application>
                    <server>
                        <host>localhost</host>
                        <port>8080</port>
                        <ssl enabled="true">
                            <certificate>/path/to/cert.pem</certificate>
                        </ssl>
                    </server>
                    <features>
                        <feature name="content-management" enabled="true"/>
                        <feature name="user-authentication" enabled="true"/>
                        <feature name="file-upload" enabled="true"/>
                    </features>
                </configuration>
                """;

            Path xmlConfigFile = tempDir.resolve("config.xml");
            Files.write(xmlConfigFile, xmlContent.getBytes(StandardCharsets.UTF_8));

            // Act
            configurationManager.loadConfiguration(xmlConfigFile.toString());

            // Assert
            assertEquals("JavaCMS", configurationManager.getProperty("application.name"), "Should load XML app name");
            assertEquals("8080", configurationManager.getProperty("server.port"), "Should load port");
            assertEquals("true", configurationManager.getProperty("server.ssl.enabled"), "Should load SSL setting");

            // Verify proper XML parsing
            String description = configurationManager.getProperty("application.description");
            assertNotNull(description, "Should load description");
            assertTrue(description.contains("Content Management System"), "Should parse full description");
        }

        @Test
        @DisplayName("Should handle configuration hot-reload with file monitoring")
        void shouldHandleConfigurationHotReloadWithFileMonitoring() throws IOException, InterruptedException {
            // Arrange
            String initialContent = "app.setting=initial-value\napp.timeout=5000\n";
            Path configFile = tempDir.resolve("hot-reload.properties");
            Files.write(configFile, initialContent.getBytes(StandardCharsets.UTF_8));

            configurationManager.loadConfiguration(configFile.toString());
            configurationManager.enableHotReload(true);

            // Verify initial state
            assertEquals("initial-value", configurationManager.getProperty("app.setting"), "Should load initial value");

            // Act - Modify configuration file
            String updatedContent = "app.setting=updated-value\napp.timeout=10000\napp.new=added-setting\n";
            Thread.sleep(100); // Ensure file timestamp changes
            Files.write(configFile, updatedContent.getBytes(StandardCharsets.UTF_8));

            // Wait for hot reload to detect changes
            Thread.sleep(1000); // Allow time for file monitoring

            // Assert - Configuration should be automatically reloaded
            assertEquals("updated-value", configurationManager.getProperty("app.setting"),
                "Should reload updated value");
            assertEquals("10000", configurationManager.getProperty("app.timeout"),
                "Should reload timeout value");
            assertEquals("added-setting", configurationManager.getProperty("app.new"),
                "Should load new setting");
        }

        @Test
        @DisplayName("Should handle concurrent configuration access safely")
        void shouldHandleConcurrentConfigurationAccessSafely() throws InterruptedException {
            // Arrange
            String configContent = "test.value=concurrent-safe\ntest.counter=0\n";
            Path configFile = tempDir.resolve("concurrent-config.properties");

            try {
                Files.write(configFile, configContent.getBytes(StandardCharsets.UTF_8));
                configurationManager.loadConfiguration(configFile.toString());
            } catch (IOException e) {
                fail("Failed to set up test configuration: " + e.getMessage());
            }

            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<String> results = Collections.synchronizedList(new ArrayList<>());

            // Act - Concurrent reads and writes
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        // Read operations
                        String value = configurationManager.getProperty("test.value");
                        results.add(value);

                        // Write operations
                        configurationManager.setProperty("test.thread." + threadId, "thread-" + threadId);

                        // Mixed operations
                        String counter = configurationManager.getProperty("test.counter", "0");
                        int newValue = Integer.parseInt(counter) + 1;
                        configurationManager.setProperty("test.counter", String.valueOf(newValue));

                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdown();

            // Assert
            assertEquals(threadCount, results.size(), "Should have results from all threads");
            assertTrue(results.stream().allMatch("concurrent-safe"::equals), "All reads should be consistent");

            // Verify all thread-specific properties were set
            for (int i = 0; i < threadCount; i++) {
                String threadProperty = configurationManager.getProperty("test.thread." + i);
                assertEquals("thread-" + i, threadProperty, "Thread property should be set");
            }
        }
    }

    /**
     * Tests for ContentExporter I/O operations.
     */
    @Nested
    @DisplayName("Content Exporter I/O Tests")
    class ContentExporterIOTests {

        @Test
        @DisplayName("Should export content to XML format with proper I/O")
        void shouldExportContentToXmlFormatWithProperIO() throws IOException {
            // Arrange
            List<Content> contentList = Arrays.asList(
                contentFactory.createContent("ARTICLE", "XML Article", "Article content for XML export", testUser),
                contentFactory.createContent("PAGE", "XML Page", "Page content for XML export", testUser),
                contentFactory.createContent("IMAGE", "XML Image", "/uploads/xml-image.jpg", testUser)
            );

            Path exportFile = tempDir.resolve("content-export.xml");

            // Act
            contentExporter.exportToXml(contentList, exportFile.toString());

            // Assert
            assertTrue(Files.exists(exportFile), "Export file should be created");
            assertTrue(Files.size(exportFile) > 0, "Export file should not be empty");

            String exportedContent = Files.readString(exportFile, StandardCharsets.UTF_8);
            assertTrue(exportedContent.startsWith("<?xml"), "Should be valid XML");
            assertTrue(exportedContent.contains("XML Article"), "Should contain article title");
            assertTrue(exportedContent.contains("XML Page"), "Should contain page title");
            assertTrue(exportedContent.contains("xml-image.jpg"), "Should contain image path");
            assertTrue(exportedContent.contains("</content>"), "Should have proper XML structure");
        }

        @Test
        @DisplayName("Should export content to JSON format with streaming")
        void shouldExportContentToJsonFormatWithStreaming() throws IOException {
            // Arrange - Large dataset for streaming test
            List<Content> largeContentList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Content content = contentFactory.createContent("ARTICLE",
                    "Streaming Article " + i, "Content " + i, testUser);
                largeContentList.add(content);
            }

            Path exportFile = tempDir.resolve("large-content-export.json");

            // Act - Export with streaming
            long startTime = System.currentTimeMillis();
            contentExporter.exportToJson(largeContentList, exportFile.toString());
            long exportTime = System.currentTimeMillis() - startTime;

            // Assert
            assertTrue(Files.exists(exportFile), "Export file should be created");
            assertTrue(Files.size(exportFile) > 50000, "Large export should create substantial file");
            assertTrue(exportTime < 10000, "Streaming export should complete in reasonable time");

            // Verify JSON structure
            String exportedContent = Files.readString(exportFile, StandardCharsets.UTF_8);
            assertTrue(exportedContent.startsWith("["), "Should be JSON array");
            assertTrue(exportedContent.endsWith("]"), "Should close JSON array");
            assertTrue(exportedContent.contains("Streaming Article 0"), "Should contain first item");
            assertTrue(exportedContent.contains("Streaming Article 999"), "Should contain last item");
        }

        @ParameterizedTest
        @MethodSource("provideExportFormats")
        @DisplayName("Should handle different export formats with proper I/O")
        void shouldHandleDifferentExportFormatsWithProperIO(String format, String expectedExtension,
                String expectedContentPattern) throws IOException {
            // Arrange
            List<Content> contentList = Arrays.asList(
                contentFactory.createContent("ARTICLE", "Export Test Article", "Test content", testUser)
            );

            Path exportFile = tempDir.resolve("format-test" + expectedExtension);

            // Act
            switch (format.toUpperCase()) {
                case "XML":
                    contentExporter.exportToXml(contentList, exportFile.toString());
                    break;
                case "JSON":
                    contentExporter.exportToJson(contentList, exportFile.toString());
                    break;
                case "CSV":
                    contentExporter.exportToCsv(contentList, exportFile.toString());
                    break;
                case "TSV":
                    contentExporter.exportToTsv(contentList, exportFile.toString());
                    break;
                default:
                    fail("Unsupported export format: " + format);
            }

            // Assert
            assertTrue(Files.exists(exportFile), "Export file should exist for format: " + format);
            String content = Files.readString(exportFile, StandardCharsets.UTF_8);
            assertTrue(content.contains(expectedContentPattern),
                "Content should match expected pattern for format: " + format);
            assertTrue(content.contains("Export Test Article"),
                "Should contain test article title in format: " + format);
        }

        private static Stream<Arguments> provideExportFormats() {
            return Stream.of(
                Arguments.of("XML", ".xml", "<?xml"),
                Arguments.of("JSON", ".json", "{"),
                Arguments.of("CSV", ".csv", "Export Test Article"),
                Arguments.of("TSV", ".tsv", "Export Test Article")
            );
        }

        @Test
        @DisplayName("Should handle export I/O errors gracefully")
        void shouldHandleExportIOErrorsGracefully() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                contentFactory.createContent("ARTICLE", "Error Test", "Content", testUser)
            );

            // Test export to invalid directory
            String invalidPath = "/invalid/directory/that/does/not/exist/export.xml";

            // Act & Assert
            assertThrows(Exception.class, () -> {
                contentExporter.exportToXml(contentList, invalidPath);
            }, "Should throw exception for invalid export path");

            // Test export with null content list
            assertThrows(Exception.class, () -> {
                contentExporter.exportToJson(null, tempDir.resolve("null-test.json").toString());
            }, "Should throw exception for null content list");
        }
    }

    /**
     * Tests for ContentImporter I/O operations.
     */
    @Nested
    @DisplayName("Content Importer I/O Tests")
    class ContentImporterIOTests {

        @Test
        @DisplayName("Should import content from XML with proper parsing")
        void shouldImportContentFromXmlWithProperParsing() throws IOException {
            // Arrange - XML content for import
            String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <contents>
                    <content>
                        <type>ARTICLE</type>
                        <title>Imported Article</title>
                        <body>This is imported article content</body>
                        <author>import-user</author>
                        <status>PUBLISHED</status>
                    </content>
                    <content>
                        <type>PAGE</type>
                        <title>Imported Page</title>
                        <body><![CDATA[<h1>Page Content</h1><p>HTML content</p>]]></body>
                        <author>import-user</author>
                        <status>DRAFT</status>
                    </content>
                </contents>
                """;

            Path importFile = tempDir.resolve("import-content.xml");
            Files.write(importFile, xmlContent.getBytes(StandardCharsets.UTF_8));

            // Act
            List<Content> importedContent = contentImporter.importFromXml(importFile.toString(), testUser);

            // Assert
            assertEquals(2, importedContent.size(), "Should import 2 content items");

            Content article = importedContent.stream()
                .filter(c -> c.getTitle().equals("Imported Article"))
                .findFirst().orElse(null);
            assertNotNull(article, "Should import article");
            assertEquals("This is imported article content", article.getBody(), "Should parse article content");

            Content page = importedContent.stream()
                .filter(c -> c.getTitle().equals("Imported Page"))
                .findFirst().orElse(null);
            assertNotNull(page, "Should import page");
            assertTrue(page.getBody().contains("<h1>Page Content</h1>"), "Should parse CDATA content");
        }

        @Test
        @DisplayName("Should import content from JSON with validation")
        void shouldImportContentFromJsonWithValidation() throws IOException {
            // Arrange - JSON content with various data types
            String jsonContent = """
                [
                    {
                        "type": "ARTICLE",
                        "title": "JSON Article",
                        "content": "Article from JSON import",
                        "author": "json-user",
                        "tags": ["java", "cms", "import"],
                        "metadata": {
                            "category": "technical",
                            "priority": "high"
                        }
                    },
                    {
                        "type": "IMAGE",
                        "title": "JSON Image",
                        "content": "/uploads/json-image.png",
                        "author": "json-user",
                        "fileSize": 204800,
                        "mimeType": "image/png"
                    }
                ]
                """;

            Path importFile = tempDir.resolve("import-content.json");
            Files.write(importFile, jsonContent.getBytes(StandardCharsets.UTF_8));

            // Act
            List<Content> importedContent = contentImporter.importFromJson(importFile.toString(), testUser);

            // Assert
            assertEquals(2, importedContent.size(), "Should import 2 content items");

            Content article = importedContent.stream()
                .filter(c -> c.getTitle().equals("JSON Article"))
                .findFirst().orElse(null);
            assertNotNull(article, "Should import JSON article");
            assertEquals("Article from JSON import", article.getBody(), "Should parse article content");

            Content image = importedContent.stream()
                .filter(c -> c.getTitle().equals("JSON Image"))
                .findFirst().orElse(null);
            assertNotNull(image, "Should import JSON image");
            assertTrue(image.getBody().contains("json-image.png"), "Should parse image path");
        }

        @Test
        @DisplayName("Should handle CSV import with proper delimiter parsing")
        void shouldHandleCsvImportWithProperDelimiterParsing() throws IOException {
            // Arrange - CSV content with various data scenarios
            String csvContent = """
                type,title,content,author,status
                ARTICLE,"CSV Article","Simple article content",csv-user,DRAFT
                PAGE,"CSV Page with comma","Page content with quotes",csv-user,PUBLISHED
                ARTICLE,"Multi-line Article","Line 1 Line 2 Line 3",csv-user,DRAFT
                IMAGE,"CSV Image","/uploads/csv-image.jpg",csv-user,PUBLISHED
                """;

            Path importFile = tempDir.resolve("import-content.csv");
            Files.write(importFile, csvContent.getBytes(StandardCharsets.UTF_8));

            // Act
            List<Content> importedContent = contentImporter.importFromCsv(importFile.toString(), testUser);

            // Assert
            assertEquals(4, importedContent.size(), "Should import 4 content items");

            // Test comma handling in title
            Content commaContent = importedContent.stream()
                .filter(c -> c.getTitle().equals("CSV Page with, comma"))
                .findFirst().orElse(null);
            assertNotNull(commaContent, "Should handle commas in CSV fields");

            // Test quote handling
            assertTrue(commaContent.getBody().contains("quotes"), "Should handle quoted content");

            // Test multi-line content
            Content multilineContent = importedContent.stream()
                .filter(c -> c.getTitle().equals("Multi-line Article"))
                .findFirst().orElse(null);
            assertNotNull(multilineContent, "Should import multi-line content");
            assertTrue(multilineContent.getBody().contains("Line 1"), "Should preserve line breaks");
        }

        @Test
        @DisplayName("Should validate imported content and handle errors")
        void shouldValidateImportedContentAndHandleErrors() throws IOException {
            // Arrange - Invalid content for testing validation
            String invalidXmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <contents>
                    <content>
                        <type>INVALID_TYPE</type>
                        <title></title>
                        <body>Content with invalid type</body>
                    </content>
                    <content>
                        <type>ARTICLE</type>
                        <title>Valid Article</title>
                        <body>Valid article content</body>
                        <author>valid-user</author>
                    </content>
                </contents>
                """;

            Path importFile = tempDir.resolve("invalid-import.xml");
            Files.write(importFile, invalidXmlContent.getBytes(StandardCharsets.UTF_8));

            // Act
            List<Content> importedContent = contentImporter.importFromXml(importFile.toString(), testUser);

            // Assert - Should skip invalid entries and import valid ones
            assertEquals(1, importedContent.size(), "Should import only valid content");
            assertEquals("Valid Article", importedContent.get(0).getTitle(), "Should import valid article");

            // Verify import validation report
            ImportResult importResult = contentImporter.getLastImportResult();
            assertNotNull(importResult, "Should have import result");
            assertEquals(1, importResult.getSuccessfulImports(), "Should report successful imports");
            assertEquals(1, importResult.getFailedImports(), "Should report failed imports");
            assertTrue(importResult.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("INVALID_TYPE")), "Should report validation errors");
        }
    }

    /**
     * Tests for IOUtils advanced I/O operations.
     */
    @Nested
    @DisplayName("IOUtils Advanced I/O Tests")
    class IOUtilsAdvancedIOTests {

        @Test
        @DisplayName("Should handle file compression and decompression")
        void shouldHandleFileCompressionAndDecompression() throws IOException {
            // Arrange
            String originalContent = "This is test content for compression testing. ".repeat(100);
            Path originalFile = tempDir.resolve("original.txt");
            Files.write(originalFile, originalContent.getBytes(StandardCharsets.UTF_8));

            Path compressedFile = tempDir.resolve("compressed.gz");
            Path decompressedFile = tempDir.resolve("decompressed.txt");

            // Act - Compress
            ioUtils.compressFile(originalFile.toString(), compressedFile.toString());

            // Assert compression
            assertTrue(Files.exists(compressedFile), "Compressed file should exist");
            long originalSize = Files.size(originalFile);
            long compressedSize = Files.size(compressedFile);
            assertTrue(compressedSize < originalSize, "Compressed file should be smaller");

            // Act - Decompress
            ioUtils.decompressFile(compressedFile.toString(), decompressedFile.toString());

            // Assert decompression
            assertTrue(Files.exists(decompressedFile), "Decompressed file should exist");
            String decompressedContent = Files.readString(decompressedFile, StandardCharsets.UTF_8);
            assertEquals(originalContent, decompressedContent, "Content should match after round-trip");
            assertEquals(Files.size(originalFile), Files.size(decompressedFile),
                "File sizes should match after decompression");
        }

        @Test
        @DisplayName("Should calculate file checksums for integrity verification")
        void shouldCalculateFileChecksumsForIntegrityVerification() throws IOException {
            // Arrange
            String testContent = "Checksum test content with specific data for verification";
            Path testFile = tempDir.resolve("checksum-test.txt");
            Files.write(testFile, testContent.getBytes(StandardCharsets.UTF_8));

            // Act
            String md5Checksum = ioUtils.calculateMD5Checksum(testFile.toString());
            String sha1Checksum = ioUtils.calculateSHA1Checksum(testFile.toString());
            String sha256Checksum = ioUtils.calculateSHA256Checksum(testFile.toString());

            // Assert
            assertNotNull(md5Checksum, "MD5 checksum should be calculated");
            assertNotNull(sha1Checksum, "SHA1 checksum should be calculated");
            assertNotNull(sha256Checksum, "SHA256 checksum should be calculated");

            assertEquals(32, md5Checksum.length(), "MD5 checksum should be 32 characters");
            assertEquals(40, sha1Checksum.length(), "SHA1 checksum should be 40 characters");
            assertEquals(64, sha256Checksum.length(), "SHA256 checksum should be 64 characters");

            assertTrue(md5Checksum.matches("[a-f0-9]+"), "MD5 should be hex string");
            assertTrue(sha1Checksum.matches("[a-f0-9]+"), "SHA1 should be hex string");
            assertTrue(sha256Checksum.matches("[a-f0-9]+"), "SHA256 should be hex string");

            // Verify consistency
            String secondMd5 = ioUtils.calculateMD5Checksum(testFile.toString());
            assertEquals(md5Checksum, secondMd5, "Checksum should be consistent");
        }

        @Test
        @DisplayName("Should handle asynchronous file operations")
        void shouldHandleAsynchronousFileOperations() throws Exception {
            // Arrange
            String sourceContent = "Async file operation test content";
            Path sourceFile = tempDir.resolve("async-source.txt");
            Files.write(sourceFile, sourceContent.getBytes(StandardCharsets.UTF_8));

            Path asyncCopyFile = tempDir.resolve("async-copy.txt");

            // Act - Asynchronous file copy
            CompletableFuture<Boolean> copyFuture = ioUtils.copyFileAsync(
                sourceFile.toString(), asyncCopyFile.toString());

            // Assert
            Boolean copyResult = copyFuture.get(5, TimeUnit.SECONDS);
            assertTrue(copyResult, "Async copy should succeed");
            assertTrue(Files.exists(asyncCopyFile), "Async copied file should exist");

            String copiedContent = Files.readString(asyncCopyFile, StandardCharsets.UTF_8);
            assertEquals(sourceContent, copiedContent, "Async copied content should match");

            // Test multiple async operations
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Path targetFile = tempDir.resolve("async-copy-" + i + ".txt");
                CompletableFuture<Boolean> future = ioUtils.copyFileAsync(
                    sourceFile.toString(), targetFile.toString());
                futures.add(future);
            }

            // Wait for all async operations
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            allOf.get(10, TimeUnit.SECONDS);

            // Verify all files were created
            for (int i = 0; i < 5; i++) {
                Path targetFile = tempDir.resolve("async-copy-" + i + ".txt");
                assertTrue(Files.exists(targetFile), "Async file " + i + " should exist");
            }
        }

        @Test
        @DisplayName("Should handle batch file operations efficiently")
        void shouldHandleBatchFileOperationsEfficiently() throws IOException {
            // Arrange - Create multiple files for batch processing
            List<Path> sourceFiles = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                String content = "Batch file content " + i + ": " + "data ".repeat(100);
                Path file = tempDir.resolve("batch-source-" + i + ".txt");
                Files.write(file, content.getBytes(StandardCharsets.UTF_8));
                sourceFiles.add(file);
            }

            Path batchDir = tempDir.resolve("batch-processed");
            Files.createDirectories(batchDir);

            // Act - Batch processing
            long startTime = System.currentTimeMillis();
            BatchProcessResult result = ioUtils.processBatchFiles(
                sourceFiles.stream().map(Path::toString).collect(ArrayList::new, ArrayList::add, ArrayList::addAll),
                batchDir.toString()
            );
            long processingTime = System.currentTimeMillis() - startTime;

            // Assert
            assertEquals(50, result.getProcessedCount(), "Should process all files");
            assertEquals(0, result.getFailedCount(), "Should have no failures");
            assertTrue(processingTime < 5000, "Batch processing should be efficient");

            // Verify processed files
            for (int i = 0; i < 50; i++) {
                Path processedFile = batchDir.resolve("processed-batch-source-" + i + ".txt");
                assertTrue(Files.exists(processedFile), "Processed file " + i + " should exist");

                String content = Files.readString(processedFile, StandardCharsets.UTF_8);
                assertTrue(content.contains("Batch file content " + i), "Content should be preserved");
            }
        }

        @Test
        @DisplayName("Should handle resource cleanup and error recovery")
        void shouldHandleResourceCleanupAndErrorRecovery() throws IOException {
            // Test resource cleanup with try-with-resources
            Path testFile = tempDir.resolve("resource-test.txt");
            String testContent = "Resource management test content";

            // Act - Proper resource management
            assertDoesNotThrow(() -> {
                try (BufferedWriter writer = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8);
                     PrintWriter printWriter = new PrintWriter(writer)) {
                    printWriter.println(testContent);
                    printWriter.flush();
                } // Resources automatically closed here
            }, "Resource cleanup should not throw exceptions");

            // Verify file was written correctly
            assertTrue(Files.exists(testFile), "File should exist after resource cleanup");
            String readContent = Files.readString(testFile, StandardCharsets.UTF_8).trim();
            assertEquals(testContent, readContent, "Content should be written correctly");

            // Test error recovery
            Path nonExistentFile = tempDir.resolve("non-existent-directory/file.txt");

            assertThrows(Exception.class, () -> {
                ioUtils.copyFileWithRecovery(testFile.toString(), nonExistentFile.toString());
            }, "Should throw exception for invalid path");

            // Test recovery with backup
            Path backupFile = tempDir.resolve("backup.txt");
            Files.write(backupFile, "backup content".getBytes(StandardCharsets.UTF_8));

            boolean recovered = ioUtils.recoverFromBackup(nonExistentFile.toString(), backupFile.toString());
            assertTrue(recovered, "Should recover from backup");
        }
    }

    // Helper classes and methods

    /**
     * Mock import result for testing.
     */
    private static class ImportResult {
        private int successfulImports = 0;
        private int failedImports = 0;
        private List<String> errorMessages = new ArrayList<>();

        public int getSuccessfulImports() { return successfulImports; }
        public int getFailedImports() { return failedImports; }
        public List<String> getErrorMessages() { return errorMessages; }

        public void addSuccess() { successfulImports++; }
        public void addFailure(String error) {
            failedImports++;
            errorMessages.add(error);
        }
    }

    /**
     * Mock batch process result for testing.
     */
    private static class BatchProcessResult {
        private int processedCount = 0;
        private int failedCount = 0;
        private List<String> errors = new ArrayList<>();

        public int getProcessedCount() { return processedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getErrors() { return errors; }

        public void addProcessed() { processedCount++; }
        public void addFailed(String error) {
            failedCount++;
            errors.add(error);
        }
    }
}
