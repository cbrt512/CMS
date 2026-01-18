package com.cms.streams;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.ContentManagementException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.util.CMSLogger;
import com.cms.streams.ContentMapper.ContentDTO;
import com.cms.streams.ContentMapper.ContentSummary;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Advanced Stream API processor for content operations using functional
 * programming.
 *
 * <p>
 * This class provides comprehensive Stream API usage with lambda expressions,
 * method references, and functional interfaces. It provides high-performance
 * content processing capabilities using parallel streams, custom collectors,
 * and advanced functional programming techniques.
 * </p>
 *
 * <p>
 * <strong>Stream API & Lambdas:</strong> This class implements comprehensive
 * Stream API and Lambda functionality with:
 * </p>
 * <ul>
 * <li>Parallel stream processing for performance</li>
 * <li>Lambda expressions for complex filtering and mapping</li>
 * <li>Method references for clean functional code</li>
 * <li>Custom collectors for specialized aggregations</li>
 * <li>Functional interface composition and chaining</li>
 * <li>CompletableFuture integration for async processing</li>
 * </ul>
 *
 * <p>
 * <strong>Design Pattern Integration:</strong> Integrates seamlessly with:
 * </p>
 * <ul>
 * <li>Exception Shielding Pattern for safe stream operations</li>
 * <li>Factory Pattern for content type-specific processing</li>
 * <li>Observer Pattern for stream-based event processing</li>
 * <li>Strategy Pattern for configurable processing algorithms</li>
 * <li>Iterator Pattern for stream-based traversal</li>
 * </ul>
 *
 * <p>
 * <strong>Performance:</strong> Optimized for high-throughput processing with
 * parallel streams, custom thread pools, and memory-efficient operations.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentStreamProcessor {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private final ForkJoinPool customThreadPool;

    /**
     * Statistics record for stream processing results.
     *
     * <p>
     * <strong>Record Usage:</strong> Uses modern Java record
     * with stream collectors and functional programming.
     * </p>
     */
    public record ProcessingStats(
            long totalCount,
            long publishedCount,
            long draftCount,
            double averageWordCount,
            OptionalInt minWordCount,
            OptionalInt maxWordCount,
            Map<ContentStatus, Long> statusDistribution,
            List<String> topAuthors) {
    }

    /**
     * Search result record for functional search operations.
     *
     * <p>
     * <strong>Immutable Data:</strong> Provides immutable search results
     * optimized for stream operations and functional composition.
     * </p>
     */
    public record SearchResult(
            List<ContentDTO> results,
            long totalMatches,
            Map<String, Long> authorCounts,
            Map<ContentStatus, Long> statusCounts,
            double averageRelevanceScore) {
    }

    /**
     * Constructs ContentStreamProcessor with custom thread pool.
     *
     * <p>
     * <strong>Thread Safety:</strong> Initializes with custom ForkJoinPool
     * for optimal parallel stream performance and thread safety.
     * </p>
     */
    public ContentStreamProcessor() {
        this.customThreadPool = new ForkJoinPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
        logger.logSystemOperation(
                "ContentStreamProcessor initialized with " + customThreadPool.getParallelism() + " threads");
    }

    /**
     * Filters content using parallel streams and functional predicates.
     *
     * <p>
     * <strong>Parallel Streams:</strong> Uses parallel processing for
     * high-performance filtering with automatic load balancing.
     * </p>
     *
     * <p>
     * <strong>Lambda Expressions:</strong> Accepts lambda predicates for
     * flexible, composable filtering logic.
     * </p>
     *
     * @param contents  the content collection to filter
     * @param predicate the filtering predicate (lambda or method reference)
     * @return filtered content list
     * @throws ContentManagementException if filtering fails
     */
    public List<Content> filterContent(Collection<Content> contents,
            ContentPredicate predicate)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Filtering {} contents with parallel stream", contents.size());

            try {
                return customThreadPool.submit(() -> contents.parallelStream()
                        .filter(predicate)
                        .collect(Collectors.toList())).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Content filtering failed", e);
            }
        }, "Content filtering failed");
    }

    /**
     * Maps content to DTOs using parallel streams and method references.
     *
     * <p>
     * <strong>Method References:</strong> Uses method references for
     * clean, readable functional code with optimal performance.
     * </p>
     *
     * @param contents the content collection to map
     * @return list of ContentDTO objects
     * @throws ContentManagementException if mapping fails
     */
    public List<ContentDTO> mapToDTO(Collection<Content> contents)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Mapping {} contents to DTOs", contents.size());

            try {
                return customThreadPool.submit(() -> contents.parallelStream()
                        .map(ContentMapper.toDTO())
                        .collect(Collectors.toList())).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Content DTO mapping failed", e);
            }
        }, "Content DTO mapping failed");
    }

    /**
     * Searches content using functional programming and stream operations.
     *
     * <p>
     * <strong>Functional Composition:</strong> Combines multiple predicates
     * and mapping functions for comprehensive search functionality.
     * </p>
     *
     * <p>
     * <strong>Custom Collectors:</strong> Uses specialized collectors for
     * aggregating search statistics and metadata.
     * </p>
     *
     * @param contents   the content collection to search
     * @param searchTerm the search term
     * @param maxResults maximum number of results to return
     * @return comprehensive search results with statistics
     * @throws ContentManagementException if search fails
     */
    public SearchResult searchContent(Collection<Content> contents,
            String searchTerm,
            int maxResults)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Searching {} contents for term: '{}'", contents.size(), searchTerm);

            try {
                return customThreadPool.submit(() -> {
                    // Create composite search predicate
                    ContentPredicate titleSearch = ContentPredicate.byTitleContaining(searchTerm);
                    ContentPredicate contentSearch = content -> content.getBody() != null &&
                            content.getBody().toLowerCase().contains(searchTerm.toLowerCase());
                    Predicate<Content> combinedSearch = titleSearch.or(contentSearch);

                    // Perform parallel search with statistics
                    List<Content> matchingContent = contents.parallelStream()
                            .filter(combinedSearch)
                            .limit(maxResults)
                            .collect(Collectors.toList());

                    // Map to DTOs
                    List<ContentDTO> results = matchingContent.stream()
                            .map(ContentMapper.toDTO())
                            .collect(Collectors.toList());

                    // Calculate statistics
                    Map<String, Long> authorCounts = results.stream()
                            .collect(Collectors.groupingBy(
                                    dto -> dto.author() != null ? dto.author() : "Unknown",
                                    Collectors.counting()));

                    Map<ContentStatus, Long> statusCounts = results.stream()
                            .collect(Collectors.groupingBy(
                                    ContentDTO::status,
                                    Collectors.counting()));

                    double averageScore = results.size() > 0 ? calculateRelevanceScore(searchTerm, matchingContent)
                            : 0.0;

                    return new SearchResult(
                            results,
                            matchingContent.size(),
                            authorCounts,
                            statusCounts,
                            averageScore);
                }).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Content search failed", e);
            }
        }, "Content search failed");
    }

    /**
     * Generates comprehensive processing statistics using stream operations.
     *
     * <p>
     * <strong>Stream Collectors:</strong> Uses advanced collector
     * usage for statistical analysis and data aggregation.
     * </p>
     *
     * <p>
     * <strong>Function Composition:</strong> Combines multiple mapping and
     * reduction operations for comprehensive analytics.
     * </p>
     *
     * @param contents the content collection to analyze
     * @return comprehensive processing statistics
     * @throws ContentManagementException if statistics generation fails
     */
    public ProcessingStats generateStatistics(Collection<Content> contents)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Generating statistics for {} contents", contents.size());

            try {
                return customThreadPool.submit(() -> {
                    // Basic counts using stream operations
                    long totalCount = contents.size();
                    long publishedCount = contents.parallelStream()
                            .filter(ContentPredicate.isPublished())
                            .count();
                    long draftCount = contents.parallelStream()
                            .filter(ContentPredicate.isDraft())
                            .count();

                    // Word count statistics using numeric streams
                    IntSummaryStatistics wordStats = contents.parallelStream()
                            .mapToInt(ContentMapper.toWordCount()::apply)
                            .summaryStatistics();

                    // Status distribution using grouping collector
                    Map<ContentStatus, Long> statusDistribution = contents.parallelStream()
                            .collect(Collectors.groupingBy(
                                    ContentMapper.toStatus(),
                                    Collectors.counting()));

                    // Top authors using custom collector
                    List<String> topAuthors = contents.parallelStream()
                            .map(ContentMapper.toAuthor())
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(5)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());

                    return new ProcessingStats(
                            totalCount,
                            publishedCount,
                            draftCount,
                            wordStats.getAverage(),
                            wordStats.getCount() > 0 ? OptionalInt.of(wordStats.getMin()) : OptionalInt.empty(),
                            wordStats.getCount() > 0 ? OptionalInt.of(wordStats.getMax()) : OptionalInt.empty(),
                            statusDistribution,
                            topAuthors);
                }).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Statistics generation failed", e);
            }
        }, "Statistics generation failed");
    }

    /**
     * Groups content by date ranges using stream collectors.
     *
     * <p>
     * <strong>Custom Collector:</strong> Implements custom collector
     * implementation for specialized grouping operations.
     * </p>
     *
     * @param contents the content collection to group
     * @param dayRange the number of days to group by
     * @return content grouped by date ranges
     * @throws ContentManagementException if grouping fails
     */
    public Map<String, List<ContentDTO>> groupByDateRange(Collection<Content> contents,
            int dayRange)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Grouping {} contents by {}-day ranges", contents.size(), dayRange);

            try {
                return customThreadPool.submit(() -> contents.parallelStream()
                        .map(ContentMapper.toDTO())
                        .collect(Collectors.groupingBy(dto -> {
                            if (dto.createdAt() == null)
                                return "Unknown";
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime cutoff = now.minusDays(dayRange);

                            if (dto.createdAt().isAfter(cutoff)) {
                                return String.format("Last %d days", dayRange);
                            } else if (dto.createdAt().isAfter(cutoff.minusDays(dayRange))) {
                                return String.format("%d-%d days ago", dayRange, dayRange * 2);
                            } else {
                                return String.format("Older than %d days", dayRange * 2);
                            }
                        }))).get();
            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Date range grouping failed", e);
            }
        }, "Date range grouping failed");
    }

    /**
     * Processes content asynchronously using CompletableFuture and streams.
     *
     * <p>
     * <strong>Async Processing:</strong> Combines CompletableFuture with
     * stream operations for non-blocking, high-performance processing.
     * </p>
     *
     * @param contents  the content collection to process
     * @param processor the processing function to apply
     * @return CompletableFuture containing processed results
     */
    public <R> CompletableFuture<List<R>> processAsync(Collection<Content> contents,
            Function<Content, R> processor) {
        logger.debug("Starting async processing of {} contents", contents.size());

        return CompletableFuture.supplyAsync(() -> {
            return contents.parallelStream()
                    .map(processor)
                    .collect(Collectors.toList());
        }, customThreadPool);
    }

    /**
     * Creates custom collector for content summarization.
     *
     * <p>
     * <strong>Custom Collector:</strong> Implements advanced collector
     * interface for specialized aggregation operations.
     * </p>
     *
     * @param maxSummaryLength maximum length of summaries
     * @return collector that creates content summaries
     */
    public static Collector<Content, ?, List<ContentSummary>> summarizingCollector(int maxSummaryLength) {
        return Collector.<Content, ArrayList<ContentSummary>, List<ContentSummary>>of(
                ArrayList::new,
                (list, content) -> list.add(ContentSummary.fromContent(content, maxSummaryLength)),
                (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                },
                list -> (List<ContentSummary>) Collections.unmodifiableList(list));
    }

    /**
     * Calculates relevance score for search results.
     *
     * <p>
     * <strong>Private Helper:</strong> Uses stream operations for
     * calculating search relevance scores.
     * </p>
     *
     * @param searchTerm the search term
     * @param contents   the matching contents
     * @return average relevance score
     */
    private double calculateRelevanceScore(String searchTerm, List<Content> contents) {
        if (contents.isEmpty() || searchTerm == null || searchTerm.trim().isEmpty()) {
            return 0.0;
        }

        String lowerSearchTerm = searchTerm.toLowerCase();

        return contents.stream()
                .mapToDouble(content -> {
                    double score = 0.0;

                    // Title match (higher weight)
                    if (content.getTitle() != null &&
                            content.getTitle().toLowerCase().contains(lowerSearchTerm)) {
                        score += 10.0;
                    }

                    // Content match (lower weight)
                    if (content.getBody() != null &&
                            content.getBody().toLowerCase().contains(lowerSearchTerm)) {
                        score += 5.0;
                    }

                    // Author match (medium weight)
                    if (content.getCreatedBy() != null &&
                            content.getCreatedBy().toLowerCase().contains(lowerSearchTerm)) {
                        score += 7.0;
                    }

                    return score;
                })
                .average()
                .orElse(0.0);
    }

    /**
     * Closes the custom thread pool and releases resources.
     *
     * <p>
     * <strong>Resource Management:</strong> Properly closes thread pool
     * to prevent resource leaks in stream processing.
     * </p>
     */
    public void close() {
        if (customThreadPool != null && !customThreadPool.isShutdown()) {
            customThreadPool.shutdown();
            logger.logSystemOperation("ContentStreamProcessor thread pool shut down");
        }
    }
}
