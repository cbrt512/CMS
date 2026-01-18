package com.cms.patterns.observer;

import com.cms.core.model.Content;
import com.cms.core.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable event object containing comprehensive details about content
 * lifecycle events.
 *
 * <p>
 * This class represents a content management event with all necessary context
 * information for observers to make informed decisions about how to react to
 * the event. It follows the immutable object pattern for thread safety and
 * includes extensive metadata for flexible observer implementations.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Event object
 * that carries all necessary information about content changes to interested
 * observers. Immutable design ensures thread safety in concurrent environments.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Part of Observer Pattern implementation
 * that provides rich event data for content lifecycle notifications, enabling
 * sophisticated event-driven architectures with comprehensive context.
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is immutable and thread-safe.
 * All collections are defensively copied and made unmodifiable. The event
 * object can be safely shared across multiple threads without synchronization.
 * </p>
 *
 * <p>
 * <strong>Integration:</strong> Uses existing Content and User models from
 * the core system, integrating seamlessly with Factory Pattern content
 * creation and User management systems.
 * </p>
 *
 * @see ContentObserver For observers that receive these events
 * @see ContentSubject For the subject that generates these events
 * @since 1.0
 * @author Otman Hmich S007924
 */
public final class ContentEvent {

    /**
     * Enumeration of possible content event types for type-safe event handling.
     */
    public enum EventType {
        CREATED("Content Created", "New content has been created"),
        UPDATED("Content Updated", "Existing content has been modified"),
        PUBLISHED("Content Published", "Content has been made public"),
        DELETED("Content Deleted", "Content has been removed or archived"),
        STATUS_CHANGED("Status Changed", "Content status has been modified"),
        METADATA_UPDATED("Metadata Updated", "Content metadata has been updated"),
        QUERIED("Content Queried", "Content has been queried or filtered"),
        REPUBLISHED("Content Republished", "Content has been republished"),
        REJECTED("Content Rejected", "Content has been rejected during review"),
        REVIEWED("Content Reviewed", "Content has been reviewed"),
        SUBMITTED_FOR_REVIEW("Content Submitted for Review", "Content has been submitted for review"),
        CONTENT_PUBLISHED("Content Published", "Content has been published to public"),
        CONTENT_PROCESSING_COMPLETED("Processing Completed", "Content processing has been completed"),
        CONTENT_ERROR("Content Error", "An error occurred during content processing"),
        CONTENT_REPUBLISHED("Content Republished", "Content has been republished after modifications"),
        CONTENT_SUBMITTED_FOR_REVIEW("Content Submitted for Review", "Content has been submitted for review process"),
        CONTENT_REJECTED("Content Rejected", "Content has been rejected during review"),
        CONTENT_REVIEWED("Content Reviewed", "Content review has been completed"),
        CONTENT_SCHEDULED("Content Scheduled", "Content has been scheduled for future publishing");

        private final String displayName;
        private final String description;

        EventType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Core event data (immutable)
    private final String eventId;
    private final Content content;
    private final EventType eventType;
    private final LocalDateTime timestamp;
    private final User user;

    // Event context and metadata
    private final String source;
    private final String sessionId;
    private final Map<String, Object> metadata;
    private final Map<String, Object> previousValues;
    private final Set<String> changedFields;

    // Additional context
    private final String reason;
    private final LocalDateTime publicationDate;
    private final boolean isTemporaryDeletion;
    private final Map<String, String> additionalContext;

