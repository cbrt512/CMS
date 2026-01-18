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
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-publishing strategy that automatically publishes content based on predefined rules.
 *
 * <p>The AutoPublishingStrategy implements the Strategy Pattern by providing
 * a concrete algorithm for rule-based automatic content publishing. This strategy
 * evaluates content against configurable business rules and automatically publishes
 * content that meets all specified criteria without manual intervention.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as a concrete strategy implementation that encapsulates intelligent auto-publishing
 * algorithms. It shows the Strategy Pattern's capability to encapsulate
 * complex decision-making logic while maintaining interface consistency with
 * other publishing strategies.</p>
 *
 * <p><strong>Implementation:</strong> Implements part of the Strategy Pattern
 * implementation by providing a rule-based publishing algorithm that
 * showcases advanced business logic, pattern matching, and automated decision
 * making based on configurable criteria.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>RSS feed updates and syndication</li>
 *   <li>Social media content automation</li>
 *   <li>News aggregation and distribution</li>
 *   <li>E-commerce product catalog updates</li>
 *   <li>Blog post publishing based on content quality metrics</li>
 *   <li>Documentation updates with version control integration</li>
 *   <li>Marketing campaign content with trigger-based activation</li>
 * </ul>
 *
 * <p><strong>Strategy Characteristics:</strong></p>
 * <ul>
 *   <li>High Priority (80) - Automated systems need reliable execution</li>
 *   <li>Supports Batch Processing - Can evaluate and publish multiple items efficiently</li>
 *   <li>Limited Rollback Support - Depends on specific auto-publish rules</li>
 *   <li>Fast Processing Time - Optimized for automated operations</li>
 *   <li>Rule-Based Logic - Configurable criteria for automatic publishing</li>
 * </ul>
 *
 * <p><strong>Auto-Publishing Rule Categories:</strong></p>
 * <ul>
 *   <li><strong>Content Quality Rules:</strong> Word count, readability, completeness</li>
 *   <li><strong>Time-Based Rules:</strong> Creation date, modification frequency</li>
 *   <li><strong>Author-Based Rules:</strong> Author reputation, role permissions</li>
 *   <li><strong>Category-Based Rules:</strong> Content type, tags, metadata</li>
 *   <li><strong>Engagement Rules:</strong> Preview statistics, feedback scores</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For context parameter details
 * @see ImmediatePublishingStrategy For manual immediate publishing
 */
public class AutoPublishingStrategy implements PublishingStrategy {
    
    /** Strategy name identifier */
    private static final String STRATEGY_NAME = "Auto Publishing";
    
    /** Priority level for auto publishing (high priority for automation) */
    private static final int PRIORITY = 80;
    
    /** Base estimated processing time in milliseconds */
    private static final long BASE_PROCESSING_TIME = 750L;
    
    /** Default minimum word count for auto-publishing */
    private static final int DEFAULT_MIN_WORD_COUNT = 100;
    
    /** Default minimum title length for auto-publishing */
    private static final int DEFAULT_MIN_TITLE_LENGTH = 10;
    
    /** Observer subject for publishing events (integration with Observer Pattern) */
    private final ContentSubject contentSubject;
    
    /** Map to store auto-publishing rules by category */
    private final Map<String, List<AutoPublishRule>> publishingRules;
    
    /** Map to track auto-publishing statistics */
    private final Map<String, AutoPublishingStats> publishingStats;
    
    /** Set of content types that support auto-publishing */
    private final Set<String> supportedContentTypes;
    
    /**
     * Interface for auto-publishing rules.
     */
    public interface AutoPublishRule {
        /**
         * Evaluates whether the content meets this rule's criteria.
         * @param content The content to evaluate
         * @param context The publishing context
         * @return true if the rule is satisfied, false otherwise
         */
        boolean evaluate(Content content, PublishingContext context);
        
        /**
         * Returns the rule name for identification and logging.
         * @return The rule name
         */
        String getRuleName();
        
        /**
         * Returns the rule description for documentation.
         * @return The rule description
         */
        String getDescription();
        
        /**
         * Returns the rule priority for conflict resolution.
         * Higher values indicate higher priority.
         * @return The rule priority (0-100)
         */
        default int getPriority() {
            return 50;
        }
    }
    
