package com.cms.streams;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Functional interface for content filtering predicates using Stream API.
 *
 * <p>
 * This interface provides specialized predicate methods for filtering content
 * using functional programming and lambda expressions. It provides
 * powerful combination of Stream API with custom functional interfaces for
 * type-safe, composable filtering operations.
 * </p>
 *
 * <p>
 * <strong>Stream API & Lambdas:</strong> This interface implements
 * functional predicates using Stream API and Lambdas, providing type-safe
 * predicate composition for content operations.
 * </p>
 *
 * <p>
 * <strong>Design Pattern Integration:</strong> Integrates with Factory Pattern
 * for content type filtering, Iterator Pattern for stream-based traversal,
 * and Observer Pattern for filtered event processing.
 * </p>
 *
 * <p>
 * <strong>Performance:</strong> Optimized for use with parallel streams,
 * providing thread-safe predicate operations with minimal overhead.
 * </p>
 *
 * @since 1.0
 * @author Otman Hmich S007924
 */
@FunctionalInterface
public interface ContentPredicate extends Predicate<Content> {

    /**
     * Creates a predicate that filters content by status.
     *
     * <p>
     * <strong>Stream API Usage:</strong> Designed for use with stream
     * filter operations: {@code contents.stream().filter(byStatus(PUBLISHED))}.
     * </p>
     *
     * @param status the content status to filter by
     * @return predicate that tests content status equality
     * @throws IllegalArgumentException if status is null
     */
    static ContentPredicate byStatus(ContentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Content status cannot be null");
        }
        return content -> content.getStatus() == status;
    }

    /**
     * Creates a predicate that filters content by title containing text.
     *
     * <p>
     * <strong>Lambda Expression:</strong> Returns a lambda-based predicate
     * for case-insensitive title matching using functional programming.
     * </p>
     *
     * @param text the text to search for in titles
     * @return predicate that tests title contains text (case-insensitive)
     * @throws IllegalArgumentException if text is null or empty
     */
    static ContentPredicate byTitleContaining(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be null or empty");
        }
        String searchText = text.toLowerCase().trim();
        return content -> content.getTitle() != null &&
                content.getTitle().toLowerCase().contains(searchText);
    }

    /**
     * Creates a predicate that filters content by author.
     *
     * <p>
     * <strong>Method Reference:</strong> Can be used with method references
     * for clean functional code: {@code byAuthor("john")::test}.
     * </p>
     *
     * @param authorName the author name to filter by
     * @return predicate that tests author name equality (case-insensitive)
     * @throws IllegalArgumentException if authorName is null
     */
    static ContentPredicate byAuthor(String authorName) {
        if (authorName == null) {
            throw new IllegalArgumentException("Author name cannot be null");
        }
        String searchAuthor = authorName.toLowerCase().trim();
        return content -> content.getCreatedBy() != null &&
                content.getCreatedBy().toLowerCase().equals(searchAuthor);
    }

    /**
     * Creates a predicate that filters content created after a specific date.
     *
     * <p>
     * <strong>Functional Composition:</strong> Can be combined with other
     * predicates using {@code and()}, {@code or()}, and {@code negate()}
     * methods for complex filtering logic.
     * </p>
     *
     * @param date the cutoff date for content creation
     * @return predicate that tests content creation date is after specified date
     * @throws IllegalArgumentException if date is null
     */
    static ContentPredicate createdAfter(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return content -> content.getCreatedDate() != null &&
                content.getCreatedDate().isAfter(date);
    }

    /**
     * Creates a predicate that filters content created before a specific date.
     *
     * <p>
     * <strong>Stream Pipeline:</strong> Optimized for use in stream pipelines
     * with other operations like map, collect, and reduce.
     * </p>
     *
     * @param date the cutoff date for content creation
     * @return predicate that tests content creation date is before specified date
     * @throws IllegalArgumentException if date is null
     */
    static ContentPredicate createdBefore(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return content -> content.getCreatedDate() != null &&
                content.getCreatedDate().isBefore(date);
    }

    /**
     * Creates a predicate that filters content within a date range.
     *
     * <p>
     * <strong>Lambda Composition:</strong> Combines multiple lambda expressions
     * for complex date range filtering with optimal performance.
     * </p>
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate   the end date of the range (inclusive)
     * @return predicate that tests content is within the date range
     * @throws IllegalArgumentException if dates are null or startDate is after
     *                                  endDate
     */
    static ContentPredicate withinDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        return content -> content.getCreatedDate() != null &&
                !content.getCreatedDate().isBefore(startDate) &&
                !content.getCreatedDate().isAfter(endDate);
    }

    /**
     * Creates a predicate that filters content by minimum word count.
     *
     * <p>
     * <strong>Performance Optimization:</strong> Uses lazy evaluation
     * with streams to avoid unnecessary content processing.
     * </p>
     *
     * @param minWords the minimum word count required
     * @return predicate that tests content has at least minWords
     * @throws IllegalArgumentException if minWords is negative
     */
    static ContentPredicate hasMinWords(int minWords) {
        if (minWords < 0) {
            throw new IllegalArgumentException("Minimum words cannot be negative");
        }
        return content -> {
            if (content.getBody() == null)
                return false;
            long wordCount = content.getBody()
                    .trim()
                    .split("\\s+").length;
            return wordCount >= minWords;
        };
    }

    /**
     * Creates a predicate that filters published content only.
     *
     * <p>
     * <strong>Convenience Method:</strong> Provides a common predicate
     * for filtering published content in stream operations.
     * </p>
     *
     * @return predicate that tests content status is PUBLISHED
     */
    static ContentPredicate isPublished() {
        return byStatus(ContentStatus.PUBLISHED);
    }

    /**
     * Creates a predicate that filters draft content only.
     *
     * <p>
     * <strong>Method Reference Compatible:</strong> Can be used as method
     * reference: {@code ContentPredicate::isDraft}.
     * </p>
     *
     * @return predicate that tests content status is DRAFT
     */
    static ContentPredicate isDraft() {
        return byStatus(ContentStatus.DRAFT);
    }
}
