package com.cms.patterns.observer;

import com.cms.core.model.Content;
import com.cms.core.model.User;
import com.cms.util.CMSLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive notification service that handles email notifications, cache
 * invalidation,
 * search index updates, and audit trail generation for content events.
 *
 * <p>
 * This concrete observer implementation shows a sophisticated notification
 * system that reacts to content lifecycle events by performing multiple
 * coordinated
 * actions. It showcases integration with existing CMS systems and proper error
 * handling in event-driven architectures.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Concrete observer
 * that implements comprehensive notification handling for content events. shows
 * real-world application of the Observer pattern with multiple notification
 * channels
 * and sophisticated event processing.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Primary concrete observer implementation
 * that
 * showcases Collections Framework usage, Generics implementation, proper
 * Exception
 * Shielding, and integration with the existing Logging framework.
 * </p>
 *
 * <p>
 * <strong>Notification Channels:</strong>
 * <ul>
 * <li>Email notifications to stakeholders and content creators</li>
 * <li>Cache invalidation for affected content and related data</li>
 * <li>Search index updates for content discoverability</li>
 * <li>Audit trail generation for compliance and monitoring</li>
 * <li>RSS feed updates for published content</li>
 * <li>Webhook notifications to external systems</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is thread-safe and can handle
 * concurrent event notifications without synchronization issues. Uses
 * concurrent
 * collections and atomic operations for safe concurrent access.
 * </p>
 *
 * @see ContentObserver For the observer interface
 * @see ContentEvent For event data structure
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentNotificationService implements ContentObserver {

    private final CMSLogger logger;

    // Configuration and settings
    private final Set<String> emailRecipients;
    private final Map<String, String> notificationSettings;
    private final boolean emailNotificationsEnabled;
    private final boolean cacheInvalidationEnabled;
    private final boolean searchIndexingEnabled;
    private final boolean auditLoggingEnabled;

    // Notification statistics and monitoring
    private final AtomicLong notificationsProcessed;
    private final Map<ContentEvent.EventType, AtomicLong> eventTypeCounters;
    private final ConcurrentLinkedQueue<NotificationEvent> recentNotifications;

    // Async processing infrastructure
    private final ExecutorService notificationExecutor;
    private final CompletionService<NotificationResult> completionService;

    // Cache and search index management
    private final Set<String> invalidatedCacheKeys;
    private final Queue<SearchIndexUpdate> pendingIndexUpdates;

    /**
     * Internal class to track notification events for monitoring.
     */
    private static class NotificationEvent {
        final String eventId;
        final ContentEvent.EventType eventType;
        final String contentTitle;
        final LocalDateTime timestamp;
        final boolean success;
        final String details;

        NotificationEvent(String eventId, ContentEvent.EventType eventType, String contentTitle,
                boolean success, String details) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.contentTitle = contentTitle;
            this.timestamp = LocalDateTime.now();
            this.success = success;
            this.details = details;
        }
    }

    /**
     * Result of a notification operation.
     */
    private static class NotificationResult {
        final String operation;
        final boolean success;
        final String details;
        final long processingTimeMs;

        NotificationResult(String operation, boolean success, String details, long processingTimeMs) {
            this.operation = operation;
            this.success = success;
            this.details = details;
            this.processingTimeMs = processingTimeMs;
        }
    }

    /**
     * Search index update request.
     */
    private static class SearchIndexUpdate {
        final Content content;
        final String operation; // "add", "update", "remove"
        final LocalDateTime requestTime;
        final Map<String, Object> metadata;

        SearchIndexUpdate(Content content, String operation, Map<String, Object> metadata) {
            this.content = content;
            this.operation = operation;
            this.requestTime = LocalDateTime.now();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
    }

    /**
     * Constructs a ContentNotificationService with default settings.
     *
     * <p>
     * Enables all notification channels with reasonable default configuration.
     * Creates a thread pool for asynchronous notification processing.
     * </p>
     */
    public ContentNotificationService() {
        this(true, true, true, true);
    }

    /**
     * Constructs a ContentNotificationService with custom channel enablement.
     *
     * @param emailEnabled  Whether to enable email notifications
     * @param cacheEnabled  Whether to enable cache invalidation
     * @param searchEnabled Whether to enable search indexing
     * @param auditEnabled  Whether to enable audit logging
     */
    public ContentNotificationService(boolean emailEnabled, boolean cacheEnabled,
            boolean searchEnabled, boolean auditEnabled) {
        this.logger = CMSLogger.getInstance();

        // Configuration
        this.emailNotificationsEnabled = emailEnabled;
        this.cacheInvalidationEnabled = cacheEnabled;
        this.searchIndexingEnabled = searchEnabled;
        this.auditLoggingEnabled = auditEnabled;

        // Initialize collections (using Collections Framework)
        this.emailRecipients = ConcurrentHashMap.newKeySet(); // Thread-safe Set
        this.notificationSettings = new ConcurrentHashMap<>(); // Thread-safe Map
        this.eventTypeCounters = new ConcurrentHashMap<>(); // Thread-safe Map for counters
        this.recentNotifications = new ConcurrentLinkedQueue<>(); // Thread-safe Queue
        this.invalidatedCacheKeys = ConcurrentHashMap.newKeySet(); // Thread-safe Set
        this.pendingIndexUpdates = new ConcurrentLinkedQueue<>(); // Thread-safe Queue

        // Initialize counters for all event types
        for (ContentEvent.EventType type : ContentEvent.EventType.values()) {
            eventTypeCounters.put(type, new AtomicLong(0));
        }

        // Initialize statistics
        this.notificationsProcessed = new AtomicLong(0);

        // Initialize async processing
        this.notificationExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "ContentNotification-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        this.completionService = new ExecutorCompletionService<>(notificationExecutor);

        // Set up default configuration
        initializeDefaultSettings();

        logger.logSystemEvent("SERVICE_INIT", "1.0", "ContentNotificationService initialized - " +
                "email=" + emailEnabled + ", cache=" + cacheEnabled +
                ", search=" + searchEnabled + ", audit=" + auditEnabled);
    }

    /**
     * Initializes default notification settings.
     */
    private void initializeDefaultSettings() {
        // Default email settings
        emailRecipients.add("admin@cms.local");
        emailRecipients.add("editor@cms.local");

        // Default notification preferences
        notificationSettings.put("email.template", "default");
        notificationSettings.put("email.priority", "normal");
        notificationSettings.put("cache.ttl", "3600");
        notificationSettings.put("search.batch_size", "100");
        notificationSettings.put("audit.level", "detailed");
    }

    @Override
    public void onContentCreated(ContentEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            eventTypeCounters.get(ContentEvent.EventType.CREATED).incrementAndGet();

            logger.logSystemEvent("NOTIFICATION", "1.0", "Processing content creation notification - " +
                    "eventId=" + event.getEventId() +
                    ", content=" + event.getContent().getTitle() +
                    ", user=" + event.getUser().getUsername());

            // Process notifications asynchronously
            List<CompletableFuture<NotificationResult>> futures = new ArrayList<>();

            if (emailNotificationsEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> sendCreationNotificationEmail(event),
                        notificationExecutor));
            }

            if (cacheInvalidationEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> invalidateRelatedCache(event), notificationExecutor));
            }

            if (searchIndexingEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> addToSearchIndex(event), notificationExecutor));
            }

            if (auditLoggingEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> logAuditEvent(event, "CONTENT_CREATED"),
                        notificationExecutor));
            }

            // Wait for all notifications to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS); // Timeout after 10 seconds

            notificationsProcessed.incrementAndGet();
            recordNotificationEvent(event, true, "Content creation notifications sent successfully");

        } catch (Exception e) {
            handleNotificationError(event, "onContentCreated", e);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logSystemEvent("NOTIFICATION_COMPLETE", "1.0", "Content creation notification completed - " +
                "eventId=" + event.getEventId() + ", processingTime=" + processingTime + "ms");
    }

    @Override
    public void onContentUpdated(ContentEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            eventTypeCounters.get(ContentEvent.EventType.UPDATED).incrementAndGet();

            logger.logSystemEvent("NOTIFICATION", "1.0", "Processing content update notification - " +
                    "eventId=" + event.getEventId() +
                    ", content=" + event.getContent().getTitle() +
                    ", changedFields=" + event.getChangedFields().size());

            // Process notifications based on what changed
            List<CompletableFuture<NotificationResult>> futures = new ArrayList<>();

            if (emailNotificationsEnabled && shouldNotifyForUpdate(event)) {
                futures.add(
                        CompletableFuture.supplyAsync(() -> sendUpdateNotificationEmail(event), notificationExecutor));
            }

            if (cacheInvalidationEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> invalidateContentCache(event), notificationExecutor));
            }

            if (searchIndexingEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> updateSearchIndex(event), notificationExecutor));
            }

            if (auditLoggingEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> logAuditEvent(event, "CONTENT_UPDATED"),
                        notificationExecutor));
            }

            // Wait for all notifications to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);

            notificationsProcessed.incrementAndGet();
            recordNotificationEvent(event, true, "Content update notifications sent successfully");

        } catch (Exception e) {
            handleNotificationError(event, "onContentUpdated", e);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logContentActivity("Content update notification completed",
                "eventId=" + event.getEventId() + ", processingTime=" + processingTime + "ms");
    }

    @Override
    public void onContentPublished(ContentEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            eventTypeCounters.get(ContentEvent.EventType.PUBLISHED).incrementAndGet();

            logger.logContentActivity("Processing content publication notification",
                    "eventId=" + event.getEventId() +
                            ", content=" + event.getContent().getTitle() +
                            ", publicationDate=" + event.getPublicationDate());

            // Publication notifications are typically high-priority
            List<CompletableFuture<NotificationResult>> futures = new ArrayList<>();

            if (emailNotificationsEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> sendPublicationNotificationEmail(event),
                        notificationExecutor));
            }

            if (cacheInvalidationEnabled) {
                futures.add(
                        CompletableFuture.supplyAsync(() -> invalidatePublicationCache(event), notificationExecutor));
            }

            if (searchIndexingEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> updatePublicationIndex(event), notificationExecutor));
            }

            // Always log publication events for audit
            futures.add(CompletableFuture.supplyAsync(() -> logAuditEvent(event, "CONTENT_PUBLISHED"),
                    notificationExecutor));

            // Additional publication-specific notifications
            futures.add(CompletableFuture.supplyAsync(() -> updateRSSFeed(event), notificationExecutor));

            futures.add(CompletableFuture.supplyAsync(() -> sendWebhookNotifications(event), notificationExecutor));

            // Wait for all notifications to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(15, TimeUnit.SECONDS); // Longer timeout for publication

            notificationsProcessed.incrementAndGet();
            recordNotificationEvent(event, true, "Content publication notifications sent successfully");

        } catch (Exception e) {
            handleNotificationError(event, "onContentPublished", e);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logContentActivity("Content publication notification completed",
                "eventId=" + event.getEventId() + ", processingTime=" + processingTime + "ms");
    }

    @Override
    public void onContentDeleted(ContentEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            eventTypeCounters.get(ContentEvent.EventType.DELETED).incrementAndGet();

            logger.logContentActivity("Processing content deletion notification",
                    "eventId=" + event.getEventId() +
                            ", content=" + event.getContent().getTitle() +
                            ", temporary=" + event.isTemporaryDeletion() +
                            ", reason=" + event.getDeletionReason());

            // Deletion notifications and cleanup
            List<CompletableFuture<NotificationResult>> futures = new ArrayList<>();

            if (emailNotificationsEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> sendDeletionNotificationEmail(event),
                        notificationExecutor));
            }

            if (cacheInvalidationEnabled) {
                futures.add(CompletableFuture.supplyAsync(() -> cleanupContentCache(event), notificationExecutor));
            }

            if (searchIndexingEnabled && !event.isTemporaryDeletion()) {
                futures.add(CompletableFuture.supplyAsync(() -> removeFromSearchIndex(event), notificationExecutor));
            }

            // Always log deletion events for audit
            futures.add(CompletableFuture.supplyAsync(
                    () -> logAuditEvent(event, event.isTemporaryDeletion() ? "CONTENT_ARCHIVED" : "CONTENT_DELETED"),
                    notificationExecutor));

            // Wait for all notifications to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);

            notificationsProcessed.incrementAndGet();
            recordNotificationEvent(event, true, "Content deletion notifications sent successfully");

        } catch (Exception e) {
            handleNotificationError(event, "onContentDeleted", e);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logContentActivity("Content deletion notification completed",
                "eventId=" + event.getEventId() + ", processingTime=" + processingTime + "ms");
    }

    // Notification implementation methods

    private NotificationResult sendCreationNotificationEmail(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            // Simulate email sending logic
            String emailContent = buildCreationEmailContent(event);

            // In a real implementation, this would integrate with an email service
            logger.logContentActivity("Email notification sent",
                    "type=creation, recipients=" + emailRecipients.size() +
                            ", content=" + event.getContent().getTitle());

            return new NotificationResult("Email", true, "Creation email sent",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to send creation email", null, "email_notification");
            return new NotificationResult("Email", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult sendUpdateNotificationEmail(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            String emailContent = buildUpdateEmailContent(event);

            logger.logContentActivity("Email notification sent",
                    "type=update, recipients=" + emailRecipients.size() +
                            ", content=" + event.getContent().getTitle() +
                            ", changedFields=" + String.join(",", event.getChangedFields()));

            return new NotificationResult("Email", true, "Update email sent",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to send update email", null, "email_notification");
            return new NotificationResult("Email", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult sendPublicationNotificationEmail(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            String emailContent = buildPublicationEmailContent(event);

            logger.logContentActivity("Email notification sent",
                    "type=publication, recipients=" + emailRecipients.size() +
                            ", content=" + event.getContent().getTitle() +
                            ", publicationDate=" + event.getPublicationDate());

            return new NotificationResult("Email", true, "Publication email sent",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to send publication email", null, "email_notification");
            return new NotificationResult("Email", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult sendDeletionNotificationEmail(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            String emailContent = buildDeletionEmailContent(event);

            logger.logContentActivity("Email notification sent",
                    "type=deletion, recipients=" + emailRecipients.size() +
                            ", content=" + event.getContent().getTitle() +
                            ", temporary=" + event.isTemporaryDeletion());

            return new NotificationResult("Email", true, "Deletion email sent",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to send deletion email", null, "email_notification");
            return new NotificationResult("Email", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult invalidateRelatedCache(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            // Invalidate caches related to new content
            Set<String> keysToInvalidate = generateCacheKeys(event.getContent(), "related");
            invalidatedCacheKeys.addAll(keysToInvalidate);

            logger.logContentActivity("Cache invalidation completed",
                    "type=related, keys=" + keysToInvalidate.size() +
                            ", content=" + event.getContent().getTitle());

            return new NotificationResult("Cache", true, "Related cache invalidated",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to invalidate related cache", null, "cache_invalidation");
            return new NotificationResult("Cache", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult invalidateContentCache(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            Set<String> keysToInvalidate = generateCacheKeys(event.getContent(), "content");
            invalidatedCacheKeys.addAll(keysToInvalidate);

            logger.logContentActivity("Cache invalidation completed",
                    "type=content, keys=" + keysToInvalidate.size() +
                            ", content=" + event.getContent().getTitle());

            return new NotificationResult("Cache", true, "Content cache invalidated",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to invalidate content cache", null, "cache_invalidation");
            return new NotificationResult("Cache", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult invalidatePublicationCache(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            Set<String> keysToInvalidate = generateCacheKeys(event.getContent(), "publication");
            keysToInvalidate.add("published_content_list");
            keysToInvalidate.add("rss_feed");
            keysToInvalidate.add("sitemap");
            invalidatedCacheKeys.addAll(keysToInvalidate);

            logger.logContentActivity("Cache invalidation completed",
                    "type=publication, keys=" + keysToInvalidate.size() +
                            ", content=" + event.getContent().getTitle());

            return new NotificationResult("Cache", true, "Publication cache invalidated",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to invalidate publication cache", null, "cache_invalidation");
            return new NotificationResult("Cache", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult cleanupContentCache(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            Set<String> keysToInvalidate = generateCacheKeys(event.getContent(), "all");
            invalidatedCacheKeys.addAll(keysToInvalidate);

            logger.logContentActivity("Cache cleanup completed",
                    "keys=" + keysToInvalidate.size() +
                            ", content=" + event.getContent().getTitle() +
                            ", temporary=" + event.isTemporaryDeletion());

            return new NotificationResult("Cache", true, "Content cache cleaned up",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to cleanup content cache", null, "cache_cleanup");
            return new NotificationResult("Cache", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult addToSearchIndex(ContentEvent event) {
        return processSearchIndexUpdate(event.getContent(), "add", event.getMetadata());
    }

    private NotificationResult updateSearchIndex(ContentEvent event) {
        return processSearchIndexUpdate(event.getContent(), "update", event.getMetadata());
    }

    private NotificationResult updatePublicationIndex(ContentEvent event) {
        Map<String, Object> metadata = new HashMap<>(event.getMetadata());
        metadata.put("publicationDate", event.getPublicationDate());
        metadata.put("published", true);
        return processSearchIndexUpdate(event.getContent(), "update", metadata);
    }

    private NotificationResult removeFromSearchIndex(ContentEvent event) {
        return processSearchIndexUpdate(event.getContent(), "remove", event.getMetadata());
    }

    private NotificationResult processSearchIndexUpdate(Content content, String operation,
            Map<String, Object> metadata) {
        long startTime = System.currentTimeMillis();
        try {
            SearchIndexUpdate update = new SearchIndexUpdate(content, operation, metadata);
            pendingIndexUpdates.offer(update);

            logger.logContentActivity("Search index update queued",
                    "operation=" + operation +
                            ", content=" + content.getTitle() +
                            ", queueSize=" + pendingIndexUpdates.size());

            return new NotificationResult("SearchIndex", true, "Search index " + operation + " queued",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to process search index update", null, "search_indexing");
            return new NotificationResult("SearchIndex", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult updateRSSFeed(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            // Simulate RSS feed update logic
            logger.logContentActivity("RSS feed updated",
                    "content=" + event.getContent().getTitle() +
                            ", publicationDate=" + event.getPublicationDate());

            return new NotificationResult("RSS", true, "RSS feed updated",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to update RSS feed", null, "rss_update");
            return new NotificationResult("RSS", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult sendWebhookNotifications(ContentEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            // Simulate webhook notification logic
            logger.logContentActivity("Webhook notifications sent",
                    "content=" + event.getContent().getTitle() +
                            ", eventType=" + event.getEventType());

            return new NotificationResult("Webhook", true, "Webhook notifications sent",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to send webhook notifications", null, "webhook_notification");
            return new NotificationResult("Webhook", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private NotificationResult logAuditEvent(ContentEvent event, String auditEventType) {
        long startTime = System.currentTimeMillis();
        try {
            // Use the existing AuditLogger from the logging framework
            logger.logSecurityEvent("Content Event",
                    "eventId=" + event.getEventId() +
                            ", auditType=" + auditEventType +
                            ", content=" + event.getContent().getTitle() +
                            ", user=" + event.getUser().getUsername() +
                            ", timestamp=" + event.getTimestamp());

            return new NotificationResult("Audit", true, "Audit event logged",
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.logError(e, "Failed to log audit event", null, "audit_logging");
            return new NotificationResult("Audit", false, e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    // Helper methods

    private boolean shouldNotifyForUpdate(ContentEvent event) {
        // Only notify for significant updates
        Set<String> significantFields = Set.of("title", "content", "status", "publishedDate");
        return event.getChangedFields().stream().anyMatch(significantFields::contains);
    }

    private String buildCreationEmailContent(ContentEvent event) {
        return String.format(
                "New content has been created:\n\n" +
                        "Title: %s\n" +
                        "Type: %s\n" +
                        "Created by: %s\n" +
                        "Created at: %s\n\n" +
                        "Content details available in the CMS.",
                event.getContent().getTitle(),
                event.getContent().getClass().getSimpleName(),
                event.getUser().getUsername(),
                event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private String buildUpdateEmailContent(ContentEvent event) {
        return String.format(
                "Content has been updated:\n\n" +
                        "Title: %s\n" +
                        "Updated by: %s\n" +
                        "Updated at: %s\n" +
                        "Changed fields: %s\n\n" +
                        "Review the changes in the CMS.",
                event.getContent().getTitle(),
                event.getUser().getUsername(),
                event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                String.join(", ", event.getChangedFields()));
    }

    private String buildPublicationEmailContent(ContentEvent event) {
        return String.format(
                "Content has been published:\n\n" +
                        "Title: %s\n" +
                        "Published by: %s\n" +
                        "Publication date: %s\n\n" +
                        "The content is now live and accessible to users.",
                event.getContent().getTitle(),
                event.getUser().getUsername(),
                event.getPublicationDate() != null
                        ? event.getPublicationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : "Now");
    }

    private String buildDeletionEmailContent(ContentEvent event) {
        return String.format(
                "Content has been %s:\n\n" +
                        "Title: %s\n" +
                        "Deleted by: %s\n" +
                        "Deletion date: %s\n" +
                        "Reason: %s\n\n" +
                        "This action %s be reversible.",
                event.isTemporaryDeletion() ? "archived" : "deleted",
                event.getContent().getTitle(),
                event.getUser().getUsername(),
                event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                event.getDeletionReason() != null ? event.getDeletionReason() : "No reason provided",
                event.isTemporaryDeletion() ? "may" : "may not");
    }

    private Set<String> generateCacheKeys(Content content, String type) {
        Set<String> keys = new HashSet<>();

        // Content-specific keys
        keys.add("content:" + content.getId());
        keys.add("content:title:" + content.getTitle().replaceAll("[^a-zA-Z0-9]", "_"));

        // Type-specific keys
        switch (type) {
            case "related":
                keys.add("related_content:" + content.getId());
                keys.add("content_list");
                break;
            case "content":
                keys.add("content_view:" + content.getId());
                keys.add("content_summary:" + content.getId());
                break;
            case "publication":
                keys.add("published_content:" + content.getId());
                keys.add("published_list");
                break;
            case "all":
                keys.addAll(generateCacheKeys(content, "related"));
                keys.addAll(generateCacheKeys(content, "content"));
                keys.addAll(generateCacheKeys(content, "publication"));
                break;
        }

        return keys;
    }

    private void recordNotificationEvent(ContentEvent event, boolean success, String details) {
        NotificationEvent notificationEvent = new NotificationEvent(
                event.getEventId(),
                event.getEventType(),
                event.getContent().getTitle(),
                success,
                details);

        recentNotifications.offer(notificationEvent);

        // Keep only the most recent 1000 notifications
        while (recentNotifications.size() > 1000) {
            recentNotifications.poll();
        }
    }

    private void handleNotificationError(ContentEvent event, String operation, Exception error) {
        logger.logError(error, "Notification processing failed - operation=" + operation +
                ", eventId=" + event.getEventId() +
                ", content=" + event.getContent().getTitle(), null, "notification_processing");

        recordNotificationEvent(event, false, "Error: " + error.getMessage());
    }

    // Public methods for observer interface implementation

    @Override
    public String getObserverName() {
        return "Content Notification Service";
    }

    @Override
    public int getPriority() {
        return 20; // Important but not critical priority
    }

    @Override
    public boolean shouldObserve(Class<?> contentType) {
        // Observe all content types
        return Content.class.isAssignableFrom(contentType);
    }

    // Configuration and management methods

    /**
     * Adds an email recipient for notifications.
     * 
     * @param email Email address to add
     */
    public void addEmailRecipient(String email) {
        if (email != null && email.contains("@")) {
            emailRecipients.add(email);
            logger.logContentActivity("Email recipient added", "email=" + email);
        }
    }

    /**
     * Removes an email recipient.
     * 
     * @param email Email address to remove
     */
    public void removeEmailRecipient(String email) {
        if (emailRecipients.remove(email)) {
            logger.logContentActivity("Email recipient removed", "email=" + email);
        }
    }

    /**
     * Gets statistics about notification processing.
     * 
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalNotifications", notificationsProcessed.get());
        stats.put("emailRecipients", emailRecipients.size());
        stats.put("invalidatedCacheKeys", invalidatedCacheKeys.size());
        stats.put("pendingIndexUpdates", pendingIndexUpdates.size());
        stats.put("recentNotifications", recentNotifications.size());

        // Event type breakdown
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        for (Map.Entry<ContentEvent.EventType, AtomicLong> entry : eventTypeCounters.entrySet()) {
            eventCounts.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("eventTypeCounts", eventCounts);

        return stats;
    }

    /**
     * Gets recent notification events for monitoring.
     * 
     * @return List of recent notification events
     */
    public List<NotificationEvent> getRecentNotifications() {
        return new ArrayList<>(recentNotifications);
    }

    /**
     * Clears processed cache invalidation keys.
     */
    public void clearProcessedCacheKeys() {
        int cleared = invalidatedCacheKeys.size();
        invalidatedCacheKeys.clear();
        logger.logContentActivity("Cache invalidation keys cleared", "count=" + cleared);
    }

    /**
     * Processes pending search index updates.
     * 
     * @return Number of updates processed
     */
    public int processPendingIndexUpdates() {
        int processed = 0;
        SearchIndexUpdate update;

        while ((update = pendingIndexUpdates.poll()) != null) {
            try {
                // Simulate search index processing
                logger.logContentActivity("Search index update processed",
                        "operation=" + update.operation +
                                ", content=" + update.content.getTitle());
                processed++;
            } catch (Exception e) {
                logger.logError(e, "Failed to process search index update", null, "search_indexing");
            }
        }

        if (processed > 0) {
            logger.logContentActivity("Search index updates processed", "count=" + processed);
        }

        return processed;
    }

    /**
     * Shuts down the notification service and releases resources.
     */
    public void shutdown() {
        logger.logSystemEvent("SHUTDOWN", "1.0", "Shutting down ContentNotificationService");

        if (notificationExecutor != null && !notificationExecutor.isShutdown()) {
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

        logger.logSystemEvent("SHUTDOWN_COMPLETE", "1.0", "ContentNotificationService shutdown completed - " +
                "totalNotifications=" + notificationsProcessed.get());
    }
}
