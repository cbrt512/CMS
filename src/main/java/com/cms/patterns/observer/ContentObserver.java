package com.cms.patterns.observer;

/**
 * Observer interface for content management events in the CMS system.
 *
 * <p>
 * This interface defines the contract for observers that want to be notified
 * about various content lifecycle events. It implements the Observer pattern
 * to enable event-driven architecture and loose coupling between content
 * management operations and their side effects.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Observer Pattern - Observer interface
 * that defines the notification methods for content events. This enables
 * multiple observers to react to content changes without tight coupling.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Provides Observer Pattern
 * for event-driven content notifications, enabling reactive architecture for
 * cache invalidation, search indexing, notifications, and audit logging.
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li>Content lifecycle notifications (create, update, publish, delete)</li>
 * <li>Cache invalidation when content changes</li>
 * <li>Search index updates for content discoverability</li>
 * <li>Email notifications for content changes</li>
 * <li>Audit trail generation for compliance</li>
 * <li>Performance monitoring and analytics</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Integration:</strong> Works seamlessly with existing Content model
 * classes from Factory Pattern, Site hierarchy from Composite Pattern, and
 * uses Exception Shielding for error handling during notification processing.
 * </p>
 *
 * @see ContentEvent For event data structure
 * @see ContentSubject For subject that notifies observers
 * @see ContentNotificationService For concrete implementation
 * @since 1.0
 * @author Otman Hmich S007924
 */
public interface ContentObserver {

    /**
     * Notifies observer when new content is created.
     *
     * <p>
     * This method is called whenever new content is created through the
     * ContentFactory or imported through the ContentImporter. Observers can
     * implement this to trigger side effects like cache warming, search
     * indexing, or notification sending.
     * </p>
     *
     * <p>
     * <strong>Thread Safety:</strong> Implementations must be thread-safe
     * as this method may be called concurrently from multiple threads.
     * </p>
     *
     * @param event The content creation event containing details about the
     *              newly created content, creator information, and metadata
     * @throws IllegalArgumentException if event is null
     * @see ContentEvent#getContent() For accessing the created content
     * @see ContentEvent#getUser() For accessing the creator information
     */
    void onContentCreated(ContentEvent event);

    /**
     * Notifies observer when existing content is updated.
     *
     * <p>
     * This method is called whenever content properties, metadata, or status
     * are modified. This is crucial for maintaining consistency in derived
     * systems like caches, search indices, and generating change notifications.
     * </p>
     *
     * <p>
     * <strong>Performance Consideration:</strong> Since content updates may
     * be frequent, implementations should be efficient and consider batching
     * operations where possible.
     * </p>
     *
     * @param event The content update event containing both old and new content
     *              states, modifier information, and change details
     * @throws IllegalArgumentException if event is null
     * @see ContentEvent#getChangedFields() For accessing what changed
     * @see ContentEvent#getPreviousValue() For accessing old values
     */
    void onContentUpdated(ContentEvent event);

    /**
     * Notifies observer when content is published or its publication status
     * changes.
     *
     * <p>
     * This method is called when content transitions from draft to published
     * state, or when publication settings are modified. This is critical for
     * systems that need to react to content becoming publicly available.
     * </p>
     *
     * <p>
     * <strong>Use Cases:</strong> Cache invalidation, search index updates,
     * RSS feed generation, sitemap updates, and stakeholder notifications.
     * </p>
     *
     * @param event The content publication event containing publication details,
     *              timing information, and publication context
     * @throws IllegalArgumentException if event is null
     * @see ContentEvent#getPublicationDate() For accessing when content went live
     * @see ContentEvent#getMetadata() For accessing publication settings
     */
    void onContentPublished(ContentEvent event);

    /**
     * Notifies observer when content is deleted or archived.
     *
     * <p>
     * This method is called when content is removed from the system or
     * moved to archived state. Observers can implement cleanup operations,
     * remove stale references, and log deletion events for audit purposes.
     * </p>
     *
     * <p>
     * <strong>Important:</strong> This may be the last chance to access
     * content data before it's permanently removed, so observers should
     * handle any necessary cleanup or archival operations.
     * </p>
     *
     * @param event The content deletion event containing the deleted content,
     *              deletion reason, and cleanup instructions
     * @throws IllegalArgumentException if event is null
     * @see ContentEvent#getDeletionReason() For understanding why content was
     *      deleted
     * @see ContentEvent#isTemporaryDeletion() For distinguishing archive vs
     *      permanent delete
     */
    void onContentDeleted(ContentEvent event);

    /**
     * Gets a human-readable name for this observer implementation.
     *
     * <p>
     * This method provides identification for logging, debugging, and
     * administrative purposes. The name should be descriptive enough to
     * identify the observer's purpose in system logs.
     * </p>
     *
     * <p>
     * <strong>Naming Convention:</strong> Use descriptive names like
     * "Email Notification Service", "Cache Invalidation Observer", or
     * "Search Index Updater" rather than technical class names.
     * </p>
     *
     * @return A non-null, non-empty descriptive name for this observer
     */
    default String getObserverName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Indicates whether this observer should receive events for the given content
     * type.
     *
     * <p>
     * This method allows observers to filter events based on content type,
     * reducing unnecessary processing for events they don't handle. Default
     * implementation accepts all content types.
     * </p>
     *
     * <p>
     * <strong>Performance Optimization:</strong> Implementing this method
     * can significantly improve system performance by avoiding unnecessary
     * event processing for irrelevant content types.
     * </p>
     *
     * @param contentType The class of content being processed
     * @return true if this observer wants to receive events for the given
     *         content type, false otherwise
     */
    default boolean shouldObserve(Class<?> contentType) {
        return true; // Default: observe all content types
    }

    /**
     * Gets the priority level for this observer.
     *
     * <p>
     * Higher priority observers are notified first, allowing critical
     * operations like audit logging to complete before less critical
     * operations like email notifications. Priority ranges from 1 (highest)
     * to Integer.MAX_VALUE (lowest).
     * </p>
     *
     * <p>
     * <strong>Priority Guidelines:</strong>
     * <ul>
     * <li>1-10: Critical system operations (audit, security)</li>
     * <li>11-50: Important operations (cache, search index)</li>
     * <li>51-100: Normal operations (notifications, analytics)</li>
     * <li>101+: Low priority operations (reports, cleanup)</li>
     * </ul>
     * </p>
     *
     * @return The priority level for this observer (lower numbers = higher
     *         priority)
     */
    default int getPriority() {
        return 50; // Default: normal priority
    }
}
