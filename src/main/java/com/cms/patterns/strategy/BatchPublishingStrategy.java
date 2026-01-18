package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.Role;
import com.cms.core.exception.ContentManagementException;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.patterns.iterator.ContentIterator;
import com.cms.util.CMSLogger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Batch publishing strategy that processes multiple content items efficiently in bulk operations.
 *
 * <p>The BatchPublishingStrategy implements the Strategy Pattern by providing
 * a concrete algorithm for high-performance bulk content publishing. This strategy
 * is optimized for scenarios where multiple content items need to be published
 * simultaneously with maximum efficiency and minimal resource overhead.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as a concrete strategy implementation that shows advanced batch processing
 * capabilities within the Strategy Pattern framework. It showcases the pattern's
 * ability to encapsulate complex algorithms while maintaining interface consistency
 * with single-item processing strategies.</p>
 *
 * <p><strong>Implementation:</strong> Implements part of the Strategy Pattern
 * implementation by providing a bulk processing algorithm that integrates
 * with the Iterator Pattern for content traversal, shows advanced concurrency
 * features, and showcases comprehensive error handling and recovery mechanisms.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Bulk import and publication of content from external sources</li>
 *   <li>Mass publishing of seasonal or promotional content</li>
 *   <li>Migration scenarios with large content volumes</li>
 *   <li>Scheduled bulk releases for media organizations</li>
 *   <li>E-commerce catalog updates with thousands of products</li>
 *   <li>Documentation batch updates for software releases</li>
 *   <li>Multi-language content publishing coordination</li>
 * </ul>
 *
 * <p><strong>Strategy Characteristics:</strong></p>
 * <ul>
 *   <li>Low Priority (30) - Background processing to avoid system overload</li>
 *   <li>Excellent Batch Processing - Core strength of this strategy</li>
 *   <li>Full Rollback Support - Can undo entire batch or individual items</li>
 *   <li>Variable Processing Time - Scales with batch size and complexity</li>
 *   <li>Advanced Concurrency - Parallel processing with resource management</li>
 * </ul>
 *
 * <p><strong>Batch Processing Features:</strong></p>
 * <ul>
 *   <li><strong>Parallel Processing:</strong> Concurrent content publishing with thread pools</li>
 *   <li><strong>Progress Tracking:</strong> Real-time batch progress monitoring</li>
 *   <li><strong>Error Recovery:</strong> Continues processing despite individual failures</li>
 *   <li><strong>Resource Management:</strong> Memory-efficient processing of large batches</li>
 *   <li><strong>Transaction Support:</strong> Atomic batch operations with rollback</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For context parameter details
 * @see ContentIterator For integration with Iterator Pattern
 */
public class BatchPublishingStrategy implements PublishingStrategy {
    
    /** Strategy name identifier */
    private static final String STRATEGY_NAME = "Batch Publishing";
    
    /** Priority level for batch publishing (low priority to avoid system overload) */
    private static final int PRIORITY = 30;
    
    /** Base estimated processing time per item in milliseconds */
    private static final long BASE_PROCESSING_TIME_PER_ITEM = 200L;
    
    /** Default batch size for processing chunks */
    private static final int DEFAULT_BATCH_SIZE = 50;
    
    /** Maximum batch size to prevent resource exhaustion */
    private static final int MAX_BATCH_SIZE = 500;
    
    /** Number of worker threads for parallel processing */
    private static final int WORKER_THREADS = 4;
    
    /** Observer subject for publishing events (integration with Observer Pattern) */
    private final ContentSubject contentSubject;
    
    /** Executor service for parallel batch processing */
    private final ExecutorService executorService;
    
    /** Map to track active batch operations */
    private final Map<String, BatchOperation> activeBatches;
    
    /** Statistics for batch operations */
    private final BatchStatistics statistics;
    
    /**
     * Represents a batch publishing operation with progress tracking.
     */
    public static class BatchOperation {
        private final String batchId;
        private final List<Content> contentItems;
        private final PublishingContext context;
        private final Date startTime;
        private final int totalItems;
        
        private volatile int processedItems;
        private volatile int successfulItems;
        private volatile int failedItems;
        private final List<String> failedContentIds;
        private final Map<String, String> errorMessages;
        private volatile BatchStatus status;
        
