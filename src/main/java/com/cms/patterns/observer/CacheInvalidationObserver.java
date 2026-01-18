package com.cms.patterns.observer;

import com.cms.core.model.Content;
import com.cms.util.CMSLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Specialized observer for cache invalidation operations in the CMS system.
 *
 * <p>
 * This observer focuses exclusively on cache management, providing
 * sophisticated
 * cache invalidation strategies based on content relationships, hierarchies,
 * and
 * dependencies. It shows high-performance cache management with minimal
 * impact on system performance.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Specialized concrete
 * observer that handles cache invalidation with sophisticated dependency
 * tracking
 * and efficient batch processing capabilities.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> shows advanced Collections Framework
 * usage with concurrent collections, Generics for type-safe operations, and
 * integration with existing logging infrastructure.
 * </p>
 *
 * <p>
 * <strong>Cache Management Features:</strong>
 * <ul>
 * <li>Dependency-based cache invalidation</li>
 * <li>Hierarchical cache key management</li>
 * <li>Batch invalidation for performance optimization</li>
 * <li>Cache warming for frequently accessed content</li>
 * <li>TTL-based selective invalidation</li>
 * <li>Statistics and monitoring for cache effectiveness</li>
 * </ul>
 * </p>
 *
 * @see ContentObserver For the observer interface
 * @see ContentEvent For event data structure
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class CacheInvalidationObserver implements ContentObserver {

    private final CMSLogger logger;

    // Cache management collections
    private final Map<String, Set<String>> cacheKeyDependencies; // Key -> Dependent keys
    private final Map<String, Long> cacheKeyTTL; // Key -> TTL timestamp
    private final Set<String> invalidatedKeys; // Keys that have been invalidated
    private final Queue<CacheOperation> operationQueue; // Pending cache operations

    // Cache regions and configuration
    private final Map<String, CacheRegionConfig> cacheRegions;
    private final Set<String> criticalCacheKeys; // Keys that should be warmed immediately

    // Statistics and monitoring
    private final AtomicLong invalidationCount;
    private final Map<ContentEvent.EventType, AtomicLong> eventTypeStats;
    private final Map<String, AtomicLong> regionStats;

    /**
     * Configuration for cache regions with different invalidation strategies.
     */
    private static class CacheRegionConfig {
        final String regionName;
        final long defaultTTL; // Default TTL in milliseconds
        final boolean enableDependencies; // Whether to track dependencies
        final boolean enableBatching; // Whether to batch operations
        final int batchSize; // Number of operations to batch together

        CacheRegionConfig(String regionName, long defaultTTL, boolean enableDependencies,
                boolean enableBatching, int batchSize) {
            this.regionName = regionName;
            this.defaultTTL = defaultTTL;
            this.enableDependencies = enableDependencies;
            this.enableBatching = enableBatching;
            this.batchSize = batchSize;
        }
    }

    /**
     * Represents a cache operation to be performed.
     */
    private static class CacheOperation {
        final String operationType; // "invalidate", "warm", "update"
        final String cacheKey;
        final long timestamp;
        final Map<String, Object> metadata;

        CacheOperation(String operationType, String cacheKey, Map<String, Object> metadata) {
            this.operationType = operationType;
            this.cacheKey = cacheKey;
            this.timestamp = System.currentTimeMillis();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
    }

    /**
     * Constructs a CacheInvalidationObserver with default configuration.
     */
    public CacheInvalidationObserver() {
        this.logger = CMSLogger.getInstance();

        // Initialize concurrent collections for thread safety
        this.cacheKeyDependencies = new ConcurrentHashMap<>();
        this.cacheKeyTTL = new ConcurrentHashMap<>();
        this.invalidatedKeys = ConcurrentHashMap.newKeySet();
        this.operationQueue = new ConcurrentLinkedQueue<>();
        this.cacheRegions = new ConcurrentHashMap<>();
        this.criticalCacheKeys = ConcurrentHashMap.newKeySet();

        // Initialize statistics
        this.invalidationCount = new AtomicLong(0);
        this.eventTypeStats = new ConcurrentHashMap<>();
        this.regionStats = new ConcurrentHashMap<>();

        // Initialize event type statistics
        for (ContentEvent.EventType type : ContentEvent.EventType.values()) {
            eventTypeStats.put(type, new AtomicLong(0));
        }

        // Set up default cache regions
        initializeDefaultCacheRegions();

        logger.logContentActivity("CacheInvalidationObserver initialized",
                "regions=" + cacheRegions.size());
    }

    /**
     * Initializes default cache regions with appropriate configurations.
     */
    private void initializeDefaultCacheRegions() {
        // Content cache region - high dependency tracking
        cacheRegions.put("content", new CacheRegionConfig(
                "content", 3600000L, true, true, 50)); // 1 hour TTL, dependencies, batch size 50

        // List cache region - medium dependencies, larger batches
        cacheRegions.put("lists", new CacheRegionConfig(
                "lists", 1800000L, true, true, 100)); // 30 min TTL, batch size 100

        // Fragment cache region - low dependencies, small batches
        cacheRegions.put("fragments", new CacheRegionConfig(
                "fragments", 900000L, false, true, 25)); // 15 min TTL, batch size 25

        // Search cache region - high dependencies, immediate invalidation
        cacheRegions.put("search", new CacheRegionConfig(
                "search", 600000L, true, false, 1)); // 10 min TTL, no batching

        // Navigation cache region - critical for site performance
        cacheRegions.put("navigation", new CacheRegionConfig(
                "navigation", 7200000L, true, false, 1)); // 2 hour TTL, no batching

        // Initialize region statistics
        for (String regionName : cacheRegions.keySet()) {
            regionStats.put(regionName, new AtomicLong(0));
        }
    }

    @Override
    public void onContentCreated(ContentEvent event) {
        eventTypeStats.get(ContentEvent.EventType.CREATED).incrementAndGet();

        logger.logContentActivity("Processing cache invalidation for content creation",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle());

        try {
            Content content = event.getContent();

            // Invalidate content list caches
            Set<String> keysToInvalidate = generateContentListKeys(content);

            // Invalidate navigation caches if this affects site structure
            keysToInvalidate.addAll(generateNavigationKeys(content));

            // Invalidate search caches
            keysToInvalidate.addAll(generateSearchKeys(content));

            // Add cache warming for the new content
            String contentKey = generateContentKey(content);
            addCacheOperation("warm", contentKey,
                    Map.of("priority", "normal", "reason", "new_content"));

            // Process invalidations
            processInvalidations(keysToInvalidate, "content_created");

            logger.logContentActivity("Cache invalidation completed for content creation",
                    "keys=" + keysToInvalidate.size() + ", content=" + content.getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to process cache invalidation for content creation", null, "cache_invalidation");
        }
    }

    @Override
    public void onContentUpdated(ContentEvent event) {
        eventTypeStats.get(ContentEvent.EventType.UPDATED).incrementAndGet();

        logger.logContentActivity("Processing cache invalidation for content update",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle() +
                        ", changedFields=" + event.getChangedFields().size());

        try {
            Content content = event.getContent();
            Set<String> changedFields = event.getChangedFields();

            // Determine invalidation scope based on changed fields
            Set<String> keysToInvalidate = new HashSet<>();

            // Always invalidate the content itself
            String contentKey = generateContentKey(content);
            keysToInvalidate.add(contentKey);

            // Conditionally invalidate based on changes
            if (changedFields.contains("title") || changedFields.contains("content")) {
                // Major content changes - invalidate everything related
                keysToInvalidate.addAll(generateContentDependentKeys(content));
                keysToInvalidate.addAll(generateSearchKeys(content));

                // Warm critical content immediately
                if (criticalCacheKeys.contains(contentKey)) {
                    addCacheOperation("warm", contentKey,
                            Map.of("priority", "high", "reason", "critical_content_update"));
                }
            }

            if (changedFields.contains("status")) {
                // Status changes affect lists and navigation
                keysToInvalidate.addAll(generateContentListKeys(content));
                keysToInvalidate.addAll(generateNavigationKeys(content));
            }

            if (changedFields.contains("metadata")) {
                // Metadata changes might affect categorization
                keysToInvalidate.addAll(generateCategoryKeys(content));
            }

            // Process invalidations with update context
            Map<String, Object> context = Map.of(
                    "changedFields", new ArrayList<>(changedFields),
                    "reason", "content_updated");

            processInvalidationsWithContext(keysToInvalidate, context);

            logger.logContentActivity("Cache invalidation completed for content update",
                    "keys=" + keysToInvalidate.size() + ", content=" + content.getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to process cache invalidation for content update", null, "cache_invalidation");
        }
    }

    @Override
    public void onContentPublished(ContentEvent event) {
        eventTypeStats.get(ContentEvent.EventType.PUBLISHED).incrementAndGet();

        logger.logContentActivity("Processing cache invalidation for content publication",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle());

        try {
            Content content = event.getContent();

            // Publication is a critical event - invalidate extensively
            Set<String> keysToInvalidate = new HashSet<>();

            // Invalidate all content-related caches
            keysToInvalidate.addAll(generateContentDependentKeys(content));

            // Invalidate published content lists
            keysToInvalidate.addAll(generatePublishedContentKeys(content));

            // Invalidate RSS and feed caches
            keysToInvalidate.addAll(generateFeedKeys());

            // Invalidate navigation (might affect menus, breadcrumbs)
            keysToInvalidate.addAll(generateNavigationKeys(content));

            // Invalidate search indices
            keysToInvalidate.addAll(generateSearchKeys(content));

            // Invalidate sitemap
            keysToInvalidate.add("sitemap");
            keysToInvalidate.add("sitemap.xml");

            // Warm the published content immediately - it's now accessible
            String contentKey = generateContentKey(content);
            addCacheOperation("warm", contentKey,
                    Map.of("priority", "critical", "reason", "content_published"));

            // Mark as critical for future operations
            criticalCacheKeys.add(contentKey);

            // Process with high priority
            processInvalidationsWithContext(keysToInvalidate,
                    Map.of("priority", "high", "reason", "content_published"));

            logger.logContentActivity("Cache invalidation completed for content publication",
                    "keys=" + keysToInvalidate.size() + ", content=" + content.getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to process cache invalidation for content publication", null,
                    "cache_invalidation");
        }
    }

    @Override
    public void onContentDeleted(ContentEvent event) {
        eventTypeStats.get(ContentEvent.EventType.DELETED).incrementAndGet();

        logger.logContentActivity("Processing cache invalidation for content deletion",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle() +
                        ", temporary=" + event.isTemporaryDeletion());

        try {
            Content content = event.getContent();
            String contentKey = generateContentKey(content);

            Set<String> keysToInvalidate = new HashSet<>();

            if (event.isTemporaryDeletion()) {
                // Archive - content still exists but not publicly accessible
                keysToInvalidate.addAll(generatePublishedContentKeys(content));
                keysToInvalidate.addAll(generateContentListKeys(content));
                keysToInvalidate.addAll(generateNavigationKeys(content));

                // Remove from critical keys but don't invalidate content itself
                criticalCacheKeys.remove(contentKey);

            } else {
                // Permanent deletion - invalidate everything
                keysToInvalidate.addAll(generateAllContentKeys(content));
                keysToInvalidate.addAll(generateContentDependentKeys(content));
                keysToInvalidate.addAll(generateSearchKeys(content));
                keysToInvalidate.addAll(generateNavigationKeys(content));
                keysToInvalidate.addAll(generateFeedKeys());

                // Remove from critical keys and clean up dependencies
                criticalCacheKeys.remove(contentKey);
                cleanupCacheDependencies(contentKey);
            }

            // Process invalidations
            Map<String, Object> context = Map.of(
                    "temporary", event.isTemporaryDeletion(),
                    "reason", event.isTemporaryDeletion() ? "content_archived" : "content_deleted");

            processInvalidationsWithContext(keysToInvalidate, context);

            logger.logContentActivity("Cache invalidation completed for content deletion",
                    "keys=" + keysToInvalidate.size() +
                            ", temporary=" + event.isTemporaryDeletion() +
                            ", content=" + content.getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to process cache invalidation for content deletion", null, "cache_invalidation");
        }
    }

    // Cache key generation methods

    private String generateContentKey(Content content) {
        return "content:" + content.getId();
    }

    private Set<String> generateContentListKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add("content_list");
        keys.add("content_list:" + content.getClass().getSimpleName().toLowerCase());
        keys.add("recent_content");
        keys.add("content_by_user:" + content.getCreatedBy());
        return keys;
    }

    private Set<String> generatePublishedContentKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add("published_content");
        keys.add("published:" + content.getClass().getSimpleName().toLowerCase());
        keys.add("latest_published");
        keys.add("featured_content");
        return keys;
    }

    private Set<String> generateNavigationKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add("main_navigation");
        keys.add("breadcrumbs");
        keys.add("sidebar_navigation");
        keys.add("category_navigation");
        keys.add("site_menu");
        return keys;
    }

    private Set<String> generateSearchKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add("search_index");
        keys.add("search_results:*"); // Wildcard for all search result caches
        keys.add("content_search:" + content.getClass().getSimpleName().toLowerCase());
        keys.add("faceted_search");
        return keys;
    }

    private Set<String> generateCategoryKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add("category_content");
        keys.add("content_categories");
        keys.add("category_counts");
        return keys;
    }

    private Set<String> generateFeedKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("rss_feed");
        keys.add("atom_feed");
        keys.add("json_feed");
        keys.add("feed_updates");
        return keys;
    }

    private Set<String> generateContentDependentKeys(Content content) {
        Set<String> keys = new HashSet<>();
        String contentKey = generateContentKey(content);

        // Get all keys that depend on this content
        Set<String> dependentKeys = cacheKeyDependencies.get(contentKey);
        if (dependentKeys != null) {
            keys.addAll(dependentKeys);
        }

        // Add common dependent keys
        keys.add("content_summary:" + content.getId());
        keys.add("content_view:" + content.getId());
        keys.add("content_metadata:" + content.getId());
        keys.add("related_content:" + content.getId());

        return keys;
    }

    private Set<String> generateAllContentKeys(Content content) {
        Set<String> keys = new HashSet<>();
        keys.add(generateContentKey(content));
        keys.addAll(generateContentListKeys(content));
        keys.addAll(generatePublishedContentKeys(content));
        keys.addAll(generateContentDependentKeys(content));
        keys.addAll(generateCategoryKeys(content));
        return keys;
    }

    // Cache operation methods

    private void addCacheOperation(String operationType, String cacheKey, Map<String, Object> metadata) {
        CacheOperation operation = new CacheOperation(operationType, cacheKey, metadata);
        operationQueue.offer(operation);

        logger.logContentActivity("Cache operation queued",
                "operation=" + operationType +
                        ", key=" + cacheKey +
                        ", queueSize=" + operationQueue.size());
    }

    private void processInvalidations(Set<String> keys, String reason) {
        processInvalidationsWithContext(keys, Map.of("reason", reason));
    }

    private void processInvalidationsWithContext(Set<String> keys, Map<String, Object> context) {
        if (keys.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int processedKeys = 0;

        for (String key : keys) {
            try {
                // Determine which region this key belongs to
                String region = determineRegion(key);
                CacheRegionConfig config = cacheRegions.get(region);

                if (config != null && config.enableBatching) {
                    // Add to operation queue for batch processing
                    addCacheOperation("invalidate", key, context);
                } else {
                    // Process immediately
                    invalidateKey(key, context);
                }

                // Track invalidation
                invalidatedKeys.add(key);
                regionStats.get(region).incrementAndGet();
                processedKeys++;

            } catch (Exception e) {
                logger.logError(e, "Failed to invalidate cache key: " + key, null, "cache_key_invalidation");
            }
        }

        invalidationCount.addAndGet(processedKeys);

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logContentActivity("Cache invalidation batch processed",
                "keys=" + processedKeys +
                        ", processingTime=" + processingTime + "ms" +
                        ", reason=" + context.get("reason"));
    }

    private void invalidateKey(String key, Map<String, Object> context) {
        // Simulate cache invalidation logic
        logger.logContentActivity("Cache key invalidated",
                "key=" + key +
                        ", reason=" + context.get("reason"));

        // Remove TTL tracking
        cacheKeyTTL.remove(key);

        // Process dependent keys if enabled
        String region = determineRegion(key);
        CacheRegionConfig config = cacheRegions.get(region);

        if (config != null && config.enableDependencies) {
            Set<String> dependentKeys = cacheKeyDependencies.get(key);
            if (dependentKeys != null && !dependentKeys.isEmpty()) {
                processInvalidationsWithContext(dependentKeys,
                        Map.of("reason", "dependency_invalidation", "parent", key));
            }
        }
    }

    private String determineRegion(String key) {
        if (key.startsWith("content:"))
            return "content";
        if (key.startsWith("search") || key.contains("search"))
            return "search";
        if (key.contains("navigation") || key.contains("menu"))
            return "navigation";
        if (key.contains("list") || key.contains("published"))
            return "lists";
        return "fragments"; // Default region
    }

    private void cleanupCacheDependencies(String key) {
        // Remove this key from all dependency mappings
        cacheKeyDependencies.remove(key);

        // Remove this key from other keys' dependency lists
        for (Set<String> dependentSet : cacheKeyDependencies.values()) {
            dependentSet.remove(key);
        }

        logger.logContentActivity("Cache dependencies cleaned up", "key=" + key);
    }

    // Public management methods

    /**
     * Adds a cache key dependency relationship.
     *
     * @param parentKey    The parent cache key
     * @param dependentKey The key that depends on the parent
     */
    public void addCacheDependency(String parentKey, String dependentKey) {
        cacheKeyDependencies.computeIfAbsent(parentKey, k -> ConcurrentHashMap.newKeySet())
                .add(dependentKey);

        logger.logContentActivity("Cache dependency added",
                "parent=" + parentKey + ", dependent=" + dependentKey);
    }

    /**
     * Marks a cache key as critical for performance.
     *
     * @param key The cache key to mark as critical
     */
    public void markAsCritical(String key) {
        criticalCacheKeys.add(key);
        logger.logContentActivity("Cache key marked as critical", "key=" + key);
    }

    /**
     * Processes pending cache operations.
     *
     * @return Number of operations processed
     */
    public int processPendingOperations() {
        int processed = 0;
        CacheOperation operation;

        while ((operation = operationQueue.poll()) != null) {
            try {
                switch (operation.operationType) {
                    case "invalidate":
                        invalidateKey(operation.cacheKey, operation.metadata);
                        break;
                    case "warm":
                        warmCache(operation.cacheKey, operation.metadata);
                        break;
                    case "update":
                        updateCache(operation.cacheKey, operation.metadata);
                        break;
                }
                processed++;
            } catch (Exception e) {
                logger.logError(e, "Failed to process cache operation: " + operation.operationType +
                        " for key: " + operation.cacheKey, null, "cache_operation");
            }
        }

        if (processed > 0) {
            logger.logContentActivity("Pending cache operations processed", "count=" + processed);
        }

        return processed;
    }

    private void warmCache(String key, Map<String, Object> metadata) {
        // Simulate cache warming logic
        logger.logContentActivity("Cache warmed",
                "key=" + key +
                        ", priority=" + metadata.get("priority"));
    }

    private void updateCache(String key, Map<String, Object> metadata) {
        // Simulate cache update logic
        logger.logContentActivity("Cache updated",
                "key=" + key);
    }

    /**
     * Gets comprehensive cache invalidation statistics.
     *
     * @return Map of cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalInvalidations", invalidationCount.get());
        stats.put("pendingOperations", operationQueue.size());
        stats.put("trackedDependencies", cacheKeyDependencies.size());
        stats.put("criticalKeys", criticalCacheKeys.size());
        stats.put("invalidatedKeys", invalidatedKeys.size());

        // Event type breakdown
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        for (Map.Entry<ContentEvent.EventType, AtomicLong> entry : eventTypeStats.entrySet()) {
            eventCounts.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("eventTypeCounts", eventCounts);

        // Region statistics
        Map<String, Long> regionCounts = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : regionStats.entrySet()) {
            regionCounts.put(entry.getKey(), entry.getValue().get());
        }
        stats.put("regionCounts", regionCounts);

        return stats;
    }

    /**
     * Clears all invalidated keys from tracking.
     */
    public void clearInvalidatedKeys() {
        int cleared = invalidatedKeys.size();
        invalidatedKeys.clear();
        logger.logContentActivity("Invalidated keys cleared", "count=" + cleared);
    }

    // ContentObserver interface methods

    @Override
    public String getObserverName() {
        return "Cache Invalidation Observer";
    }

    @Override
    public int getPriority() {
        return 10; // High priority - cache invalidation should happen early
    }

    @Override
    public boolean shouldObserve(Class<?> contentType) {
        // Observe all content types for cache invalidation
        return Content.class.isAssignableFrom(contentType);
    }
}
