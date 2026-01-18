package com.cms.patterns.iterator;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

/**
 * Represents a single publishing task in the JavaCMS publishing pipeline.
 *
 * <p>
 * This class encapsulates all information needed to execute a content
 * publishing
 * operation, including the content to be published, timing constraints,
 * dependencies,
 * and execution status. It serves as the core unit processed by the
 * PublishingPipelineIterator in the Iterator Pattern implementation.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Iterator Pattern Support
 * <br>
 * This class provides the objects that will be iterated over by the
 * PublishingPipelineIterator, enabling sequential processing of publishing
 * operations with support for priority-based ordering and dependency
 * management.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Supports the Iterator Pattern
 * by providing structured data objects for pipeline processing,
 * integrating with existing Content model classes, and enabling comprehensive
 * workflow management for content publishing operations.
 * </p>
 *
 * <p>
 * <strong>Usage in Iterator Pattern:</strong>
 * <ul>
 * <li><strong>Sequential Processing:</strong> Enables ordered execution of
 * publishing tasks</li>
 * <li><strong>Priority Handling:</strong> Supports priority-based task
 * ordering</li>
 * <li><strong>Dependency Management:</strong> Tracks task dependencies and
 * prerequisites</li>
 * <li><strong>Status Tracking:</strong> Monitors task execution progress and
 * outcomes</li>
 * </ul>
 * </p>
 *
 * @see com.cms.patterns.iterator.PublishingPipelineIterator
 * @see com.cms.core.model.Content
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class PublishTask implements Comparable<PublishTask> {

    /** Enumeration of possible task statuses */
    public enum TaskStatus {
        /** Task is pending execution */
        PENDING,
        /** Task is currently executing */
        EXECUTING,
        /** Task completed successfully */
        COMPLETED,
        /** Task failed during execution */
        FAILED,
        /** Task was cancelled before execution */
        CANCELLED,
        /** Task is waiting for dependencies */
        WAITING
    }

    /** Enumeration of task priorities */
    public enum Priority {
        /** Critical priority - highest precedence */
        CRITICAL(1),
        /** High priority */
        HIGH(2),
        /** Normal priority - default */
        NORMAL(3),
        /** Low priority */
        LOW(4),
        /** Background priority - lowest precedence */
        BACKGROUND(5);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /** Unique identifier for this task */
    private final String taskId;

    /** The content to be published */
    private final Content<?> content;

    /** Task description for human readability */
    private String description;

    /** Current status of the task */
    private TaskStatus status;

    /** Task priority for ordering */
    private Priority priority;

    /** When the task was created */
    private final LocalDateTime createdDate;

    /** Scheduled execution time (null for immediate execution) */
    private LocalDateTime scheduledTime;

    /** When task execution started */
    private LocalDateTime startTime;

    /** When task execution completed */
    private LocalDateTime completionTime;

    /** Error message if task failed */
    private String errorMessage;

    /** Number of retry attempts */
    private int retryCount;

    /** Maximum allowed retries */
    private int maxRetries;

    /**
     * Creates a new publishing task for the specified content.
     *
     * <p>
     * Initializes a publishing task with default priority (NORMAL) and
     * PENDING status. The task is ready for immediate execution unless
     * explicitly scheduled for a later time.
     * </p>
     *
     * @param content     The content to be published (needed)
     * @param description Brief description of the publishing operation
     * @throws IllegalArgumentException if content is null or description is empty
     * @since 1.0
     */
    public PublishTask(Content<?> content, String description) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null for publishing task");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be null or empty");
        }

        this.taskId = UUID.randomUUID().toString();
        this.content = content;
        this.description = description.trim();
        this.status = TaskStatus.PENDING;
        this.priority = Priority.NORMAL;
        this.createdDate = LocalDateTime.now();
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    /**
     * Creates a new publishing task with specified priority.
     *
     * @param content     The content to be published (needed)
     * @param description Brief description of the publishing operation
     * @param priority    Task priority for execution ordering
     * @throws IllegalArgumentException if content is null, description is empty, or
     *                                  priority is null
     * @since 1.0
     */
    public PublishTask(Content<?> content, String description, Priority priority) {
        this(content, description);
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.priority = priority;
    }

    /**
     * Marks the task as started and records the start time.
     *
     * <p>
     * Updates the task status to EXECUTING and sets the start time to the current
     * moment. This method should be called when the publishing operation begins.
     * </p>
     *
     * @throws IllegalStateException if task is not in PENDING or WAITING status
     * @since 1.0
     */
    public void markStarted() {
        if (status != TaskStatus.PENDING && status != TaskStatus.WAITING) {
            throw new IllegalStateException(
                    "Task can only be started from PENDING or WAITING status, current: " + status);
        }
        this.status = TaskStatus.EXECUTING;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Marks the task as successfully completed.
     *
     * <p>
     * Updates the task status to COMPLETED and records the completion time.
     * This method should be called when the publishing operation finishes
     * successfully.
     * </p>
     *
     * @throws IllegalStateException if task is not currently executing
     * @since 1.0
     */
    public void markCompleted() {
        if (status != TaskStatus.EXECUTING) {
            throw new IllegalStateException("Task can only be completed from EXECUTING status, current: " + status);
        }
        this.status = TaskStatus.COMPLETED;
        this.completionTime = LocalDateTime.now();
    }

    /**
     * Marks the task as failed with an error message.
     *
     * <p>
     * Updates the task status to FAILED, records the completion time, and
     * stores the error message for troubleshooting. Increments retry count.
     * </p>
     *
     * @param errorMessage Description of the failure reason
     * @throws IllegalStateException if task is not currently executing
     * @since 1.0
     */
    public void markFailed(String errorMessage) {
        if (status != TaskStatus.EXECUTING) {
            throw new IllegalStateException("Task can only be failed from EXECUTING status, current: " + status);
        }
        this.status = TaskStatus.FAILED;
        this.completionTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Resets the task for retry if retry limit has not been exceeded.
     *
     * <p>
     * Changes task status back to PENDING if retry attempts are available.
     * Clears execution timing information but preserves error history.
     * </p>
     *
     * @return true if task was reset for retry, false if retry limit exceeded
     * @since 1.0
     */
    public boolean retryIfPossible() {
        if (status != TaskStatus.FAILED || retryCount >= maxRetries) {
            return false;
        }

        this.status = TaskStatus.PENDING;
        this.startTime = null;
        this.completionTime = null;
        return true;
    }

    /**
     * Determines if this task is ready for execution.
     *
     * <p>
     * A task is ready if it's in PENDING status and either has no scheduled time
     * or the scheduled time has passed.
     * </p>
     *
     * @return true if task is ready for execution
     * @since 1.0
     */
    public boolean isReady() {
        return status == TaskStatus.PENDING &&
                (scheduledTime == null || LocalDateTime.now().isAfter(scheduledTime));
    }

    /**
     * Determines if this task can be retried.
     *
     * <p>
     * A task can be retried if it failed and hasn't exceeded the maximum retry
     * limit.
     * </p>
     *
     * @return true if task is eligible for retry
     * @since 1.0
     */
    public boolean canRetry() {
        return status == TaskStatus.FAILED && retryCount < maxRetries;
    }

    // Getters and setters

    public String getTaskId() {
        return taskId;
    }

    public Content<?> getContent() {
        return content;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        this.description = description.trim();
    }

    public void setPriority(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        this.priority = priority;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public void setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        this.maxRetries = maxRetries;
    }

    /**
     * Compares this task with another for priority-based ordering.
     *
     * <p>
     * Tasks are ordered first by priority (lower priority value = higher
     * precedence),
     * then by creation date (earlier created tasks first). This enables proper
     * queue
     * ordering in the PublishingPipelineIterator.
     * </p>
     *
     * @param other The task to compare with
     * @return negative if this task has higher priority, positive if lower, 0 if
     *         equal
     * @since 1.0
     */
    @Override
    public int compareTo(PublishTask other) {
        if (this.priority.getValue() != other.priority.getValue()) {
            return Integer.compare(this.priority.getValue(), other.priority.getValue());
        }
        return this.createdDate.compareTo(other.createdDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        PublishTask task = (PublishTask) obj;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    /**
     * Returns a string representation of this publishing task.
     *
     * <p>
     * Includes task ID, status, priority, content information, and timing details
     * for debugging and logging purposes.
     * </p>
     *
     * @return formatted string representation of the task
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format("PublishTask{id='%s', status=%s, priority=%s, content='%s', created=%s}",
                taskId, status, priority,
                content.getTitle(),
                createdDate);
    }
}