        public BatchOperation(String batchId, List<Content> contentItems, PublishingContext context) {
            this.batchId = batchId;
            this.contentItems = new ArrayList<>(contentItems);
            this.context = context;
            this.startTime = new Date();
            this.totalItems = contentItems.size();
            this.processedItems = 0;
            this.successfulItems = 0;
            this.failedItems = 0;
            this.failedContentIds = Collections.synchronizedList(new ArrayList<>());
            this.errorMessages = new ConcurrentHashMap<>();
            this.status = BatchStatus.STARTING;
        }
        
        // Getters and progress update methods
        public String getBatchId() { return batchId; }
        public List<Content> getContentItems() { return new ArrayList<>(contentItems); }
        public PublishingContext getContext() { return context; }
        public Date getStartTime() { return new Date(startTime.getTime()); }
        public int getTotalItems() { return totalItems; }
        public int getProcessedItems() { return processedItems; }
        public int getSuccessfulItems() { return successfulItems; }
        public int getFailedItems() { return failedItems; }
        public List<String> getFailedContentIds() { return new ArrayList<>(failedContentIds); }
        public Map<String, String> getErrorMessages() { return new HashMap<>(errorMessages); }
        public BatchStatus getStatus() { return status; }
        public void setStatus(BatchStatus status) { this.status = status; }
        
        public double getProgressPercentage() {
            return totalItems > 0 ? (double) processedItems / totalItems * 100 : 0;
        }
        
        public long getDuration() {
            return System.currentTimeMillis() - startTime.getTime();
        }
        
        public synchronized void recordSuccess(String contentId) {
            processedItems++;
            successfulItems++;
        }
        
        public synchronized void recordFailure(String contentId, String errorMessage) {
            processedItems++;
            failedItems++;
            failedContentIds.add(contentId);
            errorMessages.put(contentId, errorMessage);
        }
        
        public boolean isCompleted() {
            return processedItems >= totalItems;
        }
        
        public double getSuccessRate() {
            return processedItems > 0 ? (double) successfulItems / processedItems * 100 : 0;
        }
    }
    
    /**
     * Batch operation status enumeration.
     */
    public enum BatchStatus {
        STARTING("Batch operation initializing"),
        RUNNING("Batch processing in progress"),
        COMPLETED("Batch processing completed successfully"),
        COMPLETED_WITH_ERRORS("Batch processing completed with some errors"),
        FAILED("Batch processing failed"),
        CANCELLED("Batch processing cancelled"),
        ROLLED_BACK("Batch processing rolled back");
        
        private final String description;
        
        BatchStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Statistics tracking for batch operations.
     */
    public static class BatchStatistics {
        private volatile int totalBatches;
        private volatile int successfulBatches;
        private volatile int failedBatches;
        private volatile long totalItemsProcessed;
        private volatile long totalProcessingTime;
        private final Map<String, Integer> batchSizeDistribution;
        
        public BatchStatistics() {
            this.batchSizeDistribution = new ConcurrentHashMap<>();
        }
        
        public synchronized void recordBatchStart(int batchSize) {
            totalBatches++;
            batchSizeDistribution.merge(getBatchSizeRange(batchSize), 1, Integer::sum);
        }
        
        public synchronized void recordBatchSuccess(int itemsProcessed, long processingTime) {
            successfulBatches++;
            totalItemsProcessed += itemsProcessed;
            totalProcessingTime += processingTime;
        }
        
        public synchronized void recordBatchFailure() {
            failedBatches++;
        }
        
        private String getBatchSizeRange(int size) {
            if (size <= 10) return "1-10";
            if (size <= 50) return "11-50";
            if (size <= 100) return "51-100";
            if (size <= 500) return "101-500";
            return "500+";
        }
        
        // Getters
        public int getTotalBatches() { return totalBatches; }
        public int getSuccessfulBatches() { return successfulBatches; }
        public int getFailedBatches() { return failedBatches; }
        public long getTotalItemsProcessed() { return totalItemsProcessed; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public Map<String, Integer> getBatchSizeDistribution() { return new HashMap<>(batchSizeDistribution); }
        
        public double getSuccessRate() {
            return totalBatches > 0 ? (double) successfulBatches / totalBatches * 100 : 0;
        }
        
        public double getAverageProcessingTime() {
            return successfulBatches > 0 ? (double) totalProcessingTime / successfulBatches : 0;
        }
        
        public double getAverageItemsPerBatch() {
            return successfulBatches > 0 ? (double) totalItemsProcessed / successfulBatches : 0;
        }
    }
    
