package com.cms.patterns.iterator;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

/**
 * Iterator implementation for traversing content collections in JavaCMS.
 *
 * <p>
 * This class provides a standardized way to iterate through collections of
 * Content objects
 * with support for filtering by content type, status, date ranges, and custom
 * predicates.
 * It implements the Iterator Pattern to enable sequential access to content
 * without exposing
 * the underlying collection structure.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Iterator Pattern Implementation
 * <br>
 * This class serves as a concrete iterator in the Iterator design pattern,
 * providing
 * sequential access to Content objects while encapsulating the traversal
 * algorithm.
 * It allows clients to iterate through content collections without needing to
 * know
 * the internal structure of the storage mechanism.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the Iterator Pattern
 * by providing controlled sequential access to content collections,
 * integrating with existing Content model classes, and supporting advanced
 * filtering capabilities for different iteration scenarios.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li><strong>Type Safety:</strong> Generic implementation ensures type-safe
 * iteration</li>
 * <li><strong>Filtering Support:</strong> Multiple built-in filters for common
 * use cases</li>
 * <li><strong>Lazy Evaluation:</strong> Efficient processing with minimal
 * memory overhead</li>
 * <li><strong>Thread Safety:</strong> Safe for concurrent read operations</li>
 * <li><strong>Custom Predicates:</strong> Extensible filtering with
 * user-defined criteria</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li>Search result processing and pagination</li>
 * <li>Content export and backup operations</li>
 * <li>Site crawling and indexing tasks</li>
 * <li>Content validation and quality checks</li>
 * <li>Publishing workflow processing</li>
 * </ul>
 * </p>
 *
 * @param <T> The specific content type being iterated
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.iterator.SiteStructureIterator
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentIterator<T extends Content<T>> implements Iterator<T> {

    /** The source collection being iterated */
    private final List<T> sourceCollection;

    /** Current position in the iteration */
    private int currentIndex;

    /** Filter predicate for content selection */
    private final Predicate<T> filter;

    /** Pre-filtered list for efficient iteration */
    private final List<T> filteredContent;

    /** Iterator for the filtered content */
    private final Iterator<T> internalIterator;

    /**
     * Creates a content iterator for the entire collection without filtering.
     *
     * <p>
     * This constructor creates an iterator that will traverse all content
     * in the provided collection in their natural order.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @throws IllegalArgumentException if collection is null
     * @since 1.0
     */
    public ContentIterator(Collection<T> collection) {
        this(collection, null);
    }

    /**
     * Creates a content iterator with custom filtering predicate.
     *
     * <p>
     * This constructor allows for custom filtering during iteration.
     * Only content that matches the provided predicate will be included
     * in the iteration sequence.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param filter     Predicate for filtering content (null for no filtering)
     * @throws IllegalArgumentException if collection is null
     * @since 1.0
     */
    public ContentIterator(Collection<T> collection, Predicate<T> filter) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection cannot be null");
        }

        // Create defensive copy for thread safety
        this.sourceCollection = new ArrayList<>(collection);
        this.filter = filter;
        this.currentIndex = 0;

        // Pre-filter content for efficient iteration
        this.filteredContent = new ArrayList<>();
        for (T content : sourceCollection) {
            if (filter == null || filter.test(content)) {
                filteredContent.add(content);
            }
        }

        this.internalIterator = filteredContent.iterator();
    }

    /**
     * Creates a content iterator filtering by content status.
     *
     * <p>
     * This factory method creates an iterator that only includes content
     * with the specified status (DRAFT, PUBLISHED, ARCHIVED, etc.).
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param status     The content status to filter by
     * @param <T>        The content type
     * @return ContentIterator filtered by status
     * @throws IllegalArgumentException if collection or status is null
     * @since 1.0
     */
    public static <T extends Content<T>> ContentIterator<T> byStatus(Collection<T> collection, ContentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new ContentIterator<>(collection, content -> content.getStatus() == status);
    }

    /**
     * Creates a content iterator filtering by content type.
     *
     * <p>
     * This factory method creates an iterator that only includes content
     * of the specified class type (ArticleContent, PageContent, etc.).
     * </p>
     *
     * @param collection  The collection of content to iterate over
     * @param contentType The class type to filter by
     * @param <T>         The content type
     * @param <U>         The specific content subtype
     * @return ContentIterator filtered by type
     * @throws IllegalArgumentException if collection or contentType is null
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    public static <T extends Content<T>> ContentIterator<T> byType(
            Collection<T> collection, Class<T> contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type cannot be null");
        }

        List<T> typedContent = new ArrayList<>();
        for (T content : collection) {
            if (contentType.isInstance(content)) {
                typedContent.add(content);
            }
        }
        return new ContentIterator<>(typedContent);
    }

    /**
     * Creates a content iterator filtering by date range.
     *
     * <p>
     * This factory method creates an iterator that only includes content
     * created within the specified date range (inclusive).
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param startDate  The start of the date range (null for no lower bound)
     * @param endDate    The end of the date range (null for no upper bound)
     * @param <T>        The content type
     * @return ContentIterator filtered by date range
     * @throws IllegalArgumentException if collection is null or date range is
     *                                  invalid
     * @since 1.0
     */
    public static <T extends Content<T>> ContentIterator<T> byDateRange(
            Collection<T> collection, LocalDateTime startDate, LocalDateTime endDate) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Predicate<T> dateFilter = content -> {
            LocalDateTime contentDate = content.getCreatedDate();
            if (contentDate == null)
                return false;

            boolean afterStart = (startDate == null) || !contentDate.isBefore(startDate);
            boolean beforeEnd = (endDate == null) || !contentDate.isAfter(endDate);

            return afterStart && beforeEnd;
        };

        return new ContentIterator<>(collection, dateFilter);
    }

    /**
     * Creates a content iterator filtering by author/creator.
     *
     * <p>
     * This factory method creates an iterator that only includes content
     * created by the specified user.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param createdBy  The username/ID of the content creator
     * @param <T>        The content type
     * @return ContentIterator filtered by creator
     * @throws IllegalArgumentException if collection or createdBy is null
     * @since 1.0
     */
    public static <T extends Content<T>> ContentIterator<T> byCreator(
            Collection<T> collection, String createdBy) {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Creator cannot be null or empty");
        }

        String targetCreator = createdBy.trim();
        return new ContentIterator<>(collection,
                content -> targetCreator.equals(content.getCreatedBy()));
    }

    /**
     * Creates a content iterator with multiple combined filters.
     *
     * <p>
     * This factory method allows combining multiple filter criteria with
     * AND logic - content must match all provided criteria to be included.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param filters    Variable number of filter predicates to combine
     * @param <T>        The content type
     * @return ContentIterator with combined filters
     * @throws IllegalArgumentException if collection is null
     * @since 1.0
     */
    @SafeVarargs
    public static <T extends Content<T>> ContentIterator<T> withFilters(
            Collection<T> collection, Predicate<T>... filters) {

        if (filters == null || filters.length == 0) {
            return new ContentIterator<>(collection);
        }

        Predicate<T> combinedFilter = content -> {
            for (Predicate<T> filter : filters) {
                if (filter != null && !filter.test(content)) {
                    return false;
                }
            }
            return true;
        };

        return new ContentIterator<>(collection, combinedFilter);
    }

    /**
     * Creates a content iterator for published content only.
     *
     * <p>
     * Convenience method for filtering content by PUBLISHED status.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param <T>        The content type
     * @return ContentIterator for published content only
     * @since 1.0
     */
    public static <T extends Content<T>> ContentIterator<T> publishedOnly(Collection<T> collection) {
        return byStatus(collection, ContentStatus.PUBLISHED);
    }

    /**
     * Creates a content iterator for draft content only.
     *
     * <p>
     * Convenience method for filtering content by DRAFT status.
     * </p>
     *
     * @param collection The collection of content to iterate over
     * @param <T>        The content type
     * @return ContentIterator for draft content only
     * @since 1.0
     */
    public static <T extends Content<T>> ContentIterator<T> draftsOnly(Collection<T> collection) {
        return byStatus(collection, ContentStatus.DRAFT);
    }

    /**
     * Returns true if there are more elements in the iteration.
     *
     * <p>
     * This method checks whether there are additional content items
     * available for iteration, respecting any applied filters.
     * </p>
     *
     * @return true if there are more elements to iterate
     * @since 1.0
     */
    @Override
    public boolean hasNext() {
        return internalIterator.hasNext();
    }

    /**
     * Returns the next element in the iteration.
     *
     * <p>
     * Advances the iterator position and returns the next content item
     * that matches the filtering criteria.
     * </p>
     *
     * @return The next content item in the sequence
     * @throws NoSuchElementException if there are no more elements
     * @since 1.0
     */
    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more content items available");
        }
        return internalIterator.next();
    }

    /**
     * Removes the current element from the underlying collection.
     *
     * <p>
     * <strong>Note:</strong> This operation is not supported as it would
     * modify the source collection, potentially causing concurrent modification
     * issues. Use collection-specific removal methods instead.
     * </p>
     *
     * @throws UnsupportedOperationException always
     * @since 1.0
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove operation not supported. Use collection-specific methods for content removal.");
    }

    /**
     * Returns the total number of items in the filtered iteration.
     *
     * <p>
     * This method returns the count of content items that match the
     * filtering criteria, which may be different from the source collection size.
     * </p>
     *
     * @return The number of content items that will be iterated
     * @since 1.0
     */
    public int size() {
        return filteredContent.size();
    }

    /**
     * Returns true if the filtered iteration is empty.
     *
     * <p>
     * Checks whether any content items match the filtering criteria.
     * </p>
     *
     * @return true if no content items match the filter
     * @since 1.0
     */
    public boolean isEmpty() {
        return filteredContent.isEmpty();
    }

    /**
     * Returns the current filter predicate being used.
     *
     * <p>
     * Provides access to the filtering logic for inspection or modification.
     * </p>
     *
     * @return The current filter predicate (may be null)
     * @since 1.0
     */
    public Predicate<T> getFilter() {
        return filter;
    }

    /**
     * Returns a list containing all remaining elements from this iterator.
     *
     * <p>
     * This method consumes the iterator, collecting all remaining elements
     * into a new list. After calling this method, the iterator will be exhausted.
     * </p>
     *
     * @return List containing all remaining content items
     * @since 1.0
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            result.add(next());
        }
        return result;
    }

    /**
     * Creates a new iterator that applies an additional filter to this iterator's
     * results.
     *
     * <p>
     * This method allows for chaining filters, creating more complex filtering
     * scenarios by combining multiple criteria.
     * </p>
     *
     * @param additionalFilter Additional filter to apply
     * @return New ContentIterator with combined filters
     * @since 1.0
     */
    public ContentIterator<T> filter(Predicate<T> additionalFilter) {
        if (additionalFilter == null) {
            return new ContentIterator<>(filteredContent, filter);
        }

        Predicate<T> combinedFilter = filter == null ? additionalFilter : filter.and(additionalFilter);

        return new ContentIterator<>(sourceCollection, combinedFilter);
    }

    /**
     * Returns a string representation of this iterator.
     *
     * <p>
     * Includes information about the filtered content count and filter status
     * for debugging purposes.
     * </p>
     *
     * @return String representation of the iterator state
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format("ContentIterator{filteredSize=%d, totalSize=%d, hasFilter=%s}",
                filteredContent.size(), sourceCollection.size(), filter != null);
    }
}