    /**
     * Statistics tracking for auto-publishing operations.
     */
    public static class AutoPublishingStats {
        private int totalEvaluations;
        private int successfulPublications;
        private int failedRuleChecks;
        private int rulePrevented;
        private long totalProcessingTime;
        private final Map<String, Integer> ruleSuccessCount;
        private final Date lastUpdate;
        
        public AutoPublishingStats() {
            this.ruleSuccessCount = new HashMap<>();
            this.lastUpdate = new Date();
        }
        
        public void recordEvaluation() { totalEvaluations++; }
        public void recordSuccess() { successfulPublications++; }
        public void recordRuleFailure() { failedRuleChecks++; }
        public void recordRulePrevention() { rulePrevented++; }
        public void addProcessingTime(long time) { totalProcessingTime += time; }
        public void recordRuleSuccess(String ruleName) {
            ruleSuccessCount.merge(ruleName, 1, Integer::sum);
        }
        
        // Getters
        public int getTotalEvaluations() { return totalEvaluations; }
        public int getSuccessfulPublications() { return successfulPublications; }
        public int getFailedRuleChecks() { return failedRuleChecks; }
        public int getRulePrevented() { return rulePrevented; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public Map<String, Integer> getRuleSuccessCount() { return new HashMap<>(ruleSuccessCount); }
        public Date getLastUpdate() { return new Date(lastUpdate.getTime()); }
        
        public double getSuccessRate() {
            return totalEvaluations > 0 ? (double) successfulPublications / totalEvaluations : 0.0;
        }
        
        public double getAverageProcessingTime() {
            return totalEvaluations > 0 ? (double) totalProcessingTime / totalEvaluations : 0.0;
        }
    }
    
    /**
     * Creates a new AutoPublishingStrategy with default configuration.
     */
    public AutoPublishingStrategy() {
        this.contentSubject = new ContentSubject();
        this.publishingRules = new ConcurrentHashMap<>();
        this.publishingStats = new ConcurrentHashMap<>();
        this.supportedContentTypes = ConcurrentHashMap.newKeySet();
        
        // Initialize default rules and supported content types
        initializeDefaultRules();
        initializeSupportedContentTypes();
    }
    
    /**
     * Creates a new AutoPublishingStrategy with the specified content subject.
     *
     * @param contentSubject The content subject for event notifications
     * @throws IllegalArgumentException If contentSubject is null
     */
    public AutoPublishingStrategy(ContentSubject contentSubject) {
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        
        this.contentSubject = contentSubject;
        this.publishingRules = new ConcurrentHashMap<>();
        this.publishingStats = new ConcurrentHashMap<>();
        this.supportedContentTypes = ConcurrentHashMap.newKeySet();
        
        initializeDefaultRules();
        initializeSupportedContentTypes();
    }
    