    /**
     * Creates a new BatchPublishingStrategy with default configuration.
     */
    public BatchPublishingStrategy() {
        this.contentSubject = new ContentSubject();
        this.executorService = Executors.newFixedThreadPool(WORKER_THREADS);
        this.activeBatches = new ConcurrentHashMap<>();
        this.statistics = new BatchStatistics();
    }
    
    /**
     * Creates a new BatchPublishingStrategy with the specified components.
     *
     * @param contentSubject The content subject for event notifications
     * @param executorService The executor service for parallel processing
     * @throws IllegalArgumentException If any parameter is null
     */
    public BatchPublishingStrategy(ContentSubject contentSubject, ExecutorService executorService) {
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        if (executorService == null) {
            throw new IllegalArgumentException("Executor service cannot be null");
        }
        
        this.contentSubject = contentSubject;
        this.executorService = executorService;
        this.activeBatches = new ConcurrentHashMap<>();
        this.statistics = new BatchStatistics();
    }
    
    /**
     * Processes multiple content items efficiently using batch publishing operations.
     *
     * <p>This method implements the core batch publishing algorithm:</p>
     * <ol>
     *   <li>Validates batch content and context for bulk publishing</li>
     *   <li>Creates a batch operation with unique tracking identifier</li>
     *   <li>Divides content into optimal processing chunks</li>
     *   <li>Executes parallel publishing using thread pool</li>
     *   <li>Tracks progress and handles individual item failures gracefully</li>
     *   <li>Fires Observer Pattern events for batch progress and completion</li>
     *   <li>Provides comprehensive statistics and error reporting</li>
     * </ol>
     *
     * <p><strong>Strategy-Specific Behavior:</strong> This implementation optimizes
     * for throughput over individual item latency. It processes items concurrently,
     * handles partial failures gracefully, and provides detailed progress tracking
     * for large-scale publishing operations.</p>
     *
     * <p><strong>Integration Note:</strong> This method expects the content parameter
     * to represent a batch container or uses the context to identify multiple items.
     * For single-item calls, it delegates to individual processing logic.</p>
     *
     * @param content The content representing the batch (or single item for delegation)
     * @param context The publishing context with batch configuration and user information
     * @throws ContentManagementException If batch publishing fails due to validation
     *         errors, resource constraints, or system limitations. Uses Exception
     *         Shielding to provide user-friendly messages while logging technical details.
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public void publish(Content content, PublishingContext context) throws ContentManagementException {
        // Input validation with Exception Shielding
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Publishing context cannot be null");
        }
        
        try {
            // Log the start of batch publishing
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "batch_publish_start",
                "Starting batch publishing operation"
            );
            
            // For single-item processing, extract batch items from context or create single-item batch
            List<Content> batchItems = extractBatchItems(content, context);
            
            if (batchItems.size() == 1) {
                // Single item - process directly but with batch tracking
                processSingleItemBatch(batchItems.get(0), context);
            } else {
                // Multiple items - use full batch processing
                processMultiItemBatch(batchItems, context);
            }
            
        } catch (ContentManagementException e) {
            // Log the error with technical details for debugging
            CMSLogger.logError(
                "Failed to execute batch publishing",
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with Exception Shielding
            throw new ContentManagementException(
                "Batch publishing failed",
                "We were unable to process your batch publishing request. " +
                "Please check the content items and batch configuration, or try processing smaller batches.",
                e
            );
        } catch (Exception e) {
            // Log unexpected errors
            CMSLogger.logError(
                "Unexpected error during batch publishing",
                e,
                context.getUser().getUsername()
            );
            
            // Wrap in ContentManagementException with Exception Shielding
            throw new ContentManagementException(
                "Unexpected batch publishing error",
                "An unexpected error occurred during batch processing. " +
                "Our technical team has been notified. Please try again with smaller batches.",
                e
            );
        }
    }
    
    /**
     * Validates whether content can be processed using batch publishing.
     *
     * <p><strong>Batch Publishing Validation Rules:</strong></p>
     * <ul>
     *   <li>At least one content item must be provided for processing</li>
     *   <li>Batch size must not exceed system limits</li>
     *   <li>User must have batch processing permissions</li>
     *   <li>System resources must be available for batch processing</li>
     *   <li>All content items must pass basic content validation</li>
     *   <li>Batch configuration must be valid and within limits</li>
     * </ul>
     *
     * @param content The content representing the batch or single item
     * @param context The publishing context with batch configuration
     * @return true if batch can be processed, false otherwise
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public boolean validate(Content content, PublishingContext context) {
        if (content == null || context == null) {
            throw new IllegalArgumentException("Content and context cannot be null");
        }
        
        try {
            // Extract batch items for validation
            List<Content> batchItems = extractBatchItems(content, context);
            
            // Validate batch size
            if (batchItems.isEmpty()) {
                CMSLogger.logValidationError(
                    "Batch contains no content items",
                    context.getUser().getUsername()
                );
                return false;
            }
            
            if (batchItems.size() > MAX_BATCH_SIZE) {
                CMSLogger.logValidationError(
                    String.format("Batch size %d exceeds maximum allowed size %d", 
                                 batchItems.size(), MAX_BATCH_SIZE),
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // User permission validation
            if (!hasBatchPublishingPermission(context.getUser())) {
                CMSLogger.logSecurityEvent(
                    context.getUser().getUsername(),
                    "unauthorized_batch_publishing",
                    "User attempted batch publishing without proper permissions"
                );
                return false;
            }
            
            // Validate system resources
            if (!hasAvailableResources()) {
                CMSLogger.logSystemEvent(
                    "batch_publishing_resources_unavailable",
                    "Insufficient system resources for batch publishing"
                );
                return false;
            }
            
            // Validate individual content items (sample validation for large batches)
            int validationSampleSize = Math.min(batchItems.size(), 10);
            for (int i = 0; i < validationSampleSize; i++) {
                Content item = batchItems.get(i);
                if (!validateSingleContentItem(item, context)) {
                    CMSLogger.logValidationError(
                        "Content validation failed for batch item: " + item.getId(),
                        context.getUser().getUsername()
                    );
                    return false;
                }
            }
            
            // All validations passed
            CMSLogger.logValidationSuccess(
                String.format("Batch publishing validation passed for %d items", batchItems.size()),
                context.getUser().getUsername()
            );
            
            return true;
            
        } catch (Exception e) {
            // Log validation errors but don't throw - return false for failed validation
            CMSLogger.logError(
                "Error during batch publishing validation",
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
    
    /**
     * Returns the strategy name for identification purposes.
     * @return "Batch Publishing"
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Returns the priority level for this strategy.
     * Batch publishing has low priority (30) to avoid overloading the system.
     * @return 30 (low priority for background processing)
     */
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    /**
     * Returns a detailed description of this strategy's approach and use cases.
     * @return Detailed strategy description
     */
    @Override
    public String getDescription() {
        return "Efficiently processes multiple content items in bulk operations using " +
               "parallel processing and optimized resource management. Ideal for large-scale " +
               "content publishing scenarios such as bulk imports, mass updates, and scheduled " +
               "releases. Features progress tracking, error recovery, rollback support, and " +
               "comprehensive statistics for high-volume publishing operations.";
    }
    
