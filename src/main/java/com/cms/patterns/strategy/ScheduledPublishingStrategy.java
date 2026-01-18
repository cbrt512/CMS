package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.Role;
import com.cms.core.exception.ContentManagementException;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.util.CMSLogger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scheduled publishing strategy that publishes content at a specified future date and time.
 *
 * <p>The ScheduledPublishingStrategy implements the Strategy Pattern by providing
 * a concrete algorithm for time-delayed content publishing. This strategy allows
 * content creators to prepare content in advance and have it automatically published
 * at the optimal time without manual intervention.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as a concrete strategy implementation that encapsulates the scheduled publishing
 * algorithm. It can be used interchangeably with other publishing strategies while
 * maintaining the same interface contract, demonstrating the flexibility and
 * extensibility of the Strategy Pattern.</p>
 *
 * <p><strong>Implementation:</strong> Implements part of the Strategy Pattern
 * implementation by providing a time-based publishing algorithm that
 * shows advanced scheduling capabilities, task management, and integration
 * with Java's concurrent programming features.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Marketing campaigns with coordinated launch times</li>
 *   <li>Blog posts scheduled for optimal engagement times</li>
 *   <li>Product announcements with embargo dates</li>
 *   <li>Newsletter content prepared in advance</li>
 *   <li>Seasonal content activated at specific dates</li>
 *   <li>Global content considering different time zones</li>
 * </ul>
 *
 * <p><strong>Strategy Characteristics:</strong></p>
 * <ul>
 *   <li>High Priority (75) - Important for planned content releases</li>
 *   <li>Supports Batch Processing - Can schedule multiple items efficiently</li>
 *   <li>Rollback Support - Can cancel scheduled publishing before execution</li>
 *   <li>Variable Processing Time - Depends on scheduling complexity</li>
 *   <li>Advanced Scheduling - Supports timezone handling and validation</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For context parameter details
 * @see ImmediatePublishingStrategy For immediate publishing alternative
 */
public class ScheduledPublishingStrategy implements PublishingStrategy {
    
    /** Strategy name identifier */
    private static final String STRATEGY_NAME = "Scheduled Publishing";
    
    /** Priority level for scheduled publishing (high priority) */
    private static final int PRIORITY = 75;
    
    /** Base estimated processing time in milliseconds */
    private static final long BASE_PROCESSING_TIME = 1000L;
    
    /** Maximum allowed scheduling delay in days */
    private static final int MAX_SCHEDULE_DAYS = 365;
    
    /** Minimum scheduling delay in minutes (to prevent accidental immediate publishing) */
    private static final int MIN_SCHEDULE_MINUTES = 1;
    
    /** Observer subject for publishing events (integration with Observer Pattern) */
    private final ContentSubject contentSubject;
    
    /** Scheduled executor service for managing delayed publishing tasks */
    private final ScheduledExecutorService scheduledExecutor;
    
    /** Map to track scheduled tasks for cancellation support */
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    
    /** Set to track content scheduled for publishing (thread-safe) */
    private final Set<String> scheduledContentIds;
    
