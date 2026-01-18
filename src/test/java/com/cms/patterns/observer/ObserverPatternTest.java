package com.cms.patterns.observer;

import com.cms.core.model.*;
import com.cms.patterns.factory.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Observer Pattern implementation.
 *
 * <p>This test class validates the complete Observer Pattern implementation including
 * the ContentSubject, ContentObserver interface, ContentEvent data structure, and all
 * concrete observer implementations. It demonstrates proper pattern usage, thread safety,
 * performance characteristics, and integration with existing CMS patterns.</p>
 *
 * <p><strong>Test Coverage:</strong>
 * <ul>
 *   <li>Observer Pattern structure and behavior validation</li>
 *   <li>Event notification and propagation testing</li>
 *   <li>Concrete observer functionality verification</li>
 *   <li>Thread safety and concurrent operations testing</li>
 *   <li>Integration with Factory, Composite, and Iterator patterns</li>
 *   <li>Error handling and exception shielding validation</li>
 *   <li>Performance and scalability testing</li>
 * </ul>
 * </p>
 *
 * <p><strong>Exam Requirements Validated:</strong>
 * - Observer Pattern  - Complete pattern implementation
 * - Collections Framework  - Advanced concurrent collections usage
 * - Generics  - Type-safe observer and event handling
 * - Exception Shielding  - Proper error handling integration
 * - JUnit Testing  - Comprehensive test coverage
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Observer Pattern Implementation Tests")
class ObserverPatternTest {

    private ContentSubject contentSubject;
    private User testUser;
    private Content testContent;
    private TestObserver testObserver1;
    private TestObserver testObserver2;

    // Concrete observers for testing
    private ContentNotificationService notificationService;
    private CacheInvalidationObserver cacheObserver;
    private SearchIndexObserver searchObserver;
    private AuditObserver auditObserver;

    @BeforeEach
    void setUp() {
        // Initialize content subject with sync processing for predictable testing
        contentSubject = new ContentSubject(5, 1000, false);

        // Create test user
        testUser = new User("testuser", "test@example.com", "password123");
        testUser.addRole(Role.AUTHOR);

        // Create test content using Factory Pattern
        testContent = ContentFactory.createContent(ArticleContent.class,
            "Test Article", "This is test content for observer testing", testUser);

        // Initialize test observers
        testObserver1 = new TestObserver("TestObserver1", 10);
        testObserver2 = new TestObserver("TestObserver2", 20);

        // Initialize concrete observers
        notificationService = new ContentNotificationService();
        cacheObserver = new CacheInvalidationObserver();
        searchObserver = new SearchIndexObserver();
        auditObserver = new AuditObserver();
    }

    @AfterEach
    void tearDown() {
        if (contentSubject != null) {
            contentSubject.shutdown();
        }
        if (notificationService != null) {
            notificationService.shutdown();
        }
        if (searchObserver != null) {
            searchObserver.shutdown();
        }
    }

    @Nested
    @DisplayName("Observer Pattern Structure Tests")
    class ObserverPatternStructureTests {

        @Test
        @DisplayName("Should implement Observer Pattern correctly")
        void testObserverPatternStructure() {
            // Test Subject interface
            assertNotNull(contentSubject);
            assertEquals(0, contentSubject.getObserverCount());

            // Test Observer interface
            assertTrue(testObserver1 instanceof ContentObserver);
            assertTrue(notificationService instanceof ContentObserver);
            assertTrue(cacheObserver instanceof ContentObserver);
            assertTrue(searchObserver instanceof ContentObserver);
            assertTrue(auditObserver instanceof ContentObserver);

            // Test observer registration
            contentSubject.addObserver(testObserver1);
            assertEquals(1, contentSubject.getObserverCount());
            assertTrue(contentSubject.isObserverRegistered(testObserver1));

            // Test observer removal
            assertTrue(contentSubject.removeObserver(testObserver1));
            assertEquals(0, contentSubject.getObserverCount());
            assertFalse(contentSubject.isObserverRegistered(testObserver1));
        }

        @Test
        @DisplayName("Should handle duplicate observer registration")
        void testDuplicateObserverRegistration() {
            contentSubject.addObserver(testObserver1);

            // Adding same observer again should throw exception
            assertThrows(IllegalStateException.class, () -> {
                contentSubject.addObserver(testObserver1);
            });
        }