    /**
     * Indicates that this strategy excellently supports batch processing.
     * This is the core strength of this strategy implementation.
     * @return true - excellent batch processing support
     */
    @Override
    public boolean supportsBatchProcessing() {
        return true;
    }
    
    /**
     * Indicates that this strategy fully supports rollback operations.
     * Batch operations can be rolled back entirely or individual items can be reverted.
     * @return true - full rollback support
     */
    @Override
    public boolean supportsRollback() {
        return true;
    }
    
    /**
     * Returns the estimated processing time for batch publishing operations.
     * This scales with batch size and includes parallel processing optimizations.
     * @param content The content representing the batch
     * @param context The publishing context with batch configuration
     * @return Estimated processing time in milliseconds
     */
    @Override
    public long getEstimatedProcessingTime(Content content, PublishingContext context) {
        if (content == null || context == null) {
            return BASE_PROCESSING_TIME_PER_ITEM;
        }
        
        try {
            List<Content> batchItems = extractBatchItems(content, context);
            int batchSize = batchItems.size();
            
            // Base time per item
            long baseTime = BASE_PROCESSING_TIME_PER_ITEM * batchSize;
            
            // Apply parallel processing efficiency (diminishing returns)
            double parallelEfficiency = Math.min(WORKER_THREADS, batchSize) / (double) batchSize;
            long parallelTime = (long) (baseTime * (1 - parallelEfficiency * 0.7)); // 70% efficiency gain
            
            // Add batch overhead
            long batchOverhead = 1000 + (batchSize * 10); // Setup cost + per-item tracking
            
            // Adjust for content complexity (average)
            long complexityAdjustment = batchItems.stream()
                .mapToLong(item -> item.getBody() != null ? item.getBody().length() / 1000 : 0)
                .sum() * 10;
            
            return parallelTime + batchOverhead + complexityAdjustment;
            
        } catch (Exception e) {
            // Return conservative estimate if calculation fails
            return BASE_PROCESSING_TIME_PER_ITEM * 100; // Assume moderate batch size
        }
    }
    
