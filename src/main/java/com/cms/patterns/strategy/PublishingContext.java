package com.cms.patterns.strategy;

import com.cms.core.model.User;
import com.cms.util.CMSLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object containing metadata and configuration for publishing operations.
 *
 * <p>The PublishingContext class serves as a data container that provides all the
 * necessary information and configuration for publishing strategies to make informed
 * decisions about how to publish content. It encapsulates user information, timing
 * details, publishing environment settings, and custom properties that guide the
 * strategy execution.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as the context parameter passed to all strategy implementations, providing them
 * with the necessary data to execute their specific algorithms. It enables strategies
 * to make decisions based on user permissions, scheduling implementations, environment
 * configuration, and custom business rules.</p>
 *
 * <p><strong>Implementation:</strong> Part of the Strategy Pattern implementation
 * that provides context data to strategy algorithms. The class shows
 * proper object-oriented design with encapsulation, immutability for critical data,
 * and defensive programming practices.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Immutable core data (user, creation date) for thread safety</li>
 *   <li>Mutable properties map for flexible configuration</li>
 *   <li>Builder pattern support for fluent construction</li>
 *   <li>Validation and input sanitization throughout</li>
 *   <li>Integration with logging framework for audit trails</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For strategies that use this context
 * @see PublishingService For the service that creates contexts
 */
public class PublishingContext {
    
    /** The user initiating the publishing operation (immutable) */
    private final User user;
    
    /** The date when this context was created (immutable) */
    private final Date createdDate;
    
    /** Scheduled date for publishing (null for immediate publishing) */
    private Date scheduledDate;
    
    /** Environment where publishing should occur (e.g., production, staging, preview) */
    private String environment;
    
    /** Publishing target (e.g., web, mobile, api, all) */
    private String target;
    
    /** Priority level for this publishing operation */
    private Priority priority;
    
    /** Whether to force publishing despite warnings */
    private boolean forcePublish;
    
    /** Whether to send notifications about this publishing operation */
    private boolean sendNotifications;
    
    /** Custom properties for strategy-specific configuration (thread-safe) */
    private final Map<String, Object> properties;
    
    /** Tags associated with this publishing context for categorization */
    private final Set<String> tags;
    
    /** Comment or description for this publishing operation */
    private String comment;
    
    /**
     * Priority levels for publishing operations.
     * Higher priority operations may get preferential treatment in scheduling and resource allocation.
     */
    public enum Priority {
        /** Emergency publishing - highest priority, bypass normal validations */
        EMERGENCY(100),
        
        /** High priority - expedited processing */
        HIGH(75),
        
        /** Normal priority - standard processing */
        NORMAL(50),
        
        /** Low priority - can be delayed if necessary */
        LOW(25),
        
        /** Background priority - process when system resources are available */
        BACKGROUND(10);
        
        private final int level;
        
        Priority(int level) {
            this.level = level;
        }
        
        /**
         * Returns the numeric priority level.
         * @return Priority level (10-100 range)
         */
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Creates a new PublishingContext with the specified user.
     *
     * <p>This constructor creates the minimal context needed for publishing
     * operations. Additional configuration should be set using the setter methods
     * or the builder pattern.</p>
     *
     * @param user The user initiating the publishing operation, must not be null
     * @throws IllegalArgumentException If user is null
     */
    public PublishingContext(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        this.user = user;
        this.createdDate = new Date();
        this.properties = new ConcurrentHashMap<>();
        this.tags = ConcurrentHashMap.newKeySet();
        this.priority = Priority.NORMAL;
        this.environment = "production";
        this.target = "web";
        this.sendNotifications = true;
        this.forcePublish = false;
        
        // Log context creation for audit purposes
        CMSLogger.logUserAction(user.getUsername(), 
            "publishing_context_created", 
            "Created new publishing context");
    }
    
