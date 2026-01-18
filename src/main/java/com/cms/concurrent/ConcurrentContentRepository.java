package com.cms.concurrent;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.exception.ContentManagementException;
import com.cms.util.CMSLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.time.LocalDateTime;

/**
 * Thread-safe content repository implementation using advanced concurrent
 * programming
 * techniques for high-performance multi-threaded content management operations.
 *
 * <p>
 * This repository provides comprehensive thread-safe operations for content
 * storage,
 * retrieval, and management using advanced Java concurrency primitives
 * including
 * ConcurrentHashMap, ReadWriteLock, and atomic operations for optimal
 * performance
 * in multi-threaded environments.
 * </p>
 *
 * <p>
 * <strong>Multithreading Implementation:</strong> Provides comprehensive
 * thread-safe repository
 * operations with ConcurrentHashMap, ReadWriteLock for read-heavy operations,
 * atomic
 * counters, concurrent collections, and proper synchronization for all CRUD
 * operations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Thread-safe CRUD operations with ConcurrentHashMap storage</li>
 * <li>ReadWriteLock optimization for concurrent read operations</li>
 * <li>Atomic operations for counters and statistics</li>
 * <li>ConcurrentLinkedQueue for operation history tracking</li>
 * <li>Thread-safe search and filtering with parallel streams</li>
 * <li>Concurrent bulk operations with batch processing</li>
 * <li>Lock-free statistics and performance monitoring</li>
 * </ul>
 *
 * <p>
 * <strong>Concurrency Features:</strong>
 * </p>
 * <ul>
 * <li><strong>Storage:</strong> ConcurrentHashMap for thread-safe content
 * storage</li>
 * <li><strong>Indexing:</strong> Concurrent indexes for fast lookups by various
 * criteria</li>
 * <li><strong>Locking:</strong> ReadWriteLock for optimized read-heavy
 * workloads</li>
 * <li><strong>Atomic Operations:</strong> AtomicLong for counters and
 * statistics</li>
 * <li><strong>Collections:</strong> Concurrent collections throughout for
 * thread safety</li>
 * </ul>
 *
 * <p>
 * <strong>Integration:</strong> Works seamlessly with AsyncContentProcessor,
 * Observer Pattern
 * for event notifications, and Factory Pattern for content creation in
 * multithreaded contexts.
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class ConcurrentContentRepository {

    private static final CMSLogger logger = CMSLogger.getInstance();

    // Thread-safe storage using concurrent collections
    private final ConcurrentHashMap<String, Content> contentStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> titleIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ContentStatus, Set<String>> statusIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> authorIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<LocalDateTime>> contentTimestamps = new ConcurrentHashMap<>();

    // ReadWriteLock for optimized concurrent access patterns
    private final ReadWriteLock repositoryLock = new ReentrantReadWriteLock();

    // Atomic counters for statistics and performance monitoring
    private final AtomicLong totalReads = new AtomicLong(0);
    private final AtomicLong totalWrites = new AtomicLong(0);
    private final AtomicLong totalDeletes = new AtomicLong(0);
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // Concurrent operation history and audit trail
    private final ConcurrentLinkedQueue<OperationRecord> operationHistory = new ConcurrentLinkedQueue<>();
    private final int MAX_HISTORY_SIZE = 10000;

    // Thread-safe configuration
    private volatile int maxConcurrentOperations = 100;
    private volatile boolean enableDetailedLogging = false;

    /**
     * Constructs a new ConcurrentContentRepository with thread-safe initialization.
     * Initializes all concurrent data structures and indexes for optimal
     * performance.
     */
    public ConcurrentContentRepository() {
        initializeIndexes();
        logger.logSystemOperation("ConcurrentContentRepository initialized with thread-safe operations");
    }

    /**
     * Saves content to the repository using thread-safe operations with automatic
     * indexing.
     * Provides strong consistency guarantees and atomic operations for concurrent
     * access.
     *
     * @param content The content to save (must not be null)
     * @return The saved content with updated metadata
     * @throws ContentManagementException if save operation fails
     */
    public Content save(Content content) throws ContentManagementException {
        if (content == null) {
            throw new ContentManagementException("Content cannot be null", "Invalid content provided");
        }

        repositoryLock.writeLock().lock();
        try {
            String contentId = content.getId();
            boolean isUpdate = contentStorage.containsKey(contentId);

            // Update timestamps atomically
            contentTimestamps.put(contentId, new AtomicReference<>(LocalDateTime.now()));

            // Remove from old indexes if updating
            if (isUpdate) {
                removeFromIndexes(contentStorage.get(contentId));
            }

            // Store content and update indexes atomically
            Content savedContent;
            try {
                savedContent = content.clone(); // Defensive copy
            } catch (CloneNotSupportedException e) {
                throw new ContentManagementException("Content cloning failed", "Unable to save content", e);
            }
            contentStorage.put(contentId, savedContent);
            addToIndexes(savedContent);

            // Update statistics
            totalWrites.incrementAndGet();

            // Record operation
            recordOperation("SAVE", contentId, isUpdate ? "UPDATE" : "CREATE");

            logger.logContentOperation(
                    (isUpdate ? "Updated" : "Created") + " content: " + content.getTitle() +
                            " (ID: " + contentId + ")");

            return savedContent;

        } catch (Exception e) {
            logger.logError("Failed to save content: " + content.getTitle(), e);
            throw new ContentManagementException("Save operation failed", "Unable to save content", e);
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    /**
     * Finds content by ID using optimized thread-safe read operations.
     * Uses ReadWriteLock for concurrent read access without blocking other readers.
     *
     * @param contentId The unique identifier of the content
     * @return Optional containing the content if found, empty otherwise
     */
    public Optional<Content> findById(String contentId) {
        if (contentId == null || contentId.trim().isEmpty()) {
            return Optional.empty();
        }

        repositoryLock.readLock().lock();
        try {
            Content content = contentStorage.get(contentId);
            totalReads.incrementAndGet();

            if (content != null) {
                cacheHits.incrementAndGet();
                recordOperation("READ", contentId, "CACHE_HIT");

                if (enableDetailedLogging) {
                    logger.logContentOperation("Found content by ID: " + contentId);
                }

                try {
                    return Optional.of(content.clone()); // Defensive copy
                } catch (CloneNotSupportedException e) {
                    logger.logError("Failed to clone content for retrieval: " + content.getId(), e);
                    return Optional.of(content); // Return original if cloning fails
                }
            } else {
                cacheMisses.incrementAndGet();
                recordOperation("READ", contentId, "CACHE_MISS");
                return Optional.empty();
            }

        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    /**
     * Finds all content with the specified status using concurrent index lookup.
     * Leverages concurrent collections and parallel streams for optimal
     * performance.
     *
     * @param status The content status to search for
     * @return List of content items with the specified status
     */
    public List<Content> findByStatus(ContentStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }

        repositoryLock.readLock().lock();
        try {
            Set<String> contentIds = statusIndex.get(status);
            if (contentIds == null) {
                return new ArrayList<>();
            }

            List<Content> results = contentIds.parallelStream()
                    .map(contentStorage::get)
                    .filter(Objects::nonNull)
                    .map(content -> {
                        try {
                            return content.clone();
                        } catch (CloneNotSupportedException e) {
                            logger.logError("Failed to clone content: " + content.getId(), e);
                            return content;
                        }
                    }) // Defensive copies
                    .collect(Collectors.toList());

            totalSearches.incrementAndGet();
            recordOperation("SEARCH", "status:" + status, "FOUND_" + results.size());

            logger.logContentOperation("Found " + results.size() + " content items with status: " + status);

            return results;

        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    /**
     * Finds content by author using concurrent index-based search.
     * Optimized for high-performance concurrent access patterns.
     *
     * @param author The author to search for
     * @return List of content items by the specified author
     */
    public List<Content> findByAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            return new ArrayList<>();
        }

        repositoryLock.readLock().lock();
        try {
            Set<String> contentIds = authorIndex.get(author);
            if (contentIds == null) {
                return new ArrayList<>();
            }

            List<Content> results = contentIds.parallelStream()
                    .map(contentStorage::get)
                    .filter(Objects::nonNull)
                    .map(content -> {
                        try {
                            return content.clone();
                        } catch (CloneNotSupportedException e) {
                            logger.logError("Failed to clone content: " + content.getId(), e);
                            return content;
                        }
                    }) // Defensive copies
                    .collect(Collectors.toList());

            totalSearches.incrementAndGet();
            recordOperation("SEARCH", "author:" + author, "FOUND_" + results.size());

            logger.logContentOperation("Found " + results.size() + " content items by author: " + author);

            return results;

        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    /**
     * Searches content using a predicate with parallel processing for performance.
     * Utilizes parallel streams for efficient filtering in concurrent environments.
     *
     * @param predicate The search predicate to apply
     * @return List of content items matching the predicate
     */
    public List<Content> search(Predicate<Content> predicate) {
        if (predicate == null) {
            return new ArrayList<>();
        }

        repositoryLock.readLock().lock();
        try {
            List<Content> results = contentStorage.values().parallelStream()
                    .filter(predicate)
                    .map(content -> {
                        try {
                            return content.clone();
                        } catch (CloneNotSupportedException e) {
                            logger.logError("Failed to clone content: " + content.getId(), e);
                            return content;
                        }
                    }) // Defensive copies
                    .collect(Collectors.toList());

            totalSearches.incrementAndGet();
            recordOperation("SEARCH", "predicate", "FOUND_" + results.size());

            logger.logContentOperation("Predicate search found " + results.size() + " content items");

            return results;

        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    /**
     * Deletes content by ID using atomic operations with index cleanup.
     * Ensures strong consistency and proper resource cleanup in concurrent
     * environment.
     *
     * @param contentId The ID of the content to delete
     * @return true if content was deleted, false if not found
     * @throws ContentManagementException if delete operation fails
     */
    public boolean delete(String contentId) throws ContentManagementException {
        if (contentId == null || contentId.trim().isEmpty()) {
            return false;
        }

        repositoryLock.writeLock().lock();
        try {
            Content content = contentStorage.get(contentId);
            if (content == null) {
                return false;
            }

            // Remove from storage and all indexes atomically
            contentStorage.remove(contentId);
            removeFromIndexes(content);
            contentTimestamps.remove(contentId);

            // Update statistics
            totalDeletes.incrementAndGet();

            // Record operation
            recordOperation("DELETE", contentId, "SUCCESS");

            logger.logContentOperation("Deleted content: " + content.getTitle() + " (ID: " + contentId + ")");

            return true;

        } catch (Exception e) {
            logger.logError("Failed to delete content: " + contentId, e);
            throw new ContentManagementException("Delete operation failed", "Unable to delete content", e);
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    /**
     * Performs bulk save operations with optimized batch processing for high
     * throughput.
     * Uses concurrent processing while maintaining consistency guarantees.
     *
     * @param contentList List of content items to save
     * @return List of successfully saved content items
     * @throws ContentManagementException if bulk operation fails
     */
    public List<Content> saveBatch(List<Content> contentList) throws ContentManagementException {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }

        repositoryLock.writeLock().lock();
        try {
            List<Content> savedContent = new ArrayList<>();

            for (Content content : contentList) {
                if (content != null) {
                    Content saved = save(content);
                    savedContent.add(saved);
                }
            }

            recordOperation("BULK_SAVE", "batch", "COUNT_" + savedContent.size());

            logger.logContentOperation("Bulk saved " + savedContent.size() + " content items");

            return savedContent;

        } catch (Exception e) {
            logger.logError("Bulk save operation failed", e);
            throw new ContentManagementException("Bulk save operation failed", "Unable to save content", e);
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    /**
     * Gets comprehensive repository statistics including concurrent operation
     * metrics.
     * Provides detailed performance and usage statistics for monitoring and
     * optimization.
     *
     * @return Map containing detailed repository statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        // Storage statistics
        stats.put("totalContentItems", contentStorage.size());
        stats.put("totalReads", totalReads.get());
        stats.put("totalWrites", totalWrites.get());
        stats.put("totalDeletes", totalDeletes.get());
        stats.put("totalSearches", totalSearches.get());

        // Cache performance
        long totalCacheOperations = cacheHits.get() + cacheMisses.get();
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        stats.put("cacheHitRatio", totalCacheOperations > 0 ? (double) cacheHits.get() / totalCacheOperations : 0.0);

        // Index statistics
        Map<String, Integer> indexSizes = new ConcurrentHashMap<>();
        indexSizes.put("titleIndex", titleIndex.size());
        indexSizes.put("statusIndex", statusIndex.size());
        indexSizes.put("authorIndex", authorIndex.size());
        stats.put("indexSizes", indexSizes);

        // Configuration
        stats.put("maxConcurrentOperations", maxConcurrentOperations);
        stats.put("enableDetailedLogging", enableDetailedLogging);
        stats.put("operationHistorySize", operationHistory.size());

        return stats;
    }

    /**
     * Gets recent operation history for audit and monitoring purposes.
     * Returns thread-safe copy of recent operations for analysis.
     *
     * @param limit Maximum number of recent operations to return
     * @return List of recent operation records
     */
    public List<OperationRecord> getRecentOperations(int limit) {
        return operationHistory.stream()
                .skip(Math.max(0, operationHistory.size() - limit))
                .collect(Collectors.toList());
    }

    /**
     * Clears all content from the repository with proper cleanup of indexes and
     * statistics.
     * Thread-safe operation that resets all concurrent data structures.
     */
    public void clear() {
        repositoryLock.writeLock().lock();
        try {
            contentStorage.clear();
            clearAllIndexes();
            contentTimestamps.clear();
            operationHistory.clear();

            // Reset statistics
            totalReads.set(0);
            totalWrites.set(0);
            totalDeletes.set(0);
            totalSearches.set(0);
            cacheHits.set(0);
            cacheMisses.set(0);

            recordOperation("CLEAR", "repository", "SUCCESS");

            logger.logSystemOperation("Repository cleared - all content and indexes reset");

        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    // Private helper methods for index management and operations

    /**
     * Initializes all concurrent indexes for optimal lookup performance.
     */
    private void initializeIndexes() {
        // Initialize status index with all possible statuses
        for (ContentStatus status : ContentStatus.values()) {
            statusIndex.put(status, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Adds content to all relevant indexes for fast lookups.
     */
    private void addToIndexes(Content content) {
        String contentId = content.getId();

        // Title index (for partial title searches)
        String titleKey = content.getTitle().toLowerCase();
        titleIndex.computeIfAbsent(titleKey, k -> ConcurrentHashMap.newKeySet()).add(contentId);

        // Status index
        statusIndex.get(content.getStatus()).add(contentId);

        // Author index
        String author = content.getCreatedBy();
        if (author != null) {
            authorIndex.computeIfAbsent(author, k -> ConcurrentHashMap.newKeySet()).add(contentId);
        }
    }

    /**
     * Removes content from all indexes during updates or deletes.
     */
    private void removeFromIndexes(Content content) {
        String contentId = content.getId();

        // Remove from title index
        String titleKey = content.getTitle().toLowerCase();
        Set<String> titleSet = titleIndex.get(titleKey);
        if (titleSet != null) {
            titleSet.remove(contentId);
            if (titleSet.isEmpty()) {
                titleIndex.remove(titleKey);
            }
        }

        // Remove from status index
        statusIndex.get(content.getStatus()).remove(contentId);

        // Remove from author index
        String author = content.getCreatedBy();
        if (author != null) {
            Set<String> authorSet = authorIndex.get(author);
            if (authorSet != null) {
                authorSet.remove(contentId);
                if (authorSet.isEmpty()) {
                    authorIndex.remove(author);
                }
            }
        }
    }

    /**
     * Clears all indexes during repository reset operations.
     */
    private void clearAllIndexes() {
        titleIndex.clear();
        statusIndex.clear();
        authorIndex.clear();

        // Reinitialize status index
        for (ContentStatus status : ContentStatus.values()) {
            statusIndex.put(status, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Records an operation in the concurrent operation history for audit purposes.
     */
    private void recordOperation(String operation, String target, String result) {
        OperationRecord record = new OperationRecord(operation, target, result, LocalDateTime.now());
        operationHistory.offer(record);

        // Maintain history size limit
        while (operationHistory.size() > MAX_HISTORY_SIZE) {
            operationHistory.poll();
        }
    }

    /**
     * Configuration methods for runtime tuning
     */
    public void setMaxConcurrentOperations(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
    }

    /**
     * Immutable record representing a repository operation for audit purposes.
     */
    public static class OperationRecord {
        private final String operation;
        private final String target;
        private final String result;
        private final LocalDateTime timestamp;

        public OperationRecord(String operation, String target, String result, LocalDateTime timestamp) {
            this.operation = operation;
            this.target = target;
            this.result = result;
            this.timestamp = timestamp;
        }

        public String getOperation() {
            return operation;
        }

        public String getTarget() {
            return target;
        }

        public String getResult() {
            return result;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("OperationRecord{operation='%s', target='%s', result='%s', timestamp=%s}",
                    operation, target, result, timestamp);
        }
    }
}