    /**
     * Private constructor to ensure immutability and proper validation.
     * Use the Builder class to create instances.
     */
    private ContentEvent(Builder builder) {
        // Validate needed fields
        if (builder.content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (builder.eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (builder.user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Generate unique event ID if not provided
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID().toString();

        // Set core fields
        this.content = builder.content;
        this.eventType = builder.eventType;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.user = builder.user;

        // Set context fields with defaults
        this.source = builder.source != null ? builder.source : "CMS-Core";
        this.sessionId = builder.sessionId;
        this.reason = builder.reason;
        this.publicationDate = builder.publicationDate;
        this.isTemporaryDeletion = builder.isTemporaryDeletion;

        // Defensively copy collections to ensure immutability
        this.metadata = builder.metadata != null
                ? Collections.unmodifiableMap(new ConcurrentHashMap<>(builder.metadata))
                : Collections.emptyMap();

        this.previousValues = builder.previousValues != null
                ? Collections.unmodifiableMap(new ConcurrentHashMap<>(builder.previousValues))
                : Collections.emptyMap();

        this.changedFields = builder.changedFields != null
                ? Collections.unmodifiableSet(new HashSet<>(builder.changedFields))
                : Collections.emptySet();

        this.additionalContext = builder.additionalContext != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.additionalContext))
                : Collections.emptyMap();
    }

    // Getter methods for all fields

    /**
     * Gets the unique identifier for this event.
     * 
     * @return Non-null unique event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the content that this event relates to.
     * 
     * @return Non-null content object
     */
    public Content getContent() {
        return content;
    }

    /**
     * Gets the type of this event.
     * 
     * @return Non-null event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Gets the timestamp when this event occurred.
     * 
     * @return Non-null timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the user who triggered this event.
     * 
     * @return Non-null user object
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the source system or component that generated this event.
     * 
     * @return Non-null source identifier
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the session ID associated with this event, if applicable.
     * 
     * @return Session ID or null if not available
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the immutable metadata map for this event.
     * 
     * @return Non-null, unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets the previous values of changed fields, if applicable.
     * 
     * @return Non-null, unmodifiable map of previous values
     */
    public Map<String, Object> getPreviousValues() {
        return previousValues;
    }

    /**
     * Gets the set of field names that were changed in this event.
     * 
     * @return Non-null, unmodifiable set of changed field names
     */
    public Set<String> getChangedFields() {
        return changedFields;
    }

    /**
     * Gets the reason for this event, if provided.
     * 
     * @return Reason string or null if not provided
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the publication date for publication events.
     * 
     * @return Publication date or null for non-publication events
     */
    public LocalDateTime getPublicationDate() {
        return publicationDate;
    }

    /**
     * Indicates whether a deletion event is temporary (archived) or permanent.
     * 
     * @return true if deletion is temporary (can be restored), false if permanent
     */
    public boolean isTemporaryDeletion() {
        return isTemporaryDeletion;
    }

    /**
     * Gets additional context information for this event.
     * 
     * @return Non-null, unmodifiable map of additional context
     */
    public Map<String, String> getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Gets a specific metadata value by key.
     * 
     * @param key The metadata key
     * @return The metadata value or null if not present
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Gets a specific previous value by field name.
     * 
     * @param fieldName The name of the changed field
     * @return The previous value or null if not available
     */
    public Object getPreviousValue(String fieldName) {
        return previousValues.get(fieldName);
    }

    /**
     * Checks if a specific field was changed in this event.
     * 
     * @param fieldName The field name to check
     * @return true if the field was changed, false otherwise
     */
    public boolean wasFieldChanged(String fieldName) {
        return changedFields.contains(fieldName);
    }

    /**
     * Gets the deletion reason for deletion events.
     * 
     * @return Deletion reason or null for non-deletion events
     */
    public String getDeletionReason() {
        return eventType == EventType.DELETED ? reason : null;
    }

    @Override
    public String toString() {
        return String.format("ContentEvent{id='%s', type=%s, content='%s', user='%s', timestamp=%s}",
                eventId, eventType, content.getTitle(), user.getUsername(),
                timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContentEvent that = (ContentEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Builder class for creating ContentEvent instances with fluent API.
     *
     * <p>
     * This builder provides a flexible way to construct ContentEvent objects
     * with optional parameters while ensuring all needed fields are set.
     * </p>
     *
     * <p>
     * <strong>Usage Example:</strong>
     * </p>
     * 
     * <pre>
     * ContentEvent event = ContentEvent.builder()
     *         .content(myContent)
     *         .eventType(EventType.CREATED)
     *         .user(currentUser)
     *         .source("ContentFactory")
     *         .addMetadata("importSource", "CSV")
     *         .build();
     * </pre>
     */
    public static class Builder {
        private String eventId;
        private Content content;
        private EventType eventType;
        private LocalDateTime timestamp;
        private User user;
        private String source;
        private String sessionId;
        private Map<String, Object> metadata;
        private Map<String, Object> previousValues;
        private Set<String> changedFields;
        private String reason;
        private LocalDateTime publicationDate;
        private boolean isTemporaryDeletion = false;
        private Map<String, String> additionalContext;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder content(Content content) {
            this.content = content;
            return this;
        }

        public Builder eventType(EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder publicationDate(LocalDateTime publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public Builder temporaryDeletion(boolean isTemporary) {
            this.isTemporaryDeletion = isTemporary;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder metadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new ConcurrentHashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new ConcurrentHashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder previousValues(Map<String, Object> previousValues) {
            this.previousValues = previousValues;
            return this;
        }

        public Builder addPreviousValue(String field, Object value) {
            if (this.previousValues == null) {
                this.previousValues = new ConcurrentHashMap<>();
            }
            this.previousValues.put(field, value);
            return this;
        }

        public Builder changedFields(Set<String> changedFields) {
            this.changedFields = changedFields;
            return this;
        }

        public Builder addChangedField(String fieldName) {
            if (this.changedFields == null) {
                this.changedFields = new HashSet<>();
            }
            this.changedFields.add(fieldName);
            return this;
        }

        public Builder additionalContext(Map<String, String> additionalContext) {
            this.additionalContext = additionalContext;
            return this;
        }

        public Builder addContext(String key, String value) {
            if (this.additionalContext == null) {
                this.additionalContext = new HashMap<>();
            }
            this.additionalContext.put(key, value);
            return this;
        }

        public ContentEvent build() {
            return new ContentEvent(this);
        }
    }

    /**
     * Creates a new builder for constructing ContentEvent instances.
     * 
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder instance with pre-set event type and content.
     * This overload provides convenience for common usage patterns.
     *
     * @param eventType The type of event
     * @param content   The content associated with the event
     * @return A new Builder instance with eventType and content pre-configured
     */
    public static Builder builder(EventType eventType, Content content) {
        return new Builder()
                .eventType(eventType)
                .content(content);
    }

    /**
     * Factory method for creating a content creation event.
     * 
     * @param content The created content
     * @param user    The user who created the content
     * @return A new ContentEvent for content creation
     */
    public static ContentEvent contentCreated(Content content, User user) {
        return builder()
                .content(content)
                .eventType(EventType.CREATED)
                .user(user)
                .source("ContentFactory")
                .build();
    }

    /**
     * Factory method for creating a content update event.
     * 
     * @param content       The updated content
     * @param user          The user who updated the content
     * @param changedFields Set of field names that were changed
     * @return A new ContentEvent for content update
     */
    public static ContentEvent contentUpdated(Content content, User user, Set<String> changedFields) {
        return builder()
                .content(content)
                .eventType(EventType.UPDATED)
                .user(user)
                .changedFields(changedFields)
                .build();
    }

    /**
     * Factory method for creating a content publication event.
     * 
     * @param content         The published content
     * @param user            The user who published the content
     * @param publicationDate When the content was published
     * @return A new ContentEvent for content publication
     */
    public static ContentEvent contentPublished(Content content, User user, LocalDateTime publicationDate) {
        return builder()
                .content(content)
                .eventType(EventType.PUBLISHED)
                .user(user)
                .publicationDate(publicationDate)
                .build();
    }

    /**
     * Factory method for creating a content deletion event.
     * 
     * @param content     The deleted content
     * @param user        The user who deleted the content
     * @param reason      The reason for deletion
     * @param isTemporary Whether the deletion is temporary (archived)
     * @return A new ContentEvent for content deletion
     */
    public static ContentEvent contentDeleted(Content content, User user, String reason, boolean isTemporary) {
        return builder()
                .content(content)
                .eventType(EventType.DELETED)
                .user(user)
                .reason(reason)
                .temporaryDeletion(isTemporary)
                .build();
    }
}
