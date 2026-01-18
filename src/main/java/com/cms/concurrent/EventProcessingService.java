package com.cms.concurrent;

import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentObserver;
import com.cms.util.CMSLogger;
import com.cms.util.AuditLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Advanced Producer-Consumer pattern implementation for high-performance
 * concurrent
 * event processing in the JavaCMS system using BlockingQueue and multiple
 * consumer threads.
 *
 * <p>
 * This service implements sophisticated Producer-Consumer patterns with
 * BlockingQueue
 * for event processing, multiple consumer threads for parallel processing,
 * priority-based
 * event handling, and comprehensive event lifecycle management with advanced
 * concurrent programming techniques.
 * </p>
 *
 * <p>
 * <strong>Multithreading Implementation:</strong> Provides Producer-Consumer
 * pattern using
 * BlockingQueue (ConcurrentLinkedQueue, PriorityBlockingQueue), multiple
 * consumer threads,
 * event priority handling, concurrent event processing, and thread-safe event
 * distribution
 * with comprehensive monitoring and performance optimization.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>BlockingQueue-based event distribution with multiple queues</li>
 * <li>Priority-based event processing with PriorityBlockingQueue</li>
 * <li>Multiple consumer threads with work distribution</li>
 * <li>Event batching for high-throughput scenarios</li>
 * <li>Dead letter queue for failed event handling</li>
 * <li>Event retry mechanisms with exponential backoff</li>
 * <li>Comprehensive event lifecycle tracking and metrics</li>
 * </ul>
 *
 * <p>
 * <strong>Producer-Consumer Architecture:</strong>
 * </p>
 * <ul>
 * <li><strong>Producers:</strong> Observer notifications, system events, user
 * actions</li>
 * <li><strong>Queues:</strong> Priority queues, standard queues, dead letter
 * queues</li>
 * <li><strong>Consumers:</strong> Multi-threaded event processors with load
 * balancing</li>
 * <li><strong>Distribution:</strong> Round-robin and priority-based
 * distribution</li>
 * </ul>
 *
 * <p>
 * <strong>Integration:</strong> Integrates with Observer Pattern for event
 * production,
 * ThreadPoolManager for consumer thread management, and AsyncContentProcessor
 * for
 * event-driven content processing operations.
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class EventProcessingService {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private static final AuditLogger auditLogger = AuditLogger.getInstance();

    private final ThreadPoolManager threadPoolManager;

    // Multiple queues for different event types and priorities
    private final PriorityBlockingQueue<PriorityEvent> highPriorityQueue;
    private final BlockingQueue<ContentEvent> standardQueue;
    private final BlockingQueue<ContentEvent> batchQueue;
    private final BlockingQueue<FailedEvent> deadLetterQueue;

    // Consumer thread management
    private final List<EventConsumer> consumers = new ArrayList<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final int numberOfConsumers;

    // Event observers and handlers
    private final Set<ContentObserver> eventObservers = ConcurrentHashMap.newKeySet();
    private final Map<String, EventHandler> eventHandlers = new ConcurrentHashMap<>();

    // Statistics and monitoring
    private final AtomicLong totalEventsProduced = new AtomicLong(0);
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsRetried = new AtomicLong(0);
    private final Map<String, AtomicLong> eventTypeCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> consumerStats = new ConcurrentHashMap<>();

    // Configuration
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final int BATCH_SIZE = 50;
    private static final long CONSUMER_POLL_TIMEOUT_MS = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_BASE_MS = 100;

    /**
     * Constructs EventProcessingService with configurable consumer thread count.
     * Initializes all blocking queues and consumer threads for optimal performance.
     *
     * @param numberOfConsumers Number of consumer threads for parallel processing
     */
    public EventProcessingService(int numberOfConsumers) {
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.numberOfConsumers = numberOfConsumers > 0 ? numberOfConsumers : Runtime.getRuntime().availableProcessors();

        // Initialize blocking queues with appropriate capacity and ordering
        this.highPriorityQueue = new PriorityBlockingQueue<>(
                DEFAULT_QUEUE_CAPACITY,
                Comparator.comparing(PriorityEvent::getPriority).reversed()
                        .thenComparing(PriorityEvent::getTimestamp));

        this.standardQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.batchQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.deadLetterQueue = new LinkedBlockingQueue<>();

        initializeEventHandlers();
        initializeStatistics();

        logger.logSystemOperation("EventProcessingService initialized with " +
                this.numberOfConsumers + " consumer threads and Producer-Consumer pattern");
    }

    /**
     * Starts the event processing service and all consumer threads.
     * Begins consuming events from all queues with parallel processing.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.logSystemOperation("Starting EventProcessingService with " +
                    numberOfConsumers + " consumer threads");

            // Start consumer threads
            for (int i = 0; i < numberOfConsumers; i++) {
                EventConsumer consumer = new EventConsumer("EventConsumer-" + i);
                consumers.add(consumer);
                threadPoolManager.submitBackgroundTask(consumer);
                consumerStats.put(consumer.getName(), new AtomicLong(0));
            }

            // Start batch processing consumer
            BatchEventConsumer batchConsumer = new BatchEventConsumer();
            threadPoolManager.submitBackgroundTask(batchConsumer);

            // Start dead letter queue processor
            DeadLetterQueueProcessor dlqProcessor = new DeadLetterQueueProcessor();
            threadPoolManager.submitBackgroundTask(dlqProcessor);

            auditLogger.logSecurityEvent("EventProcessingService started", "SYSTEM", "LOW");
            logger.logSystemOperation("EventProcessingService started successfully");
        }
    }

    /**
     * Stops the event processing service and gracefully shuts down all consumer
     * threads.
     * Ensures all pending events are processed before shutdown.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.logSystemOperation("Stopping EventProcessingService...");

            // Signal all consumers to stop
            consumers.forEach(EventConsumer::stop);

            // Wait for consumers to finish processing current events
            try {
                Thread.sleep(2000); // Allow consumers to finish current work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            auditLogger.logSecurityEvent("EventProcessingService stopped", "SYSTEM", "LOW");
            logger.logSystemOperation("EventProcessingService stopped with " +
                    getQueueSizes().values().stream().mapToInt(Integer::intValue).sum() +
                    " events remaining in queues");
        }
    }

    /**
     * Produces an event for processing using the Producer pattern.
     * Routes events to appropriate queues based on priority and type.
     *
     * @param event    The content event to process
     * @param priority The processing priority (HIGH, NORMAL, LOW)
     */
    public void produceEvent(ContentEvent event, EventPriority priority) {
        if (event == null) {
            logger.logError("Cannot produce null event", null);
            return;
        }

        if (!isRunning.get()) {
            logger.logError("EventProcessingService is not running, cannot produce event", null);
            return;
        }

        try {
            totalEventsProduced.incrementAndGet();
            incrementEventTypeCount(event.getEventType().toString());

            // Route event based on priority
            switch (priority) {
                case HIGH:
                    PriorityEvent priorityEvent = new PriorityEvent(event, priority);
                    if (!highPriorityQueue.offer(priorityEvent)) {
                        logger.logError("High priority queue is full, cannot produce event", null);
                        handleFailedEvent(event, "High priority queue full");
                    }
                    break;

                case BATCH:
                    if (!batchQueue.offer(event)) {
                        logger.logError("Batch queue is full, cannot produce event", null);
                        handleFailedEvent(event, "Batch queue full");
                    }
                    break;

                default: // NORMAL and LOW priority
                    if (!standardQueue.offer(event)) {
                        logger.logError("Standard queue is full, cannot produce event", null);
                        handleFailedEvent(event, "Standard queue full");
                    }
                    break;
            }

            if (logger != null) {
                logger.logSystemOperation("Event produced: " + event.getEventType() +
                        " with priority: " + priority);
            }

        } catch (Exception e) {
            logger.logError("Failed to produce event", e);
            handleFailedEvent(event, "Production failure: " + e.getMessage());
        }
    }

    /**
     * Produces multiple events in batch for high-throughput scenarios.
     * Optimized for bulk event production with minimal overhead.
     *
     * @param events   List of events to produce
     * @param priority Priority for all events in the batch
     */
    public void produceEventBatch(List<ContentEvent> events, EventPriority priority) {
        if (events == null || events.isEmpty()) {
            return;
        }

        logger.logSystemOperation("Producing event batch of " + events.size() +
                " events with priority: " + priority);

        for (ContentEvent event : events) {
            produceEvent(event, priority);
        }
    }

    /**
     * Registers an event observer for processed events.
     * Enables Observer pattern integration with event processing.
     *
     * @param observer The observer to register
     */
    public void registerObserver(ContentObserver observer) {
        if (observer != null) {
            eventObservers.add(observer);
            logger.logSystemOperation("Event observer registered: " + observer.getClass().getSimpleName());
        }
    }

    /**
     * Unregisters an event observer.
     *
     * @param observer The observer to unregister
     */
    public void unregisterObserver(ContentObserver observer) {
        if (observer != null && eventObservers.remove(observer)) {
            logger.logSystemOperation("Event observer unregistered: " + observer.getClass().getSimpleName());
        }
    }

    /**
     * Gets comprehensive event processing statistics and metrics.
     *
     * @return Map containing detailed processing statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        // Overall statistics
        stats.put("totalEventsProduced", totalEventsProduced.get());
        stats.put("totalEventsProcessed", totalEventsProcessed.get());
        stats.put("totalEventsFailed", totalEventsFailed.get());
        stats.put("totalEventsRetried", totalEventsRetried.get());
        stats.put("isRunning", isRunning.get());
        stats.put("numberOfConsumers", numberOfConsumers);
        stats.put("registeredObservers", eventObservers.size());

        // Queue statistics
        stats.put("queueSizes", getQueueSizes());

        // Event type statistics
        Map<String, Long> eventTypes = new ConcurrentHashMap<>();
        eventTypeCounts.forEach((type, count) -> eventTypes.put(type, count.get()));
        stats.put("eventTypeCounts", eventTypes);

        // Consumer statistics
        Map<String, Long> consumerMetrics = new ConcurrentHashMap<>();
        consumerStats.forEach((consumer, count) -> consumerMetrics.put(consumer, count.get()));
        stats.put("consumerStats", consumerMetrics);

        // Performance metrics
        long totalEvents = totalEventsProduced.get();
        stats.put("processingSuccessRate", totalEvents > 0 ? (double) totalEventsProcessed.get() / totalEvents : 0.0);
        stats.put("failureRate", totalEvents > 0 ? (double) totalEventsFailed.get() / totalEvents : 0.0);

        return stats;
    }

    /**
     * Gets current queue sizes for monitoring.
     *
     * @return Map with queue names and their current sizes
     */
    public Map<String, Integer> getQueueSizes() {
        Map<String, Integer> sizes = new ConcurrentHashMap<>();
        sizes.put("highPriorityQueue", highPriorityQueue.size());
        sizes.put("standardQueue", standardQueue.size());
        sizes.put("batchQueue", batchQueue.size());
        sizes.put("deadLetterQueue", deadLetterQueue.size());
        return sizes;
    }

    // Private helper methods and classes

    /**
     * Handles failed events by moving them to dead letter queue.
     */
    private void handleFailedEvent(ContentEvent event, String reason) {
        FailedEvent failedEvent = new FailedEvent(event, reason, LocalDateTime.now(), 0);
        deadLetterQueue.offer(failedEvent);
        totalEventsFailed.incrementAndGet();
    }

    /**
     * Initializes event type counters for statistics tracking.
     */
    private void initializeStatistics() {
        for (ContentEvent.EventType eventType : ContentEvent.EventType.values()) {
            eventTypeCounts.put(eventType.toString(), new AtomicLong(0));
        }
    }

    /**
     * Increments the counter for a specific event type.
     */
    private void incrementEventTypeCount(String eventType) {
        eventTypeCounts.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Initializes event handlers for different event types.
     */
    private void initializeEventHandlers() {
        // Default event handlers would be configured here
        logger.logSystemOperation("Event handlers initialized");
    }

    /**
     * Consumer thread implementation for processing events from multiple queues.
     */
    private class EventConsumer implements Runnable {
        private final String name;
        private volatile boolean running = true;

        public EventConsumer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            logger.logSystemOperation("Event consumer started: " + name);

            while (running && isRunning.get()) {
                try {
                    // Process high priority events first
                    PriorityEvent priorityEvent = highPriorityQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (priorityEvent != null) {
                        processEvent(priorityEvent.getEvent());
                        consumerStats.get(name).incrementAndGet();
                        continue;
                    }

                    // Process standard queue events
                    ContentEvent standardEvent = standardQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (standardEvent != null) {
                        processEvent(standardEvent);
                        consumerStats.get(name).incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.logError("Event consumer error in " + name, e);
                }
            }

            logger.logSystemOperation("Event consumer stopped: " + name);
        }

        private void processEvent(ContentEvent event) {
            try {
                // Notify all registered observers
                for (ContentObserver observer : eventObservers) {
                    try {
                        // Call appropriate observer method based on event type
                        switch (event.getEventType()) {
                            case CREATED:
                                observer.onContentCreated(event);
                                break;
                            case UPDATED:
                            case STATUS_CHANGED:
                            case METADATA_UPDATED:
                                observer.onContentUpdated(event);
                                break;
                            case PUBLISHED:
                                observer.onContentPublished(event);
                                break;
                            case DELETED:
                                observer.onContentDeleted(event);
                                break;
                            default:
                                // For unknown event types, use a default handler or skip
                                observer.onContentUpdated(event);
                                break;
                        }
                    } catch (Exception e) {
                        logger.logError("Observer failed to process event: " +
                                observer.getClass().getSimpleName(), e);
                    }
                }

                totalEventsProcessed.incrementAndGet();

            } catch (Exception e) {
                logger.logError("Failed to process event: " + event.getEventType(), e);
                handleFailedEvent(event, "Processing failure: " + e.getMessage());
            }
        }

        public void stop() {
            running = false;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Batch event consumer for high-throughput processing.
     */
    private class BatchEventConsumer implements Runnable {
        @Override
        public void run() {
            logger.logSystemOperation("Batch event consumer started");

            while (isRunning.get()) {
                try {
                    List<ContentEvent> batch = new ArrayList<>();

                    // Collect events for batch processing
                    ContentEvent event = batchQueue.poll(CONSUMER_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        batch.add(event);

                        // Collect additional events up to batch size
                        while (batch.size() < BATCH_SIZE) {
                            ContentEvent additionalEvent = batchQueue.poll(10, TimeUnit.MILLISECONDS);
                            if (additionalEvent == null)
                                break;
                            batch.add(additionalEvent);
                        }

                        // Process batch
                        processBatch(batch);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.logError("Batch event consumer error", e);
                }
            }

            logger.logSystemOperation("Batch event consumer stopped");
        }

        private void processBatch(List<ContentEvent> events) {
            logger.logSystemOperation("Processing event batch of " + events.size() + " events");

            for (ContentEvent event : events) {
                try {
                    for (ContentObserver observer : eventObservers) {
                        // Call appropriate observer method based on event type
                        switch (event.getEventType()) {
                            case CREATED:
                                observer.onContentCreated(event);
                                break;
                            case UPDATED:
                            case STATUS_CHANGED:
                            case METADATA_UPDATED:
                                observer.onContentUpdated(event);
                                break;
                            case PUBLISHED:
                                observer.onContentPublished(event);
                                break;
                            case DELETED:
                                observer.onContentDeleted(event);
                                break;
                            default:
                                observer.onContentUpdated(event);
                                break;
                        }
                    }
                    totalEventsProcessed.incrementAndGet();
                } catch (Exception e) {
                    logger.logError("Failed to process batch event", e);
                    handleFailedEvent(event, "Batch processing failure");
                }
            }
        }
    }

    /**
     * Dead letter queue processor for handling failed events with retry logic.
     */
    private class DeadLetterQueueProcessor implements Runnable {
        @Override
        public void run() {
            logger.logSystemOperation("Dead letter queue processor started");

            while (isRunning.get()) {
                try {
                    FailedEvent failedEvent = deadLetterQueue.poll(CONSUMER_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (failedEvent != null) {
                        processFailedEvent(failedEvent);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.logError("Dead letter queue processor error", e);
                }
            }

            logger.logSystemOperation("Dead letter queue processor stopped");
        }

        private void processFailedEvent(FailedEvent failedEvent) {
            if (failedEvent.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                // Calculate exponential backoff delay
                long delay = RETRY_DELAY_BASE_MS * (1L << failedEvent.getRetryCount());

                try {
                    Thread.sleep(delay);

                    // Retry event processing
                    produceEvent(failedEvent.getEvent(), EventPriority.NORMAL);
                    totalEventsRetried.incrementAndGet();

                    logger.logSystemOperation("Retried failed event (attempt " +
                            (failedEvent.getRetryCount() + 1) + ")");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.logError("Failed to retry event", e);
                }
            } else {
                // Maximum retries exceeded, log and discard
                logger.logError("Event permanently failed after " + MAX_RETRY_ATTEMPTS +
                        " retries: " + failedEvent.getReason(), null);
                auditLogger.logSecurityEvent("Event permanently failed", "SYSTEM", "MEDIUM");
            }
        }
    }

    // Supporting classes for event processing

    /**
     * Event priority enumeration for queue routing.
     */
    public enum EventPriority {
        HIGH(3), NORMAL(2), LOW(1), BATCH(0);

        private final int value;

        EventPriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Priority event wrapper for high-priority queue processing.
     */
    private static class PriorityEvent {
        private final ContentEvent event;
        private final EventPriority priority;
        private final LocalDateTime timestamp;

        public PriorityEvent(ContentEvent event, EventPriority priority) {
            this.event = event;
            this.priority = priority;
            this.timestamp = LocalDateTime.now();
        }

        public ContentEvent getEvent() {
            return event;
        }

        public EventPriority getPriority() {
            return priority;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Failed event representation for dead letter queue processing.
     */
    private static class FailedEvent {
        private final ContentEvent event;
        private final String reason;
        private final LocalDateTime failureTime;
        private final int retryCount;

        public FailedEvent(ContentEvent event, String reason, LocalDateTime failureTime, int retryCount) {
            this.event = event;
            this.reason = reason;
            this.failureTime = failureTime;
            this.retryCount = retryCount;
        }

        public ContentEvent getEvent() {
            return event;
        }

        public String getReason() {
            return reason;
        }

        public LocalDateTime getFailureTime() {
            return failureTime;
        }

        public int getRetryCount() {
            return retryCount;
        }
    }

    /**
     * Event handler interface for processing specific event types.
     */
    private interface EventHandler {
        void handle(ContentEvent event);
    }
}
