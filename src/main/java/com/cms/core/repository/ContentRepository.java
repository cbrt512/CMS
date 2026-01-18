package com.cms.core.repository;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.User;
import com.cms.util.LoggerUtil;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository implementation for Content entity management.
 *
 * <p>
 * This class provides concrete implementation of the Repository pattern for
 * Content entities,
 * demonstrating Collections Framework usage and Generics implementation.
 * It uses in-memory storage with thread-safe collections for demonstration
 * purposes.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Repository Pattern - Encapsulates data
 * access logic
 * and provides a clean interface for content management operations while
 * maintaining
 * separation between business logic and data storage concerns.
 * </p>
 *
 * <p>
 * <strong>Generics Implementation:</strong> Implements Repository&lt;Content,
 * String&gt;
 * providing type-safe operations without casting needs.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Uses comprehensive set of:
 * - ConcurrentHashMap for thread-safe content storage
 * - List collections for query results
 * - Optional for safe null handling
 * - Stream API for filtering operations
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Provides Collections Framework and
 * Generics implementation through type-safe repository operations.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentRepository implements Repository<Content, String> {

    /**
     * Thread-safe storage for content entities.
     * Using ConcurrentHashMap for concurrent access support.
     */
    private final Map<String, Content> contentStorage = new ConcurrentHashMap<>();

    /**
     * Index for author-based lookups.
     */
    private final Map<String, List<String>> authorIndex = new ConcurrentHashMap<>();

    /**
     * Index for status-based lookups.
     */
    private final Map<ContentStatus, List<String>> statusIndex = new ConcurrentHashMap<>();

    /**
     * Constructs a new ContentRepository with empty storage.
     */
    public ContentRepository() {
        // Initialize status index with all possible statuses
        for (ContentStatus status : ContentStatus.values()) {
            statusIndex.put(status, new ArrayList<>());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Content save(Content entity) throws RepositoryException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        try {
            // Store the content
            contentStorage.put(entity.getId(), entity);

            // Update indexes
            updateAuthorIndex(entity);
            updateStatusIndex(entity);

            return entity;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to save content: " + e.getMessage(),
                    "Unable to save content. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Content> findById(String id) throws RepositoryException {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(contentStorage.get(id));
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to find content by ID: " + e.getMessage(),
                    "Unable to retrieve content. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Content> findAll() throws RepositoryException {
        try {
            return new ArrayList<>(contentStorage.values());
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to retrieve all content: " + e.getMessage(),
                    "Unable to retrieve content list. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(String id) throws RepositoryException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            Content removed = contentStorage.remove(id);
            if (removed != null) {
                removeFromIndexes(removed);
            }
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to delete content: " + e.getMessage(),
                    "Unable to delete content. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Content entity) throws RepositoryException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        deleteById(entity.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(String id) throws RepositoryException {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            return contentStorage.containsKey(id);
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to check content existence: " + e.getMessage(),
                    "Unable to verify content existence. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() throws RepositoryException {
        try {
            return contentStorage.size();
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to count content: " + e.getMessage(),
                    "Unable to count content. Please try again.",
                    e);
        }
    }

    /**
     * Finds content by author.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses List for query results
     * and stream operations for filtering.
     * </p>
     *
     * @param author The author to search for
     * @return List of content by the specified author
     * @throws RepositoryException if the operation fails
     */
    public List<Content> findByAuthor(User author) throws RepositoryException {
        if (author == null) {
            return new ArrayList<>();
        }

        return findByAuthor(author.getUsername());
    }

    /**
     * Finds content by author username.
     *
     * @param authorUsername The author username to search for
     * @return List of content by the specified author
     * @throws RepositoryException if the operation fails
     */
    public List<Content> findByAuthor(String authorUsername) throws RepositoryException {
        if (authorUsername == null || authorUsername.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<String> contentIds = authorIndex.get(authorUsername);
            if (contentIds == null) {
                return new ArrayList<>();
            }

            return contentIds.stream()
                    .map(contentStorage::get)
                    .filter(content -> content != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to find content by author: " + e.getMessage(),
                    "Unable to retrieve content by author. Please try again.",
                    e);
        }
    }

    /**
     * Finds content by status.
     *
     * @param status The content status to search for
     * @return List of content with the specified status
     * @throws RepositoryException if the operation fails
     */
    public List<Content> findByStatus(ContentStatus status) throws RepositoryException {
        if (status == null) {
            return new ArrayList<>();
        }

        try {
            List<String> contentIds = statusIndex.get(status);
            if (contentIds == null) {
                return new ArrayList<>();
            }

            return contentIds.stream()
                    .map(contentStorage::get)
                    .filter(content -> content != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to find content by status: " + e.getMessage(),
                    "Unable to retrieve content by status. Please try again.",
                    e);
        }
    }

    /**
     * Searches content by title containing the specified text.
     *
     * @param titleText The text to search for in titles
     * @return List of content with titles containing the text
     * @throws RepositoryException if the operation fails
     */
    public List<Content> searchByTitle(String titleText) throws RepositoryException {
        if (titleText == null || titleText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String searchText = titleText.toLowerCase().trim();
            return contentStorage.values().stream()
                    .filter(content -> content.getTitle() != null &&
                            content.getTitle().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to search content by title: " + e.getMessage(),
                    "Unable to search content. Please try again.",
                    e);
        }
    }

    /**
     * Updates the author index when content is saved.
     */
    private void updateAuthorIndex(Content content) {
        if (content.getCreatedBy() != null && !content.getCreatedBy().trim().isEmpty()) {
            String authorUsername = content.getCreatedBy();
            authorIndex.computeIfAbsent(authorUsername, k -> new ArrayList<>())
                    .add(content.getId());
        }
    }

    /**
     * Updates the status index when content is saved.
     */
    private void updateStatusIndex(Content content) {
        if (content.getStatus() != null) {
            statusIndex.get(content.getStatus()).add(content.getId());
        }
    }

    /**
     * Removes content from all indexes when deleted.
     */
    private void removeFromIndexes(Content content) {
        // Remove from author index
        if (content.getCreatedBy() != null && !content.getCreatedBy().trim().isEmpty()) {
            List<String> authorContent = authorIndex.get(content.getCreatedBy());
            if (authorContent != null) {
                authorContent.remove(content.getId());
            }
        }

        // Remove from status index
        if (content.getStatus() != null) {
            List<String> statusContent = statusIndex.get(content.getStatus());
            if (statusContent != null) {
                statusContent.remove(content.getId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Content> findAll(int page, int size) throws RepositoryException {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        try {
            List<Content> allContent = findAll();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allContent.size());

            if (startIndex >= allContent.size()) {
                return new ArrayList<>();
            }

            return new ArrayList<>(allContent.subList(startIndex, endIndex));
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to retrieve paginated content: " + e.getMessage(),
                    "Unable to retrieve content page. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Content> saveAll(Iterable<? extends Content> entities) throws RepositoryException {
        if (entities == null) {
            throw new IllegalArgumentException("Entities cannot be null");
        }

        try {
            List<Content> savedEntities = new ArrayList<>();
            for (Content content : entities) {
                Content saved = save(content);
                savedEntities.add(saved);
            }
            return savedEntities;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to save all entities: " + e.getMessage(),
                    "Unable to save multiple content items. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Content> findAllById(Iterable<? extends String> ids) throws RepositoryException {
        if (ids == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }

        try {
            List<Content> result = new ArrayList<>();
            for (String id : ids) {
                Optional<Content> content = findById(id);
                content.ifPresent(result::add);
            }
            return result;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to find content by IDs: " + e.getMessage(),
                    "Unable to retrieve content by IDs. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long batchUpdate(Map<String, Object> updateCriteria, Map<String, Object> updateData)
            throws RepositoryException {
        if (updateCriteria == null || updateData == null) {
            throw new IllegalArgumentException("Update criteria and data cannot be null");
        }

        try {
            long updatedCount = 0;

            // For simplicity in this in-memory implementation,
            // we'll iterate through all content and apply updates based on criteria
            for (Content content : contentStorage.values()) {
                if (matchesCriteria(content, updateCriteria)) {
                    // In a real implementation, this would update the content
                    // For this demo, we'll just count matches
                    updatedCount++;
                    LoggerUtil.logDebug("ContentRepository",
                            "Would update content: " + content.getId());
                }
            }

            LoggerUtil.logInfo("ContentRepository",
                    "Batch update completed: " + updatedCount + " items would be updated");

            return updatedCount;
        } catch (Exception e) {
            throw new RepositoryException(
                    "Batch update failed: " + e.getMessage(),
                    "Unable to perform batch update. Please try again.",
                    e);
        }
    }

    /**
     * Helper method to check if content matches update criteria.
     */
    private boolean matchesCriteria(Content content, Map<String, Object> criteria) {
        // Simple implementation - in a real system this would be more sophisticated
        for (Map.Entry<String, Object> criterion : criteria.entrySet()) {
            String key = criterion.getKey();
            Object expectedValue = criterion.getValue();

            switch (key) {
                case "createdBy":
                    if (!expectedValue.equals(content.getCreatedBy())) {
                        return false;
                    }
                    break;
                case "status":
                    if (!expectedValue.equals(content.getStatus())) {
                        return false;
                    }
                    break;
                // Add more criteria as needed
                default:
                    // Unknown criterion - skip for now
                    break;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll() throws RepositoryException {
        try {
            clear();
            LoggerUtil.logInfo("ContentRepository", "All content deleted from repository");
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to delete all content: " + e.getMessage(),
                    "Unable to delete all content. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAll(Iterable<? extends Content> entities) throws RepositoryException {
        if (entities == null) {
            throw new IllegalArgumentException("Entities cannot be null");
        }

        try {
            int deleteCount = 0;
            for (Content content : entities) {
                if (contentStorage.containsKey(content.getId())) {
                    deleteById(content.getId());
                    deleteCount++;
                }
            }
            LoggerUtil.logInfo("ContentRepository",
                    "Deleted " + deleteCount + " content items");
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to delete content entities: " + e.getMessage(),
                    "Unable to delete specified content. Please try again.",
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws RepositoryException {
        // In-memory implementation doesn't need flushing
        // This method is included for interface compliance
        LoggerUtil.logDebug("ContentRepository", "Flush operation completed (no-op for in-memory storage)");
    }

    /**
     * Clears all content from the repository.
     * Used for testing and reset operations.
     *
     * @throws RepositoryException if the operation fails
     */
    public void clear() throws RepositoryException {
        try {
            contentStorage.clear();
            authorIndex.clear();
            for (ContentStatus status : ContentStatus.values()) {
                statusIndex.put(status, new ArrayList<>());
            }
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to clear repository: " + e.getMessage(),
                    "Unable to clear content. Please try again.",
                    e);
        }
    }
}