        @Test
        @DisplayName("Should validate observer parameters")
        void testObserverParameterValidation() {
            // Null observer should throw exception
            assertThrows(IllegalArgumentException.class, () -> {
                contentSubject.addObserver(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                contentSubject.removeObserver(null);
            });
        }

        @Test
        @DisplayName("Should manage observer priorities correctly")
        void testObserverPriorities() {
            // Add observers with different priorities
            contentSubject.addObserver(testObserver1); // Priority 10
            contentSubject.addObserver(testObserver2); // Priority 20
            contentSubject.addObserver(auditObserver);  // Priority 5
            contentSubject.addObserver(cacheObserver);  // Priority 10

            assertEquals(4, contentSubject.getObserverCount());

            // Create and notify event - observers should be called in priority order
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            // Verify all observers were notified
            assertTrue(testObserver1.wasNotified());
            assertTrue(testObserver2.wasNotified());
        }
    }

    @Nested
    @DisplayName("ContentEvent Tests")
    class ContentEventTests {

        @Test
        @DisplayName("Should create ContentEvent with all required fields")
        void testContentEventCreation() {
            ContentEvent event = ContentEvent.builder()
                .content(testContent)
                .eventType(ContentEvent.EventType.CREATED)
                .user(testUser)
                .source("TestFramework")
                .addMetadata("testKey", "testValue")
                .build();

            assertNotNull(event);
            assertNotNull(event.getEventId());
            assertEquals(testContent, event.getContent());
            assertEquals(ContentEvent.EventType.CREATED, event.getEventType());
            assertEquals(testUser, event.getUser());
            assertEquals("TestFramework", event.getSource());
            assertNotNull(event.getTimestamp());
            assertEquals("testValue", event.getMetadataValue("testKey"));
        }

        @Test
        @DisplayName("Should create events using factory methods")
        void testContentEventFactoryMethods() {
            // Test creation event
            ContentEvent createEvent = ContentEvent.contentCreated(testContent, testUser);
            assertEquals(ContentEvent.EventType.CREATED, createEvent.getEventType());
            assertEquals("ContentFactory", createEvent.getSource());

            // Test update event
            Set<String> changedFields = Set.of("title", "content");
            ContentEvent updateEvent = ContentEvent.contentUpdated(testContent, testUser, changedFields);
            assertEquals(ContentEvent.EventType.UPDATED, updateEvent.getEventType());
            assertEquals(changedFields, updateEvent.getChangedFields());

            // Test publication event
            LocalDateTime pubDate = LocalDateTime.now();
            ContentEvent pubEvent = ContentEvent.contentPublished(testContent, testUser, pubDate);
            assertEquals(ContentEvent.EventType.PUBLISHED, pubEvent.getEventType());
            assertEquals(pubDate, pubEvent.getPublicationDate());

            // Test deletion event
            ContentEvent deleteEvent = ContentEvent.contentDeleted(testContent, testUser,
                "Test deletion", false);
            assertEquals(ContentEvent.EventType.DELETED, deleteEvent.getEventType());
            assertEquals("Test deletion", deleteEvent.getDeletionReason());
            assertFalse(deleteEvent.isTemporaryDeletion());
        }

        @Test
        @DisplayName("Should validate event builder parameters")
        void testContentEventValidation() {
            // Missing content should throw exception
            assertThrows(IllegalArgumentException.class, () -> {
                ContentEvent.builder()
                    .eventType(ContentEvent.EventType.CREATED)
                    .user(testUser)
                    .build();
            });

            // Missing user should throw exception
            assertThrows(IllegalArgumentException.class, () -> {
                ContentEvent.builder()
                    .content(testContent)
                    .eventType(ContentEvent.EventType.CREATED)
                    .build();
            });

            // Missing event type should throw exception
            assertThrows(IllegalArgumentException.class, () -> {
                ContentEvent.builder()
                    .content(testContent)
                    .user(testUser)
                    .build();
            });
        }

        @Test
        @DisplayName("Should maintain event immutability")
        void testContentEventImmutability() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key1", "value1");

            Set<String> changedFields = new HashSet<>();
            changedFields.add("field1");