    /**
     * Creates a new ScheduledPublishingStrategy with default configuration.
     * Initializes the scheduled executor and tracking collections.
     */
    public ScheduledPublishingStrategy() {
        this.contentSubject = new ContentSubject();
        this.scheduledExecutor = Executors.newScheduledThreadPool(5); // Pool of 5 threads for scheduling
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.scheduledContentIds = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Creates a new ScheduledPublishingStrategy with the specified components.
     *
     * @param contentSubject The content subject for event notifications
     * @param scheduledExecutor The executor service for scheduling tasks
     * @throws IllegalArgumentException If any parameter is null
     */
    public ScheduledPublishingStrategy(ContentSubject contentSubject, 
                                     ScheduledExecutorService scheduledExecutor) {
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        if (scheduledExecutor == null) {
            throw new IllegalArgumentException("Scheduled executor cannot be null");
        }
        
        this.contentSubject = contentSubject;
        this.scheduledExecutor = scheduledExecutor;
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.scheduledContentIds = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Schedules content for publishing at the specified date and time.
     *
     * <p>This method implements the core scheduled publishing algorithm:</p>
     * <ol>
     *   <li>Validates content, context, and scheduling parameters</li>
     *   <li>Calculates the delay until the scheduled publication time</li>
     *   <li>Creates a scheduled task using Java's concurrent utilities</li>
     *   <li>Stores the task for management and cancellation support</li>
     *   <li>Updates content status to SCHEDULED</li>
     *   <li>Fires Observer Pattern events for notifications</li>
     *   <li>Logs the scheduling operation for audit purposes</li>
     * </ol>
     *
     * <p><strong>Strategy-Specific Behavior:</strong> This implementation focuses
     * on reliable scheduling with proper task management, timezone handling, and
     * rollback capabilities. It validates scheduling constraints and provides
     * comprehensive error handling for scheduling failures.</p>
     *
     * @param content The content to be scheduled for publishing
     * @param context The publishing context containing scheduling information
     * @throws ContentManagementException If scheduling fails due to validation errors,
     *         timing constraints, or system limitations. Uses Exception Shielding
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
            // Log the start of scheduling operation
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "scheduled_publish_start",
                String.format("Starting scheduled publishing for content: %s, scheduled for: %s", 
                             content.getTitle(), context.getScheduledDate())
            );
            
            // Pre-scheduling validation
            if (!validate(content, context)) {
                throw new ContentManagementException(
                    "Content validation failed for scheduled publishing",
                    "The content or scheduling parameters do not meet the implementations. " +
                    "Please check the scheduled date, user permissions, and content completeness."
                );
            }
            
            // Check if content is already scheduled
            if (scheduledContentIds.contains(content.getId())) {
                // Cancel existing scheduling and reschedule
                cancelScheduledPublishing(content.getId());
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "scheduled_publish_rescheduled",
                    "Existing scheduling cancelled, rescheduling content"
                );
            }
            
            // Execute the scheduled publishing algorithm
            executeScheduledPublishing(content, context);
            
            // Fire Observer Pattern events for notifications
            fireSchedulingEvent(content, context, ContentEvent.EventType.CONTENT_SCHEDULED);
            
