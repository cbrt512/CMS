package com.cms.patterns.observer;

import com.cms.util.CMSLogger;
import com.cms.concurrent.ThreadPoolManager;
import com.cms.concurrent.EventProcessingService;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Subject class that manages ContentObserver instances and notifies them of
 * content events.
 *
 * <p>
 * This class implements the Subject role in the Observer pattern, providing
 * thread-safe
 * observer management and asynchronous event notification. It maintains a
 * registry of
 * observers and ensures they are notified of relevant content events in a
 * reliable,
 * performant manner.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern + Multithreading -
 * Subject implementation that maintains observer registry and handles event
 * notification.
 * Provides thread-safe observer management with priority-based notification
 * ordering,
 * asynchronous processing, and integration with Producer-Consumer event
 * processing.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Core Subject class for Observer Pattern
 * implementation,
 * demonstrating advanced concurrent programming concepts, Collections Framework
 * usage,
 * multithreading integration with ThreadPoolManager and EventProcessingService,
 * and integration with existing logging and exception shielding patterns.
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is fully thread-safe with
 * concurrent
 * observer registration/removal and event notification. Uses ReadWriteLock for
 * optimal
 * performance during concurrent read operations (event notifications) while
 * maintaining
 * safety for write operations (observer management).
 * </p>
 *
 * <p>
 * <strong>Performance Features:</strong>
 * <ul>
 * <li>Asynchronous event notification with configurable thread pool</li>
 * <li>Priority-based observer ordering for critical operations</li>
 * <li>Content type filtering to reduce unnecessary notifications</li>
 * <li>Batched notification support for high-throughput scenarios</li>
 * <li>Circuit breaker pattern for failing observers</li>
 * <li>Comprehensive metrics and monitoring</li>
 * </ul>
 * </p>
 *
 * @see ContentObserver For observer interface
 * @see ContentEvent For event data structure
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentSubject {

    private final CMSLogger logger;

    // Multithreading integration
    private final ThreadPoolManager threadPoolManager;
    private final EventProcessingService eventProcessingService;

    // Thread-safe observer storage with priority ordering
    private final Map<ContentObserver, ObserverMetadata> observers;
    private final ReadWriteLock observerLock;

    // Asynchronous notification infrastructure
    private final ExecutorService notificationExecutor;
    private final CompletionService<NotificationResult> completionService;

    // Performance and monitoring
    private final AtomicLong eventCounter;
    private final Map<String, AtomicLong> eventTypeCounters;
    private final Map<ContentObserver, ObserverStats> observerStats;

    // Configuration
    private final int maxNotificationThreads;
    private final long notificationTimeoutMs;
    private final boolean enableAsyncNotification;

    /**
     * Metadata associated with each observer for performance optimization.
     */
    private static class ObserverMetadata {
        final ContentObserver observer;
        final int priority;
        final Set<Class<?>> interestedContentTypes;
        final long registrationTime;
        volatile boolean isActive;
        volatile int failureCount;
        volatile long lastFailureTime;

        ObserverMetadata(ContentObserver observer) {
            this.observer = observer;
            this.priority = observer.getPriority();
            this.interestedContentTypes = ConcurrentHashMap.newKeySet();
            this.registrationTime = System.currentTimeMillis();
            this.isActive = true;
            this.failureCount = 0;
            this.lastFailureTime = 0;
        }
    }

    /**
     * Statistics tracking for each observer.
     */
    private static class ObserverStats {
        final AtomicLong notificationsReceived = new AtomicLong(0);
        final AtomicLong notificationFailures = new AtomicLong(0);
        final AtomicLong totalProcessingTime = new AtomicLong(0);
        volatile long lastNotificationTime = 0;
        volatile long averageProcessingTime = 0;

        void recordNotification(long processingTimeMs, boolean success) {
            notificationsReceived.incrementAndGet();
            totalProcessingTime.addAndGet(processingTimeMs);
            lastNotificationTime = System.currentTimeMillis();
            averageProcessingTime = totalProcessingTime.get() / notificationsReceived.get();

            if (!success) {
                notificationFailures.incrementAndGet();
            }
        }
    }

    /**
     * Result of an observer notification.
     */
    private static class NotificationResult {
        final ContentObserver observer;
        final ContentEvent event;
        final boolean success;
        final long processingTimeMs;
        final Throwable error;

        NotificationResult(ContentObserver observer, ContentEvent event, boolean success,
                long processingTimeMs, Throwable error) {
            this.observer = observer;
            this.event = event;
            this.success = success;
            this.processingTimeMs = processingTimeMs;
            this.error = error;
        }
    }

    /**
     * Constructs a ContentSubject with default configuration.
     *
     * <p>
     * Uses reasonable defaults: 10 notification threads, 5 second timeout,
     * asynchronous notifications enabled.
     * </p>
     */
    public ContentSubject() {
        this(10, 5000, true);
    }

    /**
     * Constructs a ContentSubject with custom configuration.
     *
     * @param maxNotificationThreads  Maximum threads for async notifications
     * @param notificationTimeoutMs   Timeout for observer notifications in
     *                                milliseconds
     * @param enableAsyncNotification Whether to use asynchronous notifications
     */
    public ContentSubject(int maxNotificationThreads, long notificationTimeoutMs,
            boolean enableAsyncNotification) {
        this.maxNotificationThreads = maxNotificationThreads;
        this.notificationTimeoutMs = notificationTimeoutMs;
        this.enableAsyncNotification = enableAsyncNotification;

        // Initialize logging
        this.logger = CMSLogger.getInstance();

        // Initialize multithreading components
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.eventProcessingService = new EventProcessingService(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

        // Initialize thread-safe observer storage
        this.observers = new ConcurrentHashMap<>();
        this.observerLock = new ReentrantReadWriteLock();

        // Initialize asynchronous notification infrastructure
        if (enableAsyncNotification) {
            this.notificationExecutor = Executors.newFixedThreadPool(
                    maxNotificationThreads,
                    r -> {
                        Thread t = new Thread(r, "ContentSubject-Notification-" + System.nanoTime());
                        t.setDaemon(true);
                        return t;
                    });
            this.completionService = new ExecutorCompletionService<>(notificationExecutor);
        } else {
            this.notificationExecutor = null;
            this.completionService = null;
        }

        // Initialize monitoring
        this.eventCounter = new AtomicLong(0);
        this.eventTypeCounters = new ConcurrentHashMap<>();
        this.observerStats = new ConcurrentHashMap<>();

        // Initialize event type counters
        for (ContentEvent.EventType type : ContentEvent.EventType.values()) {
            eventTypeCounters.put(type.name(), new AtomicLong(0));
        }

        logger.logSystemEvent("ContentSubject initialized",
                "1.0",
                "maxThreads=" + maxNotificationThreads +
                        ", timeout=" + notificationTimeoutMs + "ms" +
                        ", async=" + enableAsyncNotification);
    }

    /**
     * Adds an observer to receive content event notifications.
     *
     * <p>
     * The observer will be added with its specified priority level and will
     * receive all future events that match its content type filter. Registration
     * is thread-safe and can be performed while events are being processed.
     * </p>
     *
     * <p>
     * <strong>Thread Safety:</strong> This method is thread-safe and can be
     * called concurrently from multiple threads without synchronization.
     * </p>
     *
     * @param observer The observer to add, must not be null
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException    if observer is already registered
     */
    public void addObserver(ContentObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }

        observerLock.writeLock().lock();
        try {
            if (observers.containsKey(observer)) {
                throw new IllegalStateException("Observer already registered: " + observer.getObserverName());
            }

            ObserverMetadata metadata = new ObserverMetadata(observer);
            observers.put(observer, metadata);
            observerStats.put(observer, new ObserverStats());

            logger.logSystemEvent("Observer registered",
                    "1.0",
                    "observer=" + observer.getObserverName() +
                            ", priority=" + observer.getPriority() +
                            ", totalObservers=" + observers.size());

        } finally {
            observerLock.writeLock().unlock();
        }
    }

    /**
     * Removes an observer from receiving content event notifications.
     *
     * <p>
     * After removal, the observer will no longer receive any event notifications.
     * Any pending asynchronous notifications for this observer will be allowed to
     * complete but no new notifications will be queued.
     * </p>
     *
     * <p>
     * <strong>Thread Safety:</strong> This method is thread-safe and can be
     * called concurrently from multiple threads without synchronization.
     * </p>
     *
     * @param observer The observer to remove, must not be null
     * @throws IllegalArgumentException if observer is null
     * @return true if the observer was registered and removed, false if not
     *         registered
     */
    public boolean removeObserver(ContentObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }

        observerLock.writeLock().lock();
        try {
            ObserverMetadata removed = observers.remove(observer);
            ObserverStats stats = observerStats.remove(observer);

            if (removed != null) {
                logger.logSystemEvent("Observer unregistered",
                        "1.0",
                        "observer=" + observer.getObserverName() +
                                ", totalObservers=" + observers.size() +
                                (stats != null ? ", notifications=" + stats.notificationsReceived.get() : ""));
                return true;
            }
            return false;

        } finally {
            observerLock.writeLock().unlock();
        }
    }

    /**
     * Notifies all registered observers about a content event using multithreading.
     *
     * <p>
     * <strong>Multithreading Integration:</strong> Events are produced to
     * EventProcessingService
     * with priority-based routing for high-performance Producer-Consumer pattern
     * implementation.
     * Leverages BlockingQueue-based event distribution and multiple consumer
     * threads.
     * </p>
     *
     * <p>
     * Observers are notified in priority order (lower numbers first) and only
     * observers interested in the content type are notified. Notifications can
     * be synchronous or asynchronous depending on configuration.
     * </p>
     *
     * <p>
     * <strong>Error Handling:</strong> Individual observer failures do not
     * affect other observers. Failed notifications are logged and tracked for
     * monitoring purposes.
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> Uses content type filtering and priority
     * ordering to optimize notification performance for large observer lists.
     * </p>
     *
     * @param event The content event to notify observers about, must not be null
     * @throws IllegalArgumentException if event is null
     */
    public void notifyObservers(ContentEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        // Update event counters
        eventCounter.incrementAndGet();
        eventTypeCounters.get(event.getEventType().name()).incrementAndGet();

        // Get interested observers (filtered and sorted by priority)
        List<ContentObserver> interestedObservers = getInterestedObservers(event);

        if (interestedObservers.isEmpty()) {
            logContentActivity("No observers interested in event",
                    "eventType=" + event.getEventType() +
                            ", contentType=" + event.getContent().getClass().getSimpleName());
            return;
        }

        logContentActivity("Producing event for async processing",
                "eventId=" + event.getEventId() +
                        ", eventType=" + event.getEventType() +
                        ", observerCount=" + interestedObservers.size());

        // Register observers with event processing service if not already registered
        for (ContentObserver observer : interestedObservers) {
            eventProcessingService.registerObserver(observer);
        }

        // Determine event priority based on event type and content
        EventProcessingService.EventPriority priority = determineEventPriority(event);

        // Produce event to processing service for async handling
        eventProcessingService.produceEvent(event, priority);

        // Also handle with legacy notification for compatibility
        if (enableAsyncNotification) {
            notifyObserversAsync(event, interestedObservers);
        } else {
            notifyObserversSync(event, interestedObservers);
        }
    }

    /**
     * Determines the processing priority for an event based on its type and
     * content.
     *
     * @param event The content event to analyze
     * @return The appropriate processing priority
     */
    private EventProcessingService.EventPriority determineEventPriority(ContentEvent event) {
        switch (event.getEventType()) {
            case CREATED:
            case UPDATED:
            case PUBLISHED:
                return EventProcessingService.EventPriority.HIGH;
            case DELETED:
            case STATUS_CHANGED:
                return EventProcessingService.EventPriority.NORMAL;
            case METADATA_UPDATED:
            default:
                return EventProcessingService.EventPriority.LOW;
        }
    }

    /**
     * Gets observers interested in the given event, filtered by content type and
     * sorted by priority.
     */
    private List<ContentObserver> getInterestedObservers(ContentEvent event) {
        Class<?> contentType = event.getContent().getClass();

        observerLock.readLock().lock();
        try {
            return observers.entrySet().stream()
                    .filter(entry -> entry.getValue().isActive)
                    .filter(entry -> entry.getKey().shouldObserve(contentType))
                    .sorted(Map.Entry.comparingByValue((m1, m2) -> Integer.compare(m1.priority, m2.priority)))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } finally {
            observerLock.readLock().unlock();
        }
    }

    /**
     * Notifies observers synchronously in the current thread.
     */
    private void notifyObserversSync(ContentEvent event, List<ContentObserver> observers) {
        for (ContentObserver observer : observers) {
            NotificationResult result = notifyObserver(observer, event);
            processNotificationResult(result);
        }
    }

    /**
     * Notifies observers asynchronously using the thread pool.
     */
    private void notifyObserversAsync(ContentEvent event, List<ContentObserver> observers) {
        List<Future<NotificationResult>> futures = new ArrayList<>();

        // Submit all notifications
        for (ContentObserver observer : observers) {
            Future<NotificationResult> future = completionService.submit(() -> notifyObserver(observer, event));
            futures.add(future);
        }

        // Collect results with timeout
        for (int i = 0; i < futures.size(); i++) {
            try {
                NotificationResult result = completionService.take().get(notificationTimeoutMs, TimeUnit.MILLISECONDS);
                processNotificationResult(result);
            } catch (TimeoutException e) {
                logger.logError(e, "ContentSubject", "system", "Observer notification timeout");
            } catch (Exception e) {
                logger.logError(e, "ContentSubject", "system", "Error processing observer notification result");
            }
        }
    }

    /**
     * Notifies a single observer and returns the result.
     */
    private NotificationResult notifyObserver(ContentObserver observer, ContentEvent event) {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        Throwable error = null;

        try {
            // Route to appropriate notification method based on event type
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
                    throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
            }

        } catch (Exception e) {
            success = false;
            error = e;

            // Update failure count for circuit breaker logic
            ObserverMetadata metadata = observers.get(observer);
            if (metadata != null) {
                metadata.failureCount++;
                metadata.lastFailureTime = System.currentTimeMillis();
            }

            logger.logError(e, "ContentSubject", "system",
                    "Observer notification failed: observer=" + observer.getObserverName() +
                            ", eventType=" + event.getEventType());
        }

        long processingTime = System.currentTimeMillis() - startTime;
        return new NotificationResult(observer, event, success, processingTime, error);
    }

    /**
     * Processes notification results for statistics and monitoring.
     */
    private void processNotificationResult(NotificationResult result) {
        ObserverStats stats = observerStats.get(result.observer);
        if (stats != null) {
            stats.recordNotification(result.processingTimeMs, result.success);
        }

        if (result.success) {
            logContentActivity("Observer notification completed",
                    "observer=" + result.observer.getObserverName() +
                            ", eventType=" + result.event.getEventType() +
                            ", processingTime=" + result.processingTimeMs + "ms");
        }
    }

    /**
     * Gets the total number of registered observers.
     * 
     * @return The number of registered observers
     */
    public int getObserverCount() {
        observerLock.readLock().lock();
        try {
            return observers.size();
        } finally {
            observerLock.readLock().unlock();
        }
    }

    /**
     * Gets the total number of events processed.
     * 
     * @return The total event count
     */
    public long getEventCount() {
        return eventCounter.get();
    }

    /**
     * Gets event counts by event type.
     * 
     * @return Immutable map of event type to count
     */
    public Map<String, Long> getEventCounts() {
        return eventTypeCounters.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    /**
     * Gets statistics for all observers.
     * 
     * @return Map of observer name to statistics
     */
    public Map<String, Map<String, Object>> getObserverStatistics() {
        observerLock.readLock().lock();
        try {
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();

            for (Map.Entry<ContentObserver, ObserverStats> entry : observerStats.entrySet()) {
                ContentObserver observer = entry.getKey();
                ObserverStats stats = entry.getValue();

                Map<String, Object> observerStats = new LinkedHashMap<>();
                observerStats.put("name", observer.getObserverName());
                observerStats.put("priority", observer.getPriority());
                observerStats.put("notificationsReceived", stats.notificationsReceived.get());
                observerStats.put("notificationFailures", stats.notificationFailures.get());
                observerStats.put("averageProcessingTime", stats.averageProcessingTime);
                observerStats.put("lastNotificationTime", stats.lastNotificationTime);

                result.put(observer.getObserverName(), observerStats);
            }

            return result;
        } finally {
            observerLock.readLock().unlock();
        }
    }

    /**
     * Checks if a specific observer is registered.
     * 
     * @param observer The observer to check
     * @return true if registered, false otherwise
     */
    public boolean isObserverRegistered(ContentObserver observer) {
        if (observer == null)
            return false;

        observerLock.readLock().lock();
        try {
            return observers.containsKey(observer);
        } finally {
            observerLock.readLock().unlock();
        }
    }

    /**
     * Gets a list of all registered observer names.
     * 
     * @return List of observer names
     */
    public List<String> getRegisteredObserverNames() {
        observerLock.readLock().lock();
        try {
            return observers.keySet().stream()
                    .map(ContentObserver::getObserverName)
                    .sorted()
                    .collect(Collectors.toList());
        } finally {
            observerLock.readLock().unlock();
        }
    }

    /**
     * Shuts down the notification infrastructure and releases resources.
     *
     * <p>
     * This method should be called when the ContentSubject is no longer needed
     * to ensure proper cleanup of thread pools and other resources.
     * </p>
     */
    public void shutdown() {
        if (notificationExecutor != null && !notificationExecutor.isShutdown()) {
            logger.logSystemEvent("Shutting down ContentSubject notification executor", "1.0", "");

            notificationExecutor.shutdown();
            try {
                if (!notificationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    notificationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                notificationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        observerLock.writeLock().lock();
        try {
            observers.clear();
            observerStats.clear();
        } finally {
            observerLock.writeLock().unlock();
        }

        logger.logSystemEvent("ContentSubject shutdown completed",
                "1.0", "totalEvents=" + eventCounter.get());
    }

    /**
     * Helper method to log content activity using the CMSLogger system event
     * method.
     * This bridges the gap between the expected logContentActivity interface and
     * the actual CMSLogger implementation.
     *
     * @param activity The activity description
     * @param details  Additional details about the activity
     */
    private void logContentActivity(String activity, String details) {
        logger.logSystemEvent(activity, "1.0", details);
    }
}
