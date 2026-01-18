package com.cms.core;

import com.cms.core.model.*;
import com.cms.core.repository.Repository;
import com.cms.core.repository.RepositoryException;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.composite.Category;
import com.cms.patterns.composite.ContentItem;
import com.cms.patterns.iterator.ContentIterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Comprehensive JUnit test suite for Collections Framework implementation.
 *
 * <p>This test class validates the complete Collections Framework usage,
 * testing all collection types (List, Set, Map, Queue) and their proper usage
 * throughout the CMS application.</p>
 *
 * <p><strong>Testing Focus:</strong> Collections Framework validation
 * - Tests proper usage of List, Set, Map, Queue collections
 * - Validates repository operations with Collections
 * - Tests concurrent collections and thread safety
 * - Verifies collection performance and data integrity
 * - Tests collection operations and transformations</p>
 *
 * <p><strong>Collections Framework Implementation Coverage:</strong>
 * - Repository pattern with generic Collections
 * - Content storage mechanisms using Maps, Lists, Sets
 * - User management collections with thread-safe implementations
 * - Site hierarchy using composite collections
 * - Iterator pattern integration with collections
 * - Performance optimization with appropriate collection types</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Collections Framework Implementation Tests")
public class CollectionsFrameworkTest {

    private ContentFactory contentFactory;
    private User testUser;
    private Site testSite;
    private Repository<Content, String> contentRepository;
    private Repository<User, String> userRepository;

    @BeforeEach
    void setUp() {
        contentFactory = new ContentFactory();
        testUser = new User("testUser", "Test User", "test@example.com", Role.EDITOR);
        testSite = new Site("Test Site", "test-site", testUser);
        contentRepository = new InMemoryContentRepository();
        userRepository = new InMemoryUserRepository();
    }

    /**
     * Tests for List collection usage throughout the application.
     */
    @Nested
    @DisplayName("List Collection Usage Tests")
    class ListCollectionUsageTests {

        @Test
        @DisplayName("Should use ArrayList for dynamic content collections")
        void shouldUseArrayListForDynamicContentCollections() {
            // Arrange
            List<Content> contentList = new ArrayList<>();

            // Act - Add various content types
            contentList.add(contentFactory.createContent("ARTICLE", "Article 1", "Content 1", testUser));
            contentList.add(contentFactory.createContent("PAGE", "Page 1", "Content 1", testUser));
            contentList.add(contentFactory.createContent("IMAGE", "Image 1", "/uploads/img1.jpg", testUser));

            // Assert
            assertEquals(3, contentList.size(), "List should contain all added content");
            assertTrue(contentList instanceof ArrayList, "Should use ArrayList implementation");

            // Test ordering preservation
            assertEquals("Article 1", contentList.get(0).getTitle(), "Should preserve insertion order");
            assertEquals("Page 1", contentList.get(1).getTitle(), "Should preserve insertion order");
            assertEquals("Image 1", contentList.get(2).getTitle(), "Should preserve insertion order");

            // Test list-specific operations
            contentList.add(1, contentFactory.createContent("VIDEO", "Video 1", "/uploads/vid1.mp4", testUser));
            assertEquals(4, contentList.size(), "Should support index-based insertion");
            assertEquals("Video 1", contentList.get(1).getTitle(), "Should insert at correct index");
        }

        @Test
        @DisplayName("Should use LinkedList for frequent insertions and deletions")
        void shouldUseLinkedListForFrequentInsertionsAndDeletions() {
            // Arrange
            LinkedList<User> userQueue = new LinkedList<>();

            // Act - Add users at different positions
            userQueue.addFirst(new User("user1", "User One", "user1@example.com", Role.VIEWER));
            userQueue.addLast(new User("user2", "User Two", "user2@example.com", Role.EDITOR));
            userQueue.add(1, new User("user3", "User Three", "user3@example.com", Role.ADMIN));

            // Assert
            assertEquals(3, userQueue.size(), "LinkedList should contain all users");
            assertEquals("user1", userQueue.getFirst().getUsername(), "Should support first element access");
            assertEquals("user2", userQueue.getLast().getUsername(), "Should support last element access");
            assertEquals("user3", userQueue.get(1).getUsername(), "Should support index-based access");

            // Test LinkedList-specific operations
            User removedFirst = userQueue.removeFirst();
            assertEquals("user1", removedFirst.getUsername(), "Should remove first element");
            assertEquals(2, userQueue.size(), "Size should decrease after removal");
        }

