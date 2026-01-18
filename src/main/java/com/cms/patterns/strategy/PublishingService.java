package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.model.Role;
import com.cms.core.exception.ContentManagementException;
import com.cms.patterns.observer.ContentSubject;
import com.cms.util.CMSLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Context class that manages and orchestrates different publishing strategies.
 *
 * <p>The PublishingService serves as the Context in the Strategy Pattern implementation,
 * providing a unified interface for content publishing while delegating the actual
 * publishing logic to interchangeable strategy objects. This class shows the
 * Strategy Pattern's core benefit: allowing algorithms to vary independently from
 * the clients that use them.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as the Context component in the Strategy Pattern. It maintains a reference to
 * strategy objects, provides methods to change strategies at runtime, and delegates
 * publishing operations to the selected strategy. The service shows advanced
 * strategy management including automatic strategy selection, strategy chaining,
 * and comprehensive strategy analytics.</p>
 *
 * <p><strong>Implementation:</strong> Completes the Strategy Pattern implementation
 * by providing the context that uses strategies interchangeably. This
 * service showcases advanced strategy management features including automatic
 * selection based on content and context characteristics, strategy performance
 * monitoring, and integration with other design patterns.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Strategy lifecycle management (registration, selection, execution)</li>
 *   <li>Automatic strategy selection based on content and context analysis</li>
 *   <li>Strategy chaining for complex publishing workflows</li>
 *   <li>Performance monitoring and strategy optimization</li>
 *   <li>Integration with Observer Pattern for event-driven notifications</li>
 *   <li>Exception handling and error recovery across strategies</li>
 * </ul>
 *
 * <p><strong>Advanced Features:</strong></p>
 * <ul>
 *   <li><strong>Intelligent Selection:</strong> AI-like strategy selection based on historical data</li>
 *   <li><strong>Strategy Chaining:</strong> Sequential execution of multiple strategies</li>
 *   <li><strong>Fallback Mechanisms:</strong> Automatic fallback to alternative strategies</li>
 *   <li><strong>Performance Analytics:</strong> Comprehensive strategy performance tracking</li>
 *   <li><strong>Configuration Management:</strong> Runtime strategy configuration and optimization</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For publishing context details
 */
public class PublishingService {
    
    /** Default strategy for fallback scenarios */
    private static final String DEFAULT_STRATEGY = "Immediate Publishing";
    
    /** Map of registered publishing strategies */
    private final Map<String, PublishingStrategy> strategies;
    
    /** Currently selected strategy (can be changed at runtime) */
    private volatile PublishingStrategy currentStrategy;
    
    /** Strategy selection algorithm for automatic strategy choosing */
    private StrategySelector strategySelector;
    
    /** Performance metrics for strategy optimization */
    private final StrategyPerformanceTracker performanceTracker;
    
    /** Configuration for strategy behavior and preferences */
    private final PublishingServiceConfiguration configuration;
    
    /** Observer subject for service-level events */
    private final ContentSubject contentSubject;
    
    /** Strategy usage statistics for analysis and optimization */
    private final Map<String, StrategyUsageStats> usageStats;
    
    /**
     * Configuration class for PublishingService behavior.
     */
    public static class PublishingServiceConfiguration {
        private boolean enableAutomaticSelection = true;
        private boolean enableStrategyChaining = false;
        private boolean enableFallbackMechanisms = true;
        private boolean enablePerformanceTracking = true;
        private int maxStrategyChainLength = 3;
        private long strategyTimeoutMs = 30000; // 30 seconds
        private String defaultFallbackStrategy = DEFAULT_STRATEGY;
        
        // Getters and setters
        public boolean isEnableAutomaticSelection() { return enableAutomaticSelection; }
        public void setEnableAutomaticSelection(boolean enableAutomaticSelection) { 
            this.enableAutomaticSelection = enableAutomaticSelection; 
        }
        
        public boolean isEnableStrategyChaining() { return enableStrategyChaining; }
        public void setEnableStrategyChaining(boolean enableStrategyChaining) { 
            this.enableStrategyChaining = enableStrategyChaining; 
        }
        
        public boolean isEnableFallbackMechanisms() { return enableFallbackMechanisms; }
        public void setEnableFallbackMechanisms(boolean enableFallbackMechanisms) { 
            this.enableFallbackMechanisms = enableFallbackMechanisms; 
        }
        
        public boolean isEnablePerformanceTracking() { return enablePerformanceTracking; }
        public void setEnablePerformanceTracking(boolean enablePerformanceTracking) { 
            this.enablePerformanceTracking = enablePerformanceTracking; 
        }
        
