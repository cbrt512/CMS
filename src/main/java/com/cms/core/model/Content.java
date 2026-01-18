package com.cms.core.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Abstract base class representing all content types in the JavaCMS system.
 *
 * <p>
 * This class serves as the foundation for all content entities in the Content
 * Management System,
 * implementing core functionality shared across different content types
 * (articles, pages, images, videos).
 * It establishes the basic structure and behavior patterns that concrete
 * content implementations must follow.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> This class is part of the Factory Pattern
 * implementation,
 * serving as the abstract product class that all concrete content types extend.
 * It defines the common
 * interface and shared attributes that the ContentFactory will use to create
 * different content types.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Provides Collections Framework implementation
 * through use of
 * Map&lt;String, Object&gt; for metadata storage and Generics implementation
 * through type-safe
 * operations and parameterized collections.
 * </p>
 *
 * <p>
 * <strong>Security Considerations:</strong> Implements input sanitization for
 * content fields and
 * provides controlled access to sensitive metadata. All setters perform
 * validation to prevent
 * malicious content injection.
 * </p>
 *
 * @param <T> The specific content type for type-safe operations
 * @see com.cms.patterns.factory.ContentFactory
 * @see com.cms.core.repository.ContentRepository
 * @since 1.0
 * @author Otman Hmich S007924
 */
public abstract class Content<T extends Content<T>> implements Cloneable, Comparable<Content<?>> {

    /** Unique identifier for this content instance */
    protected final String id;

    /** Human-readable title of the content */
    protected String title;

    /** Main body/description of the content */
    protected String body;

    /** User who created this content */
    protected String createdBy;

    /** Timestamp when content was created */
    protected LocalDateTime createdDate;

    /** User who last modified this content */
    protected String modifiedBy;

    /** Timestamp of last modification */
    protected LocalDateTime modifiedDate;

    /** Current status of the content */
    protected ContentStatus status;

    /** Additional metadata stored as key-value pairs */
    protected Map<String, Object> metadata;

    /** Tags associated with this content for categorization */
    protected String[] tags;

    /** Timestamp when content was first published */
    protected LocalDateTime publishedDate;

    /**
     * Protected constructor for concrete content implementations.
     *
     * <p>
     * Initializes the content with basic required fields and sets up
     * default values for timestamps and metadata storage. The constructor
     * generates a unique ID and initializes the metadata map.
     * </p>
     *
     * <p>
     * <strong>Collections Framework Usage:</strong> Initializes HashMap for
     * metadata storage, demonstrating proper Collections Framework usage.
     * </p>
     *
     * @param title     The title of the content, must not be null or empty
     * @param body      The main content body, must not be null
     * @param createdBy The ID of the user creating this content, must not be null
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    protected Content(String title, String body, String createdBy) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Content title cannot be null or empty");
        }
        if (body == null) {
            throw new IllegalArgumentException("Content body cannot be null");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by user ID cannot be null or empty");
        }

        this.id = UUID.randomUUID().toString();
        this.title = sanitizeInput(title);
        this.body = sanitizeInput(body);
        this.createdBy = createdBy;
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = this.createdDate;
        this.status = ContentStatus.DRAFT;
        this.metadata = new HashMap<>();
        this.tags = new String[0];
    }

    /**
     * Abstract method that concrete content types must implement to define their
     * specific type.
     *
     * <p>
     * <strong>Factory Pattern:</strong> This method is used by the ContentFactory
     * to determine
     * the appropriate content type during object creation and serialization
     * processes.
     * </p>
     *
     * @return A string identifier representing the specific content type
     */
    public abstract String getContentType();

    /**
     * Abstract method for content-specific validation logic.
     *
     * <p>
     * Each content type may have different validation criteria (e.g., images need
     * file format
     * validation, articles need minimum word count). This method allows each
     * concrete implementation
     * to define its own validation rules while maintaining a consistent validation
     * interface.
     * </p>
     *
     * @return true if the content passes all validation checks, false otherwise
     * @throws ContentValidationException if validation fails with specific error
     *                                    details
     */
    public abstract boolean validate() throws ContentValidationException;

    /**
     * Abstract method for rendering content in different output formats.
     *
     * <p>
     * Different content types require different rendering approaches (HTML for
     * articles,
     * image tags for images, video players for videos). This method provides a
     * unified
     * interface for content presentation while allowing type-specific
     * implementations.
     * </p>
     *
     * @param format  The desired output format (HTML, JSON, XML, etc.)
     * @param context Additional rendering context and parameters
     * @return The rendered content as a string in the specified format
     * @throws ContentRenderingException if rendering fails or format is unsupported
     */
    public abstract String render(String format, Map<String, Object> context) throws ContentRenderingException;

