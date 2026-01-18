package com.cms.streams;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Functional interface for content mapping operations using Stream API.
 *
 * <p>
 * This interface provides specialized mapping functions for transforming
 * content objects into various representations using functional programming
 * and lambda expressions. It utilizes the power of method references
 * and function composition in stream processing.
 * </p>
 *
 * <p>
 * <strong>Stream API & Lambdas:</strong> This interface implements
 * functional mapping using Stream API and Lambdas, providing type-safe
 * transformations with method references.
 * </p>
 *
 * <p>
 * <strong>Design Pattern Integration:</strong> Works seamlessly with
 * Factory Pattern content types, Iterator Pattern traversal, and provides
 * mapping functions for Observer Pattern event data transformation.
 * </p>
 *
 * <p>
 * <strong>Performance:</strong> All mapping functions are optimized for
 * parallel stream operations with minimal memory overhead and thread safety.
 * </p>
 *
 * @param <R> the result type of the mapping function
 * @since 1.0
 * @author Otman Hmich S007924
 */
@FunctionalInterface
public interface ContentMapper<R> extends Function<Content, R> {

    /**
     * Content Data Transfer Object for lightweight content representation.
     *
     * <p>
     * <strong>Lambda Integration:</strong> Designed specifically for use
     * with lambda expressions and method references in stream operations.
     * </p>
     */
    record ContentDTO(String id, String title, String author, ContentStatus status,
            LocalDateTime createdAt, int wordCount) {

        /**
         * Factory method for creating ContentDTO from Content using method reference.
         *
         * <p>
         * <strong>Method Reference:</strong> Use as {@code Content::toDTO}
         * in stream mapping operations.
         * </p>
         *
         * @param content the content to convert
         * @return ContentDTO representation
         */
        public static ContentDTO fromContent(Content content) {
            int wordCount = content.getBody() != null ? content.getBody().trim().split("\\s+").length : 0;

            return new ContentDTO(
                    content.getId(),
                    content.getTitle(),
                    content.getCreatedBy(),
                    content.getStatus(),
                    content.getCreatedDate(),
                    wordCount);
        }
    }

    /**
     * Content summary for analytics and reporting operations.
     *
     * <p>
     * <strong>Stream Collectors:</strong> Designed for use with custom
     * collectors and stream reduction operations.
     * </p>
     */
    record ContentSummary(String id, String title, String summary,
            LocalDateTime createdAt, String formattedDate) {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        /**
         * Creates summary from content with truncated content preview.
         *
         * <p>
         * <strong>Lambda Expression:</strong> Optimized for use in
         * stream map operations with lazy evaluation.
         * </p>
         *
         * @param content          the content to summarize
         * @param maxSummaryLength maximum length of content summary
         * @return ContentSummary with truncated content
         */
        public static ContentSummary fromContent(Content content, int maxSummaryLength) {
            String summary = content.getBody() != null ? (content.getBody().length() > maxSummaryLength
                    ? content.getBody().substring(0, maxSummaryLength) + "..."
                    : content.getBody()) : "";

            String formattedDate = content.getCreatedDate() != null ? content.getCreatedDate().format(FORMATTER)
                    : "Unknown";

            return new ContentSummary(
                    content.getId(),
                    content.getTitle(),
                    summary,
                    content.getCreatedDate(),
                    formattedDate);
        }
    }

    /**
     * Maps content to its ID using method reference.
     *
     * <p>
     * <strong>Method Reference:</strong> Use as {@code Content::getId}
     * for clean functional programming in stream operations.
     * </p>
     *
     * @return mapper function that extracts content ID
     */
    static ContentMapper<String> toId() {
        return Content::getId;
    }

    /**
     * Maps content to its title using method reference.
     *
     * <p>
     * <strong>Stream Pipeline:</strong> Optimized for use in stream
     * pipelines: {@code contents.stream().map(toTitle()).collect(toList())}.
     * </p>
     *
     * @return mapper function that extracts content title
     */
    static ContentMapper<String> toTitle() {
        return Content::getTitle;
    }

