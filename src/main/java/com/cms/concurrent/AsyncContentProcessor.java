package com.cms.concurrent;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.io.TemplateProcessor;
import com.cms.io.IOUtils;
import com.cms.util.CMSLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asynchronous content processing system using CompletableFuture for
 * high-performance
 * concurrent content operations in the JavaCMS system.
 *
 * <p>
 * This class provides comprehensive asynchronous content processing
 * capabilities,
 * implementing advanced multithreading patterns with CompletableFuture for
 * non-blocking
 * operations, parallel processing, and sophisticated workflow management.
 * </p>
 *
 * <p>
 * <strong>Multithreading Implementation:</strong> Provides
 * CompletableFuture-based async processing
 * with parallel content validation, transformation, indexing, and publishing
 * operations.
 * Implements advanced concurrent programming with thread-safe operations and
 * comprehensive error handling.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>CompletableFuture-based async content processing pipeline</li>
 * <li>Parallel content validation, sanitization, and transformation</li>
 * <li>Async content indexing and search preparation</li>
 * <li>Batch processing with configurable parallelism</li>
 * <li>Content processing metrics and performance monitoring</li>
 * <li>Integration with Observer Pattern for async event notifications</li>
 * <li>Comprehensive error handling and recovery mechanisms</li>
 * </ul>
 *
 * <p>
 * <strong>Processing Pipeline:</strong>
 * </p>
 * <ol>
 * <li><strong>Validation:</strong> Async content validation with parallel
 * checks</li>
 * <li><strong>Sanitization:</strong> Security-focused content sanitization</li>
 * <li><strong>Transformation:</strong> Content format transformation and
 * optimization</li>
 * <li><strong>Indexing:</strong> Search index preparation and full-text
 * processing</li>
 * <li><strong>Publishing:</strong> Final content publishing with
 * notifications</li>
 * </ol>
 *
 * <p>
 * <strong>Integration:</strong> Seamlessly integrates with Factory Pattern for
 * content creation,
 * Observer Pattern for async notifications, Strategy Pattern for processing
 * algorithms,
 * and I/O operations for file processing.
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class AsyncContentProcessor {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private final ThreadPoolManager threadPoolManager;
    private final ContentSubject contentSubject;
    private final TemplateProcessor templateProcessor;

    // Processing statistics and monitoring
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong processingTimeMs = new AtomicLong(0);
    private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final Map<String, ProcessingResult> processingResults = new ConcurrentHashMap<>();

    // Configuration constants
    private static final int BATCH_SIZE = 50;
    private static final long PROCESSING_TIMEOUT_MINUTES = 10;
    private static final int MAX_PARALLEL_OPERATIONS = Runtime.getRuntime().availableProcessors() * 4;

    /**
     * Constructs AsyncContentProcessor with required dependencies for async
     * processing.
     *
     * @param contentSubject    Subject for content event notifications
     * @param templateProcessor Template processor for content transformation
     */
    public AsyncContentProcessor(ContentSubject contentSubject, TemplateProcessor templateProcessor) {
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.contentSubject = contentSubject;
        this.templateProcessor = templateProcessor;
        initializeOperationCounters();

        logger.logSystemOperation("AsyncContentProcessor initialized with max " +
                MAX_PARALLEL_OPERATIONS + " parallel operations");
    }

    /**
     * Processes a single content item asynchronously through the complete
     * processing pipeline.
     * Returns CompletableFuture for non-blocking operation handling.
     *
     * <p>
     * <strong>Processing Pipeline:</strong>
     * </p>
     * <ol>
     * <li>Content validation and security checks</li>
     * <li>Content sanitization and normalization</li>
     * <li>Content transformation and optimization</li>
     * <li>Search indexing and metadata extraction</li>
     * <li>Event notification and audit logging</li>
     * </ol>
     *
     * @param content The content to process asynchronously
     * @return CompletableFuture containing the processing result
     */
    public CompletableFuture<ProcessingResult> processContentAsync(Content content) {
        if (content == null) {
            return CompletableFuture.completedFuture(
                    ProcessingResult.failure("Content cannot be null"));
        }

        String processingId = generateProcessingId();
        long startTime = System.currentTimeMillis();

        logger.logContentOperation("Starting async content processing for: " + content.getTitle() +
                " (ID: " + processingId + ")");

        return validateContentAsync(content, processingId)
                .thenCompose(validationResult -> {
                    if (!validationResult.isSuccess()) {
                        return CompletableFuture.completedFuture(validationResult);
                    }
                    return sanitizeContentAsync(content, processingId);
                })
                .thenCompose(sanitizationResult -> {
                    if (!sanitizationResult.isSuccess()) {
                        return CompletableFuture.completedFuture(sanitizationResult);
                    }
                    return transformContentAsync(content, processingId);
                })
                .thenCompose(transformationResult -> {
                    if (!transformationResult.isSuccess()) {
                        return CompletableFuture.completedFuture(transformationResult);
                    }
                    return indexContentAsync(content, processingId);
                })
                .thenCompose(indexingResult -> {
                    if (!indexingResult.isSuccess()) {
                        return CompletableFuture.completedFuture(indexingResult);
                    }
                    return publishContentAsync(content, processingId);
                })
                .handle((result, throwable) -> {
                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    processingTimeMs.addAndGet(processingTime);

                    if (throwable != null) {
                        totalFailed.incrementAndGet();
                        ProcessingResult errorResult = ProcessingResult.failure(
                                "Content processing failed: " + throwable.getMessage());
                        errorResult.setProcessingId(processingId);
                        errorResult.setProcessingTime(processingTime);
                        processingResults.put(processingId, errorResult);

                        Exception exception = (throwable instanceof Exception) ? (Exception) throwable
                                : new Exception("Async processing failed", throwable);
                        logger.logError("Async content processing failed for ID: " + processingId, exception);
                        return errorResult;
                    } else {
                        totalProcessed.incrementAndGet();
                        result.setProcessingId(processingId);
                        result.setProcessingTime(processingTime);
                        processingResults.put(processingId, result);

                        logger.logContentOperation("Async content processing completed successfully for ID: " +
                                processingId + " in " + processingTime + "ms");

                        // Fire async notification event
                        fireProcessingCompletedEvent(content, result);

                        return result;
                    }
                })
                .orTimeout(PROCESSING_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Processes multiple content items in parallel with configurable batch
     * processing.
     * Optimizes performance through parallel execution and batch operations.
     *
     * @param contentList List of content items to process
     * @return CompletableFuture containing list of processing results
     */
    public CompletableFuture<List<ProcessingResult>> processBatchAsync(List<Content> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        logger.logContentOperation("Starting batch processing for " + contentList.size() + " content items");

        // Split into batches for optimal processing
        List<List<Content>> batches = partitionList(contentList, BATCH_SIZE);

        List<CompletableFuture<List<ProcessingResult>>> batchFutures = batches.stream()
                .map(this::processBatchChunk)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> batchFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))
                .handle((results, throwable) -> {
                    if (throwable != null) {
                        Exception exception = (throwable instanceof Exception) ? (Exception) throwable
                                : new Exception("Batch processing failed", throwable);
                        logger.logError("Batch processing failed", exception);
                        return results != null ? results : new ArrayList<>();
                    }

                    logger.logContentOperation("Batch processing completed for " + results.size() + " items");
                    return results;
                });
    }

    /**
     * Performs parallel content validation with comprehensive security and quality
     * checks.
     *
     * @param content      Content to validate
     * @param processingId Unique processing identifier for tracking
     * @return CompletableFuture containing validation result
     */
    private CompletableFuture<ProcessingResult> validateContentAsync(Content content, String processingId) {
        return threadPoolManager.submitContentProcessingTask(() -> {
            incrementOperationCount("validation");

            // Parallel validation checks
            List<CompletableFuture<Boolean>> validationChecks = new ArrayList<>();

            // Title validation
            validationChecks.add(CompletableFuture
                    .supplyAsync(() -> content.getTitle() != null && !content.getTitle().trim().isEmpty() &&
                            content.getTitle().length() <= 200));

            // Content body validation
            validationChecks.add(CompletableFuture
                    .supplyAsync(() -> content.getBody() != null && !content.getBody().trim().isEmpty() &&
                            content.getBody().length() <= 100000));

            // Security validation
            validationChecks.add(CompletableFuture.supplyAsync(() -> !containsMaliciousContent(content.getBody()) &&
                    !containsMaliciousContent(content.getTitle())));

            // Metadata validation
            validationChecks.add(CompletableFuture
                    .supplyAsync(() -> content.getMetadata() != null && content.getMetadata().size() <= 50));

            // Wait for all validation checks to complete
            CompletableFuture<Void> allChecks = CompletableFuture.allOf(
                    validationChecks.toArray(new CompletableFuture[0]));

            return allChecks.thenApply(v -> {
                boolean allValid = validationChecks.stream()
                        .allMatch(CompletableFuture::join);

                if (allValid) {
                    return ProcessingResult.success("Content validation passed");
                } else {
                    return ProcessingResult.failure("Content validation failed");
                }
            }).get(); // Block on the CompletableFuture to return the result

        }).thenApply(Function.identity()); // Convert Callable result to proper type
    }

    /**
     * Performs asynchronous content sanitization to remove malicious content and
     * normalize data.
     */
    private CompletableFuture<ProcessingResult> sanitizeContentAsync(Content content, String processingId) {
        return threadPoolManager.submitContentProcessingTask(() -> {
            incrementOperationCount("sanitization");

            // Sanitize title
            String sanitizedTitle = sanitizeText(content.getTitle());
            content.setTitle(sanitizedTitle, "SYSTEM");

            // Sanitize body content
            String sanitizedBody = sanitizeText(content.getBody());
            content.setBody(sanitizedBody, "SYSTEM");

            // Normalize metadata
            normalizeMetadata(content);

            return ProcessingResult.success("Content sanitization completed");
        });
    }

    /**
     * Performs asynchronous content transformation and optimization.
     */
    private CompletableFuture<ProcessingResult> transformContentAsync(Content content, String processingId) {
        return threadPoolManager.submitContentProcessingTask(() -> {
            incrementOperationCount("transformation");

            // Apply template processing if content has templates
            if (content.getMetadata().containsKey("template")) {
                String templateName = content.getMetadata().get("template").toString();
                Map<String, Object> variables = new ConcurrentHashMap<>();
                variables.put("title", content.getTitle());
                variables.put("body", content.getBody());
                variables.put("createdDate", content.getCreatedDate());

                try {
                    String processedContent = templateProcessor.processTemplate(java.nio.file.Paths.get(templateName),
                            variables);
                    content.setBody(processedContent, "SYSTEM");
                } catch (Exception e) {
                    logger.logError("Template processing failed for content: " + content.getId(), e);
                    return ProcessingResult.failure("Template processing failed: " + e.getMessage());
                }
            }

            // Optimize content for performance
            optimizeContent(content);

            return ProcessingResult.success("Content transformation completed");
        });
    }

    /**
     * Performs asynchronous content indexing for search functionality.
     */
    private CompletableFuture<ProcessingResult> indexContentAsync(Content content, String processingId) {
        return threadPoolManager.submitContentProcessingTask(() -> {
            incrementOperationCount("indexing");

            // Extract keywords and create search index
            List<String> keywords = extractKeywords(content.getTitle() + " " + content.getBody());
            content.getMetadata().put("searchKeywords", keywords);

            // Generate content summary
            String summary = generateSummary(content.getBody(), 200);
            content.getMetadata().put("summary", summary);

            // Update search index timestamp
            content.getMetadata().put("indexedAt", LocalDateTime.now());

            return ProcessingResult.success("Content indexing completed");
        });
    }

    /**
     * Performs asynchronous content publishing with status updates.
     */
    private CompletableFuture<ProcessingResult> publishContentAsync(Content content, String processingId) {
        return threadPoolManager.submitContentProcessingTask(() -> {
            incrementOperationCount("publishing");

            // Update content status
            content.setStatus(ContentStatus.PUBLISHED, "system");
            content.getMetadata().put("publishedAt", LocalDateTime.now());
            content.getMetadata().put("processingId", processingId);

            return ProcessingResult.success("Content publishing completed");
        });
    }

    /**
     * Processes a batch chunk of content items in parallel.
     */
    private CompletableFuture<List<ProcessingResult>> processBatchChunk(List<Content> chunk) {
        List<CompletableFuture<ProcessingResult>> futures = chunk.stream()
                .map(this::processContentAsync)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * Fires an async content processing completed event through the Observer
     * pattern.
     */
    private void fireProcessingCompletedEvent(Content content, ProcessingResult result) {
        Map<String, Object> eventMetadata = new java.util.HashMap<>();
        eventMetadata.put("processingResult", result);
        eventMetadata.put("processingTime", result.getProcessingTime());

        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.UPDATED)
                .content(content)
                .timestamp(LocalDateTime.now())
                .metadata(eventMetadata)
                .build();

        // Fire event asynchronously to avoid blocking
        threadPoolManager.submitBackgroundTask(() -> {
            try {
                contentSubject.notifyObservers(event);
            } catch (Exception e) {
                logger.logError("Failed to notify observers of processing completion", e);
            }
        });
    }

    /**
     * Gets comprehensive processing statistics and performance metrics.
     *
     * @return Map containing detailed processing statistics
     */
    public Map<String, Object> getProcessingStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("totalFailed", totalFailed.get());
        stats.put("averageProcessingTimeMs",
                totalProcessed.get() > 0 ? processingTimeMs.get() / totalProcessed.get() : 0);
        stats.put("totalProcessingTimeMs", processingTimeMs.get());

        Map<String, Long> operations = new ConcurrentHashMap<>();
        operationCounts.forEach((op, count) -> operations.put(op, count.get()));
        stats.put("operationCounts", operations);

        return stats;
    }

    /**
     * Gets processing result by processing ID.
     *
     * @param processingId The processing identifier
     * @return ProcessingResult if found, null otherwise
     */
    public ProcessingResult getProcessingResult(String processingId) {
        return processingResults.get(processingId);
    }

    // Helper methods for content processing

    private void initializeOperationCounters() {
        operationCounts.put("validation", new AtomicLong(0));
        operationCounts.put("sanitization", new AtomicLong(0));
        operationCounts.put("transformation", new AtomicLong(0));
        operationCounts.put("indexing", new AtomicLong(0));
        operationCounts.put("publishing", new AtomicLong(0));
    }

    private void incrementOperationCount(String operation) {
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
    }

    private String generateProcessingId() {
        return "proc-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean containsMaliciousContent(String text) {
        if (text == null)
            return false;
        String lowerText = text.toLowerCase();
        return lowerText.contains("<script>") || lowerText.contains("javascript:") ||
                lowerText.contains("on click=") || lowerText.contains("eval(");
    }

    private String sanitizeText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("javascript:", "")
                .replaceAll("on\\w+\\s*=", "")
                .trim();
    }

    private void normalizeMetadata(Content content) {
        java.util.Set<java.util.Map.Entry<String, Object>> entrySet = content.getMetadata().entrySet();
        entrySet.removeIf(entry -> entry.getValue() == null || entry.getValue().toString().trim().isEmpty());
    }

    private void optimizeContent(Content content) {
        // Remove excessive whitespace
        content.setBody(content.getBody().replaceAll("\\s+", " ").trim(), "SYSTEM");

        // Optimize title
        content.setTitle(content.getTitle().trim(), "SYSTEM");

        // Add content optimization metadata
        content.getMetadata().put("optimized", true);
        content.getMetadata().put("optimizedAt", LocalDateTime.now());
    }

    private List<String> extractKeywords(String text) {
        if (text == null)
            return new ArrayList<>();

        return java.util.Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 3)
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
    }

    private String generateSummary(String text, int maxLength) {
        if (text == null || text.length() <= maxLength)
            return text;

        String truncated = text.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxLength - 50) {
            truncated = truncated.substring(0, lastSpace);
        }
        return truncated + "...";
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    /**
     * Represents the result of an async content processing operation.
     */
    public static class ProcessingResult {
        private final boolean success;
        private final String message;
        private String processingId;
        private long processingTime;
        private Map<String, Object> metadata = new ConcurrentHashMap<>();

        private ProcessingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ProcessingResult success(String message) {
            return new ProcessingResult(true, message);
        }

        public static ProcessingResult failure(String message) {
            return new ProcessingResult(false, message);
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getProcessingId() {
            return processingId;
        }

        public void setProcessingId(String processingId) {
            this.processingId = processingId;
        }

        public long getProcessingTime() {
            return processingTime;
        }

        public void setProcessingTime(long processingTime) {
            this.processingTime = processingTime;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return String.format("ProcessingResult{success=%s, message='%s', processingId='%s', processingTime=%dms}",
                    success, message, processingId, processingTime);
        }
    }
}
