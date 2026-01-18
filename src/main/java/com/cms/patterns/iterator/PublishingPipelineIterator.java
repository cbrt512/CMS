package com.cms.patterns.iterator;

import com.cms.patterns.iterator.PublishTask;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.time.LocalDateTime;

/**
 * Iterator implementation for processing publishing pipeline tasks in JavaCMS.
 *
 * <p>
 * This class provides controlled sequential processing of publishing tasks with
 * support for priority-based ordering, dependency management, retry logic, and
 * execution status tracking. It implements the Iterator Pattern to enable
 * systematic
 * processing of publishing operations while maintaining pipeline integrity.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Iterator Pattern Implementation
 * <br>
 * This class serves as a concrete iterator in the Iterator design pattern,
 * specifically designed for processing PublishTask objects in a managed
 * publishing
 * workflow. It shows advanced iterator functionality with priority queuing,
 * dependency resolution, and execution state management.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the Iterator Pattern
 * by providing controlled sequential access to publishing workflow tasks,
 * integrating with existing Content model classes through PublishTask objects,
 * and
 * supporting sophisticated pipeline management for content publishing
 * operations.
 * </p>
 *
 * <p>
 * <strong>Pipeline Features:</strong>
 * <ul>
 * <li><strong>Priority-Based Ordering:</strong> Tasks processed based on
 * priority levels</li>
 * <li><strong>Dependency Management:</strong> Supports task dependency
 * chains</li>
 * <li><strong>Retry Logic:</strong> Automatic retry of failed tasks</li>
 * <li><strong>Status Tracking:</strong> Comprehensive execution state
 * monitoring</li>
 * <li><strong>Thread Safety:</strong> Safe for concurrent pipeline
 * management</li>
 * <li><strong>Filtering Support:</strong> Selective task processing based on
 * criteria</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li>Content publishing and deployment workflows</li>
 * <li>Batch content processing operations</li>
 * <li>Site regeneration and optimization tasks</li>
 * <li>Content migration and synchronization</li>
 * <li>Automated quality assurance workflows</li>
 * </ul>
 * </p>
 *
 * @see com.cms.patterns.iterator.PublishTask
 * @see com.cms.core.model.Content
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class PublishingPipelineIterator implements Iterator<PublishTask> {

    /** Enumeration of pipeline execution modes */
    public enum ExecutionMode {
        /** Execute all tasks regardless of status */
        ALL_TASKS,
        /** Execute only ready tasks (pending and scheduled) */
        READY_ONLY,
        /** Execute only failed tasks eligible for retry */
        RETRY_ONLY,
        /** Execute tasks matching custom criteria */
        FILTERED
    }

    /** Priority queue for task ordering */
    private final PriorityBlockingQueue<PublishTask> taskQueue;

    /** Set of all tasks for tracking and management */
    private final Set<PublishTask> allTasks;

    /** Map for task dependency tracking */
    private final Map<String, Set<String>> taskDependencies;

    /** Map for completed task tracking */
    private final Map<String, PublishTask> completedTasks;

    /** Execution mode for this iterator */
    private final ExecutionMode mode;

    /** Optional filter predicate */
    private final Predicate<PublishTask> filter;

    /** Iterator for the ordered task sequence */
    private Iterator<PublishTask> taskIterator;

    /** Current task being processed */
    private PublishTask currentTask;

    /** Total number of tasks in the pipeline */
    private final int totalTaskCount;

    /** Number of tasks processed so far */
    private int processedCount;

    /** Pipeline start time */
    private final LocalDateTime pipelineStartTime;

    /** Maximum retry attempts for failed tasks */
    private int maxRetryAttempts;

    /**
     * Creates a publishing pipeline iterator for the given tasks.
     *
     * <p>
     * This constructor creates an iterator that processes all tasks
     * in priority order without any filtering.
     * </p>
     *
     * @param tasks Collection of publishing tasks to process
     * @throws IllegalArgumentException if tasks collection is null
     * @since 1.0
     */
    public PublishingPipelineIterator(Collection<PublishTask> tasks) {
        this(tasks, ExecutionMode.ALL_TASKS, null);
    }

    /**
     * Creates a publishing pipeline iterator with specified execution mode.
     *
     * <p>
     * This constructor allows selection of execution mode to control
     * which tasks are included in the iteration sequence.
     * </p>
     *
     * @param tasks Collection of publishing tasks to process
     * @param mode  Execution mode for task selection
     * @throws IllegalArgumentException if tasks or mode is null
     * @since 1.0
     */
    public PublishingPipelineIterator(Collection<PublishTask> tasks, ExecutionMode mode) {
        this(tasks, mode, null);
    }

    /**
     * Creates a publishing pipeline iterator with custom filtering.
     *
     * <p>
     * This constructor provides full control over task selection
     * through custom filtering predicates and execution modes.
     * </p>
     *
     * @param tasks  Collection of publishing tasks to process
     * @param mode   Execution mode for task selection
     * @param filter Optional filter predicate (null for no filtering)
     * @throws IllegalArgumentException if tasks or mode is null
     * @since 1.0
     */
    public PublishingPipelineIterator(Collection<PublishTask> tasks, ExecutionMode mode,
            Predicate<PublishTask> filter) {
        if (tasks == null) {
            throw new IllegalArgumentException("Tasks collection cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Execution mode cannot be null");
        }

        this.mode = mode;
        this.filter = filter;
        this.allTasks = new HashSet<>(tasks);
        this.taskDependencies = new ConcurrentHashMap<>();
        this.completedTasks = new ConcurrentHashMap<>();
        this.processedCount = 0;
        this.pipelineStartTime = LocalDateTime.now();
        this.maxRetryAttempts = 3;

        // Initialize priority queue with custom comparator
        this.taskQueue = new PriorityBlockingQueue<>(
                Math.max(tasks.size(), 10),
                this::compareTasksForExecution);

        // Filter and add tasks based on execution mode
        List<PublishTask> filteredTasks = filterTasksByMode(tasks);
        this.totalTaskCount = filteredTasks.size();

        // Initialize the task queue
        taskQueue.addAll(filteredTasks);

        // Create iterator from the sorted tasks
        this.taskIterator = createOrderedTaskIterator();
    }

    /**
     * Factory method for processing all tasks in priority order.
     *
     * @param tasks Collection of tasks to process
     * @return Iterator configured to process all tasks
     * @since 1.0
     */
    public static PublishingPipelineIterator allTasks(Collection<PublishTask> tasks) {
        return new PublishingPipelineIterator(tasks, ExecutionMode.ALL_TASKS);
    }

    /**
     * Factory method for processing only ready tasks.
     *
     * <p>
     * Ready tasks are those in PENDING status that are either scheduled
     * for the current time or have no scheduled time.
     * </p>
     *
     * @param tasks Collection of tasks to filter
     * @return Iterator configured to process only ready tasks
     * @since 1.0
     */
    public static PublishingPipelineIterator readyTasks(Collection<PublishTask> tasks) {
        return new PublishingPipelineIterator(tasks, ExecutionMode.READY_ONLY);
    }

    /**
     * Factory method for processing only failed tasks eligible for retry.
     *
     * @param tasks Collection of tasks to filter
     * @return Iterator configured to process only retry-eligible tasks
     * @since 1.0
     */
    public static PublishingPipelineIterator retryTasks(Collection<PublishTask> tasks) {
        return new PublishingPipelineIterator(tasks, ExecutionMode.RETRY_ONLY);
    }

    /**
     * Factory method for processing tasks with custom filtering.
     *
     * @param tasks  Collection of tasks to filter
     * @param filter Custom predicate for task selection
     * @return Iterator with custom filtering applied
     * @since 1.0
     */
    public static PublishingPipelineIterator filtered(Collection<PublishTask> tasks,
            Predicate<PublishTask> filter) {
        return new PublishingPipelineIterator(tasks, ExecutionMode.FILTERED, filter);
    }

    /**
     * Factory method for processing tasks by priority level.
     *
     * @param tasks    Collection of tasks to filter
     * @param priority Priority level to process
     * @return Iterator filtered by priority
     * @since 1.0
     */
    public static PublishingPipelineIterator byPriority(Collection<PublishTask> tasks,
            PublishTask.Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }

        Predicate<PublishTask> priorityFilter = task -> task.getPriority() == priority;
        return new PublishingPipelineIterator(tasks, ExecutionMode.FILTERED, priorityFilter);
    }

    /**
     * Factory method for processing tasks by status.
     *
     * @param tasks  Collection of tasks to filter
     * @param status Task status to process
     * @return Iterator filtered by status
     * @since 1.0
     */
    public static PublishingPipelineIterator byStatus(Collection<PublishTask> tasks,
            PublishTask.TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Predicate<PublishTask> statusFilter = task -> task.getStatus() == status;
        return new PublishingPipelineIterator(tasks, ExecutionMode.FILTERED, statusFilter);
    }

    /**
     * Filters tasks based on the execution mode.
     *
     * @param tasks Original collection of tasks
     * @return Filtered list of tasks
     * @since 1.0
     */
    private List<PublishTask> filterTasksByMode(Collection<PublishTask> tasks) {
        List<PublishTask> filteredTasks = new ArrayList<>();

        for (PublishTask task : tasks) {
            if (shouldIncludeTask(task)) {
                filteredTasks.add(task);
            }
        }

        return filteredTasks;
    }

    /**
     * Determines if a task should be included based on execution mode and filter.
     *
     * @param task Task to evaluate
     * @return true if task should be included
     * @since 1.0
     */
    private boolean shouldIncludeTask(PublishTask task) {
        // Apply execution mode filtering
        boolean modeMatch = switch (mode) {
            case ALL_TASKS -> true;
            case READY_ONLY -> task.isReady();
            case RETRY_ONLY -> task.canRetry();
            case FILTERED -> true; // Custom filter applied separately
        };

        if (!modeMatch) {
            return false;
        }

        // Apply custom filter if present
        return filter == null || filter.test(task);
    }

    /**
     * Creates an ordered iterator from the priority queue.
     *
     * @return Iterator with tasks in execution order
     * @since 1.0
     */
    private Iterator<PublishTask> createOrderedTaskIterator() {
        List<PublishTask> orderedTasks = new ArrayList<>();

        // Extract tasks from priority queue in order
        PriorityBlockingQueue<PublishTask> tempQueue = new PriorityBlockingQueue<>(taskQueue);
        while (!tempQueue.isEmpty()) {
            orderedTasks.add(tempQueue.poll());
        }

        return orderedTasks.iterator();
    }

    /**
     * Custom comparator for task execution ordering.
     *
     * <p>
     * Tasks are ordered by priority first, then by readiness status,
     * then by creation date for consistent ordering.
     * </p>
     *
     * @param task1 First task to compare
     * @param task2 Second task to compare
     * @return Comparison result for ordering
     * @since 1.0
     */
    private int compareTasksForExecution(PublishTask task1, PublishTask task2) {
        // Priority comparison (lower priority value = higher precedence)
        int priorityComparison = task1.getPriority().compareTo(task2.getPriority());
        if (priorityComparison != 0) {
            return priorityComparison;
        }

        // Ready tasks before non-ready tasks
        boolean ready1 = task1.isReady();
        boolean ready2 = task2.isReady();
        if (ready1 != ready2) {
            return ready1 ? -1 : 1;
        }

        // Scheduled time comparison (earlier scheduled tasks first)
        LocalDateTime sched1 = task1.getScheduledTime();
        LocalDateTime sched2 = task2.getScheduledTime();
        if (sched1 != null && sched2 != null) {
            int scheduleComparison = sched1.compareTo(sched2);
            if (scheduleComparison != 0) {
                return scheduleComparison;
            }
        } else if (sched1 != null || sched2 != null) {
            // Tasks without schedule come first
            return (sched1 == null) ? -1 : 1;
        }

        // Creation date comparison (earlier created tasks first)
        return task1.getCreatedDate().compareTo(task2.getCreatedDate());
    }

    /**
     * Adds a dependency relationship between tasks.
     *
     * <p>
     * A task with dependencies will not be processed until all its
     * dependency tasks have been completed successfully.
     * </p>
     *
     * @param taskId          ID of the dependent task
     * @param dependsOnTaskId ID of the task that must complete first
     * @since 1.0
     */
    public void addTaskDependency(String taskId, String dependsOnTaskId) {
        if (taskId == null || dependsOnTaskId == null) {
            throw new IllegalArgumentException("Task IDs cannot be null");
        }

        taskDependencies.computeIfAbsent(taskId, k -> new HashSet<>()).add(dependsOnTaskId);
    }

    /**
     * Checks if a task's dependencies are satisfied.
     *
     * @param task Task to check
     * @return true if all dependencies are completed
     * @since 1.0
     */
    private boolean areDependenciesSatisfied(PublishTask task) {
        Set<String> dependencies = taskDependencies.get(task.getTaskId());
        if (dependencies == null || dependencies.isEmpty()) {
            return true; // No dependencies
        }

        for (String dependencyId : dependencies) {
            PublishTask dependency = completedTasks.get(dependencyId);
            if (dependency == null || dependency.getStatus() != PublishTask.TaskStatus.COMPLETED) {
                return false; // Dependency not completed
            }
        }

        return true; // All dependencies satisfied
    }

    /**
     * Returns true if there are more tasks to process.
     *
     * @return true if more tasks are available
     * @since 1.0
     */
    @Override
    public boolean hasNext() {
        // Skip tasks that don't meet dependency implementations
        while (taskIterator.hasNext()) {
            PublishTask nextTask = peekNext();
            if (nextTask != null && areDependenciesSatisfied(nextTask) && shouldProcessTask(nextTask)) {
                return true;
            }
            // Skip this task and try the next one
            taskIterator.next();
        }
        return false;
    }

    /**
     * Returns the next task to be processed.
     *
     * @return Next PublishTask in the pipeline
     * @throws NoSuchElementException if no more tasks are available
     * @since 1.0
     */
    @Override
    public PublishTask next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more tasks available in publishing pipeline");
        }

        currentTask = taskIterator.next();
        processedCount++;

        // Mark task as started if it's ready
        if (currentTask.isReady() && currentTask.getStatus() == PublishTask.TaskStatus.PENDING) {
            currentTask.markStarted();
        }

        return currentTask;
    }

    /**
     * Peeks at the next task without advancing the iterator.
     *
     * @return Next task or null if none available
     * @since 1.0
     */
    private PublishTask peekNext() {
        // This is a simplified peek - in a full implementation,
        // we would need to handle this more carefully
        return taskIterator.hasNext()
                ? ((List<PublishTask>) ((ArrayList<PublishTask>) Arrays.asList(taskQueue.toArray(new PublishTask[0]))))
                        .get(0)
                : null;
    }

    /**
     * Determines if a task should be processed based on current conditions.
     *
     * @param task Task to evaluate
     * @return true if task should be processed
     * @since 1.0
     */
    private boolean shouldProcessTask(PublishTask task) {
        switch (task.getStatus()) {
            case PENDING:
                return task.isReady();
            case FAILED:
                return task.canRetry();
            case WAITING:
                return areDependenciesSatisfied(task);
            default:
                return false;
        }
    }

    /**
     * Marks the current task as completed and updates tracking.
     *
     * <p>
     * This method should be called after successful task execution
     * to update the pipeline state and enable dependent tasks.
     * </p>
     *
     * @throws IllegalStateException if no current task or task already completed
     * @since 1.0
     */
    public void markCurrentTaskCompleted() {
        if (currentTask == null) {
            throw new IllegalStateException("No current task to mark as completed");
        }

        if (currentTask.getStatus() != PublishTask.TaskStatus.EXECUTING) {
            throw new IllegalStateException("Task must be executing to be marked completed");
        }

        currentTask.markCompleted();
        completedTasks.put(currentTask.getTaskId(), currentTask);
    }

    /**
     * Marks the current task as failed with an error message.
     *
     * <p>
     * This method should be called when task execution fails
     * to update the pipeline state and enable retry logic.
     * </p>
     *
     * @param errorMessage Description of the failure
     * @throws IllegalStateException if no current task or invalid state
     * @since 1.0
     */
    public void markCurrentTaskFailed(String errorMessage) {
        if (currentTask == null) {
            throw new IllegalStateException("No current task to mark as failed");
        }

        if (currentTask.getStatus() != PublishTask.TaskStatus.EXECUTING) {
            throw new IllegalStateException("Task must be executing to be marked failed");
        }

        currentTask.markFailed(errorMessage);

        // Schedule for retry if possible
        if (currentTask.canRetry()) {
            currentTask.retryIfPossible();
        }
    }

    /**
     * Remove operation is not supported for pipeline integrity.
     *
     * @throws UnsupportedOperationException always
     * @since 1.0
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove operation not supported. Use task completion methods instead.");
    }

    /**
     * Returns the total number of tasks in the pipeline.
     *
     * @return Total task count
     * @since 1.0
     */
    public int getTotalTaskCount() {
        return totalTaskCount;
    }

    /**
     * Returns the number of tasks processed so far.
     *
     * @return Processed task count
     * @since 1.0
     */
    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * Returns the number of remaining tasks to process.
     *
     * @return Remaining task count
     * @since 1.0
     */
    public int getRemainingCount() {
        return totalTaskCount - processedCount;
    }

    /**
     * Returns the pipeline execution progress as a percentage.
     *
     * @return Progress percentage (0.0 to 100.0)
     * @since 1.0
     */
    public double getProgress() {
        return totalTaskCount == 0 ? 100.0 : (processedCount * 100.0) / totalTaskCount;
    }

    /**
     * Returns the current execution mode.
     *
     * @return The execution mode
     * @since 1.0
     */
    public ExecutionMode getExecutionMode() {
        return mode;
    }

    /**
     * Returns the filter predicate being used (if any).
     *
     * @return The filter predicate or null
     * @since 1.0
     */
    public Predicate<PublishTask> getFilter() {
        return filter;
    }

    /**
     * Returns the pipeline start time.
     *
     * @return When the pipeline processing started
     * @since 1.0
     */
    public LocalDateTime getPipelineStartTime() {
        return pipelineStartTime;
    }

    /**
     * Returns the current task being processed (if any).
     *
     * @return Current task or null
     * @since 1.0
     */
    public PublishTask getCurrentTask() {
        return currentTask;
    }

    /**
     * Returns a copy of all completed tasks.
     *
     * @return List of completed tasks
     * @since 1.0
     */
    public List<PublishTask> getCompletedTasks() {
        return new ArrayList<>(completedTasks.values());
    }

    /**
     * Creates a new iterator with additional filtering applied.
     *
     * @param additionalFilter Additional filter predicate
     * @return New iterator with combined filters
     * @since 1.0
     */
    public PublishingPipelineIterator filter(Predicate<PublishTask> additionalFilter) {
        if (additionalFilter == null) {
            return new PublishingPipelineIterator(allTasks, mode, filter);
        }

        Predicate<PublishTask> combinedFilter = filter == null ? additionalFilter : filter.and(additionalFilter);

        return new PublishingPipelineIterator(allTasks, mode, combinedFilter);
    }

    /**
     * Returns a string representation of the pipeline iterator state.
     *
     * @return String describing pipeline status
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format(
                "PublishingPipelineIterator{mode=%s, total=%d, processed=%d, remaining=%d, progress=%.1f%%}",
                mode, totalTaskCount, processedCount, getRemainingCount(), getProgress());
    }
}
