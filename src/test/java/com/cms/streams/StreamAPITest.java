package com.cms.streams;

import com.cms.core.model.*;
import com.cms.patterns.factory.*;
import com.cms.streams.ContentMapper.ContentDTO;
import com.cms.streams.ContentMapper.ContentSummary;
import com.cms.streams.ContentStreamProcessor.ProcessingStats;
import com.cms.streams.ContentStreamProcessor.SearchResult;
import com.cms.streams.ContentAnalyzer.AnalyticsResult;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Stream API & Lambdas implementation.
 *
 * <p><strong>Stream API & Lambdas:</strong> This test suite validates
 * the complete Stream API & Lambdas implementation, testing all functional
 * programming features, lambda expressions, method references, parallel streams,
 * custom collectors, and integration with existing design patterns.</p>
 *
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li>ContentPredicate functional interface testing</li>
 *   <li>ContentMapper method references and lambda expressions</li>
 *   <li>ContentStreamProcessor parallel stream operations</li>
 *   <li>ContentAnalyzer advanced analytics and statistics</li>
 *   <li>Integration with Observer, Strategy, Factory patterns</li>
 *   <li>Performance testing with high-volume data</li>
 *   <li>Thread safety validation for concurrent operations</li>
 * </ul>
 *
 * <p><strong>Exam Requirement Validation:</strong> Each test method includes
 * assertions that validate specific Stream API & Lambdas requirements for
 * the 5-point advanced feature implementation.</p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@DisplayName("Stream API & Lambdas Implementation Tests")
@Execution(ExecutionMode.CONCURRENT)
public class StreamAPITest {

    private ContentFactory contentFactory;
    private ContentStreamProcessor streamProcessor;
    private ContentAnalyzer contentAnalyzer;
    private List<Content> testContent;

    @BeforeEach
    void setUp() throws ContentCreationException {
        contentFactory = ContentFactory.getInstance();
        streamProcessor = new ContentStreamProcessor();
        contentAnalyzer = new ContentAnalyzer();
        testContent = createTestContent();
    }

    @AfterEach
    void tearDown() {
        if (streamProcessor != null) {
            streamProcessor.close();
        }
    }

    // === ContentPredicate Functional Interface Tests ===

    @Test
    @DisplayName("ContentPredicate.byStatus() - Lambda Expression")
    void testContentPredicateByStatus() {
        // Test functional interface with lambda expression
        ContentPredicate publishedPredicate = ContentPredicate.byStatus(ContentStatus.PUBLISHED);
        ContentPredicate draftPredicate = ContentPredicate.byStatus(ContentStatus.DRAFT);

        long publishedCount = testContent.stream()
                .filter(publishedPredicate)
                .count();

        long draftCount = testContent.stream()
                .filter(draftPredicate)
                .count();

        assertTrue(publishedCount > 0, "Should have published content");
        assertTrue(draftCount > 0, "Should have draft content");
        assertEquals(testContent.size(), publishedCount + draftCount,
                    "All content should be either published or draft");
    }

    @Test
    @DisplayName("ContentPredicate.byTitleContaining() - Functional Filtering")
    void testContentPredicateByTitle() {
        ContentPredicate titlePredicate = ContentPredicate.byTitleContaining("Test");

        List<Content> matchingContent = testContent.stream()
                .filter(titlePredicate)
                .toList();

        assertFalse(matchingContent.isEmpty(), "Should find content with 'Test' in title");
        matchingContent.forEach(content ->
            assertTrue(content.getTitle().toLowerCase().contains("test"),
                      "All results should contain 'test' in title"));
    }

    @Test
    @DisplayName("ContentPredicate.withinDateRange() - Complex Lambda Logic")
    void testContentPredicateDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        ContentPredicate dateRangePredicate = ContentPredicate.withinDateRange(start, end);

        List<Content> recentContent = testContent.stream()
                .filter(dateRangePredicate)
                .toList();