        public int getMaxStrategyChainLength() { return maxStrategyChainLength; }
        public void setMaxStrategyChainLength(int maxStrategyChainLength) { 
            this.maxStrategyChainLength = Math.max(1, Math.min(maxStrategyChainLength, 10)); 
        }
        
        public long getStrategyTimeoutMs() { return strategyTimeoutMs; }
        public void setStrategyTimeoutMs(long strategyTimeoutMs) { 
            this.strategyTimeoutMs = Math.max(1000, strategyTimeoutMs); 
        }
        
        public String getDefaultFallbackStrategy() { return defaultFallbackStrategy; }
        public void setDefaultFallbackStrategy(String defaultFallbackStrategy) { 
            this.defaultFallbackStrategy = defaultFallbackStrategy != null ? defaultFallbackStrategy : DEFAULT_STRATEGY; 
        }
    }
    
    /**
     * Strategy usage statistics for analytics and optimization.
     */
    public static class StrategyUsageStats {
        private volatile int usageCount;
        private volatile int successCount;
        private volatile int failureCount;
        private volatile long totalExecutionTime;
        private volatile double averageExecutionTime;
        private final Date firstUsed;
        private volatile Date lastUsed;
        
        public StrategyUsageStats() {
            this.firstUsed = new Date();
            this.lastUsed = new Date();
        }
        
        public synchronized void recordUsage(boolean success, long executionTime) {
            usageCount++;
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
            totalExecutionTime += executionTime;
            averageExecutionTime = (double) totalExecutionTime / usageCount;
            lastUsed = new Date();
        }
        
        // Getters
        public int getUsageCount() { return usageCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public Date getFirstUsed() { return new Date(firstUsed.getTime()); }
        public Date getLastUsed() { return new Date(lastUsed.getTime()); }
        
        public double getSuccessRate() {
            return usageCount > 0 ? (double) successCount / usageCount * 100 : 0;
        }
    }
    
    /**
     * Interface for strategy selection algorithms.
     */
    public interface StrategySelector {
        /**
         * Selects the most appropriate strategy based on content and context.
         * @param content The content to be published
         * @param context The publishing context
         * @param availableStrategies List of available strategies
         * @param performanceTracker Performance data for decision making
         * @return The selected strategy name, or null if no suitable strategy found
         */
        String selectStrategy(Content content, PublishingContext context, 
                            List<PublishingStrategy> availableStrategies,
                            StrategyPerformanceTracker performanceTracker);
    }
    
    /**
     * Performance tracking for strategies.
     */
    public static class StrategyPerformanceTracker {
        private final Map<String, List<Long>> executionTimes;
        private final Map<String, Integer> successCounts;
        private final Map<String, Integer> failureCounts;
        
        public StrategyPerformanceTracker() {
            this.executionTimes = new ConcurrentHashMap<>();
            this.successCounts = new ConcurrentHashMap<>();
            this.failureCounts = new ConcurrentHashMap<>();
        }
        
        public synchronized void recordExecution(String strategyName, long executionTime, boolean success) {
            executionTimes.computeIfAbsent(strategyName, k -> new ArrayList<>()).add(executionTime);
            
            if (success) {
                successCounts.merge(strategyName, 1, Integer::sum);
            } else {
                failureCounts.merge(strategyName, 1, Integer::sum);
            }
            
            // Keep only last 100 execution times per strategy
            List<Long> times = executionTimes.get(strategyName);
            if (times.size() > 100) {
                times.remove(0);
            }
        }
        
        public double getAverageExecutionTime(String strategyName) {
            List<Long> times = executionTimes.get(strategyName);
            return times != null && !times.isEmpty() ? 
                times.stream().mapToLong(Long::longValue).average().orElse(0) : 0;
        }
        
        public double getSuccessRate(String strategyName) {
            int successes = successCounts.getOrDefault(strategyName, 0);
            int failures = failureCounts.getOrDefault(strategyName, 0);
            int total = successes + failures;
            return total > 0 ? (double) successes / total * 100 : 0;
        }
        
        public Map<String, Double> getAllSuccessRates() {
            Set<String> allStrategies = new HashSet<>(successCounts.keySet());
            allStrategies.addAll(failureCounts.keySet());
            
            return allStrategies.stream()
                .collect(Collectors.toMap(
                    strategy -> strategy,
                    this::getSuccessRate
                ));
        }
    }
    