    /**
     * Evaluates content against auto-publishing rules and publishes if criteria are met.
     *
     * <p>This method implements the core auto-publishing algorithm:</p>
     * <ol>
     *   <li>Validates content and context for auto-publishing eligibility</li>
     *   <li>Retrieves applicable rules based on content type and context</li>
     *   <li>Evaluates content against all applicable rules</li>
     *   <li>Makes publishing decision based on rule evaluation results</li>
     *   <li>Executes automatic publishing if all criteria are met</li>
     *   <li>Fires Observer Pattern events for notifications and audit</li>
     *   <li>Updates statistics and logs the auto-publishing operation</li>
     * </ol>
     *
     * <p><strong>Strategy-Specific Behavior:</strong> This implementation focuses
     * on intelligent rule evaluation, comprehensive logging, and statistics tracking.
     * It provides detailed feedback about why content was or wasn't auto-published
     * and maintains performance metrics for optimization.</p>
     *
     * @param content The content to be evaluated for auto-publishing
     * @param context The publishing context with configuration and user information
     * @throws ContentManagementException If auto-publishing evaluation fails due to
     *         system errors, rule evaluation problems, or publishing constraints.
     *         Uses Exception Shielding to provide user-friendly messages while
     *         logging technical details for debugging.
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
        
        long startTime = System.currentTimeMillis();
        String contentType = determineContentType(content);
        AutoPublishingStats stats = publishingStats.computeIfAbsent(contentType, k -> new AutoPublishingStats());
        
        try {
            stats.recordEvaluation();
            
            // Log the start of auto-publishing evaluation
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "auto_publish_start",
                "Starting auto-publishing evaluation for content: " + content.getTitle()
            );
            
            // Pre-evaluation validation
            if (!validate(content, context)) {
                stats.recordRuleFailure();
                throw new ContentManagementException(
                    "Content validation failed for auto-publishing",
                    "The content does not meet the basic implementations for auto-publishing. " +
                    "Please check content completeness and auto-publishing configuration."
                );
            }
            
            // Execute the auto-publishing evaluation algorithm
            AutoPublishingResult result = executeAutoPublishingEvaluation(content, context);
            
            if (result.shouldPublish()) {
                // Content meets all criteria - publish automatically
                executeAutomaticPublishing(content, context, result);
                stats.recordSuccess();
                
                // Fire Observer Pattern events for successful auto-publishing
                fireAutoPublishingEvent(content, context, ContentEvent.EventType.CONTENT_PUBLISHED, result);
                
                // Log successful auto-publishing
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "auto_publish_success",
                    String.format("Successfully auto-published content '%s'. Rules satisfied: %s", 
                                 content.getTitle(), result.getSatisfiedRules())
                );
                
            } else {
                // Content doesn't meet criteria - log reasons
                stats.recordRulePrevention();
                
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "auto_publish_prevented",
                    String.format("Auto-publishing prevented for content '%s'. Failed rules: %s", 
                                 content.getTitle(), result.getFailedRules())
                );
                
                // Fire event for auto-publishing prevention
                fireAutoPublishingEvent(content, context, ContentEvent.EventType.CONTENT_PROCESSING_COMPLETED, result);
            }
            
        } catch (ContentManagementException e) {
            stats.recordRuleFailure();
            
            // Log the error with technical details for debugging
            CMSLogger.logError(
                "Failed to evaluate content for auto-publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with Exception Shielding
            throw new ContentManagementException(
                "Auto-publishing evaluation failed",
                "We were unable to evaluate your content for auto-publishing. " +
                "Please check the content and auto-publishing rules, or try manual publishing.",
                e
            );
        } catch (Exception e) {
            stats.recordRuleFailure();
            
            // Log unexpected errors
            CMSLogger.logError(
                "Unexpected error during auto-publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Wrap in ContentManagementException with Exception Shielding
            throw new ContentManagementException(
                "Unexpected auto-publishing error",
                "An unexpected error occurred during auto-publishing evaluation. " +
                "Our technical team has been notified. Please try manual publishing.",
                e
            );
        } finally {
            // Update processing time statistics
            long processingTime = System.currentTimeMillis() - startTime;
            stats.addProcessingTime(processingTime);
        }
    }
    
    /**
     * Validates whether content can be evaluated for auto-publishing.
     *
     * <p><strong>Auto-Publishing Validation Rules:</strong></p>
     * <ul>
     *   <li>All basic content validation (ID, title, body)</li>
     *   <li>Content type must support auto-publishing</li>
     *   <li>Content must not already be published</li>
     *   <li>User must have permission for auto-publishing (or system user)</li>
     *   <li>Auto-publishing must be enabled in the context</li>
     *   <li>At least one applicable rule must exist for the content type</li>
     * </ul>
     *
     * @param content The content to validate for auto-publishing
     * @param context The publishing context with configuration information
     * @return true if content can be evaluated for auto-publishing, false otherwise
     * @throws IllegalArgumentException If content or context parameters are null
     */
    @Override
    public boolean validate(Content content, PublishingContext context) {
        if (content == null || context == null) {
            throw new IllegalArgumentException("Content and context cannot be null");
        }
        
        try {
            // Basic content validation
            if (!validateBasicContent(content, context)) {
                return false;
            }
            
            // Check if content type supports auto-publishing
            String contentType = determineContentType(content);
            if (!supportedContentTypes.contains(contentType)) {
                CMSLogger.logValidationError(
                    "Content type not supported for auto-publishing: " + contentType,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Content status validation
            ContentStatus status = content.getStatus();
            if (!isValidStatusForAutoPublishing(status)) {
                CMSLogger.logValidationError(
                    "Invalid content status for auto-publishing: " + status,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Check if auto-publishing is enabled in context
            if (!isAutoPublishingEnabled(context)) {
                CMSLogger.logValidationError(
                    "Auto-publishing is disabled in the context",
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Check if applicable rules exist
            List<AutoPublishRule> applicableRules = getApplicableRules(content, context);
            if (applicableRules.isEmpty()) {
                CMSLogger.logValidationError(
                    "No auto-publishing rules found for content type: " + contentType,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // User permission validation (or system process)
            if (!hasAutoPublishingPermission(context.getUser())) {
                CMSLogger.logSecurityEvent(
                    context.getUser().getUsername(),
                    "unauthorized_auto_publishing",
                    "User attempted auto-publishing without proper permissions"
                );
                return false;
            }
            
            // All validations passed
            CMSLogger.logValidationSuccess(
                "Auto-publishing validation passed for content: " + content.getId(),
                context.getUser().getUsername()
            );
            
            return true;
            
        } catch (Exception e) {
            // Log validation errors but don't throw - return false for failed validation
            CMSLogger.logError(
                "Error during auto-publishing validation: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
    
    /**
     * Returns the strategy name for identification purposes.
     * @return "Auto Publishing"
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Returns the priority level for this strategy.
     * Auto-publishing has high priority (80) for reliable automated operations.
     * @return 80 (high priority)
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
        return "Automatically evaluates content against configurable business rules and " +
               "publishes content that meets all specified criteria without manual intervention. " +
               "Ideal for automated content workflows, RSS feeds, social media integration, " +
               "and high-volume content scenarios. Features comprehensive rule evaluation, " +
               "statistics tracking, and detailed audit logging for automated publishing decisions.";
    }
    
    /**
     * Indicates that this strategy supports batch processing.
     * Multiple content items can be evaluated efficiently in batches.
     * @return true - batch processing is supported
     */
    @Override
    public boolean supportsBatchProcessing() {
        return true;
    }
    
    /**
     * Indicates limited rollback support for auto-publishing.
     * Rollback availability depends on the specific auto-publishing rules.
     * @return true - limited rollback support
     */
    @Override
    public boolean supportsRollback() {
        return true; // Limited support - depends on rules
    }
    
    /**
     * Returns the estimated processing time for auto-publishing evaluation.
     * This includes rule evaluation, decision making, and publishing overhead.
     * @param content The content to be evaluated
     * @param context The publishing context
     * @return Estimated processing time in milliseconds
     */
    @Override
    public long getEstimatedProcessingTime(Content content, PublishingContext context) {
        long baseTime = BASE_PROCESSING_TIME;
        
        // Adjust based on content complexity
        if (content != null && content.getBody() != null) {
            int contentLength = content.getBody().length();
            if (contentLength > 5000) {
                baseTime += 200; // Extra time for content analysis
            }
        }
        
        // Adjust based on number of applicable rules
        if (content != null && context != null) {
            List<AutoPublishRule> rules = getApplicableRules(content, context);
            baseTime += rules.size() * 50; // Time per rule evaluation
        }
        
        // Adjust based on notification settings
        if (context != null && context.isSendNotifications()) {
            baseTime += 150; // Time for notification processing
        }
        
        return baseTime;
    }
    
    /**
     * Result object containing auto-publishing evaluation results.
     */
    public static class AutoPublishingResult {
        private final boolean shouldPublish;
        private final List<String> satisfiedRules;
        private final List<String> failedRules;
        private final String reason;
        private final long evaluationTime;
        
        public AutoPublishingResult(boolean shouldPublish, List<String> satisfiedRules,
                                   List<String> failedRules, String reason, long evaluationTime) {
            this.shouldPublish = shouldPublish;
            this.satisfiedRules = new ArrayList<>(satisfiedRules);
            this.failedRules = new ArrayList<>(failedRules);
            this.reason = reason;
            this.evaluationTime = evaluationTime;
        }
        
        public boolean shouldPublish() { return shouldPublish; }
        public List<String> getSatisfiedRules() { return new ArrayList<>(satisfiedRules); }
        public List<String> getFailedRules() { return new ArrayList<>(failedRules); }
        public String getReason() { return reason; }
        public long getEvaluationTime() { return evaluationTime; }
    }
    
    // Implementation methods
    
    private AutoPublishingResult executeAutoPublishingEvaluation(Content content, PublishingContext context) {
        long startTime = System.currentTimeMillis();
        
        List<AutoPublishRule> applicableRules = getApplicableRules(content, context);
        List<String> satisfiedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
        
        boolean allRulesSatisfied = true;
        
        // Evaluate each applicable rule
        for (AutoPublishRule rule : applicableRules) {
            try {
                if (rule.evaluate(content, context)) {
                    satisfiedRules.add(rule.getRuleName());
                    publishingStats.get(determineContentType(content)).recordRuleSuccess(rule.getRuleName());
                } else {
                    failedRules.add(rule.getRuleName());
                    allRulesSatisfied = false;
                }
                
                CMSLogger.logContentOperation(
                    content.getId(),
                    context.getUser().getUsername(),
                    "auto_publish_rule_evaluated",
                    String.format("Rule '%s' evaluation: %s", rule.getRuleName(), 
                                 satisfiedRules.contains(rule.getRuleName()) ? "PASSED" : "FAILED")
                );
                
            } catch (Exception e) {
                CMSLogger.logError(
                    "Error evaluating auto-publishing rule: " + rule.getRuleName(),
                    e,
                    context.getUser().getUsername()
                );
                failedRules.add(rule.getRuleName());
                allRulesSatisfied = false;
            }
        }
        
        long evaluationTime = System.currentTimeMillis() - startTime;
        
        String reason = allRulesSatisfied ? 
            "All auto-publishing rules satisfied" : 
            "One or more auto-publishing rules failed: " + String.join(", ", failedRules);
        
        return new AutoPublishingResult(allRulesSatisfied, satisfiedRules, failedRules, reason, evaluationTime);
    }
    
    private void executeAutomaticPublishing(Content content, PublishingContext context, AutoPublishingResult result) 
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
            content.setLastModifiedBy("auto-publisher"); // System user for auto-publishing
            
            // Update content metadata with auto-publishing information
            if (content.getMetadata() != null) {
                content.getMetadata().put("auto_published", true);
                content.getMetadata().put("publishing_strategy", STRATEGY_NAME);
                content.getMetadata().put("satisfied_rules", result.getSatisfiedRules());
                content.getMetadata().put("evaluation_time_ms", result.getEvaluationTime());
                content.getMetadata().put("auto_publish_timestamp", now.toInstant(ZoneOffset.UTC).toEpochMilli());
            }
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "content_auto_published",
                String.format("Content auto-published successfully. Evaluation time: %d ms", 
                             result.getEvaluationTime())
            );
            
        } catch (Exception e) {
            throw new ContentManagementException(
                "Failed to execute automatic publishing",
                "Unable to complete the automatic publishing process",
                e
            );
        }
    }
    
    private void fireAutoPublishingEvent(Content content, PublishingContext context,
                                       ContentEvent.EventType eventType, AutoPublishingResult result) {
        try {
            ContentEvent event = ContentEvent.builder(eventType, content)
                .user(context.getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("auto_publishing", true)
                .metadata("satisfied_rules", result.getSatisfiedRules())
                .metadata("failed_rules", result.getFailedRules())
                .metadata("evaluation_time_ms", result.getEvaluationTime())
                .metadata("should_publish", result.shouldPublish())
                .build();
            
            contentSubject.notifyObservers(event);
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire auto-publishing event for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
        }
    }
    
    private List<AutoPublishRule> getApplicableRules(Content content, PublishingContext context) {
        String contentType = determineContentType(content);
        List<AutoPublishRule> rules = publishingRules.getOrDefault(contentType, Collections.emptyList());
        
        // Also include general rules
        List<AutoPublishRule> generalRules = publishingRules.getOrDefault("general", Collections.emptyList());
        
        List<AutoPublishRule> allRules = new ArrayList<>(rules);
        allRules.addAll(generalRules);
        
        // Sort rules by priority (highest first)
        allRules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        return allRules;
    }
    
    private void initializeDefaultRules() {
        // Content quality rules
        List<AutoPublishRule> generalRules = new ArrayList<>();
        
        // Minimum word count rule
        generalRules.add(new AutoPublishRule() {
            @Override
            public boolean evaluate(Content content, PublishingContext context) {
                if (content.getBody() == null) return false;
                String[] words = content.getBody().trim().split("\\s+");
                return words.length >= DEFAULT_MIN_WORD_COUNT;
            }
            
            @Override
            public String getRuleName() {
                return "MinimumWordCount";
            }
            
            @Override
            public String getDescription() {
                return "Content must have at least " + DEFAULT_MIN_WORD_COUNT + " words";
            }
            
            @Override
            public int getPriority() {
                return 90;
            }
        });
        
        // Title length rule
        generalRules.add(new AutoPublishRule() {
            @Override
            public boolean evaluate(Content content, PublishingContext context) {
                return content.getTitle() != null && 
                       content.getTitle().trim().length() >= DEFAULT_MIN_TITLE_LENGTH;
            }
            
            @Override
            public String getRuleName() {
                return "MinimumTitleLength";
            }
            
            @Override
            public String getDescription() {
                return "Content title must have at least " + DEFAULT_MIN_TITLE_LENGTH + " characters";
            }
            
            @Override
            public int getPriority() {
                return 95;
            }
        });
        
        // Author permission rule
        generalRules.add(new AutoPublishRule() {
            @Override
            public boolean evaluate(Content content, PublishingContext context) {
                Role userRole = context.getUser().getRole();
                return userRole == Role.ADMINISTRATOR || 
                       userRole == Role.EDITOR || 
                       userRole == Role.PUBLISHER;
            }
            
            @Override
            public String getRuleName() {
                return "AuthorPermission";
            }
            
            @Override
            public String getDescription() {
                return "Author must have publishing permissions";
            }
            
            @Override
            public int getPriority() {
                return 100;
            }
        });
        
        publishingRules.put("general", generalRules);
        
        // Article-specific rules
        List<AutoPublishRule> articleRules = new ArrayList<>();
        articleRules.add(new AutoPublishRule() {
            @Override
            public boolean evaluate(Content content, PublishingContext context) {
                // Articles should have categories
                return content.getMetadata() != null && 
                       content.getMetadata().containsKey("category");
            }
            
            @Override
            public String getRuleName() {
                return "ArticleHasCategory";
            }
            
            @Override
            public String getDescription() {
                return "Articles must be assigned to a category";
            }
        });
        
        publishingRules.put("article", articleRules);
    }
    
    private void initializeSupportedContentTypes() {
        supportedContentTypes.add("article");
        supportedContentTypes.add("page");
        supportedContentTypes.add("general");
    }
    
    private String determineContentType(Content content) {
        String className = content.getClass().getSimpleName().toLowerCase();
        if (className.contains("article")) return "article";
        if (className.contains("page")) return "page";
        return "general";
    }
    
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
    
    private boolean isValidStatusForAutoPublishing(ContentStatus status) {
        return status == ContentStatus.DRAFT || status == ContentStatus.REVIEW;
    }
    
    private boolean isAutoPublishingEnabled(PublishingContext context) {
        // Check if auto-publishing is enabled in context properties
        Object autoPublishEnabled = context.getProperty("auto_publish_enabled");
        return autoPublishEnabled == null || Boolean.TRUE.equals(autoPublishEnabled);
    }
    
    private boolean hasAutoPublishingPermission(com.cms.core.model.User user) {
        Role role = user.getRole();
        return role == Role.ADMINISTRATOR || 
               role == Role.PUBLISHER || 
               role == Role.EDITOR;
    }
    
    /**
     * Returns current auto-publishing statistics.
     * @return Map of content type to statistics
     */
    public Map<String, AutoPublishingStats> getPublishingStatistics() {
        return new HashMap<>(publishingStats);
    }
    
    /**
     * Adds a custom auto-publishing rule for the specified content type.
     * @param contentType The content type
     * @param rule The rule to add
     */
    public void addRule(String contentType, AutoPublishRule rule) {
        publishingRules.computeIfAbsent(contentType, k -> new ArrayList<>()).add(rule);
    }
    
    /**
     * Removes a rule by name for the specified content type.
     * @param contentType The content type
     * @param ruleName The rule name to remove
     * @return true if a rule was removed, false otherwise
     */
    public boolean removeRule(String contentType, String ruleName) {
        List<AutoPublishRule> rules = publishingRules.get(contentType);
        if (rules != null) {
            return rules.removeIf(rule -> rule.getRuleName().equals(ruleName));
        }
        return false;
    }
}