    /**
     * Maps content to its author using method reference.
     *
     * <p>
     * <strong>Null Safety:</strong> Includes null safety for robust
     * stream processing with Optional-like behavior.
     * </p>
     *
     * @return mapper function that extracts content author (null-safe)
     */
    static ContentMapper<String> toAuthor() {
        return content -> content.getCreatedBy() != null ? content.getCreatedBy() : "Unknown";
    }

    /**
     * Maps content to its status using method reference.
     *
     * <p>
     * <strong>Enum Handling:</strong> Provides type-safe enum extraction
     * for use in stream operations and collectors.
     * </p>
     *
     * @return mapper function that extracts content status
     */
    static ContentMapper<ContentStatus> toStatus() {
        return Content::getStatus;
    }

    /**
     * Maps content to its creation date using method reference.
     *
     * <p>
     * <strong>Date Operations:</strong> Optimized for use with stream
     * date filtering and sorting operations.
     * </p>
     *
     * @return mapper function that extracts creation date
     */
    static ContentMapper<LocalDateTime> toCreatedAt() {
        return Content::getCreatedDate;
    }

    /**
     * Maps content to word count using lambda expression.
     *
     * <p>
     * <strong>Lambda Expression:</strong> Uses complex lambda
     * logic for calculating derived properties from content.
     * </p>
     *
     * @return mapper function that calculates word count
     */
    static ContentMapper<Integer> toWordCount() {
        return content -> content.getBody() != null ? content.getBody().trim().split("\\s+").length : 0;
    }

    /**
     * Maps content to ContentDTO using method reference.
     *
     * <p>
     * <strong>DTO Pattern:</strong> Converts to lightweight DTO for
     * efficient data transfer and serialization in stream operations.
     * </p>
     *
     * @return mapper function that creates ContentDTO
     */
    static ContentMapper<ContentDTO> toDTO() {
        return ContentDTO::fromContent;
    }

    /**
     * Maps content to ContentSummary with specified summary length.
     *
     * <p>
     * <strong>Partial Application:</strong> Implements function currying
     * and partial application in functional programming.
     * </p>
     *
     * @param maxSummaryLength maximum length of content summary
     * @return mapper function that creates ContentSummary
     * @throws IllegalArgumentException if maxSummaryLength is negative
     */
    static ContentMapper<ContentSummary> toSummary(int maxSummaryLength) {
        if (maxSummaryLength < 0) {
            throw new IllegalArgumentException("Summary length cannot be negative");
        }
        return content -> ContentSummary.fromContent(content, maxSummaryLength);
    }

    /**
     * Maps content to formatted string representation.
     *
     * <p>
     * <strong>String Templates:</strong> Uses lambda expression for
     * custom string formatting in stream operations.
     * </p>
     *
     * @param format format string (e.g., "%s by %s (%s)")
     * @return mapper function that formats content as string
     * @throws IllegalArgumentException if format is null
     */
    static ContentMapper<String> toFormattedString(String format) {
        if (format == null) {
            throw new IllegalArgumentException("Format string cannot be null");
        }
        return content -> String.format(format,
                content.getTitle() != null ? content.getTitle() : "Untitled",
                content.getCreatedBy() != null ? content.getCreatedBy() : "Unknown",
                content.getStatus() != null ? content.getStatus() : "UNKNOWN");
    }

    /**
     * Maps content to uppercase title using method chaining.
     *
     * <p>
     * <strong>Function Composition:</strong> Uses function
     * composition with method references and lambda expressions.
     * </p>
     *
     * @return mapper function that converts title to uppercase
     */
    static ContentMapper<String> toUppercaseTitle() {
        return content -> {
            String title = content.getTitle();
            return title != null ? title.toUpperCase() : "";
        };
    }

    /**
     * Maps content to its length in characters.
     *
     * <p>
     * <strong>Numeric Operations:</strong> Provides numeric mapping
     * for use with stream numeric collectors and statistics.
     * </p>
     *
     * @return mapper function that calculates content length
     */
    static ContentMapper<Integer> toContentLength() {
        return content -> content.getBody() != null ? content.getBody().length() : 0;
    }
}
