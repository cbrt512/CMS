package com.cms.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import com.cms.util.CMSLogger;

/**
 * Centralized thread pool management system for JavaCMS multithreading
 * operations.
 *
 * <p>
 * This class provides comprehensive thread pool management for the Content
 * Management System,
 * implementing advanced multithreading patterns with different executor
 * services optimized
 * for specific types of operations. It implements sophisticated concurrent
 * programming
 * techniques and resource management.
 * </p>
 *
 * <p>
 * <strong>Multithreading Implementation:</strong> Provides comprehensive thread
 * pool management
 * with multiple specialized executor services, proper resource cleanup,
 * monitoring,
 * and performance optimization for concurrent CMS operations.
 * </p>
 *
 * <p>
 * <strong>Key Features:</strong>
 * </p>
 * <ul>
 * <li>Multiple specialized thread pools for different operation types</li>
 * <li>ForkJoinPool for parallel content processing with work-stealing</li>
 * <li>ScheduledExecutorService for background tasks and scheduling</li>
 * <li>Thread factory customization with proper naming and priorities</li>
 * <li>Comprehensive monitoring and performance metrics</li>
 * <li>Graceful shutdown with timeout handling</li>
 * <li>Thread safety throughout with atomic operations</li>
 * </ul>
 *
 * <p>
 * <strong>Thread Pool Types:</strong>
 * </p>
 * <ul>
 * <li><strong>Content Processing Pool:</strong> Fixed size pool for content
 * operations</li>
 * <li><strong>I/O Operations Pool:</strong> Cached pool for file and network
 * operations</li>
 * <li><strong>Background Tasks Pool:</strong> Single threaded for maintenance
 * operations</li>
 * <li><strong>Scheduled Tasks Pool:</strong> Scheduled executor for timed
 * operations</li>
 * <li><strong>Fork Join Pool:</strong> Work-stealing pool for parallel
 * processing</li>
 * </ul>
 *
 * <p>
 * <strong>Integration:</strong> Integrates with Observer Pattern for async
 * notifications,
 * Strategy Pattern for parallel execution, and I/O operations for concurrent
 * file processing.
 * </p>
 *
 * @author Otman Hmich S007924
 * @version 1.0
 * @since 1.0
 */
public class ThreadPoolManager {

    private static final CMSLogger logger = CMSLogger.getInstance();
    private static volatile ThreadPoolManager instance;
    private static final Object lock = new Object();

    // Thread Pool Configuration Constants
    private static final int CONTENT_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int MAX_IO_POOL_SIZE = 50;
    private static final int SCHEDULED_POOL_SIZE = 3;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    // Executor Services for different operation types
    private final ExecutorService contentProcessingPool;
    private final ExecutorService ioOperationsPool;
    private final ExecutorService backgroundTasksPool;
    private final ScheduledExecutorService scheduledTasksPool;
    private final ForkJoinPool forkJoinPool;

    // Monitoring and Statistics
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    private final Map<String, AtomicLong> poolUsageStats = new ConcurrentHashMap<>();
    private final LocalDateTime startupTime;

    // Shutdown flag for graceful termination
    private volatile boolean isShutdown = false;

    /**
     * Private constructor implementing singleton pattern with thread-safe
     * initialization.
     * Initializes all thread pools with custom thread factories and monitoring.
     */
    private ThreadPoolManager() {
        this.startupTime = LocalDateTime.now();

        // Initialize thread pools with custom thread factories
        this.contentProcessingPool = Executors.newFixedThreadPool(
                CONTENT_POOL_SIZE,
                new CMSThreadFactory("ContentProcessor", Thread.NORM_PRIORITY));

        this.ioOperationsPool = Executors.newCachedThreadPool(
                new CMSThreadFactory("IOOperations", Thread.NORM_PRIORITY - 1));

        this.backgroundTasksPool = Executors.newSingleThreadExecutor(
                new CMSThreadFactory("BackgroundTasks", Thread.MIN_PRIORITY + 1));

        this.scheduledTasksPool = Executors.newScheduledThreadPool(
                SCHEDULED_POOL_SIZE,
                new CMSThreadFactory("ScheduledTasks", Thread.NORM_PRIORITY));

        this.forkJoinPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true // Enable async mode for better performance
        );

        // Initialize usage statistics
        initializeUsageStats();

