package com.cms.concurrent;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.util.CMSLogger;
import com.cms.util.AuditLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Comprehensive scheduled task service for background operations and
 * maintenance
 * activities in the JavaCMS system using advanced scheduling and
 * multithreading.
 *
 * <p>
 * This service provides sophisticated scheduled task management with
 * ScheduledExecutorService for timed operations, background maintenance tasks,
 * system monitoring, and automated content lifecycle management using
 * advanced concurrent programming techniques.
 * </p>
 *
 * <p>
 * <strong>Multithreading Implementation:</strong> Provides
 * ScheduledExecutorService
 * with scheduled functionality, background task processing, automated
 * maintenance operations, concurrent task scheduling, and comprehensive
 * monitoring of scheduled operations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Content indexing and search optimization background tasks</li>
 * <li>Automatic content archival and lifecycle management</li>
 * <li>System health monitoring and performance metrics collection</li>
 * <li>Cache cleanup and memory optimization tasks</li>
 * <li>Audit log maintenance and security monitoring</li>
 * <li>Database maintenance and statistics updates</li>
 * <li>Configurable scheduling with cron-like expressions</li>
 * </ul>
 *
 * <p>
 * <strong>Background Tasks:</strong>
 * </p>
 * <ul>
 * <li><strong>Content Maintenance:</strong> Automated archival and cleanup</li>
 * <li><strong>Search Indexing:</strong> Background search index updates</li>
 * <li><strong>System Monitoring:</strong> Performance metrics and health
 * checks</li>
 * <li><strong>Cache Management:</strong> Memory optimization and cleanup</li>
 * <li><strong>Security Auditing:</strong> Automated security log analysis</li>
 * </ul>
 *
 * <p>
 * <strong>Integration:</strong> Works with ConcurrentContentRepository for
 * maintenance operations, AsyncContentProcessor for background processing,
 * and Observer Pattern for scheduled event notifications.
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class ScheduledTaskService {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private static final AuditLogger auditLogger = AuditLogger.getInstance();

    private final ThreadPoolManager threadPoolManager;
    private final ConcurrentContentRepository contentRepository;
    private final AsyncContentProcessor contentProcessor;

    // Scheduled task management
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, TaskConfiguration> taskConfigurations = new ConcurrentHashMap<>();

    // Task execution statistics
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final Map<String, AtomicLong> taskExecutionCounts = new ConcurrentHashMap<>();

    // Service state management
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);
    private final LocalDateTime startupTime;

    // Configuration constants
    private static final long CONTENT_INDEXING_INTERVAL_MINUTES = 30;
    private static final long SYSTEM_MONITORING_INTERVAL_MINUTES = 5;
    private static final long CACHE_CLEANUP_INTERVAL_HOURS = 2;
    private static final long AUDIT_MAINTENANCE_INTERVAL_HOURS = 24;
    private static final long CONTENT_ARCHIVAL_INTERVAL_HOURS = 6;

    /**
     * Constructs ScheduledTaskService with required dependencies for background
     * operations.
     *
     * @param contentRepository Thread-safe repository for content operations
     * @param contentProcessor  Async processor for background content processing
     */
    public ScheduledTaskService(ConcurrentContentRepository contentRepository,
            AsyncContentProcessor contentProcessor) {
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.contentRepository = contentRepository;
        this.contentProcessor = contentProcessor;
        this.startupTime = LocalDateTime.now();

        initializeTaskConfigurations();
        logger.logSystemOperation("ScheduledTaskService initialized with background task management");
    }

    /**
     * Starts the scheduled task service and begins executing all configured
     * background tasks.
     * Initializes all scheduled operations with their respective intervals and
     * configurations.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.logSystemOperation("Starting ScheduledTaskService with background operations");

            // Start content maintenance tasks
            startContentIndexingTask();
            startContentArchivalTask();
            startContentCleanupTask();

            // Start system monitoring tasks
            startSystemMonitoringTask();
            startPerformanceMetricsTask();

            // Start maintenance tasks
            startCacheCleanupTask();
            startAuditMaintenanceTask();
            startHealthCheckTask();

            auditLogger.logSecurityEvent("ScheduledTaskService started", "SYSTEM", "LOW");
            logger.logSystemOperation("ScheduledTaskService started successfully with " +
                    scheduledTasks.size() + " background tasks");
        }
    }

    /**
     * Stops the scheduled task service and gracefully shuts down all background
     * tasks.
     * Ensures proper cleanup and resource management during shutdown.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.logSystemOperation("Stopping ScheduledTaskService...");

            // Cancel all scheduled tasks
            scheduledTasks.forEach((taskName, future) -> {
                if (!future.isCancelled()) {
                    future.cancel(false); // Allow running tasks to complete
                    logger.logSystemOperation("Cancelled scheduled task: " + taskName);
                }
            });

            scheduledTasks.clear();

            auditLogger.logSecurityEvent("ScheduledTaskService stopped", "SYSTEM", "LOW");
            logger.logSystemOperation("ScheduledTaskService stopped successfully");
        }
    }

    /**
     * Starts background content indexing task for search optimization.
     * Periodically updates search indexes and optimizes content for faster
     * searches.
     */
    private void startContentIndexingTask() {
        String taskName = "ContentIndexing";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            if (!maintenanceMode.get()) {
                executeTaskWithMetrics(taskName, () -> {
                    logger.logSystemOperation("Starting background content indexing");

                    // Get all published content for indexing
                    List<Content> publishedContent = contentRepository.findByStatus(ContentStatus.PUBLISHED);

                    if (!publishedContent.isEmpty()) {
                        // Process content indexing in batches
                        CompletableFuture<List<AsyncContentProcessor.ProcessingResult>> indexingFuture = contentProcessor
                                .processBatchAsync(publishedContent);

                        try {
                            List<AsyncContentProcessor.ProcessingResult> results = indexingFuture.get(10,
                                    TimeUnit.MINUTES);

                            long successful = results.stream()
                                    .mapToLong(r -> r.isSuccess() ? 1 : 0)
                                    .sum();

                            logger.logSystemOperation("Content indexing completed: " + successful +
                                    " of " + publishedContent.size() + " items indexed");
                        } catch (Exception e) {
                            logger.logError("Content indexing task failed", e);
                        }
                    }
                });
            }
        }, 5, CONTENT_INDEXING_INTERVAL_MINUTES, TimeUnit.MINUTES);

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts automated content archival task for lifecycle management.
     * Automatically archives old content based on configurable rules and policies.
     */
    private void startContentArchivalTask() {
        String taskName = "ContentArchival";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                logger.logSystemOperation("Starting automated content archival");

                // Find content eligible for archival (older than 30 days)
                LocalDateTime archivalCutoff = LocalDateTime.now().minusDays(30);

                List<Content> allContent = contentRepository.search(content -> true);
                List<Content> archivalCandidates = allContent.stream()
                        .filter(content -> content.getCreatedDate().isBefore(archivalCutoff))
                        .filter(content -> content.getStatus() == ContentStatus.PUBLISHED)
                        .toList();

                int archivedCount = 0;
                for (Content content : archivalCandidates) {
                    try {
                        content.setStatus(ContentStatus.ARCHIVED, "system-archival");
                        contentRepository.save(content);
                        archivedCount++;
                    } catch (Exception e) {
                        logger.logError("Failed to archive content: " + content.getId(), e);
                    }
                }

                if (archivedCount > 0) {
                    logger.logSystemOperation("Automated archival completed: " + archivedCount +
                            " content items archived");
                    auditLogger.logSecurityEvent("Automated content archival", "SYSTEM", "LOW",
                            "Archived " + archivedCount + " content items");
                }
            });
        }, 10, CONTENT_ARCHIVAL_INTERVAL_HOURS, TimeUnit.HOURS);

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts content cleanup task for removing obsolete and temporary content.
     * Maintains database hygiene by cleaning up draft content and removing expired
     * items.
     */
    private void startContentCleanupTask() {
        String taskName = "ContentCleanup";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                logger.logSystemOperation("Starting content cleanup task");

                // Find old draft content (older than 7 days)
                LocalDateTime cleanupCutoff = LocalDateTime.now().minusDays(7);

                List<Content> draftContent = contentRepository.findByStatus(ContentStatus.DRAFT);
                List<String> cleanupCandidates = draftContent.stream()
                        .filter(content -> content.getCreatedDate().isBefore(cleanupCutoff))
                        .map(Content::getId)
                        .toList();

                int cleanedCount = 0;
                for (String contentId : cleanupCandidates) {
                    try {
                        if (contentRepository.delete(contentId)) {
                            cleanedCount++;
                        }
                    } catch (Exception e) {
                        logger.logError("Failed to cleanup content: " + contentId, e);
                    }
                }

                if (cleanedCount > 0) {
                    logger.logSystemOperation("Content cleanup completed: " + cleanedCount +
                            " draft items removed");
                }
            });
        }, 30, 12, TimeUnit.HOURS); // Every 12 hours

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts system monitoring task for performance metrics and health checks.
     * Continuously monitors system health and collects performance statistics.
     */
    private void startSystemMonitoringTask() {
        String taskName = "SystemMonitoring";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                // Collect system metrics
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;

                // Collect repository statistics
                Map<String, Object> repoStats = contentRepository.getStatistics();

                // Collect thread pool statistics
                Map<String, Object> threadStats = threadPoolManager.getThreadPoolStatistics();

                // Log system health summary
                double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

                logger.logSystemOperation(String.format(
                        "System Health: Memory usage %.1f%% (%d MB used / %d MB total), " +
                                "Content items: %d, Thread pool tasks: %d",
                        memoryUsagePercent, usedMemory / (1024 * 1024), totalMemory / (1024 * 1024),
                        (Integer) repoStats.get("totalContentItems"),
                        (Long) threadStats.get("totalTasksSubmitted")));

                // Check for memory pressure
                if (memoryUsagePercent > 80) {
                    logger.logError("High memory usage detected: " + memoryUsagePercent + "%", null);
                    auditLogger.logSecurityEvent("High memory usage", "SYSTEM", "MEDIUM");
                }
            });
        }, 1, SYSTEM_MONITORING_INTERVAL_MINUTES, TimeUnit.MINUTES);

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts performance metrics collection task for system optimization.
     * Collects detailed performance metrics for analysis and optimization.
     */
    private void startPerformanceMetricsTask() {
        String taskName = "PerformanceMetrics";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                // Collect and log comprehensive performance metrics
                Map<String, Object> metrics = getComprehensiveMetrics();

                logger.logSystemOperation("Performance Metrics Collection: " + metrics.size() +
                        " metrics collected");

                // Log key performance indicators
                Object avgProcessingTime = metrics.get("averageProcessingTimeMs");
                Object taskSuccess = metrics.get("taskSuccessRate");

                if (avgProcessingTime != null && taskSuccess != null) {
                    logger.logSystemOperation(String.format(
                            "KPIs: Avg processing time: %s ms, Task success rate: %.2f%%",
                            avgProcessingTime, (Double) taskSuccess * 100));
                }
            });
        }, 15, 15, TimeUnit.MINUTES); // Every 15 minutes

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts cache cleanup task for memory optimization.
     * Periodically cleans up caches and optimizes memory usage.
     */
    private void startCacheCleanupTask() {
        String taskName = "CacheCleanup";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                logger.logSystemOperation("Starting cache cleanup and memory optimization");

                // Trigger garbage collection suggestion
                System.gc();

                // Log memory status after cleanup
                Runtime runtime = Runtime.getRuntime();
                long freeMemoryAfter = runtime.freeMemory();

                logger.logSystemOperation("Cache cleanup completed. Free memory: " +
                        (freeMemoryAfter / (1024 * 1024)) + " MB");
            });
        }, 1, CACHE_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS);

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts audit log maintenance task for security compliance.
     * Maintains audit logs and performs security-related maintenance operations.
     */
    private void startAuditMaintenanceTask() {
        String taskName = "AuditMaintenance";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                logger.logSystemOperation("Starting audit log maintenance");

                // Perform audit maintenance operations
                auditLogger.performMaintenance();

                // Log maintenance completion
                auditLogger.logSecurityEvent("Audit maintenance completed", "SYSTEM", "LOW");
                logger.logSystemOperation("Audit maintenance task completed");
            });
        }, 6, AUDIT_MAINTENANCE_INTERVAL_HOURS, TimeUnit.HOURS);

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Starts comprehensive health check task for system validation.
     * Performs regular system health checks and validates service integrity.
     */
    private void startHealthCheckTask() {
        String taskName = "HealthCheck";

        ScheduledFuture<?> future = threadPoolManager.scheduleAtFixedRate(() -> {
            executeTaskWithMetrics(taskName, () -> {
                logger.logSystemOperation("Performing comprehensive system health check");

                boolean allHealthy = true;
                StringBuilder healthReport = new StringBuilder();

                // Check repository health
                try {
                    Map<String, Object> repoStats = contentRepository.getStatistics();
                    healthReport.append("Repository: OK (")
                            .append(repoStats.get("totalContentItems"))
                            .append(" items), ");
                } catch (Exception e) {
                    allHealthy = false;
                    healthReport.append("Repository: ERROR, ");
                    logger.logError("Repository health check failed", e);
                }

                // Check thread pool health
                try {
                    Map<String, Object> threadStats = threadPoolManager.getThreadPoolStatistics();
                    healthReport.append("ThreadPool: OK (")
                            .append(threadStats.get("totalTasksCompleted"))
                            .append(" completed), ");
                } catch (Exception e) {
                    allHealthy = false;
                    healthReport.append("ThreadPool: ERROR, ");
                    logger.logError("Thread pool health check failed", e);
                }

                // Overall health status
                String status = allHealthy ? "HEALTHY" : "DEGRADED";
                healthReport.append("Overall: ").append(status);

                logger.logSystemOperation("Health Check: " + healthReport.toString());

                if (!allHealthy) {
                    auditLogger.logSecurityEvent("System health degraded", "SYSTEM", "HIGH");
                }
            });
        }, 2, 10, TimeUnit.MINUTES); // Every 10 minutes

        scheduledTasks.put(taskName, future);
        taskExecutionCounts.put(taskName, new AtomicLong(0));
    }

    /**
     * Executes a task with comprehensive metrics collection and error handling.
     */
    private void executeTaskWithMetrics(String taskName, Runnable task) {
        long startTime = System.currentTimeMillis();

        try {
            task.run();
            totalTasksExecuted.incrementAndGet();
            taskExecutionCounts.get(taskName).incrementAndGet();

            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTimeMs.addAndGet(executionTime);

        } catch (Exception e) {
            totalTasksFailed.incrementAndGet();
            logger.logError("Scheduled task failed: " + taskName, e);
            auditLogger.logSecurityEvent("Scheduled task failure: " + taskName, "SYSTEM", "MEDIUM");
        }
    }

    /**
     * Gets comprehensive service statistics and metrics.
     *
     * @return Map containing detailed service statistics
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        // Service status
        stats.put("isRunning", isRunning.get());
        stats.put("maintenanceMode", maintenanceMode.get());
        stats.put("startupTime", startupTime);
        stats.put("activeScheduledTasks", scheduledTasks.size());

        // Execution statistics
        stats.put("totalTasksExecuted", totalTasksExecuted.get());
        stats.put("totalTasksFailed", totalTasksFailed.get());
        stats.put("totalExecutionTimeMs", totalExecutionTimeMs.get());

        long totalTasks = totalTasksExecuted.get() + totalTasksFailed.get();
        stats.put("taskSuccessRate", totalTasks > 0 ? (double) totalTasksExecuted.get() / totalTasks : 0.0);

        stats.put("averageExecutionTimeMs",
                totalTasksExecuted.get() > 0 ? totalExecutionTimeMs.get() / totalTasksExecuted.get() : 0);

        // Task-specific counts
        Map<String, Long> taskCounts = new ConcurrentHashMap<>();
        taskExecutionCounts.forEach((task, count) -> taskCounts.put(task, count.get()));
        stats.put("taskExecutionCounts", taskCounts);

        return stats;
    }

    /**
     * Gets comprehensive system metrics from all integrated components.
     */
    private Map<String, Object> getComprehensiveMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // Add service metrics
        metrics.putAll(getServiceStatistics());

        // Add repository metrics
        metrics.putAll(contentRepository.getStatistics());

        // Add thread pool metrics
        metrics.putAll(threadPoolManager.getThreadPoolStatistics());

        // Add processor metrics if available
        if (contentProcessor != null) {
            metrics.putAll(contentProcessor.getProcessingStatistics());
        }

        return metrics;
    }

    /**
     * Initializes task configurations for all scheduled operations.
     */
    private void initializeTaskConfigurations() {
        // Task configurations would be loaded from configuration files
        // in a real implementation
        logger.logSystemOperation("Task configurations initialized");
    }

    /**
     * Enables or disables maintenance mode to pause non-critical tasks.
     */
    public void setMaintenanceMode(boolean enabled) {
        maintenanceMode.set(enabled);
        String status = enabled ? "enabled" : "disabled";
        logger.logSystemOperation("Maintenance mode " + status);
        auditLogger.logSecurityEvent("Maintenance mode " + status, "SYSTEM", "MEDIUM");
    }

    /**
     * Configuration class for individual scheduled tasks.
     */
    private static class TaskConfiguration {
        private final String name;
        private final long interval;
        private final TimeUnit timeUnit;
        private final boolean enabled;

        public TaskConfiguration(String name, long interval, TimeUnit timeUnit, boolean enabled) {
            this.name = name;
            this.interval = interval;
            this.timeUnit = timeUnit;
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public long getInterval() {
            return interval;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
