package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.Role;
import com.cms.core.exception.ContentManagementException;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.util.CMSLogger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Immediate publishing strategy that publishes content without delay.
 *
 * <p>The ImmediatePublishingStrategy implements the Strategy Pattern by providing
 * a concrete algorithm for publishing content immediately upon request. This strategy
 * is designed for content that needs to be made available to users as quickly as
 * possible, bypassing scheduling delays or approval workflows.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as a concrete strategy implementation in the Strategy Pattern. It encapsulates
 * the immediate publishing algorithm, making it interchangeable with other publishing
 * strategies while maintaining the same interface contract.</p>
 *
 * <p><strong>Implementation:</strong> Implements part of the Strategy Pattern
 * implementation by providing a specific publishing algorithm that can be
 * used interchangeably with other strategies. shows proper object-oriented
 * design with encapsulation of algorithm-specific behavior.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Breaking news articles that need immediate publication</li>
 *   <li>Emergency notifications and announcements</li>
 *   <li>Time-sensitive promotional content</li>
 *   <li>Quick updates to existing published content</li>
 *   <li>Manual publishing by authorized users</li>
 * </ul>
 *
 * <p><strong>Strategy Characteristics:</strong></p>
 * <ul>
 *   <li>High Priority (85) - Immediate execution preferred</li>
 *   <li>No Batch Processing - Each item processed individually for speed</li>
 *   <li>Rollback Support - Can unpublish content if needed</li>
 *   <li>Fast Processing Time - Minimal overhead operations</li>
 *   <li>Permission Validation - Ensures user has publish rights</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For context parameter details
 * @see ScheduledPublishingStrategy For time-delayed publishing
 */
public class ImmediatePublishingStrategy implements PublishingStrategy {
    
    /** Strategy name identifier */
    private static final String STRATEGY_NAME = "Immediate Publishing";
    
    /** Priority level for immediate publishing (high priority) */
    private static final int PRIORITY = 85;
    
    /** Estimated processing time in milliseconds */
    private static final long ESTIMATED_PROCESSING_TIME = 500L;
    
    /** Observer subject for publishing events (integration with Observer Pattern) */
    private final ContentSubject contentSubject;
    
    /**
     * Creates a new ImmediatePublishingStrategy.
     * Initializes the content subject for Observer Pattern integration.
     */
    public ImmediatePublishingStrategy() {
        this.contentSubject = new ContentSubject();
    }
    
    /**
     * Creates a new ImmediatePublishingStrategy with the specified content subject.
     *
     * @param contentSubject The content subject for event notifications
     * @throws IllegalArgumentException If contentSubject is null
     */
    public ImmediatePublishingStrategy(ContentSubject contentSubject) {
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        this.contentSubject = contentSubject;
    }
    
