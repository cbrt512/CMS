package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.User;
import com.cms.core.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.stream.Stream;

/**
 * Comprehensive JUnit test suite for the Factory Pattern implementation.
 *
 * <p>
 * This test class validates the complete Factory Pattern implementation
 * , testing all four content types and
 * factory operations to ensure proper pattern compliance.
 * </p>
 *
 * <p>
 * <strong>Testing Focus:</strong> JUnit Testing - Factory Pattern validation
 * - Tests all 4 content types (Article, Page, Image, Video)
 * - Validates Factory Pattern compliance and proper object creation
 * - Tests error handling and exception shielding
 * - Verifies type safety and generics usage
 * </p>
 *
 * <p>
 * <strong>Testing Strategy:</strong>
 * - Unit tests for each content type creation
 * - Parameterized tests for multiple content types
 * - Error handling and edge case testing
 * - Factory method validation and type verification
 * - Integration testing with existing patterns
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Factory Pattern Implementation Tests")
public class FactoryPatternTest {

    private ContentFactory contentFactory;
    private User testUser;

    @BeforeEach
    void setUp() {
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
    }

    /**
     * Tests for Article content creation through Factory Pattern.
     */
    @Nested
    @DisplayName("Article Content Factory Tests")
    class ArticleContentTests {

        @Test
        @DisplayName("Should create valid Article content with all properties")
        void shouldCreateValidArticleContent() {
            // Arrange
            String title = "Test Article";
            String content = "This is a test article content.";

            // Act
            Content article = contentFactory.createContent("ARTICLE", title, content, testUser);

            // Assert
            assertNotNull(article, "Article should not be null");
            assertTrue(article instanceof ArticleContent, "Should be ArticleContent instance");
            assertEquals(title, article.getTitle(), "Title should match");
            assertEquals(content, article.getBody(), "Content should match");
            assertEquals(testUser.getUsername(), article.getCreatedBy(), "Creator should match");
            assertEquals(ContentStatus.DRAFT, article.getStatus(), "Initial status should be DRAFT");
            assertNotNull(article.getId(), "Article should have an ID");
            assertNotNull(article.getCreatedDate(), "Article should have creation timestamp");
        }

        @Test
        @DisplayName("Should handle Article-specific properties correctly")
        void shouldHandleArticleSpecificProperties() {
            // Arrange & Act
            ArticleContent article = (ArticleContent) contentFactory.createContent("ARTICLE",
                    "News Article", "Breaking news content", testUser);

            // Assert - ArticleContent specific methods
            assertNotNull(article.getWordCount(), "Word count should be calculated");
            assertTrue(article.getWordCount() > 0, "Word count should be positive");
            assertNotNull(article.getEstimatedReadTime(), "Read time should be calculated");
            assertTrue(article.isPublishable(), "Article should be publishable by default");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   ", "\n\t  " })
        @DisplayName("Should handle empty or whitespace Article content gracefully")
        void shouldHandleEmptyArticleContent(String emptyContent) {
            // Act & Assert
            assertDoesNotThrow(() -> {
                Content article = contentFactory.createContent("ARTICLE", "Title", emptyContent, testUser);
                assertNotNull(article);
                assertNotNull(article.getBody());
            }, "Factory should handle empty content gracefully");
        }
    }

    /**
     * Tests for Page content creation through Factory Pattern.
     */
    @Nested
    @DisplayName("Page Content Factory Tests")
    class PageContentTests {

        @Test
        @DisplayName("Should create valid Page content with navigation properties")
        void shouldCreateValidPageContent() {
            // Arrange
            String title = "About Us";
            String content = "<h1>About Our Company</h1><p>We are a leading...</p>";

            // Act
            Content page = contentFactory.createContent("PAGE", title, content, testUser);

            // Assert
            assertNotNull(page, "Page should not be null");
            assertTrue(page instanceof PageContent, "Should be PageContent instance");
            assertEquals(title, page.getTitle(), "Title should match");
            assertEquals(content, page.getBody(), "Content should match");
            assertEquals(ContentStatus.DRAFT, page.getStatus(), "Initial status should be DRAFT");

            // PageContent specific assertions
            PageContent pageContent = (PageContent) page;
            assertNotNull(pageContent.getSlug(), "Page should have a slug");
            assertFalse(pageContent.getSlug().isEmpty(), "Slug should not be empty");
            assertTrue(pageContent.isNavigationVisible(), "Page should be visible in navigation by default");
        }

        @Test
        @DisplayName("Should generate URL-friendly slugs for Page content")
        void shouldGenerateUrlFriendlySlugs() {
            // Arrange
            String[] titles = {
                    "About Us & Our Team",
                    "Contact Information!",
                    "Privacy Policy - Updated 2024",
                    "FAQ   Multiple   Spaces"
            };

            // Act & Assert
            for (String title : titles) {
                PageContent page = (PageContent) contentFactory.createContent("PAGE", title, "Content", testUser);
                String slug = page.getSlug();

                assertNotNull(slug, "Slug should not be null");
                assertFalse(slug.contains(" "), "Slug should not contain spaces");
                assertFalse(slug.contains("&"), "Slug should not contain special characters");
                assertTrue(slug.matches("^[a-z0-9-]+$"),
                        "Slug should only contain lowercase letters, numbers, and hyphens");
            }
        }

        @Test
        @DisplayName("Should handle Page HTML content validation")
        void shouldHandlePageHtmlContentValidation() {
            // Arrange
            String htmlContent = "<script>alert('xss')</script><p>Safe content</p>";

            // Act
            PageContent page = (PageContent) contentFactory.createContent("PAGE", "Test Page", htmlContent, testUser);

            // Assert
            assertNotNull(page.getBody(), "Content should not be null");
            assertFalse(page.getBody().contains("<script>"), "Script tags should be sanitized");
            assertTrue(page.getBody().contains("<p>"), "Safe HTML tags should be preserved");
        }
    }

    /**
     * Tests for Image content creation through Factory Pattern.
     */
    @Nested
    @DisplayName("Image Content Factory Tests")
    class ImageContentTests {

        @Test
        @DisplayName("Should create valid Image content with metadata")
        void shouldCreateValidImageContent() {
            // Arrange
            String title = "Company Logo";
            String imagePath = "/uploads/images/logo.png";

            // Act
            Content image = contentFactory.createContent("IMAGE", title, imagePath, testUser);

            // Assert
            assertNotNull(image, "Image should not be null");
            assertTrue(image instanceof ImageContent, "Should be ImageContent instance");
            assertEquals(title, image.getTitle(), "Title should match");
            assertEquals(imagePath, image.getBody(), "Image path should match");

            // ImageContent specific assertions
            ImageContent imageContent = (ImageContent) image;
            assertNotNull(imageContent.getFilePath(), "Image should have file path");
            assertNotNull(imageContent.getMimeType(), "Image should have MIME type");
            assertTrue(imageContent.getFileSize() >= 0, "File size should be non-negative");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/uploads/photo.jpg",
                "/uploads/banner.jpeg",
                "/uploads/icon.png",
                "/uploads/graphic.gif",
                "/uploads/image.bmp"
        })
        @DisplayName("Should handle various image file extensions")
        void shouldHandleVariousImageExtensions(String imagePath) {
            // Act
            ImageContent image = (ImageContent) contentFactory.createContent("IMAGE",
                    "Test Image", imagePath, testUser);

            // Assert
            assertNotNull(image.getMimeType(), "MIME type should be detected");
            assertTrue(image.getMimeType().startsWith("image/"), "Should be image MIME type");
            assertEquals(imagePath, image.getFilePath(), "File path should match");
        }

        @Test
        @DisplayName("Should validate image file extensions and paths")
        void shouldValidateImageFileExtensions() {
            // Arrange
            String invalidPath = "/uploads/document.pdf";

            // Act & Assert
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("IMAGE", "Invalid Image", invalidPath, testUser);
            }, "Should throw exception for non-image file");
        }

        @Test
        @DisplayName("Should generate appropriate thumbnails and alt text")
        void shouldGenerateImageMetadata() {
            // Arrange
            String imagePath = "/uploads/products/laptop.jpg";
            String title = "Gaming Laptop Product Image";

            // Act
            ImageContent image = (ImageContent) contentFactory.createContent("IMAGE", title, imagePath, testUser);

            // Assert
            assertNotNull(image.getAltText(), "Alt text should be generated");
            assertFalse(image.getAltText().isEmpty(), "Alt text should not be empty");
            assertNotNull(image.getThumbnailPath(), "Thumbnail path should be generated");
            assertTrue(image.getThumbnailPath().contains("thumb"), "Thumbnail path should contain 'thumb'");
        }
    }

    /**
     * Tests for Video content creation through Factory Pattern.
     */
    @Nested
    @DisplayName("Video Content Factory Tests")
    class VideoContentTests {

        @Test
        @DisplayName("Should create valid Video content with media properties")
        void shouldCreateValidVideoContent() {
            // Arrange
            String title = "Product Demo Video";
            String videoPath = "/uploads/videos/demo.mp4";

            // Act
            Content video = contentFactory.createContent("VIDEO", title, videoPath, testUser);

            // Assert
            assertNotNull(video, "Video should not be null");
            assertTrue(video instanceof VideoContent, "Should be VideoContent instance");
            assertEquals(title, video.getTitle(), "Title should match");
            assertEquals(videoPath, video.getBody(), "Video path should match");

            // VideoContent specific assertions
            VideoContent videoContent = (VideoContent) video;
            assertNotNull(videoContent.getFilePath(), "Video should have file path");
            assertNotNull(videoContent.getMimeType(), "Video should have MIME type");
            assertTrue(videoContent.getFileSize() >= 0, "File size should be non-negative");
            assertTrue(videoContent.getDurationSeconds() >= 0, "Duration should be non-negative");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/uploads/training.mp4",
                "/uploads/presentation.avi",
                "/uploads/webinar.mov",
                "/uploads/tutorial.wmv"
        })
        @DisplayName("Should handle various video file formats")
        void shouldHandleVariousVideoFormats(String videoPath) {
            // Act
            VideoContent video = (VideoContent) contentFactory.createContent("VIDEO",
                    "Test Video", videoPath, testUser);

            // Assert
            assertNotNull(video.getMimeType(), "MIME type should be detected");
            assertTrue(video.getMimeType().startsWith("video/"), "Should be video MIME type");
            assertEquals(videoPath, video.getFilePath(), "File path should match");
            assertNotNull(video.getThumbnailPath(), "Thumbnail should be generated");
        }

        @Test
        @DisplayName("Should validate video file extensions")
        void shouldValidateVideoFileExtensions() {
            // Arrange
            String invalidPath = "/uploads/audio.mp3";

            // Act & Assert
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("VIDEO", "Invalid Video", invalidPath, testUser);
            }, "Should throw exception for non-video file");
        }

        @Test
        @DisplayName("Should handle video metadata and encoding information")
        void shouldHandleVideoMetadata() {
            // Arrange
            String videoPath = "/uploads/training/module1.mp4";
            String title = "Training Module 1 - Introduction";

            // Act
            VideoContent video = (VideoContent) contentFactory.createContent("VIDEO", title, videoPath, testUser);

            // Assert
            assertNotNull(video.getResolution(), "Resolution should be detected");
            assertNotNull(video.getBitrate(), "Bitrate should be detected");
            assertNotNull(video.getCodec(), "Codec should be detected");
            assertTrue(video.getDurationSeconds() >= 0, "Duration should be valid");
            assertNotNull(video.getThumbnailPath(), "Thumbnail should be generated");
        }
    }

    /**
     * Factory Pattern compliance and general functionality tests.
     */
    @Nested
    @DisplayName("Factory Pattern Compliance Tests")
    class FactoryPatternComplianceTests {

        @ParameterizedTest
        @EnumSource(value = ContentType.class, names = { "ARTICLE", "PAGE", "IMAGE", "VIDEO" })
        @DisplayName("Should create all content types through factory method")
        void shouldCreateAllContentTypes(ContentType contentType) {
            // Arrange
            String title = "Test " + contentType.name();
            String content = "Test content for " + contentType.name();

            // Act
            Content result = contentFactory.createContent(contentType.name(), title, content, testUser);

            // Assert
            assertNotNull(result, "Content should not be null");
            assertEquals(title, result.getTitle(), "Title should match");
            assertEquals(testUser.getUsername(), result.getCreatedBy(), "Creator should match");
            assertNotNull(result.getId(), "Content should have an ID");
            assertNotNull(result.getCreatedDate(), "Content should have creation timestamp");
        }

        @Test
        @DisplayName("Should throw exception for unsupported content type")
        void shouldThrowExceptionForUnsupportedContentType() {
            // Act & Assert
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("PODCAST", "Test", "Content", testUser);
            }, "Should throw exception for unsupported content type");
        }

        @Test
        @DisplayName("Should validate required parameters")
        void shouldValidateRequiredParameters() {
            // Test null content type
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent(null, "Title", "Content", testUser);
            }, "Should throw exception for null content type");

            // Test null title
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("ARTICLE", null, "Content", testUser);
            }, "Should throw exception for null title");

            // Test null user
            assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("ARTICLE", "Title", "Content", null);
            }, "Should throw exception for null user");
        }

        @Test
        @DisplayName("Should maintain factory method consistency")
        void shouldMaintainFactoryMethodConsistency() {
            // Arrange
            String title = "Consistency Test";
            String content = "Testing factory consistency";

            // Act - Create multiple instances of same type
            Content article1 = contentFactory.createContent("ARTICLE", title, content, testUser);
            Content article2 = contentFactory.createContent("ARTICLE", title, content, testUser);

            // Assert - Should be different instances but same type
            assertNotSame(article1, article2, "Should create new instances");
            assertEquals(article1.getClass(), article2.getClass(), "Should be same class type");
            assertNotEquals(article1.getId(), article2.getId(), "Should have different IDs");
        }

        @ParameterizedTest
        @MethodSource("provideContentCreationData")
        @DisplayName("Should handle various content creation scenarios")
        void shouldHandleVariousContentCreationScenarios(String type, String title, String content,
                Class<?> expectedClass) {
            // Act
            Content result = contentFactory.createContent(type, title, content, testUser);

            // Assert
            assertNotNull(result, "Content should not be null");
            assertEquals(expectedClass, result.getClass(), "Should create correct content type");
            assertEquals(title, result.getTitle(), "Title should match");
        }

        private static Stream<Arguments> provideContentCreationData() {
            return Stream.of(
                    Arguments.of("ARTICLE", "Blog Post", "Article content", ArticleContent.class),
                    Arguments.of("PAGE", "Landing Page", "<h1>Welcome</h1>", PageContent.class),
                    Arguments.of("IMAGE", "Product Photo", "/uploads/product.jpg", ImageContent.class),
                    Arguments.of("VIDEO", "Tutorial Video", "/uploads/tutorial.mp4", VideoContent.class));
        }
    }

    /**
     * Integration tests with other patterns and systems.
     */
    @Nested
    @DisplayName("Factory Pattern Integration Tests")
    class FactoryIntegrationTests {

        @Test
        @DisplayName("Should integrate with Exception Shielding pattern")
        void shouldIntegrateWithExceptionShielding() {
            // Act & Assert - Test that user-friendly exceptions are thrown
            ContentCreationException exception = assertThrows(ContentCreationException.class, () -> {
                contentFactory.createContent("INVALID_TYPE", "Title", "Content", testUser);
            });

            assertNotNull(exception.getMessage(), "Exception should have user-friendly message");
            assertFalse(exception.getMessage().contains("NullPointerException"),
                    "Should not expose technical details");
            assertTrue(exception.getMessage().contains("content type"),
                    "Should provide context about the error");
        }

        @Test
        @DisplayName("Should work with Collections Framework")
        void shouldWorkWithCollectionsFramework() {
            // Arrange
            java.util.List<Content> contentList = new java.util.ArrayList<>();

            // Act - Create multiple content types
            contentList.add(contentFactory.createContent("ARTICLE", "Article 1", "Content 1", testUser));
            contentList.add(contentFactory.createContent("PAGE", "Page 1", "Content 1", testUser));
            contentList.add(contentFactory.createContent("IMAGE", "Image 1", "/uploads/img1.jpg", testUser));
            contentList.add(contentFactory.createContent("VIDEO", "Video 1", "/uploads/vid1.mp4", testUser));

            // Assert
            assertEquals(4, contentList.size(), "Should have all content types");
            assertTrue(contentList.stream().allMatch(c -> c.getCreatedBy().equals(testUser.getUsername())),
                    "All content should be created by same user");
            assertTrue(contentList.stream().allMatch(c -> c.getStatus() == ContentStatus.DRAFT),
                    "All content should start as draft");
        }

        @Test
        @DisplayName("Should support Generics type safety")
        void shouldSupportGenericsTypeSafety() {
            // Arrange
            java.util.Map<String, Content> contentMap = new java.util.HashMap<>();

            // Act
            Content article = contentFactory.createContent("ARTICLE", "Test Article", "Content", testUser);
            contentMap.put(article.getId(), article);

            // Assert - Type safety verification
            Content retrieved = contentMap.get(article.getId());
            assertNotNull(retrieved, "Content should be retrievable");
            assertEquals(article.getId(), retrieved.getId(), "IDs should match");
            assertTrue(retrieved instanceof ArticleContent, "Type should be preserved");
        }

        @Test
        @DisplayName("Should maintain content status lifecycle")
        void shouldMaintainContentStatusLifecycle() {
            // Act
            Content content = contentFactory.createContent("ARTICLE", "Status Test", "Content", testUser);

            // Assert initial state
            assertEquals(ContentStatus.DRAFT, content.getStatus(), "Should start as DRAFT");

            // Test status transitions
            content.publish();
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(), "Should transition to PUBLISHED");

            content.archive();
            assertEquals(ContentStatus.ARCHIVED, content.getStatus(), "Should transition to ARCHIVED");
        }
    }

    /**
     * Performance and stress tests for factory operations.
     */
    @Nested
    @DisplayName("Factory Pattern Performance Tests")
    class FactoryPerformanceTests {

        @Test
        @DisplayName("Should handle bulk content creation efficiently")
        void shouldHandleBulkContentCreation() {
            // Arrange
            int contentCount = 1000;
            long startTime = System.currentTimeMillis();

            // Act
            java.util.List<Content> contentList = new java.util.ArrayList<>();
            for (int i = 0; i < contentCount; i++) {
                Content content = contentFactory.createContent("ARTICLE",
                        "Article " + i, "Content " + i, testUser);
                contentList.add(content);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assertEquals(contentCount, contentList.size(), "Should create all content");
            assertTrue(duration < 5000, "Should complete within 5 seconds"); // Performance threshold
            assertTrue(contentList.stream().allMatch(c -> c.getId() != null),
                    "All content should have valid IDs");
        }

        @Test
        @DisplayName("Should handle concurrent content creation safely")
        void shouldHandleConcurrentContentCreation() throws InterruptedException {
            // Arrange
            int threadCount = 10;
            int contentPerThread = 100;
            java.util.List<Thread> threads = new java.util.ArrayList<>();
            java.util.concurrent.ConcurrentLinkedQueue<Content> results = new java.util.concurrent.ConcurrentLinkedQueue<>();

            // Act
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < contentPerThread; j++) {
                        Content content = contentFactory.createContent("ARTICLE",
                                "Thread " + threadId + " Article " + j, "Content", testUser);
                        results.add(content);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Assert
            assertEquals(threadCount * contentPerThread, results.size(),
                    "Should create all content from all threads");

            // Verify unique IDs
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (Content content : results) {
                assertTrue(ids.add(content.getId()), "All IDs should be unique");
            }
        }
    }

    // Helper enum for parameterized tests
    private enum ContentType {
        ARTICLE, PAGE, IMAGE, VIDEO
    }
}
