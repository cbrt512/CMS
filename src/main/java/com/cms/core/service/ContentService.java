package com.cms.core.service;

import com.cms.core.model.*;
import com.cms.core.repository.ContentRepository;
import com.cms.patterns.factory.ContentFactory;
import com.cms.patterns.composite.SiteComponent;
import com.cms.patterns.composite.ContentItem;
import com.cms.patterns.iterator.*;
import com.cms.patterns.observer.*;
import com.cms.patterns.strategy.*;
import com.cms.patterns.shield.CMSException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.security.InputSanitizer;
import com.cms.security.SecurityValidator;
import com.cms.util.LoggerUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business service for content management operations in the CMS.
 *
 * <p>
 * This service orchestrates all content-related business logic and serves as
 * the
 * primary integration point for multiple design patterns. It coordinates
 * content
 * lifecycle operations from creation through publication, implementing
 * comprehensive
 * business workflows while maintaining security and data integrity.
 * </p>
 *
 * <p>
 * <strong>Design Pattern Integration:</strong>
 * - Factory Pattern: Uses ContentFactory for creating different content types
 * - Composite Pattern: Manages content within site hierarchy structures
 * - Iterator Pattern: Provides content traversal and batch operations
 * - Observer Pattern: Publishes content events for notifications and caching
 * - Strategy Pattern: Delegates publishing decisions to pluggable strategies
 * - Exception Shielding: Provides user-friendly error handling
 * </p>
 *
 * <p>
 * <strong>Technologies Implemented:</strong>
 * - Business Logic Orchestration (Service Layer Architecture)
 * - Pattern Integration (provides meaningful usage of all implemented patterns)
 * - Security Implementation (input validation, authorization checks)
 * - Collections Framework (comprehensive usage in business operations)
 * - Exception Handling (shielded error propagation)
 * </p>
 *
 * <p>
 * <strong>Security Features:</strong>
 * - Input sanitization for all content operations
 * - Authorization validation for content access
 * - Audit logging for content changes
 * - Exception shielding to prevent information disclosure
 * </p>
 *
 * @see com.cms.patterns.factory.ContentFactory
 * @see com.cms.patterns.composite.SiteComponent
 * @see com.cms.patterns.observer.ContentSubject
 * @see com.cms.patterns.strategy.PublishingService
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentService extends ContentSubject {

    private final ContentRepository contentRepository;
    private final PublishingService publishingService;
    private final AuthorizationService authorizationService;

    // Collections for managing content operations
    private final Map<String, Content> contentCache;
    private final Queue<ContentOperation> pendingOperations;
    private final Set<String> lockedContentIds;

    /**
     * Constructs a new ContentService with required dependencies.
     *
     * @param contentRepository    Repository for content persistence
     * @param publishingService    Service for publication strategies
     * @param authorizationService Service for access control
     * @throws IllegalArgumentException if any parameter is null
     */
    public ContentService(ContentRepository contentRepository,
            PublishingService publishingService,
            AuthorizationService authorizationService) {
        if (contentRepository == null || publishingService == null || authorizationService == null) {
            throw new IllegalArgumentException("All service dependencies must be non-null");
        }

        this.contentRepository = contentRepository;
        this.publishingService = publishingService;
        this.authorizationService = authorizationService;

        // Initialize collections for service operations
        this.contentCache = new HashMap<>();
        this.pendingOperations = new LinkedList<>();
        this.lockedContentIds = new HashSet<>();

        LoggerUtil.logInfo("ContentService", "ContentService initialized successfully");
    }

    /**
     * Creates new content using the Factory pattern with comprehensive validation.
     *
     * <p>
     * <strong>Factory Pattern Integration:</strong> Uses ContentFactory to create
     * appropriate content type instances based on the specified content type.
     * </p>
     *
     * @param contentType The type of content to create (article, page, image,
     *                    video)
     * @param properties  Content properties including title, body, author, etc.
     * @param author      The user creating the content
     * @return The created content instance
     * @throws ContentManagementException if content creation fails
     * @throws SecurityException          if user lacks creation permissions
     */
    public Content createContent(String contentType, Map<String, Object> properties, User author)
            throws ContentManagementException {

        LoggerUtil.logInfo("ContentService",
                String.format("Creating content: type=%s, author=%s", contentType, author.getUsername()));

        try {
            // Authorization check
            authorizationService.validateUserPermission(author, "CREATE_CONTENT", null);

            // Input validation and sanitization
            validateContentProperties(properties);
            Map<String, Object> sanitizedProperties = sanitizeContentProperties(properties);

            // Use Factory pattern to create content
            Content content = ContentFactory.createContent(contentType, sanitizedProperties, author.getUsername());

            // Persist content
            Content savedContent = contentRepository.save(content);

            // Update cache
            contentCache.put(savedContent.getId(), savedContent);

            // Notify observers using Observer pattern
            ContentEvent event = new ContentEvent.Builder()
                    .content(savedContent)
                    .eventType(ContentEvent.EventType.CREATED)
                    .user(author)
                    .timestamp(LocalDateTime.now())
                    .build();

            notifyObservers(event);

            LoggerUtil.logInfo("ContentService",
                    "Content created successfully: " + savedContent.getId());

            return savedContent;

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content creation failed", e);
            throw ExceptionShielder.shield(e, "Failed to create content. Please try again.",
                    "contentType=" + contentType + ", author=" + author.getUsername());
        }
    }

    /**
     * Retrieves content by ID with caching and security validation.
     *
     * @param contentId      The ID of the content to retrieve
     * @param requestingUser The user requesting the content
     * @return The requested content
     * @throws ContentManagementException if content not found or access denied
     */
    public Content getContent(String contentId, User requestingUser) throws ContentManagementException {
        LoggerUtil.logDebug("ContentService",
                "Retrieving content: " + contentId + " for user: " + requestingUser.getUsername());

        try {
            // Input validation
            if (contentId == null || contentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Content ID cannot be null or empty");
            }

            // Check cache first
            Content cachedContent = contentCache.get(contentId);
            if (cachedContent != null) {
                // Still need authorization check
                authorizationService.validateContentAccess(requestingUser, cachedContent, "READ");
                LoggerUtil.logDebug("ContentService", "Content retrieved from cache: " + contentId);
                return cachedContent;
            }

            // Retrieve from repository
            Optional<Content> contentOpt = contentRepository.findById(contentId);
            if (!contentOpt.isPresent()) {
                LoggerUtil.logWarn("ContentService", "Content not found: " + contentId);
                throw ContentManagementException.contentNotFound(contentId);
            }

            Content content = contentOpt.get();

            // Authorization check
            authorizationService.validateContentAccess(requestingUser, content, "READ");

            // Update cache
            contentCache.put(contentId, content);

            return content;

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content retrieval failed for ID: " + contentId, e);
            throw ExceptionShielder.shield(e, "Unable to retrieve content", "contentId=" + contentId);
        }
    }

    /**
     * Updates existing content with validation and change tracking.
     *
     * @param contentId    The ID of the content to update
     * @param updates      Map of field updates
     * @param updatingUser The user performing the update
     * @return The updated content
     * @throws ContentManagementException if update fails or access denied
     */
    public Content updateContent(String contentId, Map<String, Object> updates, User updatingUser)
            throws ContentManagementException {

        LoggerUtil.logInfo("ContentService",
                String.format("Updating content: %s by user: %s", contentId, updatingUser.getUsername()));

        try {
            // Retrieve existing content
            Content existingContent = getContent(contentId, updatingUser);

            // Authorization check for modification
            authorizationService.validateContentAccess(updatingUser, existingContent, "UPDATE");

            // Check if content is locked
            if (lockedContentIds.contains(contentId)) {
                throw new ContentManagementException(
                        "Content is currently being modified",
                        "Content " + contentId + " is locked for editing",
                        contentId,
                        "update");
            }

            // Lock content during update
            lockedContentIds.add(contentId);

            try {
                // Validate and sanitize updates
                validateContentProperties(updates);
                Map<String, Object> sanitizedUpdates = sanitizeContentProperties(updates);

                // Apply updates to content
                Content updatedContent = applyContentUpdates(existingContent, sanitizedUpdates, updatingUser);
                // Modification info is updated automatically by content setter methods

                // Persist changes
                Content savedContent = contentRepository.save(updatedContent);

                // Update cache
                contentCache.put(contentId, savedContent);

                // Notify observers
                ContentEvent event = new ContentEvent.Builder()
                        .content(savedContent)
                        .eventType(ContentEvent.EventType.UPDATED)
                        .user(updatingUser)
                        .timestamp(LocalDateTime.now())
                        .build();

                notifyObservers(event);

                LoggerUtil.logInfo("ContentService", "Content updated successfully: " + contentId);

                return savedContent;

            } finally {
                // Always unlock content
                lockedContentIds.remove(contentId);
            }

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content update failed for ID: " + contentId, e);
            throw ExceptionShielder.shield(e, "Failed to update content", "contentId=" + contentId);
        }
    }

    /**
     * Publishes content using the Strategy pattern for publication workflows.
     *
     * <p>
     * <strong>Strategy Pattern Integration:</strong> Delegates publication logic
     * to the PublishingService which applies appropriate publishing strategies.
     * </p>
     *
     * @param contentId         The ID of the content to publish
     * @param publishingUser    The user requesting publication
     * @param publishingContext Additional context for publication strategy
     * @return The published content
     * @throws ContentManagementException if publication fails
     */
    public Content publishContent(String contentId, User publishingUser,
            PublishingContext publishingContext) throws ContentManagementException {

        LoggerUtil.logInfo("ContentService",
                String.format("Publishing content: %s by user: %s", contentId, publishingUser.getUsername()));

        try {
            // Retrieve content
            Content content = getContent(contentId, publishingUser);

            // Authorization check for publication
            authorizationService.validateContentAccess(publishingUser, content, "PUBLISH");

            // Validate content is ready for publication
            validateContentForPublication(content);

            // Use Strategy pattern for publication
            publishingService.publishContent(content, publishingContext);

            // Update content status
            content.setStatus(ContentStatus.PUBLISHED, publishingUser.getUsername());

            // Persist changes
            Content publishedContent = contentRepository.save(content);

            // Update cache
            contentCache.put(contentId, publishedContent);

            // Notify observers
            ContentEvent event = new ContentEvent.Builder()
                    .content(publishedContent)
                    .eventType(ContentEvent.EventType.PUBLISHED)
                    .user(publishingUser)
                    .timestamp(LocalDateTime.now())
                    .build();

            notifyObservers(event);

            LoggerUtil.logInfo("ContentService", "Content published successfully: " + contentId);

            return publishedContent;

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content publication failed for ID: " + contentId, e);
            throw ExceptionShielder.shield(e, "Failed to publish content", "contentId=" + contentId);
        }
    }

    /**
     * Deletes content with proper authorization and cleanup.
     *
     * @param contentId    The ID of the content to delete
     * @param deletingUser The user requesting deletion
     * @throws ContentManagementException if deletion fails or access denied
     */
    public void deleteContent(String contentId, User deletingUser) throws ContentManagementException {
        LoggerUtil.logInfo("ContentService",
                String.format("Deleting content: %s by user: %s", contentId, deletingUser.getUsername()));

        try {
            // Retrieve content
            Content content = getContent(contentId, deletingUser);

            // Authorization check for deletion
            authorizationService.validateContentAccess(deletingUser, content, "DELETE");

            // Check if content can be deleted (e.g., not referenced elsewhere)
            validateContentForDeletion(content);

            // Remove from repository
            contentRepository.deleteById(contentId);

            // Remove from cache
            contentCache.remove(contentId);

            // Remove any locks
            lockedContentIds.remove(contentId);

            // Notify observers
            ContentEvent event = new ContentEvent.Builder()
                    .content(content)
                    .eventType(ContentEvent.EventType.DELETED)
                    .user(deletingUser)
                    .timestamp(LocalDateTime.now())
                    .build();

            notifyObservers(event);

            LoggerUtil.logInfo("ContentService", "Content deleted successfully: " + contentId);

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content deletion failed for ID: " + contentId, e);
            throw ExceptionShielder.shield(e, "Failed to delete content", "contentId=" + contentId);
        }
    }

    /**
     * Searches content using the Iterator pattern for efficient traversal.
     *
     * <p>
     * <strong>Iterator Pattern Integration:</strong> Uses ContentIterator to
     * traverse and filter content collections efficiently.
     * </p>
     *
     * @param searchQuery The search query
     * @param searchUser  The user performing the search
     * @return List of matching content items
     * @throws ContentManagementException if search fails
     */
    public List<Content> searchContent(String searchQuery, User searchUser) throws ContentManagementException {
        LoggerUtil.logInfo("ContentService",
                String.format("Searching content: query='%s' by user: %s", searchQuery, searchUser.getUsername()));

        try {
            // Sanitize search query
            String sanitizedQuery = InputSanitizer.sanitizeSearchQuery(searchQuery);

            if (sanitizedQuery.isEmpty()) {
                LoggerUtil.logWarn("ContentService", "Empty or invalid search query");
                return new ArrayList<>();
            }

            // Get all content with authorization filtering
            List<Content> allContent = contentRepository.findAll();

            // Use Iterator pattern for efficient filtering
            ContentIterator iterator = new ContentIterator(allContent);
            List<Content> searchResults = new ArrayList<>();

            while (iterator.hasNext()) {
                Content content = iterator.next();

                // Check user authorization for this content
                try {
                    authorizationService.validateContentAccess(searchUser, content, "READ");

                    // Check if content matches search query
                    if (contentMatchesQuery(content, sanitizedQuery)) {
                        searchResults.add(content);
                    }
                } catch (SecurityException e) {
                    // Skip content user cannot access
                    continue;
                }
            }

            // Sort results by relevance (simplified scoring)
            searchResults.sort((c1, c2) -> Integer.compare(calculateRelevanceScore(c2, sanitizedQuery),
                    calculateRelevanceScore(c1, sanitizedQuery)));

            LoggerUtil.logInfo("ContentService",
                    "Search completed: found " + searchResults.size() + " results");

            return searchResults;

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Content search failed", e);
            throw ExceptionShielder.shield(e, "Search operation failed", "query=" + searchQuery);
        }
    }

    /**
     * Gets content within a site hierarchy using the Composite pattern.
     *
     * <p>
     * <strong>Composite Pattern Integration:</strong> Traverses site component
     * hierarchy to retrieve content organized within the site structure.
     * </p>
     *
     * @param siteComponent  The site component to search within
     * @param requestingUser The user requesting the content
     * @return List of content within the specified site component
     * @throws ContentManagementException if operation fails
     */
    public List<Content> getContentInHierarchy(SiteComponent siteComponent, User requestingUser)
            throws ContentManagementException {

        LoggerUtil.logDebug("ContentService", "Getting content in hierarchy for user: " + requestingUser.getUsername());

        try {
            List<Content> hierarchyContent = new ArrayList<>();

            // Use SiteStructureIterator for hierarchy traversal
            SiteStructureIterator iterator = new SiteStructureIterator(siteComponent);

            while (iterator.hasNext()) {
                SiteComponent component = iterator.next();

                // If component contains content, add it with authorization check
                if (component instanceof ContentItem) {
                    ContentItem contentItem = (ContentItem) component;
                    Content content = contentItem.getContent();

                    try {
                        authorizationService.validateContentAccess(requestingUser, content, "READ");
                        hierarchyContent.add(content);
                    } catch (SecurityException e) {
                        // Skip unauthorized content
                        continue;
                    }
                }
            }

            LoggerUtil.logDebug("ContentService",
                    "Retrieved " + hierarchyContent.size() + " content items from hierarchy");

            return hierarchyContent;

        } catch (Exception e) {
            LoggerUtil.logError("ContentService", "Hierarchy content retrieval failed", e);
            throw ExceptionShielder.shield(e, "Failed to retrieve content from hierarchy", null);
        }
    }

    /**
     * Validates content properties for creation/update operations.
     */
    private void validateContentProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("Content properties cannot be null or empty");
        }

        // Validate required properties
        if (!properties.containsKey("title") || properties.get("title") == null) {
            throw new IllegalArgumentException("Content title is required");
        }

        // Additional validation as needed
        Object title = properties.get("title");
        if (title instanceof String) {
            String titleStr = (String) title;
            InputSanitizer.validateInput(titleStr, "title", 200);
        }
    }

    /**
     * Sanitizes content properties using InputSanitizer.
     */
    private Map<String, Object> sanitizeContentProperties(Map<String, Object> properties) {
        Map<String, Object> sanitized = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;
                switch (key) {
                    case "title":
                        sanitized.put(key, InputSanitizer.sanitizeTitle(stringValue));
                        break;
                    case "body":
                    case "content":
                        sanitized.put(key, InputSanitizer.sanitizeContent(stringValue));
                        break;
                    default:
                        sanitized.put(key, InputSanitizer.sanitizeText(stringValue, 1000));
                        break;
                }
            } else {
                sanitized.put(key, value);
            }
        }

        return sanitized;
    }

    /**
     * Applies updates to existing content instance.
     */
    private Content applyContentUpdates(Content existing, Map<String, Object> updates, User updatingUser) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "title":
                    if (value instanceof String) {
                        existing.setTitle((String) value, updatingUser.getUsername());
                    }
                    break;
                case "body":
                case "content":
                    if (value instanceof String) {
                        existing.setBody((String) value, updatingUser.getUsername());
                    }
                    break;
                // Add more update fields as needed
            }
        }

        return existing;
    }

    /**
     * Validates content is ready for publication.
     */
    private void validateContentForPublication(Content content) throws ContentManagementException {
        if (content.getTitle() == null || content.getTitle().trim().isEmpty()) {
            throw ContentManagementException.validationError(content.getId(),
                    "Content must have a title before publication");
        }

        if (content.getBody() == null || content.getBody().trim().isEmpty()) {
            throw ContentManagementException.validationError(content.getId(),
                    "Content must have body text before publication");
        }
    }

    /**
     * Validates content can be deleted.
     */
    private void validateContentForDeletion(Content content) throws ContentManagementException {
        // Check if content is currently published and needs special handling
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            LoggerUtil.logWarn("ContentService",
                    "Deleting published content: " + content.getId());
        }

        // Additional deletion validation can be added here
    }

    /**
     * Checks if content matches search query.
     */
    private boolean contentMatchesQuery(Content content, String query) {
        String lowerQuery = query.toLowerCase();

        return (content.getTitle() != null && content.getTitle().toLowerCase().contains(lowerQuery)) ||
                (content.getBody() != null && content.getBody().toLowerCase().contains(lowerQuery));
    }

    /**
     * Calculates relevance score for search results.
     */
    private int calculateRelevanceScore(Content content, String query) {
        int score = 0;
        String lowerQuery = query.toLowerCase();

        if (content.getTitle() != null && content.getTitle().toLowerCase().contains(lowerQuery)) {
            score += 10; // Title matches are more relevant
        }

        if (content.getBody() != null && content.getBody().toLowerCase().contains(lowerQuery)) {
            score += 5; // Body matches are less relevant
        }

        return score;
    }

    /**
     * Inner class for content operation tracking.
     */
    private static class ContentOperation {
        private final String operationType;
        private final String contentId;
        private final String userId;
        private final long timestamp;

        public ContentOperation(String operationType, String contentId, String userId) {
            this.operationType = operationType;
            this.contentId = contentId;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters for the fields
        public String getOperationType() {
            return operationType;
        }

        public String getContentId() {
            return contentId;
        }

        public String getUserId() {
            return userId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