    /**
     * Publishes content immediately using this strategy's algorithm.
     *
     * <p>This method implements the core immediate publishing algorithm:</p>
     * <ol>
     *   <li>Validates content and user permissions</li>
     *   <li>Sets content status to PUBLISHED</li>
     *   <li>Updates publication timestamp</li>
     *   <li>Fires Observer Pattern events for notifications</li>
     *   <li>Logs the publishing operation for audit purposes</li>
     * </ol>
     *
     * <p><strong>Strategy-Specific Behavior:</strong> This implementation prioritizes
     * speed over extensive validation, making it suitable for trusted content that
     * needs immediate availability. It bypasses approval workflows and scheduling
     * constraints.</p>
     *
     * @param content The content to be published immediately
     * @param context The publishing context with user and configuration information
     * @throws ContentManagementException If publishing fails due to validation errors,
     *         permission issues, or content state problems. Uses Exception Shielding
     *         to provide user-friendly messages while logging technical details.
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public void publish(Content content, PublishingContext context) throws ContentManagementException {
        // Input validation with Exception Shielding
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Publishing context cannot be null");
        }
        
        try {
            // Log the start of publishing operation
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "immediate_publish_start",
                "Starting immediate publishing for content: " + content.getTitle()
            );
            
            // Pre-publication validation
            if (!validate(content, context)) {
                throw new ContentManagementException(
                    "Content validation failed for immediate publishing",
                    "The content does not meet the implementations for immediate publishing. " +
                    "Please check content status, user permissions, and content completeness."
                );
            }
            
            // Check if content is already published
            if (content.getStatus() == ContentStatus.PUBLISHED) {
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "immediate_publish_already_published",
                    "Content is already published, skipping publication"
                );
                
                // Still fire events for consistency, but with different event type
                firePublishingEvent(content, context, ContentEvent.EventType.CONTENT_REPUBLISHED);
                return;
            }
            
            // Execute the immediate publishing algorithm
            executeImmediatePublishing(content, context);
            
            // Fire Observer Pattern events for notifications
            firePublishingEvent(content, context, ContentEvent.EventType.CONTENT_PUBLISHED);
            
            // Log successful publishing
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "immediate_publish_success",
                String.format("Successfully published content '%s' immediately", content.getTitle())
            );
            
            // Log performance metrics
            CMSLogger.logPerformanceMetric(
                "immediate_publishing_duration",
                ESTIMATED_PROCESSING_TIME,
                "Content ID: " + content.getId()
            );
            
        } catch (ContentManagementException e) {
            // Log the error with technical details for debugging
            CMSLogger.logError(
                "Failed to publish content immediately: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with Exception Shielding - user gets friendly message,
            // technical details are logged but not exposed
            throw new ContentManagementException(
                "Publishing failed due to system error",
                "We were unable to publish your content immediately. " +
                "Please try again or contact support if the problem persists.",
                e
            );
        } catch (Exception e) {
            // Log unexpected errors
            CMSLogger.logError(
                "Unexpected error during immediate publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Wrap in ContentManagementException with Exception Shielding
            throw new ContentManagementException(
                "Unexpected publishing error",
                "An unexpected error occurred during publishing. " +
                "Our technical team has been notified. Please try again later.",
                e
            );
        }
    }
    
    /**
     * Validates whether content can be published immediately using this strategy.
     *
     * <p><strong>Immediate Publishing Validation Rules:</strong></p>
     * <ul>
     *   <li>Content must not be null and have a valid ID</li>
     *   <li>Content must have a title and body (basic completeness check)</li>
     *   <li>User must have PUBLISHER, EDITOR, or ADMINISTRATOR role</li>
     *   <li>Content status must allow publishing (DRAFT, REVIEW, or already PUBLISHED)</li>
     *   <li>Content must not be archived or deleted</li>
     *   <li>If force publishing is disabled, content must pass basic quality checks</li>
     * </ul>
     *
     * @param content The content to validate
     * @param context The publishing context with user information
     * @return true if content can be published immediately, false otherwise
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public boolean validate(Content content, PublishingContext context) {
        if (content == null || context == null) {
            throw new IllegalArgumentException("Content and context cannot be null");
        }
        
        try {
            // Basic content validation
            if (content.getId() == null || content.getId().trim().isEmpty()) {
                CMSLogger.logValidationError("Content ID is missing", context.getUser().getUsername());
                return false;
            }
            
            if (content.getTitle() == null || content.getTitle().trim().isEmpty()) {
                CMSLogger.logValidationError("Content title is missing", context.getUser().getUsername());
                return false;
            }
            
            if (content.getBody() == null || content.getBody().trim().isEmpty()) {
                CMSLogger.logValidationError("Content body is missing", context.getUser().getUsername());
                return false;
            }
            
            // User permission validation
            Role userRole = context.getUser().getRole();
            if (!hasPublishPermission(userRole)) {
                CMSLogger.logSecurityEvent(
                    context.getUser().getUsername(),
                    "unauthorized_publish_attempt",
                    "User attempted immediate publishing without proper permissions"
                );
                return false;
            }
            
            // Content status validation
            ContentStatus status = content.getStatus();
            if (!isValidStatusForPublishing(status)) {
                CMSLogger.logValidationError(
                    "Invalid content status for immediate publishing: " + status,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Additional quality checks if force publishing is disabled
            if (!context.isForcePublish()) {
                if (!performQualityChecks(content, context)) {
                    return false;
                }
            }
            
            // All validations passed
            CMSLogger.logValidationSuccess(
                "Immediate publishing validation passed for content: " + content.getId(),
                context.getUser().getUsername()
            );
            
            return true;
            
        } catch (Exception e) {
            // Log validation errors but don't throw - return false for failed validation
            CMSLogger.logError(
                "Error during immediate publishing validation: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
    
    /**
     * Returns the strategy name for identification purposes.
     * @return "Immediate Publishing"
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Returns the priority level for this strategy.
     * Immediate publishing has high priority (85) for quick execution.
     * @return 85 (high priority)
     */
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    /**
     * Returns a detailed description of this strategy's approach and use cases.
     * @return Detailed strategy description
     */
    @Override
    public String getDescription() {
        return "Publishes content immediately without delay. Ideal for time-sensitive content " +
               "such as breaking news, emergency announcements, or urgent updates. Bypasses " +
               "approval workflows and scheduling constraints to ensure immediate availability. " +
               "Requires appropriate user permissions and basic content validation.";
    }
    
    /**
     * Indicates that this strategy does not support batch processing.
     * Immediate publishing processes each item individually for maximum speed.
     * @return false - no batch processing support
     */
    @Override
    public boolean supportsBatchProcessing() {
        return false;
    }
    
    /**
     * Indicates that this strategy supports rollback operations.
     * Published content can be unpublished if needed.
     * @return true - rollback is supported
     */
    @Override
    public boolean supportsRollback() {
        return true;
    }
    
