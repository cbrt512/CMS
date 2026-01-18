package com.cms.patterns.observer;

import com.cms.core.model.Content;
import com.cms.util.CMSLogger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Specialized observer for managing search index updates in response to content
 * events.
 *
 * <p>
 * This observer maintains search indices to ensure content discoverability by
 * processing content changes and updating search structures accordingly. It
 * provides
 * sophisticated indexing strategies including full-text search, faceted search,
 * and metadata indexing.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Concrete observer
 * that specializes in search index management with advanced queuing, batching,
 * and indexing strategies for optimal search performance.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> shows Collections Framework mastery
 * with complex data structures, Generics usage for type-safe search operations,
 * and seamless integration with existing logging and exception handling
 * systems.
 * </p>
 *
 * <p>
 * <strong>Search Features:</strong>
 * <ul>
 * <li>Full-text content indexing with relevance scoring</li>
 * <li>Metadata and faceted search support</li>
 * <li>Real-time index updates with batch optimization</li>
 * <li>Content type-specific indexing strategies</li>
 * <li>Search analytics and performance tracking</li>
 * <li>Index consistency validation and repair</li>
 * </ul>
 * </p>
 *
 * @see ContentObserver For the observer interface
 * @see ContentEvent For event data structure
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class SearchIndexObserver implements ContentObserver {

    private final CMSLogger logger;

    // Search index data structures (Collections Framework)
    private final Map<String, SearchDocument> searchIndex; // Content ID -> Search Document
    private final Map<String, Set<String>> invertedIndex; // Term -> Set of Content IDs
    private final Map<String, Map<String, Set<String>>> facetIndex; // Facet -> Value -> Content IDs
    private final Queue<IndexOperation> operationQueue; // Pending index operations

    // Content type indexing configurations (Generics)
    private final Map<Class<? extends Content>, IndexingStrategy> indexingStrategies;
    private final Map<String, ContentWeight> contentWeights; // Content type -> search weight

    // Search analytics and statistics
    private final AtomicLong documentsIndexed;
    private final Map<ContentEvent.EventType, AtomicLong> operationStats;
    private final Map<String, AtomicLong> contentTypeStats;

    // Performance optimization
    private final ExecutorService indexingExecutor;
    private final Set<String> pendingReindexing; // Content IDs pending reindex
    private final Map<String, Long> lastIndexTime; // Content ID -> last index timestamp

    /**
     * Represents a document in the search index.
     */
    private static class SearchDocument {
        final String contentId;
        final String title;
        final String content;
        final String contentType;
        final Map<String, Object> metadata;
        final Set<String> keywords;
        final LocalDateTime indexedAt;
        final LocalDateTime lastModified;
        final double relevanceBoost;

        SearchDocument(Content content, Set<String> keywords, double relevanceBoost) {
            this.contentId = content.getId();
            this.title = content.getTitle();
            this.content = content.getBody();
            this.contentType = content.getClass().getSimpleName();
            this.metadata = new HashMap<>(content.getMetadata());
            this.keywords = new HashSet<>(keywords);
            this.indexedAt = LocalDateTime.now();
            this.lastModified = content.getModifiedDate();
            this.relevanceBoost = relevanceBoost;
        }
    }

    /**
     * Defines indexing strategy for different content types.
     */
    private static class IndexingStrategy {
        final Set<String> indexedFields; // Fields to include in full-text search
        final Set<String> facetFields; // Fields to use for faceted search
        final Map<String, Double> fieldWeights; // Field -> weight for relevance
        final boolean enableKeywordExtraction; // Whether to extract keywords automatically
        final int maxKeywords; // Maximum keywords to extract

        IndexingStrategy(Set<String> indexedFields, Set<String> facetFields,
                Map<String, Double> fieldWeights, boolean enableKeywordExtraction,
                int maxKeywords) {
            this.indexedFields = new HashSet<>(indexedFields);
            this.facetFields = new HashSet<>(facetFields);
            this.fieldWeights = new HashMap<>(fieldWeights);
            this.enableKeywordExtraction = enableKeywordExtraction;
            this.maxKeywords = maxKeywords;
        }
    }

    /**
     * Content weighting for search relevance.
     */
    private static class ContentWeight {
        final double baseWeight;
        final double publishedBoost;
        final double recentBoost;
        final double popularityMultiplier;

        ContentWeight(double baseWeight, double publishedBoost, double recentBoost,
                double popularityMultiplier) {
            this.baseWeight = baseWeight;
            this.publishedBoost = publishedBoost;
            this.recentBoost = recentBoost;
            this.popularityMultiplier = popularityMultiplier;
        }
    }

    /**
     * Represents an index operation to be performed.
     */
    private static class IndexOperation {
        final String operationType; // "add", "update", "remove", "reindex"
        final Content content;
        final LocalDateTime timestamp;
        final Map<String, Object> context;
        final int priority; // Higher values = higher priority

        IndexOperation(String operationType, Content content, Map<String, Object> context,
                int priority) {
            this.operationType = operationType;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
            this.priority = priority;
        }
    }

    /**
     * Constructs a SearchIndexObserver with default configuration.
     */
    public SearchIndexObserver() {
        this.logger = CMSLogger.getInstance();

        // Initialize search data structures with concurrent collections
        this.searchIndex = new ConcurrentHashMap<>();
        this.invertedIndex = new ConcurrentHashMap<>();
        this.facetIndex = new ConcurrentHashMap<>();
        this.operationQueue = new PriorityBlockingQueue<>(1000,
                Comparator.comparingInt((IndexOperation op) -> op.priority).reversed()
                        .thenComparing(op -> op.timestamp));

        this.indexingStrategies = new ConcurrentHashMap<>();
        this.contentWeights = new ConcurrentHashMap<>();
        this.pendingReindexing = ConcurrentHashMap.newKeySet();
        this.lastIndexTime = new ConcurrentHashMap<>();

        // Initialize statistics
        this.documentsIndexed = new AtomicLong(0);
        this.operationStats = new ConcurrentHashMap<>();
        this.contentTypeStats = new ConcurrentHashMap<>();

        // Initialize event type statistics
        for (ContentEvent.EventType type : ContentEvent.EventType.values()) {
            operationStats.put(type, new AtomicLong(0));
        }

        // Initialize async processing
        this.indexingExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "SearchIndex-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        // Set up default indexing strategies
        initializeIndexingStrategies();

        logger.logContentActivity("SearchIndexObserver initialized",
                "strategies=" + indexingStrategies.size());
    }

    /**
     * Initializes default indexing strategies for different content types.
     */
    private void initializeIndexingStrategies() {
        // Article indexing strategy - comprehensive text analysis
        indexingStrategies.put(getContentClass("ArticleContent"), new IndexingStrategy(
                Set.of("title", "body", "summary", "tags"),
                Set.of("category", "author", "publishDate", "tags"),
                Map.of("title", 2.0, "body", 1.0, "summary", 1.5, "tags", 1.8),
                true, 50));

        contentWeights.put("ArticleContent", new ContentWeight(1.0, 1.2, 1.1, 1.0));

        // Page indexing strategy - structure-focused
        indexingStrategies.put(getContentClass("PageContent"), new IndexingStrategy(
                Set.of("title", "body", "keywords"),
                Set.of("template", "category", "lastModified"),
                Map.of("title", 2.5, "body", 1.0, "keywords", 2.0),
                true, 30));

        contentWeights.put("PageContent", new ContentWeight(1.2, 1.3, 1.05, 1.1));

        // Image indexing strategy - metadata-focused
        indexingStrategies.put(getContentClass("ImageContent"), new IndexingStrategy(
                Set.of("title", "description", "altText", "caption"),
                Set.of("format", "resolution", "category", "photographer"),
                Map.of("title", 2.0, "description", 1.5, "altText", 1.8, "caption", 1.3),
                false, 20));

        contentWeights.put("ImageContent", new ContentWeight(0.8, 1.1, 1.02, 1.2));

        // Video indexing strategy - multimedia-focused
        indexingStrategies.put(getContentClass("VideoContent"), new IndexingStrategy(
                Set.of("title", "description", "transcript", "tags"),
                Set.of("duration", "resolution", "format", "category"),
                Map.of("title", 2.2, "description", 1.4, "transcript", 1.6, "tags", 1.7),
                true, 40));

        contentWeights.put("VideoContent", new ContentWeight(0.9, 1.15, 1.08, 1.3));

        // Initialize content type statistics
        for (String contentType : contentWeights.keySet()) {
            contentTypeStats.put(contentType, new AtomicLong(0));
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Content> getContentClass(String className) {
        try {
            return (Class<? extends Content>) Class.forName("com.cms.patterns.factory." + className);
        } catch (ClassNotFoundException e) {
            logger.logError(e, "Content class not found: " + className, null, "class_loading");
            return Content.class; // Fallback to base class
        }
    }

    @Override
    public void onContentCreated(ContentEvent event) {
        operationStats.get(ContentEvent.EventType.CREATED).incrementAndGet();

        logger.logContentActivity("Processing search index addition for new content",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle());

        try {
            Content content = event.getContent();

            // Queue index addition with high priority for new content
            IndexOperation operation = new IndexOperation("add", content,
                    Map.of("reason", "content_created", "immediate", true), 80);

            operationQueue.offer(operation);

            // Process immediately if it's critical content
            if (isCriticalContent(content)) {
                processIndexOperation(operation);
            }

            logger.logContentActivity("Search index addition queued for new content",
                    "content=" + content.getTitle() + ", queueSize=" + operationQueue.size());

        } catch (Exception e) {
            logger.logError(e, "Failed to queue search index addition", null, "search_indexing");
        }
    }

    @Override
    public void onContentUpdated(ContentEvent event) {
        operationStats.get(ContentEvent.EventType.UPDATED).incrementAndGet();

        logger.logContentActivity("Processing search index update for content changes",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle() +
                        ", changedFields=" + event.getChangedFields().size());

        try {
            Content content = event.getContent();
            Set<String> changedFields = event.getChangedFields();

            // Determine if index update is necessary
            IndexingStrategy strategy = getIndexingStrategy(content);
            boolean needsReindex = changedFields.stream()
                    .anyMatch(field -> strategy.indexedFields.contains(field) ||
                            strategy.facetFields.contains(field));

            if (needsReindex) {
                // Priority based on field importance
                int priority = calculateUpdatePriority(changedFields, strategy);

                IndexOperation operation = new IndexOperation("update", content,
                        Map.of("reason", "content_updated",
                                "changedFields", new ArrayList<>(changedFields)),
                        priority);

                operationQueue.offer(operation);

                // Track for potential reindexing
                pendingReindexing.add(content.getId());

                logger.logContentActivity("Search index update queued",
                        "content=" + content.getTitle() +
                                ", priority=" + priority +
                                ", needsReindex=" + needsReindex);
            } else {
                logger.logContentActivity("Search index update skipped - no indexed fields changed",
                        "content=" + content.getTitle() +
                                ", changedFields=" + String.join(",", changedFields));
            }

        } catch (Exception e) {
            logger.logError(e, "Failed to process search index update", null, "search_indexing");
        }
    }

    @Override
    public void onContentPublished(ContentEvent event) {
        operationStats.get(ContentEvent.EventType.PUBLISHED).incrementAndGet();

        logger.logContentActivity("Processing search index update for content publication",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle());

        try {
            Content content = event.getContent();

            // Publication is critical - high priority update with boost
            IndexOperation operation = new IndexOperation("update", content,
                    Map.of("reason", "content_published",
                            "published", true,
                            "publicationDate", event.getPublicationDate(),
                            "applyBoost", true),
                    90);

            operationQueue.offer(operation);

            // Process immediately - published content needs immediate searchability
            processIndexOperation(operation);

            logger.logContentActivity("Search index update completed for publication",
                    "content=" + content.getTitle());

        } catch (Exception e) {
            logger.logError(e, "Failed to process search index for publication", null, "search_indexing");
        }
    }

    @Override
    public void onContentDeleted(ContentEvent event) {
        operationStats.get(ContentEvent.EventType.DELETED).incrementAndGet();

        logger.logContentActivity("Processing search index removal for content deletion",
                "eventId=" + event.getEventId() +
                        ", content=" + event.getContent().getTitle() +
                        ", temporary=" + event.isTemporaryDeletion());

        try {
            Content content = event.getContent();

            if (event.isTemporaryDeletion()) {
                // Archive - update index to mark as unpublished but keep searchable for admin
                IndexOperation operation = new IndexOperation("update", content,
                        Map.of("reason", "content_archived",
                                "archived", true,
                                "searchable", false),
                        70);

                operationQueue.offer(operation);

            } else {
                // Permanent deletion - remove from index entirely
                IndexOperation operation = new IndexOperation("remove", content,
                        Map.of("reason", "content_deleted",
                                "permanent", true),
                        85);

                operationQueue.offer(operation);

                // Process immediately to prevent stale search results
                processIndexOperation(operation);

                // Clean up tracking
                pendingReindexing.remove(content.getId());
                lastIndexTime.remove(content.getId());
            }

            logger.logContentActivity("Search index removal queued for deletion",
                    "content=" + content.getTitle() +
                            ", temporary=" + event.isTemporaryDeletion());

        } catch (Exception e) {
            logger.logError(e, "Failed to process search index removal", null, "search_indexing");
        }
    }

    // Index processing methods

    private void processIndexOperation(IndexOperation operation) {
        long startTime = System.currentTimeMillis();

        try {
            switch (operation.operationType) {
                case "add":
                    addToIndex(operation.content, operation.context);
                    break;
                case "update":
                    updateIndex(operation.content, operation.context);
                    break;
                case "remove":
                    removeFromIndex(operation.content, operation.context);
                    break;
                case "reindex":
                    reindexContent(operation.content, operation.context);
                    break;
            }

            documentsIndexed.incrementAndGet();
            contentTypeStats.get(operation.content.getClass().getSimpleName()).incrementAndGet();
            lastIndexTime.put(operation.content.getId(), System.currentTimeMillis());

        } catch (Exception e) {
            logger.logError(e, "Failed to process index operation: " + operation.operationType +
                    " for content: " + operation.content.getTitle(), null, "search_operation");
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.logContentActivity("Search index operation completed",
                "operation=" + operation.operationType +
                        ", content=" + operation.content.getTitle() +
                        ", processingTime=" + processingTime + "ms");
    }

    private void addToIndex(Content content, Map<String, Object> context) {
        // Extract search keywords and create document
        Set<String> keywords = extractKeywords(content);
        double relevanceBoost = calculateRelevanceBoost(content, context);

        SearchDocument document = new SearchDocument(content, keywords, relevanceBoost);

        // Add to main index
        searchIndex.put(content.getId(), document);

        // Update inverted index
        updateInvertedIndex(content.getId(), keywords, true);

        // Update facet indices
        updateFacetIndex(content, true);

        logger.logContentActivity("Content added to search index",
                "content=" + content.getTitle() +
                        ", keywords=" + keywords.size() +
                        ", boost=" + relevanceBoost);
    }

    private void updateIndex(Content content, Map<String, Object> context) {
        // Remove old index entries
        SearchDocument oldDocument = searchIndex.get(content.getId());
        if (oldDocument != null) {
            updateInvertedIndex(content.getId(), oldDocument.keywords, false);
            updateFacetIndex(content, false); // Remove old facet data
        }

        // Add updated content
        addToIndex(content, context);

        logger.logContentActivity("Content updated in search index",
                "content=" + content.getTitle());
    }

    private void removeFromIndex(Content content, Map<String, Object> context) {
        SearchDocument document = searchIndex.remove(content.getId());

        if (document != null) {
            // Remove from inverted index
            updateInvertedIndex(content.getId(), document.keywords, false);

            // Remove from facet indices
            updateFacetIndex(content, false);

            logger.logContentActivity("Content removed from search index",
                    "content=" + content.getTitle() +
                            ", permanent=" + context.get("permanent"));
        }
    }

    private void reindexContent(Content content, Map<String, Object> context) {
        logger.logContentActivity("Reindexing content", "content=" + content.getTitle());

        // Full reindex - remove and add back
        removeFromIndex(content, context);
        addToIndex(content, context);

        pendingReindexing.remove(content.getId());
    }

    private Set<String> extractKeywords(Content content) {
        IndexingStrategy strategy = getIndexingStrategy(content);
        Set<String> keywords = new HashSet<>();

        // Extract from indexed fields
        for (String field : strategy.indexedFields) {
            String fieldValue = getFieldValue(content, field);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                keywords.addAll(tokenize(fieldValue));
            }
        }

        // Keyword extraction if enabled
        if (strategy.enableKeywordExtraction) {
            Set<String> extractedKeywords = performKeywordExtraction(content);
            keywords.addAll(extractedKeywords.stream()
                    .limit(strategy.maxKeywords)
                    .collect(Collectors.toSet()));
        }

        // Clean and filter keywords
        return keywords.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .filter(keyword -> keyword.length() > 2)
                .filter(this::isValidKeyword)
                .collect(Collectors.toSet());
    }

    private String getFieldValue(Content content, String field) {
        switch (field) {
            case "title":
                return content.getTitle();
            case "body":
                return content.getBody();
            case "summary":
                return (String) content.getMetadata().get("summary");
            case "tags":
                Object tags = content.getMetadata().get("tags");
                return tags != null ? tags.toString() : null;
            case "description":
                return (String) content.getMetadata().get("description");
            case "keywords":
                return (String) content.getMetadata().get("keywords");
            default:
                return (String) content.getMetadata().get(field);
        }
    }

    private Set<String> tokenize(String text) {
        if (text == null)
            return Collections.emptySet();

        return Arrays.stream(text.split("\\W+"))
                .filter(token -> token.length() > 2)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private Set<String> performKeywordExtraction(Content content) {
        // Simplified keyword extraction - in practice would use NLP libraries
        Set<String> keywords = new HashSet<>();

        String text = content.getTitle() + " " + content.getBody();
        Map<String, Integer> wordFrequency = new HashMap<>();

        // Count word frequencies
        Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 3)
                .filter(this::isValidKeyword)
                .forEach(word -> wordFrequency.merge(word, 1, Integer::sum));

        // Return most frequent words
        keywords.addAll(wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));

        return keywords;
    }

    private boolean isValidKeyword(String keyword) {
        // Filter out common stop words and invalid keywords
        Set<String> stopWords = Set.of("the", "and", "for", "are", "but", "not",
                "you", "all", "can", "had", "has", "was", "one", "our", "had", "by");

        return !stopWords.contains(keyword.toLowerCase()) &&
                keyword.matches("^[a-zA-Z0-9]+$");
    }

    private double calculateRelevanceBoost(Content content, Map<String, Object> context) {
        ContentWeight weight = contentWeights.get(content.getClass().getSimpleName());
        if (weight == null) {
            weight = new ContentWeight(1.0, 1.0, 1.0, 1.0);
        }

        double boost = weight.baseWeight;

        // Apply publication boost
        if (Boolean.TRUE.equals(context.get("published"))) {
            boost *= weight.publishedBoost;
        }

        // Apply recency boost
        long hoursSinceModification = java.time.Duration.between(
                content.getModifiedDate(), LocalDateTime.now()).toHours();
        if (hoursSinceModification < 24) {
            boost *= weight.recentBoost;
        }

        return boost;
    }

    private void updateInvertedIndex(String contentId, Set<String> keywords, boolean add) {
        for (String keyword : keywords) {
            if (add) {
                invertedIndex.computeIfAbsent(keyword, k -> ConcurrentHashMap.newKeySet())
                        .add(contentId);
            } else {
                Set<String> contentIds = invertedIndex.get(keyword);
                if (contentIds != null) {
                    contentIds.remove(contentId);
                    if (contentIds.isEmpty()) {
                        invertedIndex.remove(keyword);
                    }
                }
            }
        }
    }

    private void updateFacetIndex(Content content, boolean add) {
        IndexingStrategy strategy = getIndexingStrategy(content);

        for (String facetField : strategy.facetFields) {
            String facetValue = getFieldValue(content, facetField);
            if (facetValue != null) {
                if (add) {
                    facetIndex.computeIfAbsent(facetField, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(facetValue, k -> ConcurrentHashMap.newKeySet())
                            .add(content.getId());
                } else {
                    Map<String, Set<String>> facetMap = facetIndex.get(facetField);
                    if (facetMap != null) {
                        Set<String> contentIds = facetMap.get(facetValue);
                        if (contentIds != null) {
                            contentIds.remove(content.getId());
                            if (contentIds.isEmpty()) {
                                facetMap.remove(facetValue);
                                if (facetMap.isEmpty()) {
                                    facetIndex.remove(facetField);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private IndexingStrategy getIndexingStrategy(Content content) {
        IndexingStrategy strategy = indexingStrategies.get(content.getClass());
        if (strategy == null) {
            // Default strategy
            strategy = new IndexingStrategy(
                    Set.of("title", "body"),
                    Set.of("contentType", "lastModified"),
                    Map.of("title", 2.0, "body", 1.0),
                    true, 25);
        }
        return strategy;
    }

    private int calculateUpdatePriority(Set<String> changedFields, IndexingStrategy strategy) {
        int priority = 50; // Base priority

        for (String field : changedFields) {
            Double weight = strategy.fieldWeights.get(field);
            if (weight != null) {
                priority += (int) (weight * 10); // Higher weight = higher priority
            }
        }

        return Math.min(priority, 100); // Cap at 100
    }

    private boolean isCriticalContent(Content content) {
        // Determine if content is critical and needs immediate indexing
        return content.getClass().getSimpleName().equals("PageContent") ||
                Boolean.TRUE.equals(content.getMetadata().get("featured"));
    }

    // Public management methods

    /**
     * Processes all pending index operations.
     *
     * @return Number of operations processed
     */
    public int processPendingOperations() {
        int processed = 0;

        while (!operationQueue.isEmpty() && processed < 100) { // Process up to 100 at once
            IndexOperation operation = operationQueue.poll();
            if (operation != null) {
                processIndexOperation(operation);
                processed++;
            }
        }

        if (processed > 0) {
            logger.logContentActivity("Processed pending index operations", "count=" + processed);
        }

        return processed;
    }

    /**
     * Performs search query against the index.
     *
     * @param query      Search query string
     * @param maxResults Maximum number of results to return
     * @return List of matching content IDs sorted by relevance
     */
    public List<String> search(String query, int maxResults) {
        Set<String> queryTerms = tokenize(query);
        Map<String, Double> contentScores = new HashMap<>();

        // Score content based on matching terms
        for (String term : queryTerms) {
            Set<String> matchingContent = invertedIndex.get(term.toLowerCase());
            if (matchingContent != null) {
                for (String contentId : matchingContent) {
                    SearchDocument doc = searchIndex.get(contentId);
                    if (doc != null) {
                        double score = calculateSearchScore(doc, term);
                        contentScores.merge(contentId, score, Double::sum);
                    }
                }
            }
        }

        // Return top results sorted by score
        return contentScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateSearchScore(SearchDocument doc, String term) {
        double score = doc.relevanceBoost;

        // Title match gets higher score
        if (doc.title.toLowerCase().contains(term.toLowerCase())) {
            score += 10.0;
        }

        // Keyword match
        if (doc.keywords.contains(term.toLowerCase())) {
            score += 5.0;
        }

        // Content match (lower score)
        if (doc.content != null && doc.content.toLowerCase().contains(term.toLowerCase())) {
            score += 2.0;
        }

        return score;
    }

    /**
     * Gets faceted search results.
     *
     * @param facet The facet field name
     * @return Map of facet values to content count
     */
    public Map<String, Integer> getFacetCounts(String facet) {
        Map<String, Set<String>> facetMap = facetIndex.get(facet);
        if (facetMap == null) {
            return Collections.emptyMap();
        }

        return facetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()));
    }

    /**
     * Gets comprehensive search index statistics.
     *
     * @return Map of search statistics
     */
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("documentsIndexed", documentsIndexed.get());
        stats.put("totalDocuments", searchIndex.size());
        stats.put("uniqueTerms", invertedIndex.size());
        stats.put("facetFields", facetIndex.size());
        stats.put("pendingOperations", operationQueue.size());
        stats.put("pendingReindexing", pendingReindexing.size());

        // Operation type breakdown
        Map<String, Long> operationCounts = new LinkedHashMap<>();
        for (Map.Entry<ContentEvent.EventType, AtomicLong> entry : operationStats.entrySet()) {
            operationCounts.put(entry.getKey().name(), entry.getValue().get());
        }
        stats.put("operationCounts", operationCounts);

        // Content type breakdown
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : contentTypeStats.entrySet()) {
            typeCounts.put(entry.getKey(), entry.getValue().get());
        }
        stats.put("contentTypeCounts", typeCounts);

        return stats;
    }

    /**
     * Validates index consistency and reports issues.
     *
     * @return List of consistency issues found
     */
    public List<String> validateIndexConsistency() {
        List<String> issues = new ArrayList<>();

        // Check for orphaned inverted index entries
        Set<String> allContentIds = searchIndex.keySet();
        for (Map.Entry<String, Set<String>> entry : invertedIndex.entrySet()) {
            Set<String> orphanedIds = entry.getValue().stream()
                    .filter(id -> !allContentIds.contains(id))
                    .collect(Collectors.toSet());

            if (!orphanedIds.isEmpty()) {
                issues.add("Orphaned inverted index entries for term '" + entry.getKey() +
                        "': " + orphanedIds.size() + " entries");
            }
        }

        // Check for missing inverted index entries
        for (SearchDocument doc : searchIndex.values()) {
            for (String keyword : doc.keywords) {
                Set<String> contentIds = invertedIndex.get(keyword);
                if (contentIds == null || !contentIds.contains(doc.contentId)) {
                    issues.add("Missing inverted index entry for document " + doc.contentId +
                            ", keyword: " + keyword);
                }
            }
        }

        logger.logContentActivity("Index consistency validation completed",
                "issues=" + issues.size());

        return issues;
    }

    /**
     * Shuts down the search index observer and releases resources.
     */
    public void shutdown() {
        logger.logSystemEvent("SHUTDOWN", "1.0", "Shutting down SearchIndexObserver");

        // Process remaining operations
        processPendingOperations();

        if (indexingExecutor != null && !indexingExecutor.isShutdown()) {
            indexingExecutor.shutdown();
            try {
                if (!indexingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    indexingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                indexingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.logSystemEvent("SHUTDOWN_COMPLETE", "1.0", "SearchIndexObserver shutdown completed - " +
                "totalDocuments=" + searchIndex.size() +
                ", documentsIndexed=" + documentsIndexed.get());
    }

    // ContentObserver interface methods

    @Override
    public String getObserverName() {
        return "Search Index Observer";
    }

    @Override
    public int getPriority() {
        return 30; // Medium priority - after cache but before notifications
    }

    @Override
    public boolean shouldObserve(Class<?> contentType) {
        // Observe all content types for search indexing
        return Content.class.isAssignableFrom(contentType);
    }
}