    /**
     * Creates a PublishingContext with user and scheduled date.
     *
     * @param user The user initiating the publishing operation
     * @param scheduledDate The date when content should be published (null for immediate)
     * @throws IllegalArgumentException If user is null
     */
    public PublishingContext(User user, Date scheduledDate) {
        this(user);
        this.scheduledDate = scheduledDate != null ? new Date(scheduledDate.getTime()) : null;
    }
    
    /**
     * Returns the user who initiated this publishing operation.
     * @return The user (never null)
     */
    public User getUser() {
        return user;
    }
    
    /**
     * Returns the date when this context was created.
     * @return The creation date (never null)
     */
    public Date getCreatedDate() {
        return new Date(createdDate.getTime()); // Defensive copy
    }
    
    /**
     * Returns the scheduled date for publishing.
     * @return The scheduled date, or null for immediate publishing
     */
    public Date getScheduledDate() {
        return scheduledDate != null ? new Date(scheduledDate.getTime()) : null;
    }
    
    /**
     * Sets the scheduled date for publishing.
     * @param scheduledDate The scheduled date, or null for immediate publishing
     * @return This context for method chaining
     */
    public PublishingContext setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate != null ? new Date(scheduledDate.getTime()) : null;
        return this;
    }
    
    /**
     * Returns the publishing environment.
     * @return The environment (e.g., "production", "staging", "preview")
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Sets the publishing environment.
     * @param environment The target environment
     * @return This context for method chaining
     * @throws IllegalArgumentException If environment is null or empty
     */
    public PublishingContext setEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }
        this.environment = environment.trim().toLowerCase();
        return this;
    }
    
    /**
     * Returns the publishing target.
     * @return The target (e.g., "web", "mobile", "api", "all")
     */
    public String getTarget() {
        return target;
    }
    
    /**
     * Sets the publishing target.
     * @param target The publishing target
     * @return This context for method chaining
     * @throws IllegalArgumentException If target is null or empty
     */
    public PublishingContext setTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Target cannot be null or empty");
        }
        this.target = target.trim().toLowerCase();
        return this;
    }
    
    /**
     * Returns the priority level for this publishing operation.
     * @return The priority level
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority level for this publishing operation.
     * @param priority The priority level
     * @return This context for method chaining
     * @throws IllegalArgumentException If priority is null
     */
    public PublishingContext setPriority(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.priority = priority;
        return this;
    }
    
    /**
     * Returns whether to force publishing despite warnings.
     * @return true to force publishing, false to respect warnings
     */
    public boolean isForcePublish() {
        return forcePublish;
    }
    
    /**
     * Sets whether to force publishing despite warnings.
     * @param forcePublish true to force publishing, false to respect warnings
     * @return This context for method chaining
     */
    public PublishingContext setForcePublish(boolean forcePublish) {
        this.forcePublish = forcePublish;
        return this;
    }
    
    /**
     * Returns whether to send notifications about this publishing operation.
     * @return true to send notifications, false otherwise
     */
    public boolean isSendNotifications() {
        return sendNotifications;
    }
    
    /**
     * Sets whether to send notifications about this publishing operation.
     * @param sendNotifications true to send notifications, false otherwise
     * @return This context for method chaining
     */
    public PublishingContext setSendNotifications(boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
        return this;
    }
    
    /**
     * Returns the comment for this publishing operation.
     * @return The comment, or null if none set
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * Sets a comment describing this publishing operation.
     * @param comment The comment (will be sanitized)
     * @return This context for method chaining
     */
    public PublishingContext setComment(String comment) {
        this.comment = comment != null ? comment.trim() : null;
        return this;
    }
    
    /**
     * Returns a read-only view of the custom properties.
     * @return Unmodifiable map of custom properties
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    /**
     * Sets a custom property for strategy-specific configuration.
     * @param key The property key
     * @param value The property value
     * @return This context for method chaining
     * @throws IllegalArgumentException If key is null or empty
     */
    public PublishingContext setProperty(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key cannot be null or empty");
        }
        properties.put(key.trim(), value);
        return this;
    }
    
    /**
     * Gets a custom property value.
     * @param key The property key
     * @return The property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Gets a custom property value with type casting.
     * @param key The property key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The property value cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Removes a custom property.
     * @param key The property key to remove
     * @return This context for method chaining
     */
    public PublishingContext removeProperty(String key) {
        properties.remove(key);
        return this;
    }
    
    /**
     * Returns a read-only view of the tags associated with this context.
     * @return Unmodifiable set of tags
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }
    
    /**
     * Adds a tag to this publishing context.
     * @param tag The tag to add (will be sanitized and normalized)
     * @return This context for method chaining
     * @throws IllegalArgumentException If tag is null or empty
     */
    public PublishingContext addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        tags.add(tag.trim().toLowerCase());
        return this;
    }
    
    /**
     * Adds multiple tags to this publishing context.
     * @param tagsToAdd Collection of tags to add
     * @return This context for method chaining
     */
    public PublishingContext addTags(Collection<String> tagsToAdd) {
        if (tagsToAdd != null) {
            for (String tag : tagsToAdd) {
                if (tag != null && !tag.trim().isEmpty()) {
                    tags.add(tag.trim().toLowerCase());
                }
            }
        }
        return this;
    }
    
    /**
     * Removes a tag from this publishing context.
     * @param tag The tag to remove
     * @return This context for method chaining
     */
    public PublishingContext removeTag(String tag) {
        tags.remove(tag);
        return this;
    }
    
    /**
     * Checks if this context has a specific tag.
     * @param tag The tag to check for
     * @return true if the tag is present, false otherwise
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    /**
     * Determines if this is scheduled publishing based on the scheduled date.
     * @return true if scheduledDate is set and in the future, false otherwise
     */
    public boolean isScheduledPublishing() {
        return scheduledDate != null && scheduledDate.after(new Date());
    }
    
    /**
     * Determines if this is immediate publishing.
     * @return true if no scheduled date is set or scheduled date is now/past
     */
    public boolean isImmediatePublishing() {
        return !isScheduledPublishing();
    }
    
    /**
     * Creates a new builder for constructing PublishingContext instances.
     * @param user The user for the context
     * @return A new builder instance
     */
    public static Builder builder(User user) {
        return new Builder(user);
    }
    
    /**
     * Builder class for fluent PublishingContext construction.
     */
    public static class Builder {
        private final PublishingContext context;
        
        private Builder(User user) {
            this.context = new PublishingContext(user);
        }
        
        public Builder scheduledDate(Date scheduledDate) {
            context.setScheduledDate(scheduledDate);
            return this;
        }
        
        public Builder environment(String environment) {
            context.setEnvironment(environment);
            return this;
        }
        
        public Builder target(String target) {
            context.setTarget(target);
            return this;
        }
        
        public Builder priority(Priority priority) {
            context.setPriority(priority);
            return this;
        }
        
        public Builder forcePublish(boolean forcePublish) {
            context.setForcePublish(forcePublish);
            return this;
        }
        
        public Builder sendNotifications(boolean sendNotifications) {
            context.setSendNotifications(sendNotifications);
            return this;
        }
        
        public Builder comment(String comment) {
            context.setComment(comment);
            return this;
        }
        
        public Builder property(String key, Object value) {
            context.setProperty(key, value);
            return this;
        }
        
        public Builder tag(String tag) {
            context.addTag(tag);
            return this;
        }
        
        public Builder tags(Collection<String> tags) {
            context.addTags(tags);
            return this;
        }
        
        public PublishingContext build() {
            return context;
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PublishingContext{user='%s', environment='%s', target='%s', priority='%s', " +
            "scheduled=%s, forcePublish=%s, notifications=%s, properties=%d, tags=%s}",
            user.getUsername(), environment, target, priority.name(),
            scheduledDate != null ? scheduledDate : "immediate",
            forcePublish, sendNotifications, properties.size(), tags
        );
    }
}