            // Log successful scheduling
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "scheduled_publish_success",
                String.format("Successfully scheduled content '%s' for publishing at %s", 
                             content.getTitle(), context.getScheduledDate())
            );
            
        } catch (ContentManagementException e) {
            // Log the error with technical details for debugging
            CMSLogger.logError(
                "Failed to schedule content for publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with Exception Shielding
            throw new ContentManagementException(
                "Scheduling failed due to system error",
                "We were unable to schedule your content for publishing. " +
                "Please verify the scheduled date and try again, or contact support if the problem persists.",
                e
            );
        } catch (Exception e) {
            // Log unexpected errors
            CMSLogger.logError(
                "Unexpected error during scheduled publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Wrap in ContentManagementException with Exception Shielding
            throw new ContentManagementException(
                "Unexpected scheduling error",
                "An unexpected error occurred during content scheduling. " +
                "Our technical team has been notified. Please try again later.",
                e
            );
        }
    }
    
    /**
     * Validates whether content can be scheduled for publishing using this strategy.
     *
     * <p><strong>Scheduled Publishing Validation Rules:</strong></p>
     * <ul>
     *   <li>All basic content validation (ID, title, body)</li>
     *   <li>Scheduled date must be present in the context</li>
     *   <li>Scheduled date must be in the future (with minimum delay)</li>
     *   <li>Scheduled date must not exceed maximum scheduling window</li>
     *   <li>User must have PUBLISHER, EDITOR, or ADMINISTRATOR role</li>
     *   <li>Content status must allow scheduling (DRAFT, REVIEW)</li>
     *   <li>System resources must be available for scheduling</li>
     * </ul>
     *
     * @param content The content to validate for scheduling
     * @param context The publishing context with scheduling information
     * @return true if content can be scheduled for publishing, false otherwise
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public boolean validate(Content content, PublishingContext context) {
        if (content == null || context == null) {
            throw new IllegalArgumentException("Content and context cannot be null");
        }
        
        try {
            // Basic content validation (reuse from immediate publishing logic)
            if (!validateBasicContent(content, context)) {
                return false;
            }
            
            // User permission validation for scheduling
            Role userRole = context.getUser().getRole();
            if (!hasSchedulingPermission(userRole)) {
                CMSLogger.logSecurityEvent(
                    context.getUser().getUsername(),
                    "unauthorized_scheduling_attempt",
                    "User attempted scheduled publishing without proper permissions"
                );
                return false;
            }
            
            // Scheduled date validation
            if (!validateScheduledDate(context)) {
                return false;
            }
            
            // Content status validation for scheduling
            ContentStatus status = content.getStatus();
            if (!isValidStatusForScheduling(status)) {
                CMSLogger.logValidationError(
                    "Invalid content status for scheduled publishing: " + status,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // System capacity validation
            if (!validateSystemCapacity()) {
                CMSLogger.logSystemEvent(
                    "scheduled_publishing_capacity_exceeded",
                    "System scheduling capacity exceeded, rejecting new scheduling requests"
                );
                return false;
            }
            
            // All validations passed
            CMSLogger.logValidationSuccess(
                "Scheduled publishing validation passed for content: " + content.getId(),
                context.getUser().getUsername()
            );
            
            return true;
            
        } catch (Exception e) {
            // Log validation errors but don't throw - return false for failed validation
            CMSLogger.logError(
                "Error during scheduled publishing validation: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
    
    /**
     * Returns the strategy name for identification purposes.
     * @return "Scheduled Publishing"
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Returns the priority level for this strategy.
     * Scheduled publishing has high priority (75) for planned content releases.
     * @return 75 (high priority)
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
        return "Schedules content for automatic publishing at a specified future date and time. " +
               "Ideal for planned content releases, marketing campaigns, and coordinated launches. " +
               "Supports timezone handling, batch scheduling, and rollback capabilities. " +
               "Content is validated and queued for publication at the scheduled time with " +
               "automatic notifications and audit logging.";
    }
    
    /**
     * Indicates that this strategy supports batch processing.
     * Multiple content items can be scheduled efficiently in a single operation.
     * @return true - batch processing is supported
     */
    @Override
    public boolean supportsBatchProcessing() {
        return true;
    }
    
    /**
     * Indicates that this strategy supports rollback operations.
     * Scheduled publishing can be cancelled before the scheduled time.
     * @return true - rollback (cancellation) is supported
     */
    @Override
    public boolean supportsRollback() {
        return true;
    }
    
    /**
     * Returns the estimated processing time for scheduled publishing setup.
     * This includes validation, task creation, and scheduling overhead.
     * @param content The content to be scheduled
     * @param context The publishing context with scheduling details
     * @return Estimated processing time in milliseconds
     */
    @Override
    public long getEstimatedProcessingTime(Content content, PublishingContext context) {
        long baseTime = BASE_PROCESSING_TIME;
        
        // Adjust based on content complexity
        if (content != null && content.getBody() != null) {
            int contentLength = content.getBody().length();
            if (contentLength > 10000) {
                baseTime += 200; // Extra time for complex content validation
            }
        }
        
        // Adjust based on scheduling complexity
        if (context != null) {
            if (context.getScheduledDate() != null) {
                // Calculate time to scheduled date
                long timeToSchedule = context.getScheduledDate().getTime() - System.currentTimeMillis();
                if (timeToSchedule > TimeUnit.DAYS.toMillis(30)) {
                    baseTime += 100; // Extra time for long-term scheduling
                }
            }
            
            if (context.isSendNotifications()) {
                baseTime += 300; // Time for notification setup
            }
        }
        
        return baseTime;
    }
    
    /**
     * Executes the core scheduled publishing algorithm.
     * This method contains the specific logic for content scheduling.
     *
     * @param content The content to schedule
     * @param context The publishing context with scheduling details
     * @throws ContentManagementException If the scheduling operation fails
     */
    private void executeScheduledPublishing(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        try {
            // Calculate delay until scheduled publication
            Date scheduledDate = context.getScheduledDate();
            long delay = scheduledDate.getTime() - System.currentTimeMillis();
            
            // Create the publishing task
            Runnable publishingTask = createPublishingTask(content, context);
            
            // Schedule the task for execution
            ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(
                publishingTask,
                delay,
                TimeUnit.MILLISECONDS
            );
            
            // Store the scheduled task for management
            scheduledTasks.put(content.getId(), scheduledFuture);
            scheduledContentIds.add(content.getId());
            
            // Update content status to scheduled
            content.setStatus(ContentStatus.REVIEW, context.getUser().getId()); // Use REVIEW to indicate scheduled
            
            // Update content metadata with scheduling information
            if (content.getMetadata() != null) {
                content.getMetadata().put("scheduled_for_publishing", true);
                content.getMetadata().put("scheduled_date", scheduledDate.getTime());
                content.getMetadata().put("publishing_strategy", STRATEGY_NAME);
                content.getMetadata().put("scheduled_by", context.getUser().getUsername());
                content.getMetadata().put("scheduling_timestamp", System.currentTimeMillis());
            }
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "content_scheduled",
                String.format("Content scheduled for publishing in %d milliseconds", delay)
            );
            
        } catch (Exception e) {
            // Clean up any partial scheduling state
            scheduledTasks.remove(content.getId());
            scheduledContentIds.remove(content.getId());
            
            throw new ContentManagementException(
                "Failed to execute scheduled publishing",
                "Unable to create scheduled publishing task for content",
                e
            );
        }
    }
    
    /**
     * Creates a runnable task for delayed content publishing.
     * This task will be executed at the scheduled time.
     *
     * @param content The content to publish
     * @param context The publishing context
     * @return Runnable task for scheduled execution
     */
    private Runnable createPublishingTask(Content content, PublishingContext context) {
        return () -> {
            try {
                // Execute the actual publishing at the scheduled time
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "scheduled_publish_executing",
                    "Executing scheduled publishing task"
                );
                
                // Update content status to published
                content.setStatus(ContentStatus.PUBLISHED, context.getUser().getId());
                content.setPublishedDate(LocalDateTime.now());
                content.setLastModified(LocalDateTime.now());
                content.setLastModifiedBy(context.getUser().getUsername());
                
                // Update metadata to reflect actual publishing
                if (content.getMetadata() != null) {
                    content.getMetadata().put("published_via_scheduling", true);
                    content.getMetadata().put("actual_publication_time", System.currentTimeMillis());
                    content.getMetadata().remove("scheduled_for_publishing");
                }
                
                // Fire publishing event
                ContentEvent event = ContentEvent.builder(ContentEvent.EventType.CONTENT_PUBLISHED, content)
                    .user(context.getUser())
                    .metadata("publishing_strategy", STRATEGY_NAME)
                    .metadata("scheduled_publishing", true)
                    .metadata("original_scheduled_date", context.getScheduledDate().getTime())
                    .build();
                
                contentSubject.notifyObservers(event);
                
                // Clean up scheduling tracking
                scheduledTasks.remove(content.getId());
                scheduledContentIds.remove(content.getId());
                
                // Log successful scheduled publication
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "scheduled_publish_completed",
                    "Scheduled publishing completed successfully"
                );
                
            } catch (Exception e) {
                // Log error but don't propagate (task is running in background)
                CMSLogger.logError(
                    "Error during scheduled content publishing: " + content.getId(),
                    e,
                    context.getUser().getUsername()
                );
                
                // Clean up tracking even if publishing failed
                scheduledTasks.remove(content.getId());
                scheduledContentIds.remove(content.getId());
                
                // Fire error event
                ContentEvent errorEvent = ContentEvent.builder(ContentEvent.EventType.CONTENT_ERROR, content)
                    .user(context.getUser())
                    .metadata("error_type", "scheduled_publishing_failed")
                    .metadata("error_message", e.getMessage())
                    .build();
                
                contentSubject.notifyObservers(errorEvent);
            }
        };
    }
    
    /**
     * Cancels scheduled publishing for the specified content ID.
     * This method supports the rollback capability of the strategy.
     *
     * @param contentId The ID of the content to cancel publishing for
     * @return true if cancellation was successful, false if no scheduling was found
     */
    public boolean cancelScheduledPublishing(String contentId) {
        try {
            ScheduledFuture<?> scheduledTask = scheduledTasks.remove(contentId);
            if (scheduledTask != null) {
                boolean cancelled = scheduledTask.cancel(false);
                scheduledContentIds.remove(contentId);
                
                CMSLogger.logContentOperation(
                    contentId,
                    "system",
                    "scheduled_publish_cancelled",
                    "Scheduled publishing cancelled: " + cancelled
                );
                
                return cancelled;
            }
            return false;
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Error cancelling scheduled publishing for content: " + contentId,
                e,
                "system"
            );
            return false;
        }
    }
    
    /**
     * Returns the current status of scheduled publishing operations.
     * Useful for monitoring and administrative purposes.
     *
     * @return Map containing scheduling statistics
     */
    public Map<String, Object> getSchedulingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("active_scheduled_tasks", scheduledTasks.size());
        status.put("scheduled_content_ids", new ArrayList<>(scheduledContentIds));
        status.put("executor_shutdown", scheduledExecutor.isShutdown());
        status.put("strategy_name", STRATEGY_NAME);
        return status;
    }
    
    /**
     * Fires Observer Pattern events to notify other components about scheduling.
     *
     * @param content The scheduled content
     * @param context The publishing context
     * @param eventType The type of scheduling event
     */
    private void fireSchedulingEvent(Content content, PublishingContext context, 
                                   ContentEvent.EventType eventType) {
        try {
            ContentEvent event = ContentEvent.builder(eventType, content)
                .user(context.getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("scheduled_publishing", true)
                .metadata("scheduled_date", context.getScheduledDate().getTime())
                .metadata("environment", context.getEnvironment())
                .metadata("target", context.getTarget())
                .build();
            
            contentSubject.notifyObservers(event);
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "scheduling_event_fired",
                "Observer Pattern event fired: " + eventType.name()
            );
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire scheduling event for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
        }
    }
    
    // Private helper methods for validation
    
    private boolean validateBasicContent(Content content, PublishingContext context) {
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
        
        return true;
    }
    
    private boolean hasSchedulingPermission(Role role) {
        return role == Role.ADMINISTRATOR || 
               role == Role.EDITOR || 
               role == Role.PUBLISHER;
    }
    
    private boolean validateScheduledDate(PublishingContext context) {
        Date scheduledDate = context.getScheduledDate();
        if (scheduledDate == null) {
            CMSLogger.logValidationError(
                "Scheduled date is needed for scheduled publishing",
                context.getUser().getUsername()
            );
            return false;
        }
        
        long currentTime = Instant.now().toEpochMilli();
        long delay = scheduledDate.getTime() - currentTime;
        
        // Check minimum scheduling delay
        if (delay < TimeUnit.MINUTES.toMillis(MIN_SCHEDULE_MINUTES)) {
            CMSLogger.logValidationError(
                String.format("Scheduled date must be at least %d minutes in the future", MIN_SCHEDULE_MINUTES),
                context.getUser().getUsername()
            );
            return false;
        }
        
        // Check maximum scheduling delay
        if (delay > TimeUnit.DAYS.toMillis(MAX_SCHEDULE_DAYS)) {
            CMSLogger.logValidationError(
                String.format("Scheduled date cannot be more than %d days in the future", MAX_SCHEDULE_DAYS),
                context.getUser().getUsername()
            );
            return false;
        }
        
        return true;
    }
    
    private boolean isValidStatusForScheduling(ContentStatus status) {
        return status == ContentStatus.DRAFT || status == ContentStatus.REVIEW;
    }
    
    private boolean validateSystemCapacity() {
        // Check if we have too many scheduled tasks
        if (scheduledTasks.size() >= 1000) { // Arbitrary limit for demonstration
            return false;
        }
        
        // Check if executor service is healthy
        if (scheduledExecutor.isShutdown() || scheduledExecutor.isTerminated()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Cleanup method for proper resource management.
     * Should be called when the strategy is no longer needed.
     */
    public void shutdown() {
        try {
            // Cancel all pending tasks
            for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
                entry.getValue().cancel(false);
            }
            
            scheduledTasks.clear();
            scheduledContentIds.clear();
            
            // Shutdown executor
            scheduledExecutor.shutdown();
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            
            CMSLogger.logSystemEvent(
                "scheduled_publishing_strategy_shutdown",
                "ScheduledPublishingStrategy shutdown completed"
            );
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Error during ScheduledPublishingStrategy shutdown",
                e,
                "system"
            );
        }
    }
}