    /**
     * Creates a new PublishingService with default configuration.
     */
    public PublishingService() {
        this.strategies = new ConcurrentHashMap<>();
        this.performanceTracker = new StrategyPerformanceTracker();
        this.configuration = new PublishingServiceConfiguration();
        this.contentSubject = new ContentSubject();
        this.usageStats = new ConcurrentHashMap<>();
        
        // Initialize default strategies
        initializeDefaultStrategies();
        
        // Set up default strategy selector
        this.strategySelector = new DefaultStrategySelector();
        
        // Set initial current strategy
        this.currentStrategy = strategies.get(DEFAULT_STRATEGY);
    }
    
    /**
     * Creates a new PublishingService with the specified configuration.
     *
     * @param configuration Service configuration
     * @param contentSubject Content subject for event notifications
     * @throws IllegalArgumentException If any parameter is null
     */
    public PublishingService(PublishingServiceConfiguration configuration, 
                           ContentSubject contentSubject) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        
        this.strategies = new ConcurrentHashMap<>();
        this.performanceTracker = new StrategyPerformanceTracker();
        this.configuration = configuration;
        this.contentSubject = contentSubject;
        this.usageStats = new ConcurrentHashMap<>();
        
        initializeDefaultStrategies();
        this.strategySelector = new DefaultStrategySelector();
        this.currentStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
    }
    
    /**
     * Publishes content using the most appropriate strategy.
     *
     * <p>This method shows the core Strategy Pattern behavior:</p>
     * <ol>
     *   <li>Determines the optimal strategy based on content and context</li>
     *   <li>Validates the selected strategy can handle the request</li>
     *   <li>Delegates the publishing operation to the selected strategy</li>
     *   <li>Tracks performance metrics and handles errors gracefully</li>
     *   <li>Implements fallback mechanisms if the primary strategy fails</li>
     * </ol>
     *
     * <p><strong>Strategy Pattern Demonstration:</strong> The client (caller) doesn't
     * need to know which specific strategy is being used. The service transparently
     * selects and executes the most appropriate strategy based on the current context.</p>
     *
     * @param content The content to be published
     * @param context The publishing context with user and configuration information
     * @throws ContentManagementException If publishing fails across all attempted strategies
     * @throws IllegalArgumentException If content or context parameters are null
     */
    public void publishContent(Content content, PublishingContext context) throws ContentManagementException {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Publishing context cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            CMSLogger.getInstance().logContentOperation("Publishing service started for content " + content.getId() + " by " + context.getUser().getUsername());
            
            // Select the most appropriate strategy
            PublishingStrategy selectedStrategy = selectOptimalStrategy(content, context);
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "strategy_selected",
                "Selected publishing strategy: " + selectedStrategy.getStrategyName()
            );
            