        logger.logSystemOperation("ThreadPoolManager initialized with " + CONTENT_POOL_SIZE +
                " content processing threads and " + SCHEDULED_POOL_SIZE + " scheduled task threads");
    }

    /**
     * Gets the singleton instance of ThreadPoolManager with double-checked locking.
     *
     * @return The singleton ThreadPoolManager instance
     */
    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    /**
     * Submits a content processing task to the dedicated content processing thread
     * pool.
     *
     * @param task The content processing task to execute
     * @return CompletableFuture for async result handling
     * @throws RejectedExecutionException if the task cannot be accepted for
     *                                    execution
     */
    public <T> CompletableFuture<T> submitContentProcessingTask(Callable<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPoolManager has been shut down");
        }

        incrementTaskCounter("content");
        totalTasksSubmitted.incrementAndGet();

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.call();
                totalTasksCompleted.incrementAndGet();
                return result;
            } catch (Exception e) {
                totalTasksFailed.incrementAndGet();
                logger.logError("Content processing task failed", e);
                throw new RuntimeException("Content processing failed", e);
            }
        }, contentProcessingPool);

        logger.logContentOperation("Content processing task submitted");
        return future;
    }

    /**
     * Submits an I/O operation task to the cached I/O thread pool.
     *
     * @param task The I/O operation task to execute
     * @return CompletableFuture for async result handling
     */
    public <T> CompletableFuture<T> submitIOTask(Callable<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPoolManager has been shut down");
        }

        incrementTaskCounter("io");
        totalTasksSubmitted.incrementAndGet();

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                T result = task.call();
                totalTasksCompleted.incrementAndGet();
                return result;
            } catch (Exception e) {
                totalTasksFailed.incrementAndGet();
                logger.logError("I/O operation task failed", e);
                throw new RuntimeException("I/O operation failed", e);
            }
        }, ioOperationsPool);

        logger.logSystemOperation("I/O operation task submitted");
        return future;
    }

    /**
     * Submits a background maintenance task to the single-threaded background pool.
     *
     * @param task The background task to execute
     * @return CompletableFuture for async result handling
     */
    public CompletableFuture<Void> submitBackgroundTask(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPoolManager has been shut down");
        }

        incrementTaskCounter("background");
        totalTasksSubmitted.incrementAndGet();

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                task.run();
                totalTasksCompleted.incrementAndGet();
            } catch (Exception e) {
                totalTasksFailed.incrementAndGet();
                logger.logError("Background task failed", e);
                throw new RuntimeException("Background task failed", e);
            }
        }, backgroundTasksPool);

        logger.logSystemOperation("Background task submitted");
        return future;
    }

    /**
     * Schedules a task for repeated execution at fixed rate.
     *
     * @param task         The task to execute repeatedly
     * @param initialDelay Initial delay before first execution
     * @param period       Period between successive executions
     * @param unit         Time unit for delays and period
     * @return ScheduledFuture for task control
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay,
            long period, TimeUnit unit) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPoolManager has been shut down");
        }

        incrementTaskCounter("scheduled");

        ScheduledFuture<?> future = scheduledTasksPool.scheduleAtFixedRate(() -> {
            try {
                task.run();
                totalTasksCompleted.incrementAndGet();
            } catch (Exception e) {
                totalTasksFailed.incrementAndGet();
                logger.logError("Scheduled task failed", e);
            }
        }, initialDelay, period, unit);

        logger.logSystemOperation("Scheduled task registered with " + period + " " +
                unit.toString().toLowerCase() + " period");
        return future;
    }

    /**
     * Submits a parallel processing task to the ForkJoinPool for work-stealing
     * execution.
     *
     * @param task The ForkJoinTask to execute in parallel
     * @return CompletableFuture for async result handling
     */
    public <T> CompletableFuture<T> submitParallelTask(ForkJoinTask<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPoolManager has been shut down");
        }

        incrementTaskCounter("parallel");
        totalTasksSubmitted.incrementAndGet();

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                T result = forkJoinPool.invoke(task);
                totalTasksCompleted.incrementAndGet();
                return result;
            } catch (Exception e) {
                totalTasksFailed.incrementAndGet();
                logger.logError("Parallel task failed", e);
                throw new RuntimeException("Parallel task failed", e);
            }
        });

        logger.logSystemOperation("Parallel processing task submitted to ForkJoinPool");
        return future;
    }

    /**
     * Gets comprehensive thread pool statistics and performance metrics.
     *
     * @return Map containing detailed thread pool usage statistics
     */
    public Map<String, Object> getThreadPoolStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        // Overall statistics
        stats.put("startupTime", startupTime);
        stats.put("totalTasksSubmitted", totalTasksSubmitted.get());
        stats.put("totalTasksCompleted", totalTasksCompleted.get());
        stats.put("totalTasksFailed", totalTasksFailed.get());
        stats.put("isShutdown", isShutdown);

        // Pool-specific statistics
        Map<String, Long> poolUsage = new ConcurrentHashMap<>();
        poolUsageStats.forEach((pool, count) -> poolUsage.put(pool, count.get()));
        stats.put("poolUsageStats", poolUsage);

        // Thread pool details
        if (contentProcessingPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) contentProcessingPool;
            Map<String, Object> contentStats = new ConcurrentHashMap<>();
            contentStats.put("activeCount", tpe.getActiveCount());
            contentStats.put("completedTaskCount", tpe.getCompletedTaskCount());
            contentStats.put("corePoolSize", tpe.getCorePoolSize());
            contentStats.put("poolSize", tpe.getPoolSize());
            contentStats.put("queueSize", tpe.getQueue().size());
            stats.put("contentProcessingPool", contentStats);
        }

        // ForkJoinPool statistics
        Map<String, Object> forkJoinStats = new ConcurrentHashMap<>();
        forkJoinStats.put("activeThreadCount", forkJoinPool.getActiveThreadCount());
        forkJoinStats.put("parallelism", forkJoinPool.getParallelism());
        forkJoinStats.put("queuedTaskCount", forkJoinPool.getQueuedTaskCount());
        forkJoinStats.put("runningThreadCount", forkJoinPool.getRunningThreadCount());
        forkJoinStats.put("stealCount", forkJoinPool.getStealCount());
        stats.put("forkJoinPool", forkJoinStats);

        return stats;
    }

    /**
     * Initiates graceful shutdown of all thread pools with proper resource cleanup.
     * Waits for currently executing tasks to complete before terminating.
     */
    public void shutdown() {
        if (isShutdown) {
            return;
        }

        logger.logSystemOperation("ThreadPoolManager shutdown initiated");
        isShutdown = true;

        // Shutdown all executor services
        shutdownExecutorService(contentProcessingPool, "Content Processing Pool");
        shutdownExecutorService(ioOperationsPool, "I/O Operations Pool");
        shutdownExecutorService(backgroundTasksPool, "Background Tasks Pool");
        shutdownExecutorService(scheduledTasksPool, "Scheduled Tasks Pool");

        // Shutdown ForkJoinPool
        forkJoinPool.shutdown();
        try {
            if (!forkJoinPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.logError("ForkJoinPool did not terminate gracefully, forcing shutdown", null);
                forkJoinPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            forkJoinPool.shutdownNow();
        }

        // Log final statistics
        Map<String, Object> finalStats = getThreadPoolStatistics();
        logger.logSystemOperation("ThreadPoolManager shutdown completed. Final stats: " + finalStats);
    }

    /**
     * Helper method to shutdown an ExecutorService gracefully.
     */
    private void shutdownExecutorService(ExecutorService executor, String poolName) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.logError(poolName + " did not terminate gracefully, forcing shutdown", null);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * Initializes usage statistics counters for all thread pools.
     */
    private void initializeUsageStats() {
        poolUsageStats.put("content", new AtomicLong(0));
        poolUsageStats.put("io", new AtomicLong(0));
        poolUsageStats.put("background", new AtomicLong(0));
        poolUsageStats.put("scheduled", new AtomicLong(0));
        poolUsageStats.put("parallel", new AtomicLong(0));
    }

    /**
     * Increments the usage counter for a specific thread pool.
     */
    private void incrementTaskCounter(String poolType) {
        poolUsageStats.computeIfAbsent(poolType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Custom ThreadFactory for creating named threads with specific priorities.
     */
    private static class CMSThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final int priority;

        CMSThreadFactory(String namePrefix, int priority) {
            this.namePrefix = "CMS-" + namePrefix + "-";
            this.priority = priority;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false); // Keep JVM alive for proper shutdown
            t.setPriority(priority);
            t.setUncaughtExceptionHandler((thread, e) -> {
                Exception exception = (e instanceof Exception) ? (Exception) e : new Exception("Uncaught throwable", e);
                CMSLogger.getInstance().logError("Uncaught exception in thread " + thread.getName(), exception);
            });
            return t;
        }
    }
}