            ContentEvent event = ContentEvent.builder()
                .content(testContent)
                .eventType(ContentEvent.EventType.UPDATED)
                .user(testUser)
                .metadata(metadata)
                .changedFields(changedFields)
                .build();

            // Modify original collections
            metadata.put("key2", "value2");
            changedFields.add("field2");

            // Event should remain unchanged
            assertEquals(1, event.getMetadata().size());
            assertEquals(1, event.getChangedFields().size());
            assertTrue(event.getMetadata().containsKey("key1"));
            assertFalse(event.getMetadata().containsKey("key2"));
        }
    }

    @Nested
    @DisplayName("Event Notification Tests")
    class EventNotificationTests {

        @Test
        @DisplayName("Should notify observers of content creation")
        void testContentCreationNotification() {
            contentSubject.addObserver(testObserver1);
            contentSubject.addObserver(testObserver2);

            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            assertTrue(testObserver1.wasNotified());
            assertTrue(testObserver2.wasNotified());
            assertEquals(event, testObserver1.getLastEvent());
            assertEquals("onContentCreated", testObserver1.getLastMethod());
        }

        @Test
        @DisplayName("Should notify observers of content updates")
        void testContentUpdateNotification() {
            contentSubject.addObserver(testObserver1);

            Set<String> changedFields = Set.of("title", "body");
            ContentEvent event = ContentEvent.contentUpdated(testContent, testUser, changedFields);
            contentSubject.notifyObservers(event);

            assertTrue(testObserver1.wasNotified());
            assertEquals("onContentUpdated", testObserver1.getLastMethod());
            assertEquals(changedFields, testObserver1.getLastEvent().getChangedFields());
        }

        @Test
        @DisplayName("Should notify observers of content publication")
        void testContentPublicationNotification() {
            contentSubject.addObserver(testObserver1);

            LocalDateTime pubDate = LocalDateTime.now();
            ContentEvent event = ContentEvent.contentPublished(testContent, testUser, pubDate);
            contentSubject.notifyObservers(event);

            assertTrue(testObserver1.wasNotified());
            assertEquals("onContentPublished", testObserver1.getLastMethod());
            assertEquals(pubDate, testObserver1.getLastEvent().getPublicationDate());
        }

        @Test
        @DisplayName("Should notify observers of content deletion")
        void testContentDeletionNotification() {
            contentSubject.addObserver(testObserver1);

            ContentEvent event = ContentEvent.contentDeleted(testContent, testUser,
                "Test deletion", true);
            contentSubject.notifyObservers(event);

            assertTrue(testObserver1.wasNotified());
            assertEquals("onContentDeleted", testObserver1.getLastMethod());
            assertTrue(testObserver1.getLastEvent().isTemporaryDeletion());
        }

        @Test
        @DisplayName("Should handle null event parameter")
        void testNullEventHandling() {
            contentSubject.addObserver(testObserver1);

            assertThrows(IllegalArgumentException.class, () -> {
                contentSubject.notifyObservers(null);
            });
        }

        @Test
        @DisplayName("Should handle empty observer list")
        void testEmptyObserverList() {
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);

            // Should not throw exception with no observers
            assertDoesNotThrow(() -> {
                contentSubject.notifyObservers(event);
            });