        @Test
        @DisplayName("Should use CopyOnWriteArrayList for thread-safe collections")
        void shouldUseCopyOnWriteArrayListForThreadSafeCollections() {
            // Arrange
            CopyOnWriteArrayList<Content> threadSafeContentList = new CopyOnWriteArrayList<>();
            int threadCount = 10;
            int contentPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();

            // Act - Concurrent additions
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<?> future = executor.submit(() -> {
                    for (int j = 0; j < contentPerThread; j++) {
                        Content content = contentFactory.createContent("ARTICLE",
                            "Thread " + threadId + " Article " + j, "Content", testUser);
                        threadSafeContentList.add(content);
                    }
                });
                futures.add(future);
            }

            // Wait for all threads to complete
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Thread execution should not fail");
                }
            });

            executor.shutdown();

            // Assert
            assertEquals(threadCount * contentPerThread, threadSafeContentList.size(),
                "Should handle concurrent additions correctly");

            // Verify no duplicates based on content uniqueness
            Set<String> titles = threadSafeContentList.stream()
                .map(Content::getTitle)
                .collect(Collectors.toSet());
            assertEquals(threadCount * contentPerThread, titles.size(),
                "All content titles should be unique");
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 100, 1000, 10000})
        @DisplayName("Should handle various list sizes efficiently")
        void shouldHandleVariousListSizesEfficiently(int size) {
            // Arrange
            List<Content> contentList = new ArrayList<>(size);
            long startTime = System.currentTimeMillis();

            // Act
            for (int i = 0; i < size; i++) {
                contentList.add(contentFactory.createContent("ARTICLE",
                    "Article " + i, "Content " + i, testUser));
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assertEquals(size, contentList.size(), "Should contain all elements");
            assertTrue(duration < 5000, "Should complete within reasonable time for size " + size);

            // Test random access performance
            startTime = System.currentTimeMillis();
            for (int i = 0; i < Math.min(size, 1000); i++) {
                int randomIndex = (int) (Math.random() * size);
                assertNotNull(contentList.get(randomIndex), "Should support efficient random access");
            }
            endTime = System.currentTimeMillis();
            assertTrue(endTime - startTime < 100, "Random access should be fast");
        }
    }

    /**
     * Tests for Set collection usage for unique data management.
     */
    @Nested
    @DisplayName("Set Collection Usage Tests")
    class SetCollectionUsageTests {

        @Test
        @DisplayName("Should use HashSet for unique element storage")
        void shouldUseHashSetForUniqueElementStorage() {
            // Arrange
            Set<String> uniqueTags = new HashSet<>();

            // Act - Add tags, including duplicates
            uniqueTags.add("java");
            uniqueTags.add("cms");
            uniqueTags.add("web");
            uniqueTags.add("java"); // Duplicate
            uniqueTags.add("cms");  // Duplicate

            // Assert
            assertEquals(3, uniqueTags.size(), "Set should contain only unique elements");
            assertTrue(uniqueTags.contains("java"), "Should contain java tag");
            assertTrue(uniqueTags.contains("cms"), "Should contain cms tag");
            assertTrue(uniqueTags.contains("web"), "Should contain web tag");

            // Test set operations
            Set<String> additionalTags = Set.of("spring", "hibernate", "java");
            Set<String> allTags = new HashSet<>(uniqueTags);
            allTags.addAll(additionalTags);

            assertEquals(5, allTags.size(), "Union should contain all unique elements");
            assertTrue(allTags.containsAll(uniqueTags), "Union should contain all original tags");
        }

        @Test
        @DisplayName("Should use TreeSet for sorted unique collections")
        void shouldUseTreeSetForSortedUniqueCollections() {
            // Arrange
            Set<String> sortedUsernames = new TreeSet<>();

            // Act - Add usernames in random order
            sortedUsernames.add("zoe");
            sortedUsernames.add("alice");
            sortedUsernames.add("bob");
            sortedUsernames.add("charlie");
            sortedUsernames.add("alice"); // Duplicate

            // Assert
            assertEquals(4, sortedUsernames.size(), "Should contain only unique elements");

            // Test sorted order
            List<String> sortedList = new ArrayList<>(sortedUsernames);
            assertEquals("alice", sortedList.get(0), "Should be sorted alphabetically");
            assertEquals("bob", sortedList.get(1), "Should be sorted alphabetically");
            assertEquals("charlie", sortedList.get(2), "Should be sorted alphabetically");
            assertEquals("zoe", sortedList.get(3), "Should be sorted alphabetically");

            // Test range operations
            NavigableSet<String> navigableSet = (TreeSet<String>) sortedUsernames;
            Set<String> subSet = navigableSet.subSet("b", "d");
            assertEquals(2, subSet.size(), "Subset should contain bob and charlie");
            assertTrue(subSet.contains("bob"), "Subset should contain bob");
            assertTrue(subSet.contains("charlie"), "Subset should contain charlie");
        }

        @Test
        @DisplayName("Should use LinkedHashSet for insertion-ordered unique collections")
        void shouldUseLinkedHashSetForInsertionOrderedUniqueCollections() {
            // Arrange
            Set<Content> uniqueContent = new LinkedHashSet<>();

            // Act - Add content in specific order
            Content article = contentFactory.createContent("ARTICLE", "Article", "Content", testUser);
            Content page = contentFactory.createContent("PAGE", "Page", "Content", testUser);
            Content image = contentFactory.createContent("IMAGE", "Image", "/uploads/img.jpg", testUser);

            uniqueContent.add(article);
            uniqueContent.add(page);
            uniqueContent.add(image);
            uniqueContent.add(article); // Duplicate

            // Assert
            assertEquals(3, uniqueContent.size(), "Should contain only unique elements");

            // Test insertion order preservation
            Iterator<Content> iterator = uniqueContent.iterator();
            assertEquals(article.getId(), iterator.next().getId(), "Should preserve insertion order");
            assertEquals(page.getId(), iterator.next().getId(), "Should preserve insertion order");
            assertEquals(image.getId(), iterator.next().getId(), "Should preserve insertion order");
        }

        @Test
        @DisplayName("Should handle set operations and transformations")
        void shouldHandleSetOperationsAndTransformations() {
            // Arrange
            Set<Role> adminRoles = new HashSet<>(Arrays.asList(Role.ADMIN, Role.EDITOR));
            Set<Role> editorRoles = new HashSet<>(Arrays.asList(Role.EDITOR, Role.VIEWER));

            // Act & Assert - Union
            Set<Role> unionRoles = new HashSet<>(adminRoles);
            unionRoles.addAll(editorRoles);
            assertEquals(3, unionRoles.size(), "Union should contain all unique roles");
            assertTrue(unionRoles.containsAll(Arrays.asList(Role.ADMIN, Role.EDITOR, Role.VIEWER)),
                "Union should contain all roles");

            // Act & Assert - Intersection
            Set<Role> intersectionRoles = new HashSet<>(adminRoles);
            intersectionRoles.retainAll(editorRoles);
            assertEquals(1, intersectionRoles.size(), "Intersection should contain only EDITOR");
            assertTrue(intersectionRoles.contains(Role.EDITOR), "Intersection should contain EDITOR");

            // Act & Assert - Difference
            Set<Role> differenceRoles = new HashSet<>(adminRoles);
            differenceRoles.removeAll(editorRoles);
            assertEquals(1, differenceRoles.size(), "Difference should contain only ADMIN");
            assertTrue(differenceRoles.contains(Role.ADMIN), "Difference should contain ADMIN");
        }
    }

    /**
     * Tests for Map collection usage for key-value data management.
     */
    @Nested
    @DisplayName("Map Collection Usage Tests")
    class MapCollectionUsageTests {

        @Test
        @DisplayName("Should use HashMap for content storage and retrieval")
        void shouldUseHashMapForContentStorageAndRetrieval() {
            // Arrange
            Map<String, Content> contentMap = new HashMap<>();

            // Act - Store content with various keys
            Content article = contentFactory.createContent("ARTICLE", "Article", "Content", testUser);
            Content page = contentFactory.createContent("PAGE", "Page", "Content", testUser);
            Content image = contentFactory.createContent("IMAGE", "Image", "/uploads/img.jpg", testUser);

            contentMap.put(article.getId(), article);
            contentMap.put("page-key", page);
            contentMap.put("image-" + System.currentTimeMillis(), image);

            // Assert
            assertEquals(3, contentMap.size(), "Map should contain all content");
            assertEquals(article, contentMap.get(article.getId()), "Should retrieve by ID");
            assertEquals(page, contentMap.get("page-key"), "Should retrieve by custom key");
            assertTrue(contentMap.containsValue(image), "Should contain image content");

            // Test map operations
            assertTrue(contentMap.containsKey(article.getId()), "Should contain article key");
            assertNull(contentMap.get("non-existent-key"), "Should return null for missing key");

            Content removed = contentMap.remove(article.getId());
            assertEquals(article, removed, "Should return removed content");
            assertEquals(2, contentMap.size(), "Size should decrease after removal");
        }

        @Test
        @DisplayName("Should use ConcurrentHashMap for thread-safe map operations")
        void shouldUseConcurrentHashMapForThreadSafeMapOperations() {
            // Arrange
            ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();
            int threadCount = 10;
            int usersPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();

            // Act - Concurrent map operations
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<?> future = executor.submit(() -> {
                    for (int j = 0; j < usersPerThread; j++) {
                        String username = "thread" + threadId + "_user" + j;
                        User user = new User(username, "User " + j,
                            username + "@example.com", Role.VIEWER);
                        userMap.put(username, user);
                    }
                });
                futures.add(future);
            }

            // Wait for all threads to complete
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    fail("Thread execution should not fail");
                }
            });

            executor.shutdown();

            // Assert
            assertEquals(threadCount * usersPerThread, userMap.size(),
                "Should handle concurrent map operations correctly");

            // Test concurrent read operations
            userMap.forEach((key, value) -> {
                assertNotNull(key, "Key should not be null");
                assertNotNull(value, "Value should not be null");
                assertEquals(key, value.getUsername(), "Key should match username");
            });
        }

        @Test
        @DisplayName("Should use TreeMap for sorted key-value collections")
        void shouldUseTreeMapForSortedKeyValueCollections() {
            // Arrange
            Map<LocalDateTime, Content> timeOrderedContent = new TreeMap<>();

            // Act - Add content with timestamps
            LocalDateTime now = LocalDateTime.now();
            Content oldContent = contentFactory.createContent("ARTICLE", "Old Article", "Content", testUser);
            Content recentContent = contentFactory.createContent("PAGE", "Recent Page", "Content", testUser);
            Content futureContent = contentFactory.createContent("IMAGE", "Future Image", "/uploads/img.jpg", testUser);

            timeOrderedContent.put(now.minusDays(1), oldContent);
            timeOrderedContent.put(now.plusHours(1), futureContent);
            timeOrderedContent.put(now, recentContent);

            // Assert
            assertEquals(3, timeOrderedContent.size(), "Should contain all content");

            // Test sorted order
            List<LocalDateTime> sortedKeys = new ArrayList<>(timeOrderedContent.keySet());
            assertTrue(sortedKeys.get(0).isBefore(sortedKeys.get(1)), "Keys should be sorted");
            assertTrue(sortedKeys.get(1).isBefore(sortedKeys.get(2)), "Keys should be sorted");

            // Test range operations
            NavigableMap<LocalDateTime, Content> navigableMap = (TreeMap<LocalDateTime, Content>) timeOrderedContent;
            Map<LocalDateTime, Content> recentMap = navigableMap.tailMap(now);
            assertEquals(2, recentMap.size(), "Should contain recent and future content");
        }

        @Test
        @DisplayName("Should handle complex map operations and transformations")
        void shouldHandleComplexMapOperationsAndTransformations() {
            // Arrange
            Map<String, List<Content>> contentByCategory = new HashMap<>();

            // Act - Group content by category
            Content article1 = contentFactory.createContent("ARTICLE", "Tech Article", "Content", testUser);
            Content article2 = contentFactory.createContent("ARTICLE", "News Article", "Content", testUser);
            Content page1 = contentFactory.createContent("PAGE", "About Page", "Content", testUser);

            contentByCategory.computeIfAbsent("articles", k -> new ArrayList<>()).add(article1);
            contentByCategory.computeIfAbsent("articles", k -> new ArrayList<>()).add(article2);
            contentByCategory.computeIfAbsent("pages", k -> new ArrayList<>()).add(page1);

            // Assert
            assertEquals(2, contentByCategory.size(), "Should have two categories");
            assertEquals(2, contentByCategory.get("articles").size(), "Articles category should have 2 items");
            assertEquals(1, contentByCategory.get("pages").size(), "Pages category should have 1 item");

            // Test map transformations
            Map<String, Integer> categoryCount = contentByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));

            assertEquals(2, categoryCount.get("articles").intValue(), "Should count articles correctly");
            assertEquals(1, categoryCount.get("pages").intValue(), "Should count pages correctly");
        }
    }

    /**
     * Tests for Queue collection usage for ordered processing.
     */
    @Nested
    @DisplayName("Queue Collection Usage Tests")
    class QueueCollectionUsageTests {

        @Test
        @DisplayName("Should use ArrayDeque for double-ended queue operations")
        void shouldUseArrayDequeForDoubleEndedQueueOperations() {
            // Arrange
            Deque<Content> processingQueue = new ArrayDeque<>();

            // Act - Add content to both ends
            Content urgentContent = contentFactory.createContent("ARTICLE", "Urgent News", "Breaking news", testUser);
            Content normalContent = contentFactory.createContent("PAGE", "Normal Page", "Regular content", testUser);
            Content lowPriorityContent = contentFactory.createContent("IMAGE", "Background Image", "/uploads/bg.jpg", testUser);

            processingQueue.addFirst(urgentContent); // High priority
            processingQueue.addLast(normalContent);  // Normal priority
            processingQueue.addLast(lowPriorityContent); // Low priority

            // Assert
            assertEquals(3, processingQueue.size(), "Queue should contain all content");
            assertEquals(urgentContent, processingQueue.peekFirst(), "Urgent content should be first");
            assertEquals(lowPriorityContent, processingQueue.peekLast(), "Low priority content should be last");

            // Test processing order
            Content processed = processingQueue.removeFirst();
            assertEquals(urgentContent, processed, "Should process urgent content first");
            assertEquals(2, processingQueue.size(), "Queue size should decrease");
        }

        @Test
        @DisplayName("Should use PriorityQueue for priority-based processing")
        void shouldUsePriorityQueueForPriorityBasedProcessing() {
            // Arrange - Priority queue with custom comparator
            PriorityQueue<ContentProcessingTask> taskQueue = new PriorityQueue<>(
                Comparator.comparing(ContentProcessingTask::getPriority)
                    .thenComparing(ContentProcessingTask::getCreatedAt)
            );

            // Act - Add tasks with different priorities
            ContentProcessingTask highPriorityTask = new ContentProcessingTask(
                contentFactory.createContent("ARTICLE", "Critical Update", "Content", testUser),
                TaskPriority.HIGH
            );
            ContentProcessingTask mediumPriorityTask = new ContentProcessingTask(
                contentFactory.createContent("PAGE", "Regular Update", "Content", testUser),
                TaskPriority.MEDIUM
            );
            ContentProcessingTask lowPriorityTask = new ContentProcessingTask(
                contentFactory.createContent("IMAGE", "Optional Image", "/uploads/opt.jpg", testUser),
                TaskPriority.LOW
            );

            taskQueue.offer(mediumPriorityTask);
            taskQueue.offer(lowPriorityTask);
            taskQueue.offer(highPriorityTask);

            // Assert - Should process in priority order
            assertEquals(3, taskQueue.size(), "Queue should contain all tasks");
            assertEquals(highPriorityTask, taskQueue.peek(), "High priority task should be first");

            ContentProcessingTask processedTask = taskQueue.poll();
            assertEquals(highPriorityTask, processedTask, "Should process high priority task first");

            processedTask = taskQueue.poll();
            assertEquals(mediumPriorityTask, processedTask, "Should process medium priority task second");

            processedTask = taskQueue.poll();
            assertEquals(lowPriorityTask, processedTask, "Should process low priority task last");
        }

        @Test
        @DisplayName("Should use BlockingQueue for producer-consumer scenarios")
        void shouldUseBlockingQueueForProducerConsumerScenarios() throws InterruptedException {
            // Arrange
            BlockingQueue<Content> contentQueue = new LinkedBlockingQueue<>(10);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            AtomicInteger producedCount = new AtomicInteger(0);
            AtomicInteger consumedCount = new AtomicInteger(0);
            int totalContent = 20;

            // Act - Producer threads
            for (int i = 0; i < 2; i++) {
                final int producerId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < totalContent / 2; j++) {
                            Content content = contentFactory.createContent("ARTICLE",
                                "Producer " + producerId + " Content " + j, "Content", testUser);
                            contentQueue.put(content); // Blocking put
                            producedCount.incrementAndGet();
                            Thread.sleep(10); // Simulate processing time
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Consumer threads
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        while (consumedCount.get() < totalContent) {
                            Content content = contentQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (content != null) {
                                consumedCount.incrementAndGet();
                                // Simulate content processing
                                Thread.sleep(5);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Should complete within timeout");

            // Assert
            assertEquals(totalContent, producedCount.get(), "Should produce all content");
            assertEquals(totalContent, consumedCount.get(), "Should consume all content");
            assertTrue(contentQueue.isEmpty() || contentQueue.size() < 5, "Queue should be mostly empty");
        }
    }

    /**
     * Tests for Collection integration with other patterns.
     */
    @Nested
    @DisplayName("Collection Integration Tests")
    class CollectionIntegrationTests {

        @Test
        @DisplayName("Should integrate Collections with Repository pattern")
        void shouldIntegrateCollectionsWithRepositoryPattern() {
            // Act - Use repository operations
            Content article = contentFactory.createContent("ARTICLE", "Repository Article", "Content", testUser);
            Content page = contentFactory.createContent("PAGE", "Repository Page", "Content", testUser);

            contentRepository.save(article);
            contentRepository.save(page);

            // Assert
            List<Content> allContent = contentRepository.findAll();
            assertEquals(2, allContent.size(), "Repository should return all content");
            assertTrue(allContent.contains(article), "Should contain saved article");
            assertTrue(allContent.contains(page), "Should contain saved page");

            // Test repository search operations
            Content foundArticle = contentRepository.findById(article.getId());
            assertEquals(article, foundArticle, "Should find content by ID");

            assertTrue(contentRepository.existsById(article.getId()), "Should confirm existence");
            assertEquals(2, contentRepository.count(), "Should count all content");
        }

        @Test
        @DisplayName("Should integrate Collections with Composite pattern")
        void shouldIntegrateCollectionsWithCompositePattern() {
            // Arrange
            Category rootCategory = new Category("Root Category", "Root category description");
            Category subCategory1 = new Category("Sub Category 1", "First subcategory");
            Category subCategory2 = new Category("Sub Category 2", "Second subcategory");

            Content article = contentFactory.createContent("ARTICLE", "Article", "Content", testUser);
            Content page = contentFactory.createContent("PAGE", "Page", "Content", testUser);

            ContentItem articleItem = new ContentItem(article);
            ContentItem pageItem = new ContentItem(page);

            // Act - Build hierarchy using collections
            rootCategory.add(subCategory1);
            rootCategory.add(subCategory2);
            subCategory1.add(articleItem);
            subCategory2.add(pageItem);

            // Assert
            List<SiteComponent> rootChildren = rootCategory.getChildren();
            assertEquals(2, rootChildren.size(), "Root should have 2 subcategories");

            List<SiteComponent> sub1Children = subCategory1.getChildren();
            assertEquals(1, sub1Children.size(), "Sub category 1 should have 1 content item");
            assertEquals(articleItem, sub1Children.get(0), "Should contain article item");

            // Test hierarchical operations with collections
            assertEquals(2, rootCategory.getItemCount(), "Should count all items in hierarchy");
        }

        @Test
        @DisplayName("Should integrate Collections with Iterator pattern")
        void shouldIntegrateCollectionsWithIteratorPattern() {
            // Arrange
            List<Content> contentList = new ArrayList<>();
            contentList.add(contentFactory.createContent("ARTICLE", "Published Article", "Content", testUser));
            contentList.add(contentFactory.createContent("PAGE", "Draft Page", "Content", testUser));
            contentList.add(contentFactory.createContent("IMAGE", "Image Content", "/uploads/img.jpg", testUser));

            // Publish first article
            contentList.get(0).publish();

            // Act - Use iterator with collections
            ContentIterator<Content> publishedIterator = ContentIterator.publishedOnly(contentList);
            ContentIterator<Content> allIterator = ContentIterator.all(contentList);

            // Assert
            List<Content> publishedContent = new ArrayList<>();
            while (publishedIterator.hasNext()) {
                publishedContent.add(publishedIterator.next());
            }
            assertEquals(1, publishedContent.size(), "Should find only published content");
            assertEquals(ContentStatus.PUBLISHED, publishedContent.get(0).getStatus(),
                "Content should be published");

            List<Content> allContent = new ArrayList<>();
            while (allIterator.hasNext()) {
                allContent.add(allIterator.next());
            }
            assertEquals(3, allContent.size(), "Should iterate over all content");
        }

        @Test
        @DisplayName("Should handle complex collection transformations")
        void shouldHandleComplexCollectionTransformations() {
            // Arrange
            List<Content> contentList = Arrays.asList(
                contentFactory.createContent("ARTICLE", "Java Tutorial", "Learn Java", testUser),
                contentFactory.createContent("ARTICLE", "Spring Guide", "Spring Framework", testUser),
                contentFactory.createContent("PAGE", "About Us", "Company info", testUser),
                contentFactory.createContent("PAGE", "Contact", "Contact details", testUser),
                contentFactory.createContent("IMAGE", "Logo", "/uploads/logo.png", testUser)
            );

            // Act - Complex transformations using streams and collections
            Map<String, List<Content>> contentByType = contentList.stream()
                .collect(Collectors.groupingBy(content ->
                    content.getClass().getSimpleName().replace("Content", "").toUpperCase()));

            Map<String, Long> contentCountByType = contentList.stream()
                .collect(Collectors.groupingBy(
                    content -> content.getClass().getSimpleName().replace("Content", "").toUpperCase(),
                    Collectors.counting()));

            Set<String> uniqueCreators = contentList.stream()
                .map(Content::getCreatedBy)
                .collect(Collectors.toSet());

            // Assert
            assertEquals(3, contentByType.size(), "Should group by 3 content types");
            assertEquals(2, contentByType.get("ARTICLE").size(), "Should have 2 articles");
            assertEquals(2, contentByType.get("PAGE").size(), "Should have 2 pages");
            assertEquals(1, contentByType.get("IMAGE").size(), "Should have 1 image");

            assertEquals(2L, contentCountByType.get("ARTICLE").longValue(), "Should count articles");
            assertEquals(2L, contentCountByType.get("PAGE").longValue(), "Should count pages");
            assertEquals(1L, contentCountByType.get("IMAGE").longValue(), "Should count images");

            assertEquals(1, uniqueCreators.size(), "Should have 1 unique creator");
            assertTrue(uniqueCreators.contains(testUser.getUsername()), "Should contain test user");
        }
    }

    /**
     * Performance tests for collection operations.
     */
    @Nested
    @DisplayName("Collection Performance Tests")
    class CollectionPerformanceTests {

        @ParameterizedTest
        @ValueSource(ints = {1000, 10000, 100000})
        @DisplayName("Should handle large collection operations efficiently")
        void shouldHandleLargeCollectionOperationsEfficiently(int size) {
            // Arrange
            List<Content> contentList = new ArrayList<>(size);
            Map<String, Content> contentMap = new HashMap<>(size);
            Set<String> contentIds = new HashSet<>(size);

            long startTime = System.currentTimeMillis();

            // Act - Add large number of elements
            for (int i = 0; i < size; i++) {
                Content content = contentFactory.createContent("ARTICLE",
                    "Article " + i, "Content " + i, testUser);
                contentList.add(content);
                contentMap.put(content.getId(), content);
                contentIds.add(content.getId());
            }

            long addTime = System.currentTimeMillis() - startTime;

            // Test retrieval performance
            startTime = System.currentTimeMillis();
            for (int i = 0; i < Math.min(size, 1000); i++) {
                int randomIndex = (int) (Math.random() * size);

                // List access (O(1) for ArrayList)
                Content fromList = contentList.get(randomIndex);
                assertNotNull(fromList, "Should retrieve from list");

                // Map access (O(1) for HashMap)
                Content fromMap = contentMap.get(fromList.getId());
                assertEquals(fromList, fromMap, "Should retrieve same content from map");

                // Set contains check (O(1) for HashSet)
                assertTrue(contentIds.contains(fromList.getId()), "Should find ID in set");
            }

            long retrievalTime = System.currentTimeMillis() - startTime;

            // Assert performance thresholds
            assertTrue(addTime < size / 10, "Addition should be efficient for size " + size);
            assertTrue(retrievalTime < 1000, "Retrieval should be fast");
        }

        @Test
        @DisplayName("Should demonstrate collection performance characteristics")
        void shouldDemonstrateCollectionPerformanceCharacteristics() {
            int size = 10000;

            // ArrayList vs LinkedList insertion performance
            long startTime = System.currentTimeMillis();
            List<String> arrayList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                arrayList.add("Element " + i);
            }
            long arrayListTime = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            List<String> linkedList = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                linkedList.add("Element " + i);
            }
            long linkedListTime = System.currentTimeMillis() - startTime;

            // HashMap vs TreeMap insertion performance
            startTime = System.currentTimeMillis();
            Map<String, String> hashMap = new HashMap<>();
            for (int i = 0; i < size; i++) {
                hashMap.put("Key " + i, "Value " + i);
            }
            long hashMapTime = System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            Map<String, String> treeMap = new TreeMap<>();
            for (int i = 0; i < size; i++) {
                treeMap.put("Key " + i, "Value " + i);
            }
            long treeMapTime = System.currentTimeMillis() - startTime;

            // Assert performance characteristics
            assertTrue(arrayListTime <= linkedListTime * 2,
                "ArrayList should be comparable to LinkedList for sequential addition");
            assertTrue(hashMapTime <= treeMapTime,
                "HashMap should be faster than TreeMap for insertions");

            System.out.println("Performance Results for " + size + " elements:");
            System.out.println("ArrayList: " + arrayListTime + "ms");
            System.out.println("LinkedList: " + linkedListTime + "ms");
            System.out.println("HashMap: " + hashMapTime + "ms");
            System.out.println("TreeMap: " + treeMapTime + "ms");
        }
    }

    // Helper classes for testing

    /**
     * Task priority enumeration for priority queue testing.
     */
    private enum TaskPriority {
        LOW(3), MEDIUM(2), HIGH(1);

        private final int priority;

        TaskPriority(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Content processing task for queue testing.
     */
    private static class ContentProcessingTask {
        private final Content content;
        private final TaskPriority priority;
        private final LocalDateTime createdAt;

        public ContentProcessingTask(Content content, TaskPriority priority) {
            this.content = content;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
        }

        public Content getContent() { return content; }
        public TaskPriority getPriority() { return priority; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * In-memory repository implementation for testing.
     */
    private static class InMemoryContentRepository implements Repository<Content, String> {
        private final Map<String, Content> storage = new ConcurrentHashMap<>();

        @Override
        public Content save(Content entity) {
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Content findById(String id) {
            return storage.get(id);
        }

        @Override
        public List<Content> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public Content update(Content entity) {
            storage.put(entity.getId(), entity);
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
     * In-memory user repository implementation for testing.
     */
    private static class InMemoryUserRepository implements Repository<User, String> {
        private final Map<String, User> storage = new ConcurrentHashMap<>();

        @Override
        public User save(User entity) {
            storage.put(entity.getUsername(), entity);
            return entity;
        }

        @Override
        public User findById(String id) {
            return storage.get(id);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public User update(User entity) {
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
}