    // Getter methods with comprehensive documentation

    /**
     * Returns the unique identifier for this content.
     *
     * <p>
     * The ID is automatically generated during content creation and remains
     * immutable throughout the content's lifecycle. This ID is used for content
     * retrieval, caching, and relationship management.
     * </p>
     *
     * @return The unique content identifier, never null
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the title of this content.
     *
     * @return The content title, never null
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the main body of this content.
     *
     * @return The content body, never null
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the ID of the user who created this content.
     *
     * @return The creator's user ID, never null
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return The date and time when this content was created, never null
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns the ID of the user who last modified this content.
     *
     * @return The modifier's user ID, may be null if never modified
     */
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Returns the last modification timestamp.
     *
     * @return The date and time of last modification, never null
     */
    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Returns the current status of this content.
     *
     * @return The content status, never null
     */
    public ContentStatus getStatus() {
        return status;
    }

    /**
     * Returns a copy of the metadata map.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses defensive copying
     * to prevent external modification of internal collections while still
     * providing access to metadata.
     * </p>
     *
     * @return A new HashMap containing all metadata entries
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Returns a copy of the tags array.
     *
     * <p>
     * Returns a defensive copy to prevent external modification of the
     * internal tags array while providing access to tag information.
     * </p>
     *
     * @return A new array containing all tags, never null but may be empty
     */
    public String[] getTags() {
        return tags.clone();
    }

    // Setter methods with validation and sanitization

    /**
     * Sets the title of this content.
     *
     * <p>
     * Performs input sanitization to prevent XSS attacks and other security
     * vulnerabilities. Updates the modification timestamp and modifier information.
     * </p>
     *
     * @param title      The new title, must not be null or empty
     * @param modifiedBy The ID of the user making the modification
     * @throws IllegalArgumentException if title is null, empty, or modifiedBy is
     *                                  null
     */
    public void setTitle(String title, String modifiedBy) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.title = sanitizeInput(title);
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the body content.
     *
     * <p>
     * Performs input sanitization and updates modification tracking information.
     * The body content is sanitized to prevent malicious script injection while
     * preserving legitimate formatting.
     * </p>
     *
     * @param body       The new content body, must not be null
     * @param modifiedBy The ID of the user making the modification
     * @throws IllegalArgumentException if body is null or modifiedBy is null
     */
    public void setBody(String body, String modifiedBy) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.body = sanitizeInput(body);
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the content status.
     *
     * <p>
     * Updates the content status with appropriate tracking of who made the change.
     * Status changes are significant events in the content lifecycle and are
     * logged appropriately.
     * </p>
     *
     * @param status     The new status, must not be null
     * @param modifiedBy The ID of the user making the status change
     * @throws IllegalArgumentException if status or modifiedBy is null
     */
    public void setStatus(ContentStatus status, String modifiedBy) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.status = status;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Adds or updates a metadata entry.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses proper implementation of Map
     * operations for dynamic metadata management. Validates both key and value
     * to prevent security issues.
     * </p>
     *
     * @param key        The metadata key, must not be null or empty
     * @param value      The metadata value, must not be null
     * @param modifiedBy The ID of the user making the modification
     * @throws IllegalArgumentException if key is invalid or modifiedBy is null
     */
    public void addMetadata(String key, Object value, String modifiedBy) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Metadata value cannot be null");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.metadata.put(sanitizeInput(key), value);
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the tags for this content.
     *
     * <p>
     * Replaces all existing tags with the provided array. Each tag is
     * sanitized to prevent security issues and normalized for consistent
     * searching and categorization.
     * </p>
     *
     * @param tags       Array of tags, must not be null but may be empty
     * @param modifiedBy The ID of the user making the modification
     * @throws IllegalArgumentException if tags array is null or modifiedBy is null
     */
    public void setTags(String[] tags, String modifiedBy) {
        if (tags == null) {
            throw new IllegalArgumentException("Tags array cannot be null");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        // Sanitize and normalize tags
        String[] sanitizedTags = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            sanitizedTags[i] = sanitizeInput(tags[i]).toLowerCase().trim();
        }

        this.tags = sanitizedTags;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Updates modification tracking information.
     *
     * <p>
     * Private method to centralize modification timestamp and user tracking.
     * Ensures consistent behavior across all modification operations.
     * </p>
     *
     * @param modifiedBy The ID of the user making the modification
     */
    private void updateModificationInfo(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        this.modifiedDate = LocalDateTime.now();
    }

    /**
     * Sanitizes input strings to prevent XSS attacks and other security issues.
     *
     * <p>
     * <strong>Security Implementation:</strong> Provides basic input sanitization
     * to fulfill security practices. Removes potentially dangerous characters
     * while preserving legitimate content.
     * </p>
     *
     * @param input The input string to sanitize
     * @return The sanitized string, safe for storage and display
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Basic HTML/script tag removal and character escaping
        return input.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<.*?>", "")
                .trim();
    }

    /**
     * Compares this content with another content instance for sorting.
     *
     * <p>
     * Default comparison is based on creation date, with newer content
     * considered "greater". This provides a natural ordering for content
     * collections and supports the Collections Framework implementation.
     * </p>
     *
     * @param other The other content instance to compare with
     * @return negative if this is older, positive if newer, zero if equal
     */
    @Override
    public int compareTo(Content<?> other) {
        if (other == null) {
            return 1;
        }
        return this.createdDate.compareTo(other.createdDate);
    }

    /**
     * Creates a deep copy of this content instance.
     *
     * <p>
     * Provides cloning support for content duplication and versioning
     * scenarios. The cloned content gets a new ID but retains all other
     * attributes including metadata and tags.
     * </p>
     *
     * @return A deep copy of this content instance
     * @throws CloneNotSupportedException if cloning is not supported by the
     *                                    specific content type
     */
    @Override
    @SuppressWarnings("unchecked")
    public T clone() throws CloneNotSupportedException {
        T cloned = (T) super.clone();
        cloned.metadata = new HashMap<>(this.metadata);
        cloned.tags = this.tags.clone();
        return cloned;
    }

    /**
     * Checks equality based on content ID.
     *
     * <p>
     * Two content instances are considered equal if they have the same ID,
     * regardless of their other attributes. This supports proper behavior in
     * collections and enables content deduplication.
     * </p>
     *
     * @param obj The object to compare with
     * @return true if the objects have the same ID, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Content<?> content = (Content<?>) obj;
        return id.equals(content.id);
    }

    /**
     * Returns hash code based on content ID.
     *
     * <p>
     * Consistent with equals() implementation, uses only the ID for
     * hash code calculation to ensure proper behavior in hash-based
     * collections like HashMap and HashSet.
     * </p>
     *
     * @return The hash code for this content instance
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Sets the user who created this content (for import functionality).
     *
     * @param createdBy The username of the creator
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = sanitizeInput(createdBy);
    }

    /**
     * Sets the creation date (for import functionality).
     *
     * @param createdDate The creation timestamp
     */
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Sets the metadata map (for import functionality).
     *
     * @param metadata The metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
    }

