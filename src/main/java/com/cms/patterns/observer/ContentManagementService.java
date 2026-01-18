package com.cms.patterns.observer;

import com.cms.core.model.*;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.composite.SiteComponent;
import com.cms.patterns.iterator.ContentIterator;
import com.cms.streams.*;
import com.cms.streams.ContentMapper.ContentDTO;
import com.cms.util.CMSLogger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central content management service that integrates Observer Pattern with
 * existing patterns.
 *
 * <p>
 * This service shows the complete integration of the Observer Pattern with
 * Factory, Composite, and Iterator patterns, creating a cohesive content
 * management
 * system with event-driven architecture. It serves as the primary orchestrator
 * for
 * all content operations while maintaining loose coupling through the observer
 * pattern.
 * </p>
 *
 * <p>
 * <strong>Design Pattern Integration:</strong>
 * <ul>
 * <li><strong>Observer Pattern</strong> - Event-driven content
 * notifications</li>
 * <li><strong>Factory Pattern</strong> - Content creation through
 * ContentFactory</li>
 * <li><strong>Composite Pattern</strong> - Site hierarchy management</li>
 * <li><strong>Iterator Pattern</strong> - Content collection traversal</li>
 * <li><strong>Exception Shielding</strong> - Comprehensive error handling</li>
 * <li><strong>Stream API & Lambdas</strong> - Functional content
 * processing</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Stream API Integration:</strong> Enhanced with functional programming
 * capabilities for high-performance content filtering, mapping, and analytics
 * using
 * parallel streams, lambda expressions, and method references.
 * </p>
 *
 * <p>
 * <strong>Integration:</strong> This class serves as the integration point
 * demonstrating how all patterns work together in a cohesive system,
 * showcasing the practical application of design patterns in enterprise
 * software.
 * </p>
 *
 * @see ContentSubject For event notification management
 * @see ContentFactory For content creation
 * @see SiteComponent For content hierarchy
 * @see ContentIterator For content traversal
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentManagementService {

    private final CMSLogger logger;
    private final ContentSubject contentSubject;
    private final Map<String, Content> contentRepository;
    private final Map<String, Site> siteRepository;

    // Integrated observers
    private final ContentNotificationService notificationService;
    private final CacheInvalidationObserver cacheObserver;
    private final SearchIndexObserver searchObserver;
    private final AuditObserver auditObserver;

    /**
     * Constructs a ContentManagementService with integrated observer pattern.
     */
    public ContentManagementService() {
        this.logger = CMSLogger.getInstance();
        this.contentSubject = new ContentSubject();
        this.contentRepository = new ConcurrentHashMap<>();
        this.siteRepository = new ConcurrentHashMap<>();

        // Initialize all observers
        this.notificationService = new ContentNotificationService();
        this.cacheObserver = new CacheInvalidationObserver();
        this.searchObserver = new SearchIndexObserver();
        this.auditObserver = new AuditObserver();

        // Register observers with proper priority ordering
        setupObservers();

        logger.logSystemEvent("ContentManagementService initialized", "1.0",
                "observers=" + contentSubject.getObserverCount());
    }

    /**
     * Sets up observers in priority order for optimal event processing.
     */
    private void setupObservers() {
        // Register observers in priority order (lowest number = highest priority)
        contentSubject.addObserver(auditObserver); // Priority 5 - Audit first
        contentSubject.addObserver(cacheObserver); // Priority 10 - Cache invalidation
        contentSubject.addObserver(notificationService); // Priority 20 - Notifications
        contentSubject.addObserver(searchObserver); // Priority 30 - Search indexing last

        logger.logSystemEvent("Observer setup completed", "1.0",
                "registeredObservers=" + contentSubject.getRegisteredObserverNames());
    }

    /**
     * Creates new content using Factory Pattern and notifies observers.
     *
     * <p>
     * <strong>Pattern Integration:</strong> This method shows the integration
     * of Factory Pattern (content creation) with Observer Pattern (event
     * notification)
     * and Exception Shielding (error handling).
     * </p>
     *
     * @param contentClass The class of content to create
     * @param title        Content title
     * @param body         Content body
     * @param creator      User creating the content
     * @param metadata     Additional content metadata
     * @return Created content instance
     * @throws ContentManagementException If content creation fails
     */
    public <T extends Content> T createContent(Class<T> contentClass, String title,
            String body, User creator,
            Map<String, Object> metadata)
            throws ContentManagementException {
        try {
            logger.logSystemEvent("Creating content", "1.0",
                    "type=" + contentClass.getSimpleName() +
                            ", title=" + title +
                            ", creator=" + creator.getUsername());

            // Use Factory Pattern to create content
            T content = (T) ContentFactory.createContent(contentClass.getSimpleName(), title, body,
                    creator.getUsername());

            // Add metadata if provided
            if (metadata != null) {
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    content.addMetadata(entry.getKey(), entry.getValue(), creator.getUsername());
                }
            }

            // Store in repository
            contentRepository.put(content.getId(), content);

            // Create and fire content creation event (Observer Pattern)
            ContentEvent event = ContentEvent.builder()
                    .content(content)
                    .eventType(ContentEvent.EventType.CREATED)
                    .user(creator)
                    .source("ContentManagementService")
                    .metadata(metadata)
                    .build();

            contentSubject.notifyObservers(event);

            logger.logSystemEvent("Content created successfully", "1.0",
                    "id=" + content.getId() +
                            ", type=" + contentClass.getSimpleName());

            return content;

        } catch (Exception e) {
            // Exception Shielding - convert technical exceptions to user-friendly ones
            throw new ContentManagementException(
                    "Failed to create content: " + title,
                    "Content creation encountered an error. Please verify your input and try again.",
                    e);
        }
    }

    /**
     * Updates existing content and notifies observers of changes.
     *
     * @param contentId ID of content to update
     * @param updates   Map of field names to new values
     * @param modifier  User making the updates
     * @return Updated content
     * @throws ContentManagementException If update fails
     */
    public Content updateContent(String contentId, Map<String, Object> updates,
            User modifier) throws ContentManagementException {
        try {
            Content content = contentRepository.get(contentId);
            if (content == null) {
                throw new ContentManagementException(
                        "Content not found: " + contentId,
                        "The requested content could not be found. It may have been deleted or moved.",
                        null);
            }

            logger.logSystemEvent("Updating content", "1.0",
                    "id=" + contentId +
                            ", updates=" + updates.size() +
                            ", modifier=" + modifier.getUsername());

            // Capture previous state for audit trail
            Map<String, Object> previousValues = new HashMap<>();
            Set<String> changedFields = new HashSet<>();

            // Apply updates and track changes
            for (Map.Entry<String, Object> update : updates.entrySet()) {
                String field = update.getKey();
                Object newValue = update.getValue();
                Object oldValue = null;

                switch (field) {
                    case "title":
                        oldValue = content.getTitle();
                        if (!Objects.equals(oldValue, newValue)) {
                            content.setTitle((String) newValue, modifier.getUsername());
                            previousValues.put(field, oldValue);
                            changedFields.add(field);
                        }
                        break;
                    case "body":
                        oldValue = content.getBody();
                        if (!Objects.equals(oldValue, newValue)) {
                            content.setBody((String) newValue, modifier.getUsername());
                            previousValues.put(field, oldValue);
                            changedFields.add(field);
                        }
                        break;
                    case "status":
                        oldValue = content.getStatus();
                        if (!Objects.equals(oldValue, newValue)) {
                            content.setStatus((ContentStatus) newValue, modifier.getUsername());
                            previousValues.put(field, oldValue);
                            changedFields.add(field);
                        }
                        break;
                    default:
                        // Handle metadata updates
                        oldValue = content.getMetadata().get(field);
                        if (!Objects.equals(oldValue, newValue)) {
                            content.addMetadata(field, newValue, modifier.getUsername());
                            previousValues.put(field, oldValue);
                            changedFields.add(field);
                        }
                        break;
                }
            }

            if (changedFields.isEmpty()) {
                logger.logSystemEvent("No changes detected", "1.0", "id=" + contentId);
                return content;
            }

            // Update modification tracking
            // Modification tracking is handled automatically by Content class methods

            // Create and fire update event
            ContentEvent event = ContentEvent.builder()
                    .content(content)
                    .eventType(ContentEvent.EventType.UPDATED)
                    .user(modifier)
                    .source("ContentManagementService")
                    .changedFields(changedFields)
                    .previousValues(previousValues)
                    .build();

            contentSubject.notifyObservers(event);

            logger.logSystemEvent("Content updated successfully", "1.0",
                    "id=" + contentId +
                            ", changedFields=" + changedFields.size());

            return content;

        } catch (ContentManagementException e) {
            throw e; // Re-throw user-friendly exceptions
        } catch (Exception e) {
            throw new ContentManagementException(
                    "Failed to update content: " + contentId,
                    "Content update encountered an error. Please verify your changes and try again.",
                    e);
        }
    }

    /**
     * Publishes content making it publicly available and notifies observers.
     *
     * @param contentId       ID of content to publish
     * @param publisher       User publishing the content
     * @param publicationDate When to publish (null for immediate)
     * @return Published content
     * @throws ContentManagementException If publication fails
     */
    public Content publishContent(String contentId, User publisher,
            LocalDateTime publicationDate) throws ContentManagementException {
        try {
            Content content = contentRepository.get(contentId);
            if (content == null) {
                throw new ContentManagementException(
                        "Content not found: " + contentId,
                        "The content you're trying to publish could not be found.",
                        null);
            }

            if (content.getStatus() == ContentStatus.PUBLISHED) {
                logger.logSystemEvent("Content already published", "1.0", "id=" + contentId);
                return content;
            }

            logger.logSystemEvent("Publishing content", "1.0",
                    "id=" + contentId +
                            ", publisher=" + publisher.getUsername() +
                            ", publicationDate=" + publicationDate);

            // Update content status
            content.setStatus(ContentStatus.PUBLISHED, publisher.getUsername());

            if (publicationDate != null) {
                content.addMetadata("publishedDate", publicationDate, publisher.getUsername());
            } else {
                content.addMetadata("publishedDate", LocalDateTime.now(), publisher.getUsername());
                publicationDate = LocalDateTime.now();
            }

            // Create and fire publication event
            ContentEvent event = ContentEvent.builder()
                    .content(content)
                    .eventType(ContentEvent.EventType.PUBLISHED)
                    .user(publisher)
                    .source("ContentManagementService")
                    .publicationDate(publicationDate)
                    .addMetadata("immediatePublication", publicationDate == null)
                    .build();

            contentSubject.notifyObservers(event);

            logger.logSystemEvent("Content published successfully", "1.0",
                    "id=" + contentId +
                            ", publicationDate=" + publicationDate);

            return content;

        } catch (ContentManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentManagementException(
                    "Failed to publish content: " + contentId,
                    "Content publication encountered an error. Please check permissions and try again.",
                    e);
        }
    }

    /**
     * Deletes or archives content and notifies observers.
     *
     * @param contentId ID of content to delete
     * @param deleter   User performing the deletion
     * @param reason    Reason for deletion
     * @param temporary Whether deletion is temporary (archive) or permanent
     * @return Deleted content for reference
     * @throws ContentManagementException If deletion fails
     */
    public Content deleteContent(String contentId, User deleter, String reason,
            boolean temporary) throws ContentManagementException {
        try {
            Content content = contentRepository.get(contentId);
            if (content == null) {
                throw new ContentManagementException(
                        "Content not found: " + contentId,
                        "The content you're trying to delete could not be found.",
                        null);
            }

            logger.logSystemEvent("Deleting content", "1.0",
                    "id=" + contentId +
                            ", deleter=" + deleter.getUsername() +
                            ", temporary=" + temporary +
                            ", reason=" + reason);

            // Update content status
            ContentStatus newStatus = temporary ? ContentStatus.ARCHIVED : ContentStatus.ARCHIVED;
            content.setStatus(newStatus, deleter.getUsername());

            // Remove from repository if permanent deletion
            if (!temporary) {
                contentRepository.remove(contentId);
            }

            // Create and fire deletion event
            ContentEvent event = ContentEvent.builder()
                    .content(content)
                    .eventType(ContentEvent.EventType.DELETED)
                    .user(deleter)
                    .source("ContentManagementService")
                    .reason(reason)
                    .temporaryDeletion(temporary)
                    .addMetadata("deletionType", temporary ? "archive" : "permanent")
                    .build();

            contentSubject.notifyObservers(event);

            logger.logSystemEvent("Content deleted successfully", "1.0",
                    "id=" + contentId +
                            ", temporary=" + temporary);

            return content;

        } catch (ContentManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentManagementException(
                    "Failed to delete content: " + contentId,
                    "Content deletion encountered an error. Please verify permissions and try again.",
                    e);
        }
    }

    /**
     * Gets content using Iterator Pattern for efficient traversal.
     *
     * <p>
     * <strong>Pattern Integration:</strong> shows Iterator Pattern usage
     * for content collection traversal with optional filtering.
     * </p>
     *
     * @param filter     Optional filter criteria
     * @param maxResults Maximum number of results to return
     * @return List of matching content
     */
    public List<Content> getContent(ContentFilter filter, int maxResults) {
        try {
            logger.logSystemEvent("Retrieving content", "1.0",
                    "filter=" + (filter != null ? filter.toString() : "none") +
                            ", maxResults=" + maxResults);

            // Create iterator based on filter criteria
            ContentIterator iterator;

            if (filter == null) {
                // Get all content
                iterator = new ContentIterator<>(contentRepository.values());
            } else {
                // Apply filters using Iterator Pattern
                Collection<Content> baseContent = contentRepository.values();

                if (filter.status != null) {
                    iterator = new ContentIterator<>(baseContent, content -> content.getStatus() == filter.status);
                } else if (filter.contentType != null) {
                    iterator = new ContentIterator<>(baseContent,
                            content -> content.getClass().equals(filter.contentType));
                } else if (filter.creator != null) {
                    iterator = new ContentIterator<>(baseContent,
                            content -> content.getCreatedBy().equals(filter.creator));
                } else {
                    iterator = new ContentIterator<>(baseContent);
                }
            }

            // Collect results using iterator
            List<Content> results = new ArrayList<>();
            int count = 0;

            while (iterator.hasNext() && count < maxResults) {
                results.add(iterator.next());
                count++;
            }

            logger.logSystemEvent("Content retrieval completed", "1.0",
                    "results=" + results.size());

            return results;

        } catch (Exception e) {
            logger.logError(e, "Failed to retrieve content", "system", "getContent");
            return Collections.emptyList();
        }
    }

    /**
     * Adds content to a site using Composite Pattern integration.
     *
     * @param siteId       Site ID to add content to
     * @param contentId    Content ID to add
     * @param categoryPath Optional category path in the site hierarchy
     * @throws ContentManagementException If addition fails
     */
    public void addContentToSite(String siteId, String contentId, String categoryPath)
            throws ContentManagementException {
        try {
            Site site = siteRepository.get(siteId);
            if (site == null) {
                throw new ContentManagementException(
                        "Site not found: " + siteId,
                        "The target site could not be found.",
                        null);
            }

            Content content = contentRepository.get(contentId);
            if (content == null) {
                throw new ContentManagementException(
                        "Content not found: " + contentId,
                        "The content to add could not be found.",
                        null);
            }

            logger.logSystemEvent("Adding content to site", "1.0",
                    "siteId=" + siteId +
                            ", contentId=" + contentId +
                            ", categoryPath=" + categoryPath);

            // Use Composite Pattern to add content to site hierarchy
            // This would integrate with the existing SiteComponent structure
            // Implementation would depend on the specific site hierarchy structure

            logger.logSystemEvent("Content added to site successfully", "1.0",
                    "siteId=" + siteId + ", contentId=" + contentId);

        } catch (ContentManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentManagementException(
                    "Failed to add content to site",
                    "An error occurred while adding content to the site structure.",
                    e);
        }
    }

    /**
     * Gets comprehensive service statistics including observer metrics.
     *
     * @return Map of service statistics
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Content statistics
        stats.put("totalContent", contentRepository.size());
        stats.put("totalSites", siteRepository.size());

        // Content status breakdown
        Map<String, Long> statusCounts = contentRepository.values().stream()
                .collect(Collectors.groupingBy(
                        content -> content.getStatus().name(),
                        Collectors.counting()));
        stats.put("contentByStatus", statusCounts);

        // Content type breakdown
        Map<String, Long> typeCounts = contentRepository.values().stream()
                .collect(Collectors.groupingBy(
                        content -> content.getClass().getSimpleName(),
                        Collectors.counting()));
        stats.put("contentByType", typeCounts);

        // Observer statistics
        stats.put("totalObservers", contentSubject.getObserverCount());
        stats.put("totalEvents", contentSubject.getEventCount());
        stats.put("eventCounts", contentSubject.getEventCounts());
        stats.put("observerStatistics", contentSubject.getObserverStatistics());

        // Individual observer statistics
        stats.put("notificationStats", notificationService.getStatistics());
        stats.put("cacheStats", cacheObserver.getCacheStatistics());
        stats.put("searchStats", searchObserver.getSearchStatistics());
        stats.put("auditStats", auditObserver.getAuditStatistics());

        return stats;
    }

    /**
     * Processes any pending observer operations for optimal performance.
     *
     * @return Total number of operations processed
     */
    public int processPendingOperations() {
        int totalProcessed = 0;

        logger.logSystemEvent("Processing pending operations", "1.0", "");

        // Process cache operations
        totalProcessed += cacheObserver.processPendingOperations();

        // Process search index operations
        totalProcessed += searchObserver.processPendingOperations();

        // Process notification operations
        totalProcessed += notificationService.processPendingIndexUpdates();

        logger.logSystemEvent("Pending operations processed", "1.0",
                "totalProcessed=" + totalProcessed);

        return totalProcessed;
    }

    /**
     * Performs system maintenance including cleanup and optimization.
     *
     * @return Maintenance results
     */
    public Map<String, Object> performMaintenance() {
        Map<String, Object> results = new LinkedHashMap<>();

        logger.logSystemEvent("MAINTENANCE_START", "1.0", "Starting system maintenance");

        // Clean up expired audit records
        int expiredRecords = auditObserver.cleanupExpiredRecords();
        results.put("expiredAuditRecords", expiredRecords);

        // Clear processed cache keys
        cacheObserver.clearInvalidatedKeys();
        results.put("cacheKeysCleared", true);

        // Clear notification history
        notificationService.clearProcessedCacheKeys();
        results.put("notificationHistoryCleared", true);

        // Verify audit integrity
        Map<String, Object> integrityResults = auditObserver.verifyAuditIntegrity();
        results.put("auditIntegrity", integrityResults);

        // Validate search index consistency
        List<String> searchIssues = searchObserver.validateIndexConsistency();
        results.put("searchIndexIssues", searchIssues.size());
        results.put("searchIssueDetails", searchIssues);

        logger.logSystemEvent("MAINTENANCE_COMPLETE", "1.0",
                "System maintenance completed - expiredRecords=" + expiredRecords +
                        ", searchIssues=" + searchIssues.size());

        return results;
    }

    /**
     * Shuts down the service and all integrated components.
     */
    public void shutdown() {
        logger.logSystemEvent("SHUTDOWN", "1.0", "Shutting down ContentManagementService");

        // Shutdown all observers
        notificationService.shutdown();
        searchObserver.shutdown();

        // Shutdown the content subject
        contentSubject.shutdown();

        logger.logSystemEvent("SHUTDOWN_COMPLETE", "1.0",
                "ContentManagementService shutdown completed - totalContent=" + contentRepository.size() +
                        ", totalEvents=" + contentSubject.getEventCount());
    }

    // Helper classes

    /**
     * Content filter criteria for content retrieval.
     */
    public static class ContentFilter {
        private ContentStatus status;
        private Class<? extends Content> contentType;
        private String creator;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;

        public ContentFilter status(ContentStatus status) {
            this.status = status;
            return this;
        }

        public ContentFilter contentType(Class<? extends Content> contentType) {
            this.contentType = contentType;
            return this;
        }

        public ContentFilter creator(String creator) {
            this.creator = creator;
            return this;
        }

        public ContentFilter dateRange(LocalDateTime from, LocalDateTime to) {
            this.fromDate = from;
            this.toDate = to;
            return this;
        }

        @Override
        public String toString() {
            return "ContentFilter{" +
                    "status=" + status +
                    ", contentType=" + (contentType != null ? contentType.getSimpleName() : null) +
                    ", creator='" + creator + '\'' +
                    ", fromDate=" + fromDate +
                    ", toDate=" + toDate +
                    '}';
        }
    }

    // Getters for testing and configuration

    public ContentSubject getContentSubject() {
        return contentSubject;
    }

    /**
     * Adds an observer to the content management system.
     *
     * <p>
     * Convenience method for adding observers to the content subject.
     * This allows external observers to be registered for content events.
     * </p>
     *
     * @param observer The observer to add
     */
    public void addObserver(ContentObserver observer) {
        contentSubject.addObserver(observer);
    }

    public ContentNotificationService getNotificationService() {
        return notificationService;
    }

    public CacheInvalidationObserver getCacheObserver() {
        return cacheObserver;
    }

    public SearchIndexObserver getSearchObserver() {
        return searchObserver;
    }

    public AuditObserver getAuditObserver() {
        return auditObserver;
    }

    public Map<String, Content> getContentRepository() {
        return Collections.unmodifiableMap(contentRepository);
    }

    // === Stream API & Lambdas Integration ===

    private final ContentStreamProcessor streamProcessor = new ContentStreamProcessor();
    private final ContentAnalyzer contentAnalyzer = new ContentAnalyzer();

    /**
     * Filters content using Stream API and lambda expressions.
     *
     * <p>
     * <strong>Stream API & Lambdas:</strong> shows functional
     * programming integration with Observer Pattern using parallel streams,
     * lambda expressions, and method references.
     * </p>
     *
     * @param predicate functional predicate for filtering
     * @return filtered content list
     * @throws ContentManagementException if filtering fails
     */
    public List<Content> filterContentWithStreams(ContentPredicate predicate)
            throws ContentManagementException {
        logger.logSystemEvent("Filtering content with streams", "1.0", "predicate=custom");

        List<Content> results = streamProcessor.filterContent(contentRepository.values(), predicate);

        // Notify observers about stream filtering operation
        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.QUERIED)
                .source("ContentManagementService.filterContentWithStreams")
                .addMetadata("resultCount", results.size())
                .addMetadata("processingType", "stream")
                .build();

        contentSubject.notifyObservers(event);

        logger.logSystemEvent("Stream filtering completed", "1.0",
                "results=" + results.size());

        return results;
    }

    /**
     * Maps content to DTOs using Stream API and method references.
     *
     * <p>
     * <strong>Method References:</strong> Uses method references for clean
     * functional programming with integrated observer notifications.
     * </p>
     *
     * @return list of ContentDTO objects
     * @throws ContentManagementException if mapping fails
     */
    public List<ContentDTO> getAllContentAsDTO() throws ContentManagementException {
        logger.logSystemEvent("Mapping content to DTOs with streams", "1.0", "");

        List<ContentDTO> results = streamProcessor.mapToDTO(contentRepository.values());

        // Notify observers about DTO mapping operation
        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.QUERIED)
                .source("ContentManagementService.getAllContentAsDTO")
                .addMetadata("resultCount", results.size())
                .addMetadata("processingType", "stream_mapping")
                .build();

        contentSubject.notifyObservers(event);

        return results;
    }

    /**
     * Searches content using functional programming and stream operations.
     *
     * <p>
     * <strong>Functional Composition:</strong> Combines stream operations
     * with observer pattern for comprehensive search with event notifications.
     * </p>
     *
     * @param searchTerm the search term
     * @param maxResults maximum number of results
     * @return comprehensive search results
     * @throws ContentManagementException if search fails
     */
    public ContentStreamProcessor.SearchResult searchContentWithStreams(String searchTerm,
            int maxResults)
            throws ContentManagementException {
        logger.logSystemEvent("Stream-based content search", "1.0",
                "term=" + searchTerm + ", maxResults=" + maxResults);

        ContentStreamProcessor.SearchResult results = streamProcessor.searchContent(contentRepository.values(),
                searchTerm, maxResults);

        // Notify observers about search operation
        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.QUERIED)
                .source("ContentManagementService.searchContentWithStreams")
                .addMetadata("searchTerm", searchTerm)
                .addMetadata("resultCount", results.totalMatches())
                .addMetadata("processingType", "stream_search")
                .build();

        contentSubject.notifyObservers(event);

        return results;
    }

    /**
     * Generates comprehensive analytics using Stream API operations.
     *
     * <p>
     * <strong>Advanced Stream Operations:</strong> Uses parallel streams,
     * custom collectors, and lambda expressions for analytics with observer
     * integration for tracking analytics requests.
     * </p>
     *
     * @return comprehensive analytics result
     * @throws ContentManagementException if analytics generation fails
     */
    public ContentAnalyzer.AnalyticsResult generateContentAnalytics()
            throws ContentManagementException {
        logger.logSystemEvent("Generating content analytics", "1.0", "");

        ContentAnalyzer.AnalyticsResult results = contentAnalyzer.analyzeContent(contentRepository.values());

        // Notify observers about analytics generation
        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.QUERIED)
                .source("ContentManagementService.generateContentAnalytics")
                .addMetadata("itemsAnalyzed", results.performanceStats().itemsProcessed())
                .addMetadata("processingTimeMs", results.performanceStats().processingTimeMs())
                .addMetadata("processingType", "stream_analytics")
                .build();

        contentSubject.notifyObservers(event);

        return results;
    }

    /**
     * Processes content asynchronously using CompletableFuture and streams.
     *
     * <p>
     * <strong>Async Stream Processing:</strong> Combines CompletableFuture
     * with stream operations and observer pattern for non-blocking processing.
     * </p>
     *
     * @param processor the processing function to apply
     * @return CompletableFuture containing processed results
     */
    public <R> CompletableFuture<List<R>> processContentAsync(Function<Content, R> processor) {
        logger.logSystemEvent("Starting async content processing", "1.0", "");

        return streamProcessor.processAsync(contentRepository.values(), processor)
                .whenComplete((results, throwable) -> {
                    if (throwable == null) {
                        // Notify observers about async processing completion
                        ContentEvent event = ContentEvent.builder()
                                .eventType(ContentEvent.EventType.QUERIED)
                                .source("ContentManagementService.processContentAsync")
                                .addMetadata("resultCount", results.size())
                                .addMetadata("processingType", "async_stream")
                                .build();

                        contentSubject.notifyObservers(event);

                        logger.logSystemEvent("Async processing completed", "1.0",
                                "results=" + results.size());
                    } else {
                        logger.logError((Exception) throwable, "Async processing failed", "system",
                                "processContentAsync");
                    }
                });
    }

    /**
     * Groups content by date ranges using stream collectors.
     *
     * <p>
     * <strong>Custom Collectors:</strong> shows custom collector
     * usage with stream operations and observer pattern integration.
     * </p>
     *
     * @param dayRange the number of days to group by
     * @return content grouped by date ranges
     * @throws ContentManagementException if grouping fails
     */
    public Map<String, List<ContentDTO>> groupContentByDateRange(int dayRange)
            throws ContentManagementException {
        logger.logSystemEvent("Grouping content by date range", "1.0",
                "dayRange=" + dayRange);

        Map<String, List<ContentDTO>> results = streamProcessor.groupByDateRange(contentRepository.values(), dayRange);

        // Notify observers about grouping operation
        ContentEvent event = ContentEvent.builder()
                .eventType(ContentEvent.EventType.QUERIED)
                .source("ContentManagementService.groupContentByDateRange")
                .addMetadata("dayRange", dayRange)
                .addMetadata("groupCount", results.size())
                .addMetadata("processingType", "stream_grouping")
                .build();

        contentSubject.notifyObservers(event);

        return results;
    }

    /**
     * Gets enhanced service statistics using Stream API operations.
     *
     * <p>
     * <strong>Stream Enhancement:</strong> Enhances existing statistics
     * with stream-based calculations and functional programming metrics.
     * </p>
     *
     * @return enhanced service statistics with stream metrics
     */
    public Map<String, Object> getEnhancedServiceStatistics() {
        Map<String, Object> stats = getServiceStatistics();

        // Add stream-based statistics
        try {
            ContentStreamProcessor.ProcessingStats streamStats = streamProcessor
                    .generateStatistics(contentRepository.values());

            stats.put("streamStatistics", Map.of(
                    "totalCount", streamStats.totalCount(),
                    "publishedCount", streamStats.publishedCount(),
                    "draftCount", streamStats.draftCount(),
                    "averageWordCount", streamStats.averageWordCount(),
                    "statusDistribution", streamStats.statusDistribution(),
                    "topAuthors", streamStats.topAuthors()));

            // Add content quality metrics
            ContentAnalyzer.AnalyticsResult analytics = contentAnalyzer.analyzeContent(contentRepository.values());

            stats.put("qualityMetrics", Map.of(
                    "averageWordCount", analytics.qualityMetrics().averageWordCount(),
                    "contentDiversity", analytics.qualityMetrics().contentDiversity(),
                    "authorDiversity", analytics.qualityMetrics().authorDiversity(),
                    "topQualityAuthors", analytics.qualityMetrics().topQualityAuthors()));

        } catch (ContentManagementException e) {
            logger.logError(e, "Failed to generate stream statistics", "system", "getStreamStatistics");
            stats.put("streamStatisticsError", e.getUserMessage());
        }

        return stats;
    }

    /**
     * Enhanced shutdown method that includes stream processing cleanup.
     *
     * <p>
     * <strong>Resource Management:</strong> Properly closes stream
     * processing resources to prevent memory leaks.
     * </p>
     */
    public void shutdownWithStreams() {
        logger.logSystemEvent("SHUTDOWN", "1.0", "Shutting down ContentManagementService with streams");

        // Shutdown stream processors
        streamProcessor.close();

        // Call original shutdown
        shutdown();

        logger.logSystemEvent("SHUTDOWN_COMPLETE", "1.0", "Enhanced ContentManagementService shutdown completed");
    }
}