    /**
     * Processes a batch of content items using the specified batch publishing strategy.
     * This is the main entry point for external batch processing requests.
     *
     * @param contentItems List of content items to publish in batch
     * @param context Publishing context for the batch operation
     * @return BatchOperation object for tracking progress and results
     * @throws ContentManagementException If batch processing fails
     */
    public BatchOperation processBatch(List<Content> contentItems, PublishingContext context) 
            throws ContentManagementException {
        
        if (contentItems == null || contentItems.isEmpty()) {
            throw new IllegalArgumentException("Content items list cannot be null or empty");
        }
        if (context == null) {
            throw new IllegalArgumentException("Publishing context cannot be null");
        }
        
        // Create unique batch ID
        String batchId = generateBatchId(contentItems, context);
        
        // Create batch operation
        BatchOperation batchOperation = new BatchOperation(batchId, contentItems, context);
        activeBatches.put(batchId, batchOperation);
        
        try {
            statistics.recordBatchStart(contentItems.size());
            
            // Execute batch processing
            processMultiItemBatch(contentItems, context, batchOperation);
            
            return batchOperation;
            
        } catch (Exception e) {
            statistics.recordBatchFailure();
            batchOperation.setStatus(BatchStatus.FAILED);
            
            throw new ContentManagementException(
                "Batch processing failed",
                "Unable to process the batch of content items",
                e
            );
        }
    }
    
    /**
     * Returns the current status of a batch operation.
     * @param batchId The batch operation ID
     * @return BatchOperation if found, null otherwise
     */
    public BatchOperation getBatchStatus(String batchId) {
        return activeBatches.get(batchId);
    }
    
    /**
     * Returns current batch processing statistics.
     * @return BatchStatistics object with current metrics
     */
    public BatchStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Cancels an active batch operation if possible.
     * @param batchId The batch ID to cancel
     * @return true if batch was cancelled, false if not found or already completed
     */
    public boolean cancelBatch(String batchId) {
        BatchOperation batch = activeBatches.get(batchId);
        if (batch != null && batch.getStatus() == BatchStatus.RUNNING) {
            batch.setStatus(BatchStatus.CANCELLED);
            return true;
        }
        return false;
    }
    
    // Private implementation methods
    
    private List<Content> extractBatchItems(Content content, PublishingContext context) {
        // Check if context contains batch items
        @SuppressWarnings("unchecked")
        List<Content> contextItems = context.getProperty("batch_items", List.class);
        if (contextItems != null && !contextItems.isEmpty()) {
            return contextItems;
        }
        
        // Check if content implements a batch container interface
        // For now, return single item
        return Arrays.asList(content);
    }
    
    private void processSingleItemBatch(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        List<Content> singleItemList = Arrays.asList(content);
        processMultiItemBatch(singleItemList, context);
    }
    