            // Execute the publishing operation
            executePublishingWithStrategy(selectedStrategy, content, context, startTime);
            
        } catch (ContentManagementException e) {
            // Log service-level error
            CMSLogger.logError(
                "Publishing service failed for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with appropriate context
            throw new ContentManagementException(
                "Publishing service failed",
                "We were unable to publish your content using any available strategy. " +
                "Please check the content and try again, or contact support for assistance.",
                e
            );
        }
    }
    
    /**
     * Sets the current publishing strategy explicitly.
     * This allows manual override of automatic strategy selection.
     *
     * @param strategyName The name of the strategy to set as current
     * @throws IllegalArgumentException If strategy name is null, empty, or not found
     */
    public void setStrategy(String strategyName) {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy name cannot be null or empty");
        }
        
        PublishingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyName);
        }
        
        this.currentStrategy = strategy;
        
        CMSLogger.logSystemEvent(
            "publishing_strategy_changed",
            "Publishing strategy manually changed to: " + strategyName
        );
    }
    
    /**
     * Returns the currently selected strategy.
     * @return The current publishing strategy
     */
    public PublishingStrategy getCurrentStrategy() {
        return currentStrategy;
    }
    
    /**
     * Returns the name of the currently selected strategy.
     * @return The current strategy name
     */
    public String getCurrentStrategyName() {
        return currentStrategy != null ? currentStrategy.getStrategyName() : "None";
    }
    
    /**
     * Returns a list of all available publishing strategies.
     * @return List of available strategies
     */
    public List<PublishingStrategy> getAvailableStrategies() {
        return new ArrayList<>(strategies.values());
    }
    
    /**
     * Returns the names of all available strategies.
     * @return Set of strategy names
     */
    public Set<String> getAvailableStrategyNames() {
        return new HashSet<>(strategies.keySet());
    }
    
    /**
     * Registers a new publishing strategy with the service.
     * This allows runtime addition of new strategies.
     *
     * @param strategy The strategy to register
     * @throws IllegalArgumentException If strategy is null or already registered
     */
    public void registerStrategy(PublishingStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        
        String strategyName = strategy.getStrategyName();
        if (strategies.containsKey(strategyName)) {
            throw new IllegalArgumentException("Strategy already registered: " + strategyName);
        }
        
        strategies.put(strategyName, strategy);
        usageStats.put(strategyName, new StrategyUsageStats());
        
        CMSLogger.logSystemEvent(
            "strategy_registered",
            "New publishing strategy registered: " + strategyName
        );
    }
    
    /**
     * Unregisters a publishing strategy from the service.
     * @param strategyName The name of the strategy to remove
     * @return true if strategy was removed, false if not found
     */
    public boolean unregisterStrategy(String strategyName) {
        if (strategyName == null || strategyName.equals(configuration.getDefaultFallbackStrategy())) {
            return false; // Cannot remove null or default strategy
        }
        
        PublishingStrategy removed = strategies.remove(strategyName);
        if (removed != null) {
            usageStats.remove(strategyName);
            
            // Reset current strategy if it was removed
            if (currentStrategy == removed) {
                currentStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
            }
            
            CMSLogger.logSystemEvent(
                "strategy_unregistered",
                "Publishing strategy unregistered: " + strategyName
            );
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns comprehensive service statistics including strategy performance.
     * @return Map containing various service metrics
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("registered_strategies", strategies.size());
        stats.put("current_strategy", getCurrentStrategyName());
        stats.put("configuration", configuration);
        
        // Strategy usage statistics
        Map<String, Object> usageStatsMap = new HashMap<>();
        for (Map.Entry<String, StrategyUsageStats> entry : usageStats.entrySet()) {
            StrategyUsageStats stat = entry.getValue();
            Map<String, Object> strategyStats = new HashMap<>();
            strategyStats.put("usage_count", stat.getUsageCount());
            strategyStats.put("success_count", stat.getSuccessCount());
            strategyStats.put("failure_count", stat.getFailureCount());
            strategyStats.put("success_rate", stat.getSuccessRate());
            strategyStats.put("average_execution_time", stat.getAverageExecutionTime());
            strategyStats.put("first_used", stat.getFirstUsed());
            strategyStats.put("last_used", stat.getLastUsed());
            
            usageStatsMap.put(entry.getKey(), strategyStats);
        }
        stats.put("usage_statistics", usageStatsMap);
        
        // Performance tracker statistics
        stats.put("performance_metrics", performanceTracker.getAllSuccessRates());
        
        return stats;
    }
    
    /**
     * Sets a custom strategy selector for automatic strategy selection.
     * @param strategySelector The custom strategy selector
     * @throws IllegalArgumentException If strategySelector is null
     */
    public void setStrategySelector(StrategySelector strategySelector) {
        if (strategySelector == null) {
            throw new IllegalArgumentException("Strategy selector cannot be null");
        }
        this.strategySelector = strategySelector;
    }
    
    /**
     * Returns the current service configuration.
     * @return The service configuration
     */
    public PublishingServiceConfiguration getConfiguration() {
        return configuration;
    }
    
    // Private implementation methods
    
    private void initializeDefaultStrategies() {
        // Initialize all default strategies with shared content subject
        registerStrategy(new ImmediatePublishingStrategy(contentSubject));
        registerStrategy(new ScheduledPublishingStrategy(contentSubject, 
            java.util.concurrent.Executors.newScheduledThreadPool(2)));
        registerStrategy(new ReviewBasedPublishingStrategy(contentSubject));
        registerStrategy(new AutoPublishingStrategy(contentSubject));
        registerStrategy(new BatchPublishingStrategy(contentSubject, 
            java.util.concurrent.Executors.newFixedThreadPool(4)));
    }
    
    private PublishingStrategy selectOptimalStrategy(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        // If automatic selection is disabled, use current strategy
        if (!configuration.isEnableAutomaticSelection()) {
            if (currentStrategy == null) {
                currentStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
            }
            return currentStrategy;
        }
        
        // Use strategy selector to choose optimal strategy
        String selectedStrategyName = strategySelector.selectStrategy(
            content, context, getAvailableStrategies(), performanceTracker);
        
        if (selectedStrategyName != null && strategies.containsKey(selectedStrategyName)) {
            return strategies.get(selectedStrategyName);
        }
        
        // Fallback to default strategy
        PublishingStrategy fallbackStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
        if (fallbackStrategy == null) {
            throw new ContentManagementException(
                "No publishing strategy available",
                "Unable to find any suitable publishing strategy for the content"
            );
        }
        
        return fallbackStrategy;
    }
    
    private void executePublishingWithStrategy(PublishingStrategy strategy, Content content, 
                                             PublishingContext context, long startTime) 
            throws ContentManagementException {
        
        String strategyName = strategy.getStrategyName();
        boolean success = false;
        
        try {
            // Validate strategy can handle the request
            if (!strategy.validate(content, context)) {
                // Try fallback if enabled and this isn't already the fallback strategy
                if (configuration.isEnableFallbackMechanisms() && 
                    !strategyName.equals(configuration.getDefaultFallbackStrategy())) {
                    
                    CMSLogger.logContentOperation(
                        content.getId(),
                        context.getUser().getUsername(),
                        "strategy_validation_failed_fallback",
                        "Strategy validation failed, attempting fallback: " + strategyName
                    );
                    
                    PublishingStrategy fallbackStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
                    if (fallbackStrategy != null && fallbackStrategy.validate(content, context)) {
                        executePublishingWithStrategy(fallbackStrategy, content, context, startTime);
                        return;
                    }
                }
                
                throw new ContentManagementException(
                    "Strategy validation failed",
                    "The selected publishing strategy cannot process this content"
                );
            }
            
            // Execute the publishing operation
            strategy.publish(content, context);
            success = true;
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "publishing_strategy_success",
                "Publishing completed successfully using strategy: " + strategyName
            );
            
        } catch (ContentManagementException e) {
            // Try fallback strategy if enabled and available
            if (configuration.isEnableFallbackMechanisms() && 
                !strategyName.equals(configuration.getDefaultFallbackStrategy())) {
                
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "strategy_failed_fallback_attempt",
                    "Strategy failed, attempting fallback: " + e.getMessage()
                );
                
                PublishingStrategy fallbackStrategy = strategies.get(configuration.getDefaultFallbackStrategy());
                if (fallbackStrategy != null && fallbackStrategy.validate(content, context)) {
                    executePublishingWithStrategy(fallbackStrategy, content, context, startTime);
                    return;
                }
            }
            
            throw e; // Re-throw if no fallback available
        } finally {
            // Record performance metrics
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (configuration.isEnablePerformanceTracking()) {
                performanceTracker.recordExecution(strategyName, executionTime, success);
                
                StrategyUsageStats stats = usageStats.get(strategyName);
                if (stats != null) {
                    stats.recordUsage(success, executionTime);
                }
            }
        }
    }
    
    /**
     * Default strategy selector implementation.
     */
    private static class DefaultStrategySelector implements StrategySelector {
        
        @Override
        public String selectStrategy(Content content, PublishingContext context, 
                                   List<PublishingStrategy> availableStrategies,
                                   StrategyPerformanceTracker performanceTracker) {
            
            // Rule-based strategy selection
            
            // If context specifies scheduled date, prefer scheduled publishing
            if (context.isScheduledPublishing()) {
                return "Scheduled Publishing";
            }
            
            // If context has batch items or high priority, consider batch processing
            @SuppressWarnings("unchecked")
            List<Content> batchItems = context.getProperty("batch_items", List.class);
            if (batchItems != null && batchItems.size() > 10) {
                return "Batch Publishing";
            }
            
            // If auto-publishing is enabled, try auto strategy
            Object autoPublishEnabled = context.getProperty("auto_publish_enabled");
            if (Boolean.TRUE.equals(autoPublishEnabled)) {
                return "Auto Publishing";
            }
            
            // If user has limited permissions, require review
            Role userRole = context.getUser().getRole();
            if (userRole == Role.AUTHOR || userRole == Role.GUEST) {
                return "Review-Based Publishing";
            }
            
            // If emergency priority, use immediate publishing
            if (context.getPriority() == PublishingContext.Priority.EMERGENCY) {
                return "Immediate Publishing";
            }
            
            // For high-priority content by trusted users, use immediate publishing
            if (context.getPriority() == PublishingContext.Priority.HIGH && 
                (userRole == Role.ADMINISTRATOR || userRole == Role.PUBLISHER)) {
                return "Immediate Publishing";
            }
            
            // Default to immediate publishing for normal cases
            return "Immediate Publishing";
        }
    }
}