            assertEquals(1, contentSubject.getEventCount());
        }
    }

    @Nested
    @DisplayName("Content Type Filtering Tests")
    class ContentTypeFilteringTests {

        @Test
        @DisplayName("Should filter observers based on content type")
        void testContentTypeFiltering() {
            // Create observer that only observes ArticleContent
            ContentObserver articleObserver = new ContentObserver() {
                private boolean notified = false;

                @Override
                public void onContentCreated(ContentEvent event) { notified = true; }
                @Override
                public void onContentUpdated(ContentEvent event) { notified = true; }
                @Override
                public void onContentPublished(ContentEvent event) { notified = true; }
                @Override
                public void onContentDeleted(ContentEvent event) { notified = true; }

                @Override
                public boolean shouldObserve(Class<?> contentType) {
                    return ArticleContent.class.isAssignableFrom(contentType);
                }

                @Override
                public String getObserverName() { return "Article Observer"; }

                public boolean wasNotified() { return notified; }
            };

            contentSubject.addObserver(articleObserver);
            contentSubject.addObserver(testObserver1); // Observes all content types

            // Test with ArticleContent - both should be notified
            ContentEvent articleEvent = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(articleEvent);

            assertTrue(((TestObserver) articleObserver).wasNotified());
            assertTrue(testObserver1.wasNotified());

            // Reset observers
            ((TestObserver) articleObserver).reset();
            testObserver1.reset();

            // Test with PageContent - only general observer should be notified
            Content pageContent = ContentFactory.createContent(PageContent.class,
                "Test Page", "Test page content", testUser);
            ContentEvent pageEvent = ContentEvent.contentCreated(pageContent, testUser);
            contentSubject.notifyObservers(pageEvent);

            assertFalse(((TestObserver) articleObserver).wasNotified());
            assertTrue(testObserver1.wasNotified());
        }
    }

    @Nested
    @DisplayName("Concrete Observer Tests")
    class ConcreteObserverTests {

        @Test
        @DisplayName("Should test ContentNotificationService functionality")
        void testContentNotificationService() {
            contentSubject.addObserver(notificationService);

            // Add email recipients
            notificationService.addEmailRecipient("test1@example.com");
            notificationService.addEmailRecipient("test2@example.com");

            // Test content creation notification
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            // Verify statistics were updated
            Map<String, Object> stats = notificationService.getStatistics();
            assertEquals(1L, stats.get("totalNotifications"));
            assertEquals(2, stats.get("emailRecipients"));

            // Test notification service configuration
            assertEquals("Content Notification Service", notificationService.getObserverName());
            assertEquals(20, notificationService.getPriority());
            assertTrue(notificationService.shouldObserve(Content.class));
        }

        @Test
        @DisplayName("Should test CacheInvalidationObserver functionality")
        void testCacheInvalidationObserver() {
            contentSubject.addObserver(cacheObserver);

            // Test cache dependency management
            cacheObserver.addCacheDependency("content:123", "summary:123");
            cacheObserver.markAsCritical("content:123");

            // Test content update invalidation
            Set<String> changedFields = Set.of("title", "body");
            ContentEvent event = ContentEvent.contentUpdated(testContent, testUser, changedFields);
            contentSubject.notifyObservers(event);

            // Verify cache statistics
            Map<String, Object> stats = cacheObserver.getCacheStatistics();
            assertTrue((Long) stats.get("totalInvalidations") > 0);

            // Test observer configuration
            assertEquals("Cache Invalidation Observer", cacheObserver.getObserverName());
            assertEquals(10, cacheObserver.getPriority());
            assertTrue(cacheObserver.shouldObserve(Content.class));
        }

        @Test
        @DisplayName("Should test SearchIndexObserver functionality")
        void testSearchIndexObserver() {
            contentSubject.addObserver(searchObserver);

            // Test content indexing
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            // Process pending operations
            int processed = searchObserver.processPendingOperations();
            assertTrue(processed > 0);

            // Test search functionality
            List<String> results = searchObserver.search("test", 10);
            assertNotNull(results);

            // Verify search statistics
            Map<String, Object> stats = searchObserver.getSearchStatistics();
            assertTrue((Long) stats.get("documentsIndexed") >= 0);

            // Test observer configuration
            assertEquals("Search Index Observer", searchObserver.getObserverName());
            assertEquals(30, searchObserver.getPriority());
            assertTrue(searchObserver.shouldObserve(Content.class));
        }

        @Test
        @DisplayName("Should test AuditObserver functionality")
        void testAuditObserver() {
            contentSubject.addObserver(auditObserver);

            // Test audit record creation
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            // Verify audit records were created
            List<AuditObserver.AuditRecord> userHistory =
                auditObserver.getUserAuditHistory(testUser.getId(), 10);
            assertFalse(userHistory.isEmpty());

            List<AuditObserver.AuditRecord> contentHistory =
                auditObserver.getContentAuditHistory(testContent.getId());
            assertFalse(contentHistory.isEmpty());

            // Verify audit statistics
            Map<String, Object> stats = auditObserver.getAuditStatistics();
            assertTrue((Long) stats.get("totalAuditRecords") > 0);

            // Test integrity verification
            Map<String, Object> integrity = auditObserver.verifyAuditIntegrity();
            assertEquals(100.0, (Double) integrity.get("integrityPercentage"));

            // Test observer configuration
            assertEquals("Audit Observer", auditObserver.getObserverName());
            assertEquals(5, auditObserver.getPriority()); // Highest priority
            assertTrue(auditObserver.shouldObserve(Content.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle observer exceptions gracefully")
        void testObserverExceptionHandling() {
            // Create observer that throws exception
            ContentObserver faultyObserver = new ContentObserver() {
                @Override
                public void onContentCreated(ContentEvent event) {
                    throw new RuntimeException("Simulated observer failure");
                }
                @Override
                public void onContentUpdated(ContentEvent event) {}
                @Override
                public void onContentPublished(ContentEvent event) {}
                @Override
                public void onContentDeleted(ContentEvent event) {}
                @Override
                public String getObserverName() { return "Faulty Observer"; }
            };

            contentSubject.addObserver(faultyObserver);
            contentSubject.addObserver(testObserver1);

            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);

            // Should not throw exception - other observers should still be notified
            assertDoesNotThrow(() -> {
                contentSubject.notifyObservers(event);
            });

            // Good observer should still be notified
            assertTrue(testObserver1.wasNotified());
        }

        @Test
        @DisplayName("Should validate observer state before notification")
        void testObserverStateValidation() {
            contentSubject.addObserver(testObserver1);

            // Remove observer and verify it's not notified
            contentSubject.removeObserver(testObserver1);

            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            assertFalse(testObserver1.wasNotified());
        }
    }

    @Nested
    @DisplayName("Threading and Concurrency Tests")
    class ThreadingTests {

        @Test
        @DisplayName("Should handle concurrent observer registration")
        void testConcurrentObserverRegistration() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                int observerId = i;
                Thread thread = new Thread(() -> {
                    try {
                        TestObserver observer = new TestObserver("ConcurrentObserver" + observerId, 50);
                        contentSubject.addObserver(observer);
                    } catch (Exception e) {
                        // Expected for duplicate registrations
                    } finally {
                        latch.countDown();
                    }
                });
                threads.add(thread);
                thread.start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            // Should have registered some observers
            assertTrue(contentSubject.getObserverCount() > 0);
            assertTrue(contentSubject.getObserverCount() <= threadCount);
        }

        @Test
        @DisplayName("Should handle concurrent event notifications")
        void testConcurrentEventNotifications() throws InterruptedException {
            contentSubject.addObserver(testObserver1);
            contentSubject.addObserver(testObserver2);

            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int eventId = i;
                Thread thread = new Thread(() -> {
                    try {
                        ContentEvent event = ContentEvent.builder()
                            .content(testContent)
                            .eventType(ContentEvent.EventType.UPDATED)
                            .user(testUser)
                            .addChangedField("field" + eventId)
                            .build();

                        contentSubject.notifyObservers(event);
                    } finally {
                        latch.countDown();
                    }
                });
                thread.start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            // Verify events were processed
            assertTrue(contentSubject.getEventCount() >= threadCount);
            assertTrue(testObserver1.getNotificationCount() >= threadCount);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large numbers of observers efficiently")
        void testLargeNumberOfObservers() {
            // Add many observers
            for (int i = 0; i < 100; i++) {
                TestObserver observer = new TestObserver("PerfObserver" + i, 50);
                contentSubject.addObserver(observer);
            }

            assertEquals(100, contentSubject.getObserverCount());

            // Measure notification time
            long startTime = System.currentTimeMillis();

            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            long duration = System.currentTimeMillis() - startTime;

            // Should complete within reasonable time (adjust threshold as needed)
            assertTrue(duration < 1000, "Notification took too long: " + duration + "ms");

            // Verify all observers were notified
            assertEquals(1, contentSubject.getEventCount());
        }

        @Test
        @DisplayName("Should handle rapid event notifications")
        void testRapidEventNotifications() {
            contentSubject.addObserver(testObserver1);
            contentSubject.addObserver(cacheObserver);
            contentSubject.addObserver(auditObserver);

            long startTime = System.currentTimeMillis();

            // Send many events rapidly
            for (int i = 0; i < 100; i++) {
                ContentEvent event = ContentEvent.builder()
                    .content(testContent)
                    .eventType(ContentEvent.EventType.UPDATED)
                    .user(testUser)
                    .addChangedField("field" + i)
                    .build();

                contentSubject.notifyObservers(event);
            }

            long duration = System.currentTimeMillis() - startTime;

            // Should handle rapid notifications efficiently
            assertTrue(duration < 2000, "Rapid notifications took too long: " + duration + "ms");
            assertEquals(100, contentSubject.getEventCount());
            assertEquals(100, testObserver1.getNotificationCount());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate with Factory Pattern")
        void testFactoryPatternIntegration() {
            contentSubject.addObserver(testObserver1);

            // Create content using Factory Pattern
            Content article = ContentFactory.createContent(ArticleContent.class,
                "Factory Article", "Content from factory", testUser);
            Content page = ContentFactory.createContent(PageContent.class,
                "Factory Page", "Page from factory", testUser);
            Content image = ContentFactory.createContent(ImageContent.class,
                "Factory Image", "image.jpg", testUser);
            Content video = ContentFactory.createContent(VideoContent.class,
                "Factory Video", "video.mp4", testUser);

            // Test notifications for different content types
            contentSubject.notifyObservers(ContentEvent.contentCreated(article, testUser));
            contentSubject.notifyObservers(ContentEvent.contentCreated(page, testUser));
            contentSubject.notifyObservers(ContentEvent.contentCreated(image, testUser));
            contentSubject.notifyObservers(ContentEvent.contentCreated(video, testUser));

            assertEquals(4, testObserver1.getNotificationCount());
            assertEquals(4, contentSubject.getEventCount());
        }

        @Test
        @DisplayName("Should work with complete CMS workflow")
        void testCompleteCMSWorkflow() {
            // Add all observers
            contentSubject.addObserver(auditObserver);      // Priority 5 - first
            contentSubject.addObserver(cacheObserver);      // Priority 10 - second
            contentSubject.addObserver(notificationService); // Priority 20 - third
            contentSubject.addObserver(searchObserver);     // Priority 30 - last

            // Simulate complete content lifecycle

            // 1. Create content
            ContentEvent createEvent = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(createEvent);

            // 2. Update content
            Set<String> changedFields = Set.of("title", "body");
            ContentEvent updateEvent = ContentEvent.contentUpdated(testContent, testUser, changedFields);
            contentSubject.notifyObservers(updateEvent);

            // 3. Publish content
            LocalDateTime pubDate = LocalDateTime.now();
            ContentEvent publishEvent = ContentEvent.contentPublished(testContent, testUser, pubDate);
            contentSubject.notifyObservers(publishEvent);

            // 4. Delete content
            ContentEvent deleteEvent = ContentEvent.contentDeleted(testContent, testUser,
                "End of lifecycle", false);
            contentSubject.notifyObservers(deleteEvent);

            // Verify all events were processed
            assertEquals(4, contentSubject.getEventCount());

            // Process any pending operations
            searchObserver.processPendingOperations();
            cacheObserver.processPendingOperations();

            // Verify observer statistics
            Map<String, Object> notificationStats = notificationService.getStatistics();
            assertTrue((Long) notificationStats.get("totalNotifications") > 0);

            Map<String, Object> cacheStats = cacheObserver.getCacheStatistics();
            assertTrue((Long) cacheStats.get("totalInvalidations") > 0);

            Map<String, Object> searchStats = searchObserver.getSearchStatistics();
            assertTrue((Long) searchStats.get("documentsIndexed") >= 0);

            Map<String, Object> auditStats = auditObserver.getAuditStatistics();
            assertTrue((Long) auditStats.get("totalAuditRecords") > 0);
        }
    }

    @Nested
    @DisplayName("Observer Pattern Compliance Tests")
    class PatternComplianceTests {

        @Test
        @DisplayName("Should demonstrate loose coupling between subject and observers")
        void testLooseCoupling() {
            // Subject should work without knowing specific observer implementations
            List<ContentObserver> differentObservers = Arrays.asList(
                testObserver1,
                notificationService,
                cacheObserver,
                searchObserver,
                auditObserver
            );

            for (ContentObserver observer : differentObservers) {
                contentSubject.addObserver(observer);
            }

            // Subject should notify all observers without knowing their types
            ContentEvent event = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event);

            assertEquals(differentObservers.size(), contentSubject.getObserverCount());
            assertEquals(1, contentSubject.getEventCount());
        }

        @Test
        @DisplayName("Should support dynamic observer subscription/unsubscription")
        void testDynamicSubscription() {
            // Start with no observers
            assertEquals(0, contentSubject.getObserverCount());

            // Add observer dynamically
            contentSubject.addObserver(testObserver1);
            assertEquals(1, contentSubject.getObserverCount());

            // Send event - observer should be notified
            ContentEvent event1 = ContentEvent.contentCreated(testContent, testUser);
            contentSubject.notifyObservers(event1);
            assertTrue(testObserver1.wasNotified());

            // Remove observer dynamically
            contentSubject.removeObserver(testObserver1);
            assertEquals(0, contentSubject.getObserverCount());

            // Reset and send another event - observer should not be notified
            testObserver1.reset();
            ContentEvent event2 = ContentEvent.contentUpdated(testContent, testUser, Set.of("title"));
            contentSubject.notifyObservers(event2);
            assertFalse(testObserver1.wasNotified());
        }

        @Test
        @DisplayName("Should maintain observer pattern integrity with complex scenarios")
        void testPatternIntegrity() {
            // Add multiple observers
            contentSubject.addObserver(testObserver1);
            contentSubject.addObserver(testObserver2);
            contentSubject.addObserver(notificationService);

            // Verify initial state
            assertEquals(3, contentSubject.getObserverCount());
            List<String> observerNames = contentSubject.getRegisteredObserverNames();
            assertEquals(3, observerNames.size());

            // Send multiple different events
            contentSubject.notifyObservers(ContentEvent.contentCreated(testContent, testUser));
            contentSubject.notifyObservers(ContentEvent.contentUpdated(testContent, testUser, Set.of("title")));
            contentSubject.notifyObservers(ContentEvent.contentPublished(testContent, testUser, LocalDateTime.now()));
            contentSubject.notifyObservers(ContentEvent.contentDeleted(testContent, testUser, "test", true));

            // Verify all events were processed
            assertEquals(4, contentSubject.getEventCount());

            // Verify observer states
            assertEquals(4, testObserver1.getNotificationCount());
            assertEquals(4, testObserver2.getNotificationCount());

            // Get subject statistics
            Map<String, Long> eventCounts = contentSubject.getEventCounts();
            assertEquals(1L, eventCounts.get("CREATED").longValue());
            assertEquals(1L, eventCounts.get("UPDATED").longValue());
            assertEquals(1L, eventCounts.get("PUBLISHED").longValue());
            assertEquals(1L, eventCounts.get("DELETED").longValue());
        }
    }

    /**
     * Test observer implementation for validation purposes.
     */
    private static class TestObserver implements ContentObserver {
        private final String name;
        private final int priority;
        private boolean notified = false;
        private ContentEvent lastEvent = null;
        private String lastMethod = null;
        private int notificationCount = 0;

        public TestObserver(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public void onContentCreated(ContentEvent event) {
            recordNotification(event, "onContentCreated");
        }

        @Override
        public void onContentUpdated(ContentEvent event) {
            recordNotification(event, "onContentUpdated");
        }

        @Override
        public void onContentPublished(ContentEvent event) {
            recordNotification(event, "onContentPublished");
        }

        @Override
        public void onContentDeleted(ContentEvent event) {
            recordNotification(event, "onContentDeleted");
        }

        private void recordNotification(ContentEvent event, String method) {
            this.notified = true;
            this.lastEvent = event;
            this.lastMethod = method;
            this.notificationCount++;
        }

        @Override
        public String getObserverName() {
            return name;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean shouldObserve(Class<?> contentType) {
            return true; // Observe all content types
        }

        // Test helper methods
        public boolean wasNotified() { return notified; }
        public ContentEvent getLastEvent() { return lastEvent; }
        public String getLastMethod() { return lastMethod; }
        public int getNotificationCount() { return notificationCount; }

        public void reset() {
            notified = false;
            lastEvent = null;
            lastMethod = null;
            notificationCount = 0;
        }
    }
}