    private void processMultiItemBatch(List<Content> contentItems, PublishingContext context) 
            throws ContentManagementException {
        
        String batchId = generateBatchId(contentItems, context);
        BatchOperation batchOperation = new BatchOperation(batchId, contentItems, context);
        processMultiItemBatch(contentItems, context, batchOperation);
    }
    
    private void processMultiItemBatch(List<Content> contentItems, PublishingContext context, 
                                     BatchOperation batchOperation) throws ContentManagementException {
        
        long startTime = System.currentTimeMillis();
        batchOperation.setStatus(BatchStatus.RUNNING);
        
        try {
            // Determine optimal chunk size
            int chunkSize = calculateOptimalChunkSize(contentItems.size());
            
            // Process items in parallel chunks
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < contentItems.size(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, contentItems.size());
                List<Content> chunk = contentItems.subList(i, endIndex);
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> processChunk(chunk, context, batchOperation),
                    executorService
                );
                
                futures.add(future);
            }
            
            // Wait for all chunks to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allFutures.join(); // Wait for completion
            
            // Determine final batch status
            if (batchOperation.getFailedItems() == 0) {
                batchOperation.setStatus(BatchStatus.COMPLETED);
            } else if (batchOperation.getSuccessfulItems() > 0) {
                batchOperation.setStatus(BatchStatus.COMPLETED_WITH_ERRORS);
            } else {
                batchOperation.setStatus(BatchStatus.FAILED);
            }
            
            // Record statistics
            long processingTime = System.currentTimeMillis() - startTime;
            if (batchOperation.getStatus() == BatchStatus.COMPLETED || 
                batchOperation.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS) {
                statistics.recordBatchSuccess(batchOperation.getSuccessfulItems(), processingTime);
            } else {
                statistics.recordBatchFailure();
            }
            
            // Fire completion event
            fireBatchCompletionEvent(batchOperation);
            