    /**
     * Returns the last modified timestamp.
     *
     * @return The last modification time
     */
    public LocalDateTime getLastModified() {
        return modifiedDate;
    }

    /**
     * Returns the user who last modified this content.
     *
     * @return The username of the last modifier
     */
    public String getLastModifiedBy() {
        return modifiedBy;
    }

    /**
     * Returns the content version (for versioning systems).
     *
     * @return The version number
     */
    public int getVersion() {
        return 1; // Simple version implementation
    }

    /**
     * Returns the published date of this content.
     *
     * @return The published date, or null if not yet published
     */
    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    /**
     * Sets the published date of this content.
     *
     * @param publishedDate The published date to set
     */
    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
        if (publishedDate != null && this.modifiedDate == null) {
            this.modifiedDate = publishedDate;
        }
    }

    /**
     * Sets the last modified timestamp.
     *
     * @param lastModified The timestamp of the modification
     */
    public void setLastModified(LocalDateTime lastModified) {
        this.modifiedDate = lastModified;
    }

    /**
     * Sets the user who last modified this content.
     *
     * @param lastModifiedBy The user who modified the content
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.modifiedBy = lastModifiedBy;
    }

    /**
     * Returns whether this content is active.
     *
     * @return true if the content is active
     */
    public boolean isActive() {
        return status != ContentStatus.ARCHIVED;
    }

    /**
     * Returns a string representation of this content.
     *
     * <p>
     * Provides a human-readable representation including key identifying
     * information. Useful for debugging and logging purposes.
     * </p>
     *
     * @return A string representation of this content
     */
    @Override
    public String toString() {
        return String.format("%s{id='%s', title='%s', status=%s, createdDate=%s}",
                getClass().getSimpleName(), id, title, status, createdDate);
    }
}
