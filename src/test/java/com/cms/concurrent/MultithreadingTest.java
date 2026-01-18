package com.cms.concurrent;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.ArticleContent;
import com.cms.core.model.PageContent;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.patterns.observer.ContentObserver;
import com.cms.io.TemplateProcessor;
import com.cms.core.exception.ContentManagementException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for multithreading features in JavaCMS.
 *
 * <p>This test suite validates all multithreading components including ThreadPoolManager,
 * AsyncContentProcessor, ConcurrentContentRepository, ScheduledTaskService, EventProcessingService,
 * and integration with existing patterns like Observer and Strategy patterns.</p>
 *
 * <p><strong>Multithreading:</strong> Comprehensive testing of all concurrent
 * programming features including thread-safe operations, CompletableFuture usage,
 * Producer-Consumer patterns, scheduled tasks, concurrent collections, and
 * performance validation under load.</p>
 *
 * <p><strong>Test Categories:</strong></p>
 * <ul>
 *   <li><strong>Thread Pool Management:</strong> ThreadPoolManager functionality</li>
 *   <li><strong>Async Processing:</strong> CompletableFuture-based operations</li>
 *   <li><strong>Concurrent Repository:</strong> Thread-safe CRUD operations</li>
 *   <li><strong>Scheduled Tasks:</strong> Background task execution</li>
 *   <li><strong>Producer-Consumer:</strong> Event processing patterns</li>
 *   <li><strong>Integration Testing:</strong> Cross-component multithreading</li>
 *   <li><strong>Performance Testing:</strong> Load testing and concurrent operations</li>
 * </ul>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class MultithreadingTest {

    private ThreadPoolManager threadPoolManager;
    private AsyncContentProcessor contentProcessor;
    private ConcurrentContentRepository repository;
    private ScheduledTaskService scheduledService;
    private EventProcessingService eventService;
    private ContentSubject contentSubject;
    private TemplateProcessor templateProcessor;

    // Test data and utilities
    private final List<Content> testContent = new ArrayList<>();
    private final AtomicInteger testCounter = new AtomicInteger(0);
    private final AtomicLong operationCounter = new AtomicLong(0);

    @BeforeAll
    void setUpAll() {
        // Initialize multithreading components
        threadPoolManager = ThreadPoolManager.getInstance();
        templateProcessor = new TemplateProcessor();
        contentSubject = new ContentSubject();
        contentProcessor = new AsyncContentProcessor(contentSubject, templateProcessor);
        repository = new ConcurrentContentRepository();
        scheduledService = new ScheduledTaskService(repository, contentProcessor);
        eventService = new EventProcessingService(4);

        // Generate test content
        generateTestContent();

        // Start services
        scheduledService.start();
        eventService.start();
    }

    @AfterAll
    void tearDownAll() {
        // Clean shutdown
        scheduledService.stop();
        eventService.stop();
        threadPoolManager.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Reset counters
        testCounter.set(0);
        operationCounter.set(0);
    }

    // ====================================
    // Thread Pool Manager Tests
    // ====================================

    @Test
    @DisplayName("ThreadPoolManager - Concurrent Task Submission")
    void testThreadPoolManagerConcurrentTasks() throws Exception {
        int numTasks = 100;
        CountDownLatch latch = new CountDownLatch(numTasks);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        // Submit concurrent content processing tasks
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            CompletableFuture<Integer> future = threadPoolManager.submitContentProcessingTask(() -> {
                Thread.sleep(10); // Simulate processing
                latch.countDown();
                return taskId * 2;
            });
            futures.add(future);
        }

        // Wait for all tasks to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS));

        // Verify all results
        for (int i = 0; i < futures.size(); i++) {
            Integer result = futures.get(i).get(5, TimeUnit.SECONDS);
            assertEquals(i * 2, result);
        }

        // Verify thread pool statistics
        Map<String, Object> stats = threadPoolManager.getThreadPoolStatistics();
        assertNotNull(stats);
        assertTrue((Long) stats.get("totalTasksCompleted") >= numTasks);
    }

    @Test
    @DisplayName("ThreadPoolManager - I/O Task Processing")
    void testThreadPoolManagerIOTasks() throws Exception {
        int numTasks = 50;
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // Submit I/O operations
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            CompletableFuture<String> future = threadPoolManager.submitIOTask(() -> {
                Thread.sleep(20); // Simulate I/O
                return "IO-Task-" + taskId;
            });
            futures.add(future);
        }

        // Collect results
        List<String> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        assertEquals(numTasks, results.size());
        assertTrue(results.contains("IO-Task-0"));
        assertTrue(results.contains("IO-Task-" + (numTasks - 1)));
    }

    @Test
    @DisplayName("ThreadPoolManager - Scheduled Task Execution")
    void testThreadPoolManagerScheduledTasks() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);

        // Schedule recurring task
        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(
            () -> executionCount.incrementAndGet(),
            100, 200, TimeUnit.MILLISECONDS
        );

        // Wait for multiple executions
        Thread.sleep(1000);
        future.cancel(false);

        // Verify task executed multiple times
        assertTrue(executionCount.get() >= 3);
    }

    // ====================================
    // Async Content Processor Tests
    // ====================================

    @Test
    @DisplayName("AsyncContentProcessor - Single Content Processing")
    void testAsyncContentProcessorSingle() throws Exception {
        Content content = testContent.get(0);

        CompletableFuture<AsyncContentProcessor.ProcessingResult> future =
            contentProcessor.processContentAsync(content);

        AsyncContentProcessor.ProcessingResult result = future.get(10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getProcessingId());
        assertTrue(result.getProcessingTime() > 0);
        assertEquals(ContentStatus.PUBLISHED, content.getStatus());
    }

    @Test
    @DisplayName("AsyncContentProcessor - Batch Processing")
    void testAsyncContentProcessorBatch() throws Exception {
        List<Content> batchContent = testContent.subList(0, 20);

        CompletableFuture<List<AsyncContentProcessor.ProcessingResult>> future =
            contentProcessor.processBatchAsync(batchContent);

        List<AsyncContentProcessor.ProcessingResult> results = future.get(30, TimeUnit.SECONDS);

        assertNotNull(results);
        assertEquals(batchContent.size(), results.size());

        long successfulResults = results.stream()
            .mapToLong(r -> r.isSuccess() ? 1 : 0)
            .sum();

        assertTrue(successfulResults >= batchContent.size() * 0.8); // At least 80% success
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20})
    @DisplayName("AsyncContentProcessor - Concurrent Processing")
    void testAsyncContentProcessorConcurrent(int concurrencyLevel) throws Exception {
        List<CompletableFuture<AsyncContentProcessor.ProcessingResult>> futures = new ArrayList<>();

        // Submit concurrent processing tasks
        for (int i = 0; i < concurrencyLevel; i++) {
            Content content = testContent.get(i % testContent.size());
            CompletableFuture<AsyncContentProcessor.ProcessingResult> future =
                contentProcessor.processContentAsync(content);
            futures.add(future);
        }

        // Wait for completion
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allOf.get(60, TimeUnit.SECONDS);

        // Verify all completed successfully
        List<AsyncContentProcessor.ProcessingResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        assertEquals(concurrencyLevel, results.size());

        long successCount = results.stream()
            .mapToLong(r -> r.isSuccess() ? 1 : 0)
            .sum();

        assertTrue(successCount >= concurrencyLevel * 0.9); // At least 90% success
    }

    // ====================================
    // Concurrent Repository Tests
    // ====================================

    @Test
    @DisplayName("ConcurrentRepository - Thread-Safe CRUD Operations")
    void testConcurrentRepositoryCRUD() throws Exception {
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        // Submit concurrent CRUD operations
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Future<Integer> future = executor.submit(() -> {
                int operations = 0;
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        // Create content
                        Content content = ContentFactory.createArticle(
                            "Article-" + threadId + "-" + i,
                            "Content body for thread " + threadId + " operation " + i,
                            "test-user"
                        );

                        // Save content (thread-safe)
                        Content saved = repository.save(content);
                        assertNotNull(saved);
                        operations++;

                        // Read content (thread-safe)
                        Optional<Content> retrieved = repository.findById(saved.getId());
                        assertTrue(retrieved.isPresent());
                        operations++;

                        // Update content (thread-safe)
                        saved.setTitle("Updated-" + saved.getTitle());
                        Content updated = repository.save(saved);
                        assertEquals("Updated-Article-" + threadId + "-" + i, updated.getTitle());
                        operations++;

                        // Periodic cleanup to avoid memory issues
                        if (i % 20 == 0) {
                            repository.delete(saved.getId());
                        }
                    }
                } finally {
                    latch.countDown();
                }
                return operations;
            });
            futures.add(future);
        }

        // Wait for completion
        assertTrue(latch.await(120, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify all operations completed
        int totalOperations = 0;
        for (Future<Integer> future : futures) {
            totalOperations += future.get();
        }

        assertEquals(numThreads * operationsPerThread * 3, totalOperations);

        // Verify repository statistics
        Map<String, Object> stats = repository.getStatistics();
        assertTrue((Long) stats.get("totalReads") > 0);
        assertTrue((Long) stats.get("totalWrites") > 0);
    }

    @Test
    @DisplayName("ConcurrentRepository - Concurrent Search Operations")
    void testConcurrentRepositorySearch() throws Exception {
        // Pre-populate repository
        List<Content> savedContent = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Content content = ContentFactory.createArticle(
                "SearchArticle-" + i,
                "Search content body " + i,
                "search-user"
            );
            savedContent.add(repository.save(content));
        }

        int numSearchThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numSearchThreads);
        CountDownLatch latch = new CountDownLatch(numSearchThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        // Concurrent search operations
        for (int t = 0; t < numSearchThreads; t++) {
            Future<Integer> future = executor.submit(() -> {
                int searchResults = 0;
                try {
                    // Search by status
                    List<Content> published = repository.findByStatus(ContentStatus.DRAFT);
                    searchResults += published.size();

                    // Search by author
                    List<Content> byAuthor = repository.findByAuthor("search-user");
                    searchResults += byAuthor.size();

                    // Predicate search
                    List<Content> filtered = repository.search(content ->
                        content.getTitle().contains("SearchArticle")
                    );
                    searchResults += filtered.size();

                } finally {
                    latch.countDown();
                }
                return searchResults;
            });
            futures.add(future);
        }

        // Wait and verify
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        for (Future<Integer> future : futures) {
            assertTrue(future.get() > 0);
        }

        // Cleanup
        for (Content content : savedContent) {
            repository.delete(content.getId());
        }
    }

    // ====================================
    // Event Processing Service Tests
    // ====================================

    @Test
    @DisplayName("EventProcessingService - Producer-Consumer Pattern")
    void testEventProcessingServiceProducerConsumer() throws Exception {
        AtomicInteger processedEvents = new AtomicInteger(0);

        // Register test observer
        ContentObserver testObserver = new ContentObserver() {
            @Override
            public void onContentEvent(ContentEvent event) {
                processedEvents.incrementAndGet();
            }

            @Override
            public String getObserverName() { return "TestObserver"; }
            @Override
            public int getPriority() { return 1; }
            @Override
            public boolean shouldObserve(Class<?> contentType) { return true; }
        };

        eventService.registerObserver(testObserver);

        // Produce events with different priorities
        int numEvents = 50;
        for (int i = 0; i < numEvents; i++) {
            Content content = testContent.get(i % testContent.size());
            ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.CREATED)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

            EventProcessingService.EventPriority priority =
                (i % 3 == 0) ? EventProcessingService.EventPriority.HIGH :
                EventProcessingService.EventPriority.NORMAL;

            eventService.produceEvent(event, priority);
        }

        // Wait for processing
        Thread.sleep(5000);

        // Verify events were processed
        assertTrue(processedEvents.get() >= numEvents * 0.8); // Allow for some processing delay

        // Verify statistics
        Map<String, Object> stats = eventService.getStatistics();
        assertTrue((Long) stats.get("totalEventsProduced") >= numEvents);
    }

    @Test
    @DisplayName("EventProcessingService - High Throughput Processing")
    void testEventProcessingServiceHighThroughput() throws Exception {
        AtomicLong processedCount = new AtomicLong(0);

        ContentObserver throughputObserver = new ContentObserver() {
            @Override
            public void onContentEvent(ContentEvent event) {
                processedCount.incrementAndGet();
            }

            @Override
            public String getObserverName() { return "ThroughputObserver"; }
            @Override
            public int getPriority() { return 1; }
            @Override
            public boolean shouldObserve(Class<?> contentType) { return true; }
        };

        eventService.registerObserver(throughputObserver);

        // High-throughput event production
        int numEvents = 1000;
        List<ContentEvent> events = new ArrayList<>();

        for (int i = 0; i < numEvents; i++) {
            Content content = testContent.get(i % testContent.size());
            ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.UPDATED)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
            events.add(event);
        }

        // Batch produce events
        eventService.produceEventBatch(events, EventProcessingService.EventPriority.BATCH);

        // Wait for processing
        Thread.sleep(10000);

        // Verify high throughput processing
        long processed = processedCount.get();
        assertTrue(processed >= numEvents * 0.7,
            "Expected at least " + (numEvents * 0.7) + " but got " + processed);

        Map<String, Object> stats = eventService.getStatistics();
        assertTrue((Double) stats.get("processingSuccessRate") > 0.5);
    }

    // ====================================
    // Integration Tests
    // ====================================

    @Test
    @DisplayName("Integration - Observer Pattern with Async Processing")
    void testObserverPatternAsyncIntegration() throws Exception {
        AtomicInteger notificationCount = new AtomicInteger(0);

        // Create test observer
        ContentObserver asyncObserver = new ContentObserver() {
            @Override
            public void onContentEvent(ContentEvent event) {
                notificationCount.incrementAndGet();
            }

            @Override
            public String getObserverName() { return "AsyncObserver"; }
            @Override
            public int getPriority() { return 5; }
            @Override
            public boolean shouldObserve(Class<?> contentType) {
                return ArticleContent.class.isAssignableFrom(contentType);
            }
        };

        contentSubject.registerObserver(asyncObserver);

        // Create and process content asynchronously
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Content content = ContentFactory.createArticle(
                        "AsyncArticle-" + testCounter.getAndIncrement(),
                        "Async content body",
                        "async-user"
                    );

                    // This should trigger async observer notifications
                    ContentEvent event = ContentEvent.builder()
                        .eventType(ContentEvent.EventType.CREATED)
                        .content(content)
                        .timestamp(LocalDateTime.now())
                        .build();

                    contentSubject.notifyObservers(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Wait for all async operations
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allOf.get(30, TimeUnit.SECONDS);

        // Allow time for async notifications
        Thread.sleep(3000);

        // Verify async notifications were received
        assertTrue(notificationCount.get() >= 15); // Allow for some async delays
    }

    @Test
    @DisplayName("Integration - End-to-End Multithreaded Workflow")
    void testEndToEndMultithreadedWorkflow() throws Exception {
        // This test simulates a complete multithreaded workflow:
        // 1. Concurrent content creation
        // 2. Async processing
        // 3. Repository storage
        // 4. Event notifications
        // 5. Scheduled cleanup

        AtomicInteger workflowEvents = new AtomicInteger(0);

        ContentObserver workflowObserver = new ContentObserver() {
            @Override
            public void onContentEvent(ContentEvent event) {
                workflowEvents.incrementAndGet();
            }

            @Override
            public String getObserverName() { return "WorkflowObserver"; }
            @Override
            public int getPriority() { return 1; }
            @Override
            public boolean shouldObserve(Class<?> contentType) { return true; }
        };

        contentSubject.registerObserver(workflowObserver);
        eventService.registerObserver(workflowObserver);

        // Phase 1: Concurrent content creation and processing
        int numWorkflows = 30;
        List<CompletableFuture<String>> workflowFutures = new ArrayList<>();

        for (int i = 0; i < numWorkflows; i++) {
            final int workflowId = i;
            CompletableFuture<String> workflow = CompletableFuture
                .supplyAsync(() -> {
                    // Create content
                    try {
                        Content content = ContentFactory.createPageContent(
                            "WorkflowPage-" + workflowId,
                            "Workflow content " + workflowId,
                            "workflow-user"
                        );
                        return content;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenCompose(content -> {
                    // Async processing
                    return contentProcessor.processContentAsync(content);
                })
                .thenCompose(result -> {
                    // Repository storage
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            if (result.isSuccess()) {
                                // Find the content from the processing result metadata
                                Content content = testContent.get(workflowId % testContent.size());
                                Content saved = repository.save(content);

                                // Fire event notification
                                ContentEvent event = ContentEvent.builder()
                                    .eventType(ContentEvent.EventType.PUBLISHED)
                                    .content(saved)
                                    .timestamp(LocalDateTime.now())
                                    .build();

                                contentSubject.notifyObservers(event);
                                eventService.produceEvent(event, EventProcessingService.EventPriority.NORMAL);

                                return "Workflow-" + workflowId + "-Complete";
                            } else {
                                return "Workflow-" + workflowId + "-Failed";
                            }
                        } catch (Exception e) {
                            return "Workflow-" + workflowId + "-Error";
                        }
                    });
                })
                .orTimeout(60, TimeUnit.SECONDS);

            workflowFutures.add(workflow);
        }

        // Wait for all workflows to complete
        CompletableFuture<Void> allWorkflows = CompletableFuture.allOf(
            workflowFutures.toArray(new CompletableFuture[0])
        );
        allWorkflows.get(120, TimeUnit.SECONDS);

        // Verify workflow completion
        List<String> results = workflowFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        long successfulWorkflows = results.stream()
            .filter(result -> result.contains("Complete"))
            .count();

        assertTrue(successfulWorkflows >= numWorkflows * 0.7,
            "Expected at least " + (numWorkflows * 0.7) + " successful workflows but got " + successfulWorkflows);

        // Allow time for async event processing
        Thread.sleep(5000);

        // Verify events were processed
        assertTrue(workflowEvents.get() > 0);

        // Verify system statistics
        Map<String, Object> threadStats = threadPoolManager.getThreadPoolStatistics();
        Map<String, Object> repoStats = repository.getStatistics();
        Map<String, Object> eventStats = eventService.getStatistics();

        assertTrue((Long) threadStats.get("totalTasksCompleted") > 0);
        assertTrue((Long) repoStats.get("totalWrites") > 0);
        assertTrue((Long) eventStats.get("totalEventsProduced") > 0);
    }

    // ====================================
    // Performance and Load Tests
    // ====================================

    @Test
    @DisplayName("Performance - High Concurrency Load Test")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void testHighConcurrencyLoadTest() throws Exception {
        int numThreads = 20;
        int operationsPerThread = 50;
        ExecutorService loadTestExecutor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        AtomicLong totalOperations = new AtomicLong(0);

        // Submit load test tasks
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            loadTestExecutor.submit(() -> {
                try {
                    startLatch.await(); // Synchronized start

                    for (int i = 0; i < operationsPerThread; i++) {
                        // Mixed operations: repository, processing, events

                        // Repository operation
                        Content content = ContentFactory.createArticle(
                            "LoadTest-" + threadId + "-" + i,
                            "Load test content",
                            "load-user"
                        );
                        repository.save(content);
                        totalOperations.incrementAndGet();

                        // Async processing
                        contentProcessor.processContentAsync(content);
                        totalOperations.incrementAndGet();

                        // Event production
                        ContentEvent event = ContentEvent.builder()
                            .eventType(ContentEvent.EventType.CREATED)
                            .content(content)
                            .timestamp(LocalDateTime.now())
                            .build();
                        eventService.produceEvent(event, EventProcessingService.EventPriority.NORMAL);
                        totalOperations.incrementAndGet();

                        // Brief pause to avoid overwhelming
                        if (i % 10 == 0) {
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception e) {
                    // Continue with other operations
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        assertTrue(completionLatch.await(2, TimeUnit.MINUTES));
        loadTestExecutor.shutdown();

        // Verify high throughput operations completed
        long expectedOperations = (long) numThreads * operationsPerThread * 3;
        assertTrue(totalOperations.get() >= expectedOperations * 0.8,
            "Expected at least " + (expectedOperations * 0.8) + " operations but got " + totalOperations.get());
    }

    // ====================================
    // Helper Methods
    // ====================================

    private void generateTestContent() {
        try {
            for (int i = 0; i < 50; i++) {
                Content article = ContentFactory.createArticle(
                    "Test Article " + i,
                    "This is test article content number " + i,
                    "test-author"
                );
                testContent.add(article);

                Content page = ContentFactory.createPageContent(
                    "Test Page " + i,
                    "This is test page content number " + i,
                    "test-author"
                );
                testContent.add(page);
            }
        } catch (Exception e) {
            fail("Failed to generate test content: " + e.getMessage());
        }
    }
}