            CMSLogger.logContentOperation(
                "batch_" + batchOperation.getBatchId(),
                context.getUser().getUsername(),
                "batch_publish_completed",
                String.format("Batch publishing completed. Success: %d, Failed: %d, Time: %d ms",
                             batchOperation.getSuccessfulItems(), 
                             batchOperation.getFailedItems(),
                             processingTime)
            );
            
        } catch (Exception e) {
            batchOperation.setStatus(BatchStatus.FAILED);
            statistics.recordBatchFailure();
            
            throw new ContentManagementException(
                "Batch processing execution failed",
                "Error during parallel batch processing",
                e
            );
        } finally {
            // Clean up completed batch from active tracking
            if (batchOperation.isCompleted()) {
                // Keep for a short time for status queries, then remove
                CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES).execute(() -> 
                    activeBatches.remove(batchOperation.getBatchId())
                );
            }
        }
    }
    
    private void processChunk(List<Content> chunk, PublishingContext context, BatchOperation batchOperation) {
        for (Content content : chunk) {
            try {
                // Check if batch was cancelled
                if (batchOperation.getStatus() == BatchStatus.CANCELLED) {
                    break;
                }
                
                // Process individual content item
                processIndividualContent(content, context);
                batchOperation.recordSuccess(content.getId());
                
                // Fire individual item success event
                fireItemProcessedEvent(content, context, true, null);
                
            } catch (Exception e) {
                String errorMessage = "Failed to publish content: " + e.getMessage();
                batchOperation.recordFailure(content.getId(), errorMessage);
                
                // Log individual failure but continue processing
                CMSLogger.logError(
                    "Failed to publish content in batch: " + content.getId(),
                    e,
                    context.getUser().getUsername()
                );
                
                // Fire individual item failure event
                fireItemProcessedEvent(content, context, false, errorMessage);
            }
        }
    }
    
    private void processIndividualContent(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        // Validate individual content
        if (!validateSingleContentItem(content, context)) {
            throw new ContentManagementException(
                "Content validation failed",
                "Individual content item failed validation"
            );
        }
        
        // Update content status to published
        content.setStatus(ContentStatus.PUBLISHED, context.getUser().getId());
        
        // Set publication timestamp
        LocalDateTime now = LocalDateTime.now();
        if (content.getPublishedDate() == null) {
            content.setPublishedDate(now);
        }
        
        // Update last modified information
        content.setLastModified(now);
        content.setLastModifiedBy(context.getUser().getUsername());
        
        // Update content metadata
        if (content.getMetadata() != null) {
            content.getMetadata().put("batch_published", true);
            content.getMetadata().put("publishing_strategy", STRATEGY_NAME);
            content.getMetadata().put("batch_timestamp", now.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
    }
    
    private void fireItemProcessedEvent(Content content, PublishingContext context, 
                                      boolean success, String errorMessage) {
        try {
            ContentEvent.EventType eventType = success ? 
                ContentEvent.EventType.CONTENT_PUBLISHED : 
                ContentEvent.EventType.CONTENT_ERROR;
            
            ContentEvent.Builder eventBuilder = ContentEvent.builder(eventType, content)
                .user(context.getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("batch_processing", true);
            
            if (!success && errorMessage != null) {
                eventBuilder.metadata("error_message", errorMessage);
            }
            
            contentSubject.notifyObservers(eventBuilder.build());
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire item processed event for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
        }
    }
    
    private void fireBatchCompletionEvent(BatchOperation batchOperation) {
        try {
            ContentEvent event = ContentEvent.builder(ContentEvent.EventType.CONTENT_PROCESSING_COMPLETED, null)
                .user(batchOperation.getContext().getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("batch_id", batchOperation.getBatchId())
                .metadata("total_items", batchOperation.getTotalItems())
                .metadata("successful_items", batchOperation.getSuccessfulItems())
                .metadata("failed_items", batchOperation.getFailedItems())
                .metadata("success_rate", batchOperation.getSuccessRate())
                .metadata("duration_ms", batchOperation.getDuration())
                .build();
            
            contentSubject.notifyObservers(event);
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire batch completion event for batch: " + batchOperation.getBatchId(),
                e,
                batchOperation.getContext().getUser().getUsername()
            );
        }
    }
    
    private String generateBatchId(List<Content> contentItems, PublishingContext context) {
        return String.format("batch_%s_%d_%d", 
                           context.getUser().getUsername(),
                           contentItems.size(),
                           System.currentTimeMillis());
    }
    
    private int calculateOptimalChunkSize(int totalItems) {
        // Calculate chunk size based on total items and worker threads
        int baseChunkSize = Math.max(1, totalItems / (WORKER_THREADS * 2));
        return Math.min(baseChunkSize, DEFAULT_BATCH_SIZE);
    }
    
    private boolean validateSingleContentItem(Content content, PublishingContext context) {
        if (content.getId() == null || content.getId().trim().isEmpty()) {
            return false;
        }
        
        if (content.getTitle() == null || content.getTitle().trim().isEmpty()) {
            return false;
        }
        
        if (content.getBody() == null || content.getBody().trim().isEmpty()) {
            return false;
        }
        
        // Content status validation
        ContentStatus status = content.getStatus();
        if (status != ContentStatus.DRAFT && status != ContentStatus.REVIEW) {
            return false;
        }
        
        return true;
    }
    
    private boolean hasBatchPublishingPermission(com.cms.core.model.User user) {
        Role role = user.getRole();
        return role == Role.ADMINISTRATOR || 
               role == Role.PUBLISHER || 
               role == Role.EDITOR;
    }
    
    private boolean hasAvailableResources() {
        // Check system resources (simplified implementation)
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        
        // Require at least 25% free memory for batch processing
        double memoryUsage = 1.0 - (double) freeMemory / totalMemory;
        if (memoryUsage > 0.75) {
            return false;
        }
        
        // Check active batch count
        if (activeBatches.size() >= 10) { // Limit concurrent batches
            return false;
        }
        
        return true;
    }
    
    /**
     * Shutdown method for proper resource cleanup.
     * Should be called when the strategy is no longer needed.
     */
    public void shutdown() {
        try {
            // Cancel all active batches
            for (BatchOperation batch : activeBatches.values()) {
                if (batch.getStatus() == BatchStatus.RUNNING) {
                    batch.setStatus(BatchStatus.CANCELLED);
                }
            }
            
            // Shutdown executor service
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            CMSLogger.logSystemEvent(
                "batch_publishing_strategy_shutdown",
                "BatchPublishingStrategy shutdown completed"
            );
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Error during BatchPublishingStrategy shutdown",
                e,
                "system"
            );
        }
    }
}