        assertFalse(recentContent.isEmpty(), "Should have recent content");
        recentContent.forEach(content -> {
            assertTrue(content.getCreatedDate().isAfter(start) || content.getCreatedDate().isEqual(start),
                      "Content should be within date range");
            assertTrue(content.getCreatedDate().isBefore(end) || content.getCreatedDate().isEqual(end),
                      "Content should be within date range");
        });
    }

    @Test
    @DisplayName("ContentPredicate Composition - Functional Chaining")
    void testPredicateComposition() {
        // Test predicate composition with and(), or(), negate()
        ContentPredicate published = ContentPredicate.isPublished();
        ContentPredicate hasMinWords = ContentPredicate.hasMinWords(50);
        ContentPredicate titleContainsTest = ContentPredicate.byTitleContaining("Test");

        // Complex predicate: Published AND (has min words OR title contains test)
        ContentPredicate complexPredicate = published
                .and(hasMinWords.or(titleContainsTest));

        List<Content> results = testContent.stream()
                .filter(complexPredicate)
                .toList();

        results.forEach(content -> {
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(),
                        "All results should be published");
            assertTrue(getWordCount(content) >= 50 ||
                      content.getTitle().toLowerCase().contains("test"),
                      "Should match word count or title criteria");
        });
    }

    // === ContentMapper Method Reference Tests ===

    @Test
    @DisplayName("ContentMapper Method References - Clean Functional Code")
    void testContentMapperMethodReferences() {
        // Test method references for clean functional programming
        List<String> titles = testContent.stream()
                .map(ContentMapper.toTitle())
                .filter(Objects::nonNull)
                .toList();

        List<String> authors = testContent.stream()
                .map(ContentMapper.toAuthor())
                .distinct()
                .toList();

        List<ContentStatus> statuses = testContent.stream()
                .map(ContentMapper.toStatus())
                .distinct()
                .toList();

        assertEquals(testContent.size(), titles.size(), "Should have all titles");
        assertFalse(authors.isEmpty(), "Should have authors");
        assertTrue(statuses.contains(ContentStatus.PUBLISHED), "Should have published status");
        assertTrue(statuses.contains(ContentStatus.DRAFT), "Should have draft status");
    }

    @Test
    @DisplayName("ContentMapper.toDTO() - Record Integration")
    void testContentMapperToDTO() {
        List<ContentDTO> dtos = testContent.stream()
                .map(ContentMapper.toDTO())
                .toList();

        assertEquals(testContent.size(), dtos.size(), "Should have same number of DTOs");

        for (int i = 0; i < testContent.size(); i++) {
            Content content = testContent.get(i);
            ContentDTO dto = dtos.get(i);

            assertEquals(content.getId(), dto.id(), "IDs should match");
            assertEquals(content.getTitle(), dto.title(), "Titles should match");
            assertEquals(content.getCreatedBy(), dto.author(), "Authors should match");
            assertEquals(content.getStatus(), dto.status(), "Statuses should match");
            assertEquals(content.getCreatedDate(), dto.createdAt(), "Creation dates should match");
        }
    }

    @Test
    @DisplayName("ContentMapper.toFormattedString() - Lambda String Formatting")
    void testContentMapperFormatting() {
        Function<Content, String> formatter = ContentMapper.toFormattedString("%s by %s (%s)");

        List<String> formattedContent = testContent.stream()
                .map(formatter)
                .toList();

        assertEquals(testContent.size(), formattedContent.size(), "Should format all content");

        formattedContent.forEach(formatted -> {
            assertTrue(formatted.contains(" by "), "Should contain author separator");
            assertTrue(formatted.contains("(") && formatted.contains(")"),
                      "Should contain status in parentheses");
        });
    }

    // === ContentStreamProcessor Parallel Stream Tests ===

    @Test
    @DisplayName("Stream Filtering - Parallel Processing")
    void testStreamFiltering() throws ContentManagementException {
        ContentPredicate publishedPredicate = ContentPredicate.isPublished();

        List<Content> results = streamProcessor.filterContent(testContent, publishedPredicate);

        assertFalse(results.isEmpty(), "Should have filtered results");
        results.forEach(content ->
            assertEquals(ContentStatus.PUBLISHED, content.getStatus(),
                        "All results should be published"));
    }

    @Test
    @DisplayName("Stream Search - Functional Search Operations")
    void testStreamSearch() throws ContentManagementException {
        String searchTerm = "test";
        int maxResults = 10;

        SearchResult results = streamProcessor.searchContent(testContent, searchTerm, maxResults);

        assertNotNull(results, "Search results should not be null");
        assertTrue(results.totalMatches() >= 0, "Should have non-negative match count");
        assertNotNull(results.results(), "Results list should not be null");
        assertNotNull(results.authorCounts(), "Author counts should not be null");
        assertNotNull(results.statusCounts(), "Status counts should not be null");
        assertTrue(results.averageRelevanceScore() >= 0, "Relevance score should be non-negative");
    }

    @Test
    @DisplayName("Stream Statistics - Custom Collectors")
    void testStreamStatistics() throws ContentManagementException {
        ProcessingStats stats = streamProcessor.generateStatistics(testContent);

        assertNotNull(stats, "Statistics should not be null");
        assertEquals(testContent.size(), stats.totalCount(), "Total count should match");
        assertTrue(stats.publishedCount() >= 0, "Published count should be non-negative");
        assertTrue(stats.draftCount() >= 0, "Draft count should be non-negative");
        assertEquals(stats.totalCount(), stats.publishedCount() + stats.draftCount(),
                    "Counts should add up to total");

        assertNotNull(stats.statusDistribution(), "Status distribution should not be null");
        assertFalse(stats.statusDistribution().isEmpty(), "Should have status distribution");
        assertNotNull(stats.topAuthors(), "Top authors should not be null");
    }

    @Test
    @DisplayName("Date Range Grouping - Stream Collectors")
    void testDateRangeGrouping() throws ContentManagementException {
        int dayRange = 7;

        Map<String, List<ContentDTO>> grouped =
                streamProcessor.groupByDateRange(testContent, dayRange);

        assertNotNull(grouped, "Grouped results should not be null");
        assertFalse(grouped.isEmpty(), "Should have grouped results");

        // Verify all content is accounted for
        int totalGrouped = grouped.values().stream()
                .mapToInt(List::size)
                .sum();
        assertEquals(testContent.size(), totalGrouped, "All content should be grouped");
    }

    @Test
    @DisplayName("Async Processing - CompletableFuture Integration")
    void testAsyncProcessing() throws ExecutionException, InterruptedException {
        Function<Content, String> processor = ContentMapper.toTitle();

        CompletableFuture<List<String>> future =
                streamProcessor.processAsync(testContent, processor);

        List<String> results = future.get();

        assertNotNull(results, "Async results should not be null");
        assertEquals(testContent.size(), results.size(), "Should process all content");

        // Verify results match expected titles
        Set<String> expectedTitles = testContent.stream()
                .map(Content::getTitle)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<String> actualTitles = new HashSet<>(results);

        assertEquals(expectedTitles, actualTitles, "Titles should match");
    }

    // === ContentAnalyzer Advanced Analytics Tests ===

    @Test
    @DisplayName("Content Analytics - Comprehensive Analysis")
    void testContentAnalytics() throws ContentManagementException {
        AnalyticsResult analytics = contentAnalyzer.analyzeContent(testContent);

        assertNotNull(analytics, "Analytics result should not be null");
        assertNotNull(analytics.contentMetrics(), "Content metrics should not be null");
        assertNotNull(analytics.authorStatistics(), "Author statistics should not be null");
        assertNotNull(analytics.statusDistribution(), "Status distribution should not be null");
        assertNotNull(analytics.trendAnalysis(), "Trend analysis should not be null");
        assertNotNull(analytics.qualityMetrics(), "Quality metrics should not be null");
        assertNotNull(analytics.performanceStats(), "Performance stats should not be null");

        // Validate content metrics
        Map<String, Double> metrics = analytics.contentMetrics();
        assertTrue(metrics.containsKey("total_count"), "Should have total count metric");
        assertEquals(testContent.size(), metrics.get("total_count").intValue(),
                    "Total count should match");

        // Validate performance stats
        assertTrue(analytics.performanceStats().processingTimeMs() > 0,
                  "Should have positive processing time");
        assertEquals(testContent.size(), analytics.performanceStats().itemsProcessed(),
                    "Items processed should match");
    }

    @Test
    @DisplayName("Search and Ranking - Functional Search")
    void testSearchAndRanking() throws ContentManagementException {
        String query = "test";
        int maxResults = 5;

        List<ContentDTO> results = contentAnalyzer.searchAndRank(testContent, query, maxResults);

        assertNotNull(results, "Search results should not be null");
        assertTrue(results.size() <= maxResults, "Should not exceed max results");

        // Verify results are relevant (contain search term)
        results.forEach(dto -> {
            boolean titleMatch = dto.title() != null &&
                    dto.title().toLowerCase().contains(query.toLowerCase());
            boolean authorMatch = dto.author() != null &&
                    dto.author().toLowerCase().contains(query.toLowerCase());

            assertTrue(titleMatch || authorMatch,
                      "Result should match search term in title or author");
        });
    }

    @Test
    @DisplayName("Content Recommendations - Machine Learning Integration")
    void testContentRecommendations() throws ContentManagementException {
        Content targetContent = testContent.get(0);
        int maxRecommendations = 3;

        List<ContentDTO> recommendations = contentAnalyzer.generateRecommendations(
                testContent, targetContent, maxRecommendations);

        assertNotNull(recommendations, "Recommendations should not be null");
        assertTrue(recommendations.size() <= maxRecommendations,
                  "Should not exceed max recommendations");

        // Verify target content is not in recommendations
        recommendations.forEach(dto ->
            assertNotEquals(targetContent.getId(), dto.id(),
                          "Target content should not be recommended"));
    }

    @Test
    @DisplayName("Async Analytics - Non-blocking Processing")
    void testAsyncAnalytics() throws ExecutionException, InterruptedException {
        CompletableFuture<AnalyticsResult> future = contentAnalyzer.analyzeAsync(testContent);

        AnalyticsResult result = future.get();

        assertNotNull(result, "Async analytics result should not be null");
        assertTrue(result.performanceStats().processingTimeMs() > 0,
                  "Should have processing time");
        assertEquals(testContent.size(), result.performanceStats().itemsProcessed(),
                    "Should process all content");
    }

    // === Integration Tests with Design Patterns ===

    @Test
    @DisplayName("Factory Pattern Integration - Stream Processing")
    void testFactoryPatternIntegration() throws ContentCreationException, ContentManagementException {
        // Create different content types using Factory Pattern
        List<Content> mixedContent = Arrays.asList(
                contentFactory.createContent(ContentFactory.ContentType.ARTICLE,
                        "Stream Article", "Content about streams", "StreamAuthor"),
                contentFactory.createContent(ContentFactory.ContentType.PAGE,
                        "Stream Page", "Page content with streams", "PageAuthor"),
                contentFactory.createContent(ContentFactory.ContentType.IMAGE,
                        "Stream Image", "Image description", "ImageAuthor"),
                contentFactory.createContent(ContentFactory.ContentType.VIDEO,
                        "Stream Video", "Video about streaming", "VideoAuthor")
        );

        // Test stream operations with factory-created content
        Map<String, Long> typeCounts = mixedContent.stream()
                .collect(HashMap::new,
                        (map, content) -> map.merge(content.getClass().getSimpleName(), 1L, Long::sum),
                        (map1, map2) -> { map1.putAll(map2); return map1; });

        assertEquals(4, typeCounts.size(), "Should have 4 different content types");
        assertTrue(typeCounts.containsKey("ArticleContent"), "Should have article content");
        assertTrue(typeCounts.containsKey("PageContent"), "Should have page content");
        assertTrue(typeCounts.containsKey("ImageContent"), "Should have image content");
        assertTrue(typeCounts.containsKey("VideoContent"), "Should have video content");
    }

    // === Performance and Thread Safety Tests ===

    @Test
    @DisplayName("High Volume Stream Processing - Performance Test")
    void testHighVolumeProcessing() throws ContentManagementException {
        // Create large dataset for performance testing
        List<Content> largeDataset = createLargeDataset(1000);

        long startTime = System.currentTimeMillis();

        ProcessingStats stats = streamProcessor.generateStatistics(largeDataset);

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        assertEquals(largeDataset.size(), stats.totalCount(), "Should process all items");
        assertTrue(processingTime < 5000, "Should complete within 5 seconds"); // Performance threshold
        assertTrue(stats.totalCount() > 900, "Should handle large volumes");
    }

    @Test
    @DisplayName("Concurrent Stream Operations - Thread Safety")
    void testConcurrentStreamOperations() throws InterruptedException, ExecutionException {
        // Test thread safety with concurrent stream operations
        List<CompletableFuture<List<ContentDTO>>> futures = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return streamProcessor.mapToDTO(testContent);
                    } catch (ContentManagementException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        allOf.get(); // Wait for all to complete

        // Verify all operations completed successfully
        for (CompletableFuture<List<ContentDTO>> future : futures) {
            List<ContentDTO> result = future.get();
            assertEquals(testContent.size(), result.size(), "Each operation should process all content");
        }
    }

    @Test
    @DisplayName("Custom Collector Implementation - Advanced Stream Operations")
    void testCustomCollector() {
        // Test custom collector for content summarization
        int maxSummaryLength = 100;

        List<ContentSummary> summaries = testContent.stream()
                .collect(ContentStreamProcessor.summarizingCollector(maxSummaryLength));

        assertEquals(testContent.size(), summaries.size(), "Should have summary for each content");

        summaries.forEach(summary -> {
            assertNotNull(summary.id(), "Summary should have ID");
            assertNotNull(summary.title(), "Summary should have title");
            assertNotNull(summary.summary(), "Summary should have content summary");
            assertTrue(summary.summary().length() <= maxSummaryLength + 3, // +3 for "..."
                      "Summary should not exceed max length");
        });
    }

    // === Helper Methods ===

    private List<Content> createTestContent() throws ContentCreationException {
        List<Content> content = new ArrayList<>();

        // Create diverse test content with different statuses and properties
        for (int i = 0; i < 20; i++) {
            ContentFactory.ContentType type = ContentFactory.ContentType.values()
                    [i % ContentFactory.ContentType.values().length];

            Content item = contentFactory.createContent(type,
                    "Test Content " + i,
                    generateTestContentBody(i),
                    "Author" + (i % 5));

            // Set different statuses
            if (i % 2 == 0) {
                item.setStatus(ContentStatus.PUBLISHED, "system");
            } else {
                item.setStatus(ContentStatus.DRAFT, "system");
            }

            // Set creation dates (some recent, some older)
            LocalDateTime baseDate = LocalDateTime.now().minusDays(i);
            item.setCreatedAt(baseDate);

            content.add(item);
        }

        return content;
    }

    private List<Content> createLargeDataset(int size) {
        List<Content> dataset = new ArrayList<>();

        try {
            for (int i = 0; i < size; i++) {
                ContentFactory.ContentType type = ContentFactory.ContentType.ARTICLE;
                Content content = contentFactory.createContent(type,
                        "Performance Test Content " + i,
                        "This is performance test content with sufficient text for testing. " +
                        "It contains multiple words to test word count calculations and other metrics.",
                        "PerfAuthor" + (i % 10));

                content.setStatus(i % 3 == 0 ? ContentStatus.PUBLISHED : ContentStatus.DRAFT, "system");
                content.setCreatedAt(LocalDateTime.now().minusDays(i % 365));

                dataset.add(content);
            }
        } catch (ContentCreationException e) {
            fail("Failed to create large dataset: " + e.getMessage());
        }

        return dataset;
    }

    private String generateTestContentBody(int index) {
        String[] sentences = {
            "This is a test content body for stream API testing.",
            "It contains multiple sentences to test word counting functionality.",
            "Stream processing should handle this content efficiently.",
            "Lambda expressions make the code more readable and maintainable.",
            "Functional programming brings powerful capabilities to Java applications."
        };

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < (index % 5) + 2; i++) {
            body.append(sentences[i % sentences.length]).append(" ");
        }

        return body.toString().trim();
    }

    private int getWordCount(Content content) {
        if (content.getBody() == null || content.getBody().trim().isEmpty()) {
            return 0;
        }
        return content.getBody().trim().split("\\s+").length;
    }
}