    /**
     * Returns the estimated processing time for immediate publishing.
     * This is typically very fast as it involves minimal operations.
     * @param content The content to be published (used for size-based estimates)
     * @param context The publishing context (used for complexity estimates)
     * @return Estimated processing time in milliseconds (typically 500ms)
     */
    @Override
    public long getEstimatedProcessingTime(Content content, PublishingContext context) {
        // Base processing time
        long baseTime = ESTIMATED_PROCESSING_TIME;
        
        // Adjust based on content size (rough estimate)
        if (content != null && content.getBody() != null) {
            int contentLength = content.getBody().length();
            if (contentLength > 10000) {
                baseTime += 100; // Extra time for larger content
            }
        }
        
        // Adjust based on notification settings
        if (context != null && context.isSendNotifications()) {
            baseTime += 200; // Time for notification processing
        }
        
        return baseTime;
    }
    
    /**
     * Executes the core immediate publishing algorithm.
     * This method contains the specific logic for immediate content publication.
     *
     * @param content The content to publish
     * @param context The publishing context
     * @throws ContentManagementException If the publishing operation fails
     */
    private void executeImmediatePublishing(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        try {
            // Update content status to published
            content.setStatus(ContentStatus.PUBLISHED, context.getUser().getId());
            
            // Set publication timestamp
            LocalDateTime now = LocalDateTime.now();
            if (content.getPublishedDate() == null) {
                content.setPublishedDate(now);
            }
            
            // Update last modified information
            content.setLastModified(now);
            content.setLastModifiedBy(context.getUser().getUsername());
            
            // If content has metadata, update publication info
            if (content.getMetadata() != null) {
                content.getMetadata().put("published_immediately", true);
                content.getMetadata().put("publication_strategy", STRATEGY_NAME);
                content.getMetadata().put("published_by", context.getUser().getUsername());
                content.getMetadata().put("publication_timestamp", now.toInstant(ZoneOffset.UTC).toEpochMilli());
            }
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "content_status_updated",
                "Content status updated to PUBLISHED"
            );
            
        } catch (Exception e) {
            throw new ContentManagementException(
                "Failed to execute immediate publishing",
                "Unable to update content status and metadata for immediate publishing",
                e
            );
        }
    }
    
    /**
     * Fires Observer Pattern events to notify other components about publishing.
     * Integrates with the existing Observer Pattern implementation.
     *
     * @param content The published content
     * @param context The publishing context
     * @param eventType The type of publishing event
     */
    private void firePublishingEvent(Content content, PublishingContext context, 
                                   ContentEvent.EventType eventType) {
        try {
            // Create content event with publishing details
            ContentEvent event = ContentEvent.builder(eventType, content)
                .user(context.getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("immediate_publishing", true)
                .metadata("environment", context.getEnvironment())
                .metadata("target", context.getTarget())
                .metadata("priority", context.getPriority().name())
                .build();
            
            // Notify observers (integrates with Observer Pattern)
            contentSubject.notifyObservers(event);
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "publishing_event_fired",
                "Observer Pattern event fired: " + eventType.name()
            );
            
        } catch (Exception e) {
            // Don't fail the entire publishing operation if event notification fails
            CMSLogger.logError(
                "Failed to fire publishing event for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
        }
    }
    
    /**
     * Checks if the user role has permission to publish content immediately.
     *
     * @param role The user's role
     * @return true if the role can publish content, false otherwise
     */
    private boolean hasPublishPermission(Role role) {
        return role == Role.ADMINISTRATOR || 
               role == Role.EDITOR || 
               role == Role.PUBLISHER;
    }
    
    /**
     * Checks if the content status allows for immediate publishing.
     *
     * @param status The content status
     * @return true if the status allows publishing, false otherwise
     */
    private boolean isValidStatusForPublishing(ContentStatus status) {
        return status == ContentStatus.DRAFT || 
               status == ContentStatus.REVIEW || 
               status == ContentStatus.PUBLISHED;
    }
    
    /**
     * Performs additional quality checks on content before publishing.
     * These checks are bypassed when force publishing is enabled.
     *
     * @param content The content to check
     * @param context The publishing context
     * @return true if quality checks pass, false otherwise
     */
    private boolean performQualityChecks(Content content, PublishingContext context) {
        try {
            // Check minimum content length
            if (content.getBody().length() < 10) {
                CMSLogger.logValidationError(
                    "Content body too short for quality standards",
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Check for needed metadata if applicable
            if (content.getMetadata() != null) {
                // Add specific quality checks based on content type or implementations
                Object category = content.getMetadata().get("category");
                if (category == null) {
                    CMSLogger.logValidationWarning(
                        "Content missing category metadata",
                        context.getUser().getUsername()
                    );
                    // Warning, but don't fail validation
                }
            }
            
            return true;
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Error during quality checks for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
}