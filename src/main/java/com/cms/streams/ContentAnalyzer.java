package com.cms.streams;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.ContentManagementException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.util.CMSLogger;
import com.cms.streams.ContentMapper.ContentDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Advanced content analytics using Stream API and functional programming.
 *
 * <p>
 * This class provides comprehensive content analysis capabilities using
 * advanced Stream API features, lambda expressions, and functional programming
 * techniques. It provides the power of stream-based analytics with parallel
 * processing, custom collectors, and functional composition.
 * </p>
 *
 * <p>
 * <strong>Stream API & Lambdas:</strong> This class implements advanced
 * Stream API and Lambda functionality with sophisticated features:
 * </p>
 * <ul>
 * <li>Parallel stream analytics for high-performance processing</li>
 * <li>Advanced lambda expressions for complex analysis operations</li>
 * <li>Method references for clean, readable functional code</li>
 * <li>Custom collectors for specialized aggregation</li>
 * <li>Functional interface composition and chaining</li>
 * <li>Stream-based statistical analysis and reporting</li>
 * </ul>
 *
 * <p>
 * <strong>Integration Features:</strong>
 * </p>
 * <ul>
 * <li>Seamless integration with Observer Pattern for analytics events</li>
 * <li>Strategy Pattern support for configurable analysis algorithms</li>
 * <li>Exception Shielding for safe analytics operations</li>
 * <li>Concurrent processing with thread-safe stream operations</li>
 * </ul>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentAnalyzer {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Comprehensive analytics result containing all analysis metrics.
     *
     * <p>
     * <strong>Record Usage:</strong> Modern Java record for immutable
     * analytics data with functional programming integration.
     * </p>
     */
    public record AnalyticsResult(
            Map<String, Double> contentMetrics,
            Map<String, Long> authorStatistics,
            Map<ContentStatus, Long> statusDistribution,
            Map<String, List<ContentDTO>> contentByDate,
            TrendAnalysis trendAnalysis,
            QualityMetrics qualityMetrics,
            PerformanceStats performanceStats) {
    }

    /**
     * Trend analysis data for content creation patterns.
     *
     * <p>
     * <strong>Functional Analytics:</strong> Uses stream operations
     * for trend calculation and pattern recognition.
     * </p>
     */
    public record TrendAnalysis(
            double dailyGrowthRate,
            Map<String, Long> monthlyTrends,
            List<String> topGrowthPeriods,
            double contentVelocity,
            Map<String, Double> authorProductivity) {
    }

    /**
     * Quality metrics for content assessment.
     *
     * <p>
     * <strong>Lambda Expressions:</strong> Calculated using complex
     * lambda expressions and functional reduction operations.
     * </p>
     */
    public record QualityMetrics(
            double averageWordCount,
            double contentDiversity,
            double authorDiversity,
            Map<String, Double> qualityScores,
            List<String> topQualityAuthors) {
    }

    /**
     * Performance statistics for analytics operations.
     *
     * <p>
     * <strong>Stream Performance:</strong> Tracks stream processing
     * performance and optimization metrics.
     * </p>
     */
    public record PerformanceStats(
            long processingTimeMs,
            long itemsProcessed,
            double itemsPerSecond,
            long memoryUsedMB,
            Map<String, Long> operationTimes) {
    }

    /**
     * Constructs ContentAnalyzer with exception shielding.
     */
    public ContentAnalyzer() {
        logger.logSystemOperation("ContentAnalyzer initialized with stream-based analytics");
    }

    /**
     * Performs comprehensive content analysis using parallel streams.
     *
     * <p>
     * <strong>Parallel Stream Processing:</strong> Uses parallel streams
     * for high-performance analytics with automatic load balancing.
     * </p>
     *
     * <p>
     * <strong>Functional Composition:</strong> Combines multiple analysis
     * functions using stream operations and lambda expressions.
     * </p>
     *
     * @param contents the content collection to analyze
     * @return comprehensive analytics result
     * @throws ContentManagementException if analysis fails
     */
    public AnalyticsResult analyzeContent(Collection<Content> contents)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.logSystemOperation("Starting comprehensive analysis of " + contents.size() + " contents");
            long startTime = System.currentTimeMillis();

            // Parallel content metrics calculation
            Map<String, Double> contentMetrics = calculateContentMetrics(contents);

            // Author statistics using stream grouping
            Map<String, Long> authorStats = contents.parallelStream()
                    .collect(Collectors.groupingBy(
                            content -> content.getCreatedBy() != null ? content.getCreatedBy() : "Unknown",
                            Collectors.counting()));

            // Status distribution using method references
            Map<ContentStatus, Long> statusDist = contents.parallelStream()
                    .collect(Collectors.groupingBy(
                            Content::getStatus,
                            Collectors.counting()));

            // Content by date using custom collector
            Map<String, List<ContentDTO>> contentByDate = contents.parallelStream()
                    .collect(Collectors.groupingBy(
                            content -> content.getCreatedDate() != null
                                    ? content.getCreatedDate().format(DATE_FORMATTER)
                                    : "Unknown",
                            Collectors.mapping(
                                    ContentMapper.toDTO()::apply,
                                    Collectors.toList())));

            // Advanced analytics
            TrendAnalysis trends = analyzeTrends(contents);
            QualityMetrics quality = analyzeQuality(contents);

            long endTime = System.currentTimeMillis();
            PerformanceStats performance = new PerformanceStats(
                    endTime - startTime,
                    contents.size(),
                    contents.size() / ((endTime - startTime) / 1000.0),
                    Runtime.getRuntime().totalMemory() / (1024 * 1024),
                    Map.of("total_analysis", endTime - startTime));

            long executionTime = endTime - startTime;
            logger.logSystemOperation(
                    "Analysis completed in " + executionTime + "ms for " + contents.size() + " items");

            return new AnalyticsResult(
                    contentMetrics,
                    authorStats,
                    statusDist,
                    contentByDate,
                    trends,
                    quality,
                    performance);
        }, "Content analysis failed");
    }

    /**
     * Analyzes content trends using time-series stream operations.
     *
     * <p>
     * <strong>Time-Series Analysis:</strong> Uses stream operations for
     * temporal pattern recognition and trend calculation.
     * </p>
     *
     * @param contents the content collection
     * @return trend analysis results
     */
    private TrendAnalysis analyzeTrends(Collection<Content> contents) {
        // Monthly trends using stream grouping and date manipulation
        Map<String, Long> monthlyTrends = contents.stream()
                .filter(content -> content.getCreatedDate() != null)
                .collect(Collectors.groupingBy(
                        content -> content.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));

        // Calculate growth rate using stream reduction
        double growthRate = monthlyTrends.values().stream()
                .mapToDouble(Long::doubleValue)
                .reduce(0, (a, b) -> b - a) / monthlyTrends.size();

        // Top growth periods using stream sorting
        List<String> topGrowthPeriods = monthlyTrends.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Content velocity calculation
        double velocity = contents.stream()
                .filter(content -> content.getCreatedDate() != null)
                .filter(content -> content.getCreatedDate().isAfter(LocalDateTime.now().minusDays(30)))
                .count() / 30.0;

        // Author productivity using functional operations
        Map<String, Double> authorProductivity = contents.stream()
                .collect(Collectors.groupingBy(
                        content -> content.getCreatedBy() != null ? content.getCreatedBy() : "Unknown",
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.size() / Math.max(1.0, getDaysSinceFirstPost(list)))));

        return new TrendAnalysis(
                growthRate,
                monthlyTrends,
                topGrowthPeriods,
                velocity,
                authorProductivity);
    }

    /**
     * Analyzes content quality using advanced stream operations.
     *
     * <p>
     * <strong>Quality Metrics:</strong> Uses lambda expressions for
     * complex quality scoring and assessment algorithms.
     * </p>
     *
     * @param contents the content collection
     * @return quality analysis results
     */
    private QualityMetrics analyzeQuality(Collection<Content> contents) {
        // Average word count using specialized stream operations
        double avgWordCount = contents.parallelStream()
                .mapToInt(content -> content.getBody() != null ? content.getBody().split("\\s+").length : 0)
                .average()
                .orElse(0.0);

        // Content diversity using functional programming
        Set<String> uniqueTitles = contents.stream()
                .map(Content::getTitle)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        double contentDiversity = uniqueTitles.size() / (double) Math.max(1, contents.size());

        // Author diversity calculation
        Set<String> uniqueAuthors = contents.stream()
                .map(Content::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        double authorDiversity = uniqueAuthors.size() / (double) Math.max(1, contents.size());

        // Quality scores using complex lambda expressions
        Map<String, Double> qualityScores = contents.stream()
                .collect(Collectors.toMap(
                        Content::getId,
                        content -> calculateQualityScore(content),
                        (existing, replacement) -> existing));

        // Top quality authors using stream operations
        List<String> topQualityAuthors = contents.stream()
                .filter(content -> content.getCreatedBy() != null)
                .collect(Collectors.groupingBy(
                        Content::getCreatedBy,
                        Collectors.averagingDouble(this::calculateQualityScore)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new QualityMetrics(
                avgWordCount,
                contentDiversity,
                authorDiversity,
                qualityScores,
                topQualityAuthors);
    }

    /**
     * Calculates comprehensive content metrics using stream operations.
     *
     * <p>
     * <strong>Metric Calculation:</strong> Uses functional reduction
     * and statistical stream operations for comprehensive metrics.
     * </p>
     *
     * @param contents the content collection
     * @return content metrics map
     */
    private Map<String, Double> calculateContentMetrics(Collection<Content> contents) {
        Map<String, Double> metrics = new HashMap<>();

        // Basic metrics using stream operations
        metrics.put("total_count", (double) contents.size());

        metrics.put("published_percentage",
                contents.stream()
                        .mapToDouble(content -> content.getStatus() == ContentStatus.PUBLISHED ? 100.0 : 0.0)
                        .average().orElse(0.0));

        metrics.put("average_title_length",
                contents.stream()
                        .filter(content -> content.getTitle() != null)
                        .mapToDouble(content -> content.getTitle().length())
                        .average().orElse(0.0));

        metrics.put("content_freshness_score",
                contents.stream()
                        .filter(content -> content.getCreatedDate() != null)
                        .mapToDouble(this::calculateFreshnessScore)
                        .average().orElse(0.0));

        // Advanced metrics using complex lambda expressions
        metrics.put("author_engagement_score",
                calculateAuthorEngagementScore(contents));

        metrics.put("content_complexity_score",
                contents.parallelStream()
                        .mapToDouble(this::calculateComplexityScore)
                        .average().orElse(0.0));

        return metrics;
    }

    /**
     * Searches and ranks content using functional programming.
     *
     * <p>
     * <strong>Functional Search:</strong> Implements search ranking using
     * stream operations, lambda expressions, and custom comparators.
     * </p>
     *
     * @param contents   the content collection to search
     * @param query      the search query
     * @param maxResults maximum number of results
     * @return ranked search results
     * @throws ContentManagementException if search fails
     */
    public List<ContentDTO> searchAndRank(Collection<Content> contents,
            String query,
            int maxResults)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Searching and ranking {} contents for: '{}'", contents.size(), query);

            return contents.parallelStream()
                    .filter(content -> matchesQuery(content, query))
                    .map(ContentMapper.toDTO()::apply)
                    .sorted((dto1, dto2) -> Double.compare(
                            calculateRelevanceScore(dto2, query),
                            calculateRelevanceScore(dto1, query)))
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }, "Content search and ranking failed");
    }

    /**
     * Generates content recommendations using collaborative filtering.
     *
     * <p>
     * <strong>Machine Learning Integration:</strong> Uses stream-based
     * algorithms for content recommendation with functional programming.
     * </p>
     *
     * @param contents           the content collection
     * @param targetContent      the target content for recommendations
     * @param maxRecommendations maximum number of recommendations
     * @return recommended content list
     * @throws ContentManagementException if recommendation generation fails
     */
    public List<ContentDTO> generateRecommendations(Collection<Content> contents,
            Content targetContent,
            int maxRecommendations)
            throws ContentManagementException {
        return ExceptionShielder.shield(() -> {
            logger.debug("Generating {} recommendations for content: {}",
                    maxRecommendations, targetContent.getTitle());

            return contents.parallelStream()
                    .filter(content -> !content.getId().equals(targetContent.getId()))
                    .map(ContentMapper.toDTO()::apply)
                    .sorted((dto1, dto2) -> Double.compare(
                            calculateSimilarityScore(dto2, targetContent),
                            calculateSimilarityScore(dto1, targetContent)))
                    .limit(maxRecommendations)
                    .collect(Collectors.toList());
        }, "Content recommendation generation failed");
    }

    /**
     * Processes content asynchronously with stream operations.
     *
     * <p>
     * <strong>Async Stream Processing:</strong> Combines CompletableFuture
     * with stream operations for high-performance async processing.
     * </p>
     *
     * @param contents the content collection
     * @return CompletableFuture with analytics result
     */
    public CompletableFuture<AnalyticsResult> analyzeAsync(Collection<Content> contents) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeContent(contents);
            } catch (ContentManagementException e) {
                logger.logError("Async analysis failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    // Private helper methods using functional programming

    private double getDaysSinceFirstPost(List<Content> authorContent) {
        return authorContent.stream()
                .filter(content -> content.getCreatedDate() != null)
                .map(Content::getCreatedDate)
                .min(LocalDateTime::compareTo)
                .map(firstPost -> java.time.Duration.between(firstPost, LocalDateTime.now()).toDays())
                .orElse(1L).doubleValue();
    }

    private double calculateQualityScore(Content content) {
        double score = 0.0;

        // Title quality (lambda expression)
        if (content.getTitle() != null && !content.getTitle().trim().isEmpty()) {
            score += Math.min(10.0, content.getTitle().length() / 5.0);
        }

        // Content quality (functional approach)
        if (content.getBody() != null) {
            int wordCount = content.getBody().split("\\s+").length;
            score += Math.min(20.0, wordCount / 50.0);
        }

        // Status bonus
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            score += 5.0;
        }

        return score;
    }

    private double calculateFreshnessScore(Content content) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = content.getCreatedDate();

        if (created == null)
            return 0.0;

        long daysSinceCreation = java.time.Duration.between(created, now).toDays();
        return Math.max(0.0, 100.0 - (daysSinceCreation * 2.0));
    }

    private double calculateComplexityScore(Content content) {
        if (content.getBody() == null)
            return 0.0;

        String text = content.getBody();
        double sentenceCount = text.split("[.!?]+").length;
        double wordCount = text.split("\\s+").length;

        return sentenceCount > 0 ? wordCount / sentenceCount : 0.0;
    }

    private double calculateAuthorEngagementScore(Collection<Content> contents) {
        Map<String, Long> authorCounts = contents.stream()
                .collect(Collectors.groupingBy(
                        content -> content.getCreatedBy() != null ? content.getCreatedBy() : "Unknown",
                        Collectors.counting()));

        return authorCounts.values().stream()
                .mapToDouble(Long::doubleValue)
                .average()
                .orElse(0.0);
    }

    private boolean matchesQuery(Content content, String query) {
        String lowerQuery = query.toLowerCase();
        return (content.getTitle() != null && content.getTitle().toLowerCase().contains(lowerQuery)) ||
                (content.getBody() != null && content.getBody().toLowerCase().contains(lowerQuery)) ||
                (content.getCreatedBy() != null && content.getCreatedBy().toLowerCase().contains(lowerQuery));
    }

    private double calculateRelevanceScore(ContentDTO dto, String query) {
        double score = 0.0;
        String lowerQuery = query.toLowerCase();

        if (dto.title() != null && dto.title().toLowerCase().contains(lowerQuery)) {
            score += 10.0;
        }
        if (dto.author() != null && dto.author().toLowerCase().contains(lowerQuery)) {
            score += 5.0;
        }
        if (dto.status() == ContentStatus.PUBLISHED) {
            score += 2.0;
        }

        return score;
    }

    private double calculateSimilarityScore(ContentDTO dto, Content targetContent) {
        double score = 0.0;

        // Author similarity
        if (dto.author() != null && dto.author().equals(targetContent.getCreatedBy())) {
            score += 10.0;
        }

        // Status similarity
        if (dto.status() == targetContent.getStatus()) {
            score += 5.0;
        }

        // Word count similarity
        int targetWordCount = targetContent.getBody() != null ? targetContent.getBody().split("\\s+").length : 0;
        double wordCountDiff = Math.abs(dto.wordCount() - targetWordCount);
        score += Math.max(0, 5.0 - (wordCountDiff / 100.0));

        return score;
    }
}
