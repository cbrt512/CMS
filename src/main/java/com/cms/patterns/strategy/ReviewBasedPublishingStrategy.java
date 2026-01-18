package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.core.model.Role;
import com.cms.core.model.User;
import com.cms.core.exception.ContentManagementException;
import com.cms.patterns.observer.ContentEvent;
import com.cms.patterns.observer.ContentSubject;
import com.cms.util.CMSLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Review-based publishing strategy that requires approval workflow before publication.
 *
 * <p>The ReviewBasedPublishingStrategy implements the Strategy Pattern by providing
 * a concrete algorithm for content publishing that incorporates an approval workflow.
 * This strategy ensures content quality and compliance by requiring designated
 * reviewers to approve content before it can be published to the public.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This class serves
 * as a concrete strategy implementation that encapsulates the review-based publishing
 * algorithm. It shows the Strategy Pattern's ability to encapsulate complex
 * business logic while maintaining interface consistency with other publishing strategies.</p>
 *
 * <p><strong>Implementation:</strong> Implements part of the Strategy Pattern
 * implementation by providing a workflow-based publishing algorithm that
 * showcases advanced business logic, state management, and integration with user
 * role management and approval processes.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Corporate content requiring legal or compliance review</li>
 *   <li>Press releases and official announcements</li>
 *   <li>Marketing materials requiring brand approval</li>
 *   <li>Technical documentation requiring expert review</li>
 *   <li>Financial content requiring accuracy validation</li>
 *   <li>Educational content requiring pedagogical review</li>
 *   <li>Multi-author collaborative content</li>
 * </ul>
 *
 * <p><strong>Strategy Characteristics:</strong></p>
 * <ul>
 *   <li>Normal Priority (50) - Balanced priority for quality control</li>
 *   <li>Supports Batch Processing - Can handle multiple items in approval workflow</li>
 *   <li>Rollback Support - Can withdraw from review or unpublish after approval</li>
 *   <li>Variable Processing Time - Depends on reviewer availability and content complexity</li>
 *   <li>Advanced Workflow - Multi-stage approval with delegation support</li>
 * </ul>
 *
 * <p><strong>Approval Workflow Stages:</strong></p>
 * <ol>
 *   <li><strong>Submission:</strong> Author submits content for review</li>
 *   <li><strong>Review Assignment:</strong> System assigns or allows manual reviewer selection</li>
 *   <li><strong>Review Process:</strong> Reviewers examine content and provide feedback</li>
 *   <li><strong>Approval/Rejection:</strong> Reviewers approve, reject, or request revisions</li>
 *   <li><strong>Publication:</strong> Approved content is automatically published</li>
 * </ol>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingStrategy For the strategy interface
 * @see PublishingContext For context parameter details
 * @see ImmediatePublishingStrategy For immediate publishing without review
 */
public class ReviewBasedPublishingStrategy implements PublishingStrategy {
    
    /** Strategy name identifier */
    private static final String STRATEGY_NAME = "Review-Based Publishing";
    
    /** Priority level for review-based publishing (normal priority) */
    private static final int PRIORITY = 50;
    
    /** Base estimated processing time in milliseconds */
    private static final long BASE_PROCESSING_TIME = 2000L;
    
    /** Maximum number of reviewers per content item */
    private static final int MAX_REVIEWERS_PER_CONTENT = 5;
    
    /** Minimum number of needed approvals (can be configured per content type) */
    private static final int DEFAULT_needed_APPROVALS = 1;
    
    /** Observer subject for publishing events (integration with Observer Pattern) */
    private final ContentSubject contentSubject;
    
    /** Map to track content in review process with review metadata */
    private final Map<String, ReviewProcess> contentInReview;
    
    /** Map to track reviewer assignments and workload */
    private final Map<String, Set<String>> reviewerAssignments;
    
    /** Map to store review configuration per content type */
    private final Map<String, ReviewConfiguration> reviewConfigurations;
    
    /**
     * Review process state tracking for content items.
     */
    public static class ReviewProcess {
        private final String contentId;
        private final String submittedBy;
        private final Date submissionDate;
        private final Set<String> assignedReviewers;
        private final Map<String, ReviewDecision> reviewDecisions;
        private final List<String> reviewComments;
        private ReviewStatus status;
        private int neededApprovals;
        
        public ReviewProcess(String contentId, String submittedBy, int neededApprovals) {
            this.contentId = contentId;
            this.submittedBy = submittedBy;
            this.submissionDate = new Date();
            this.assignedReviewers = new HashSet<>();
            this.reviewDecisions = new HashMap<>();
            this.reviewComments = new ArrayList<>();
            this.status = ReviewStatus.PENDING_REVIEW;
            this.neededApprovals = neededApprovals;
        }
        
        // Getters and setters
        public String getContentId() { return contentId; }
        public String getSubmittedBy() { return submittedBy; }
        public Date getSubmissionDate() { return new Date(submissionDate.getTime()); }
        public Set<String> getAssignedReviewers() { return new HashSet<>(assignedReviewers); }
        public Map<String, ReviewDecision> getReviewDecisions() { return new HashMap<>(reviewDecisions); }
        public List<String> getReviewComments() { return new ArrayList<>(reviewComments); }
        public ReviewStatus getStatus() { return status; }
        public void setStatus(ReviewStatus status) { this.status = status; }
        public int getneededApprovals() { return neededApprovals; }
        
        public void addReviewer(String reviewerUsername) {
            assignedReviewers.add(reviewerUsername);
        }
        
        public void addReviewDecision(String reviewerUsername, ReviewDecision decision, String comment) {
            reviewDecisions.put(reviewerUsername, decision);
            if (comment != null && !comment.trim().isEmpty()) {
                reviewComments.add(String.format("[%s] %s: %s", 
                    new Date(), reviewerUsername, comment.trim()));
            }
        }
        
        public boolean hasneededApprovals() {
            long approvals = reviewDecisions.values().stream()
                .filter(decision -> decision == ReviewDecision.APPROVED)
                .count();
            return approvals >= neededApprovals;
        }
        
        public boolean hasAnyRejection() {
            return reviewDecisions.values().contains(ReviewDecision.REJECTED);
        }
    }
    
    /**
     * Review decision enumeration.
     */
    public enum ReviewDecision {
        APPROVED("Approved for publication"),
        REJECTED("Rejected - requires revision"),
        NEEDS_REVISION("Approved with minor revisions needed"),
        PENDING("Review in progress");
        
        private final String description;
        
        ReviewDecision(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Review status enumeration for tracking workflow state.
     */
    public enum ReviewStatus {
        PENDING_REVIEW("Waiting for reviewer assignment"),
        IN_REVIEW("Under active review"),
        APPROVED("Approved for publication"),
        REJECTED("Rejected - requires revision"),
        PUBLISHED("Published after approval"),
        WITHDRAWN("Withdrawn from review");
        
        private final String description;
        
        ReviewStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Configuration for review implementations per content type.
     */
    public static class ReviewConfiguration {
        private final int neededApprovals;
        private final Set<Role> allowedReviewerRoles;
        private final boolean allowSelfReview;
        private final long reviewTimeoutDays;
        
        public ReviewConfiguration(int neededApprovals, Set<Role> allowedReviewerRoles, 
                                 boolean allowSelfReview, long reviewTimeoutDays) {
            this.neededApprovals = neededApprovals;
            this.allowedReviewerRoles = new HashSet<>(allowedReviewerRoles);
            this.allowSelfReview = allowSelfReview;
            this.reviewTimeoutDays = reviewTimeoutDays;
        }
        
        public int getneededApprovals() { return neededApprovals; }
        public Set<Role> getAllowedReviewerRoles() { return new HashSet<>(allowedReviewerRoles); }
        public boolean isAllowSelfReview() { return allowSelfReview; }
        public long getReviewTimeoutDays() { return reviewTimeoutDays; }
    }
    
    /**
     * Creates a new ReviewBasedPublishingStrategy with default configuration.
     */
    public ReviewBasedPublishingStrategy() {
        this.contentSubject = new ContentSubject();
        this.contentInReview = new ConcurrentHashMap<>();
        this.reviewerAssignments = new ConcurrentHashMap<>();
        this.reviewConfigurations = new ConcurrentHashMap<>();
        
        // Initialize default review configurations
        initializeDefaultConfigurations();
    }
    
    /**
     * Creates a new ReviewBasedPublishingStrategy with the specified content subject.
     *
     * @param contentSubject The content subject for event notifications
     * @throws IllegalArgumentException If contentSubject is null
     */
    public ReviewBasedPublishingStrategy(ContentSubject contentSubject) {
        if (contentSubject == null) {
            throw new IllegalArgumentException("Content subject cannot be null");
        }
        
        this.contentSubject = contentSubject;
        this.contentInReview = new ConcurrentHashMap<>();
        this.reviewerAssignments = new ConcurrentHashMap<>();
        this.reviewConfigurations = new ConcurrentHashMap<>();
        
        initializeDefaultConfigurations();
    }
    
    /**
     * Submits content for review-based publishing workflow.
     *
     * <p>This method implements the core review-based publishing algorithm:</p>
     * <ol>
     *   <li>Validates content and user permissions for submission</li>
     *   <li>Creates a review process with appropriate configuration</li>
     *   <li>Assigns reviewers based on content type and availability</li>
     *   <li>Updates content status to indicate review state</li>
     *   <li>Fires Observer Pattern events for workflow notifications</li>
     *   <li>Logs the review submission for audit purposes</li>
     * </ol>
     *
     * <p><strong>Strategy-Specific Behavior:</strong> This implementation focuses
     * on workflow management, reviewer assignment, and approval tracking. It
     * provides comprehensive audit trails and supports complex approval implementations
     * with multiple reviewers and configurable approval thresholds.</p>
     *
     * @param content The content to be submitted for review-based publishing
     * @param context The publishing context with user and configuration information
     * @throws ContentManagementException If review submission fails due to validation
     *         errors, reviewer availability, or workflow constraints. Uses Exception
     *         Shielding to provide user-friendly messages while logging technical details.
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
            // Log the start of review submission
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "review_publish_start",
                "Starting review-based publishing for content: " + content.getTitle()
            );
            
            // Pre-submission validation
            if (!validate(content, context)) {
                throw new ContentManagementException(
                    "Content validation failed for review-based publishing",
                    "The content does not meet the implementations for review submission. " +
                    "Please check content completeness, user permissions, and review availability."
                );
            }
            
            // Check if content is already in review
            if (contentInReview.containsKey(content.getId())) {
                ReviewProcess existingProcess = contentInReview.get(content.getId());
                if (existingProcess.getStatus() == ReviewStatus.IN_REVIEW) {
                    throw new ContentManagementException(
                        "Content already in review",
                        "This content is already under review. Please wait for the review process to complete."
                    );
                }
            }
            
            // Execute the review-based publishing algorithm
            executeReviewBasedPublishing(content, context);
            
            // Fire Observer Pattern events for notifications
            fireReviewEvent(content, context, ContentEvent.EventType.CONTENT_SUBMITTED_FOR_REVIEW);
            
            // Log successful review submission
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "review_publish_success",
                String.format("Successfully submitted content '%s' for review-based publishing", 
                             content.getTitle())
            );
            
        } catch (ContentManagementException e) {
            // Log the error with technical details for debugging
            CMSLogger.logError(
                "Failed to submit content for review: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Re-throw with Exception Shielding
            throw new ContentManagementException(
                "Review submission failed due to system error",
                "We were unable to submit your content for review. " +
                "Please verify the content implementations and try again, or contact support if the problem persists.",
                e
            );
        } catch (Exception e) {
            // Log unexpected errors
            CMSLogger.logError(
                "Unexpected error during review-based publishing: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            
            // Wrap in ContentManagementException with Exception Shielding
            throw new ContentManagementException(
                "Unexpected review submission error",
                "An unexpected error occurred during review submission. " +
                "Our technical team has been notified. Please try again later.",
                e
            );
        }
    }
    
    /**
     * Validates whether content can be submitted for review-based publishing.
     *
     * <p><strong>Review-Based Publishing Validation Rules:</strong></p>
     * <ul>
     *   <li>All basic content validation (ID, title, body)</li>
     *   <li>Content must not already be published or under review</li>
     *   <li>User must have permission to submit content for review</li>
     *   <li>At least one qualified reviewer must be available</li>
     *   <li>Content type must support review workflow</li>
     *   <li>Review configuration must be available for content type</li>
     * </ul>
     *
     * @param content The content to validate for review submission
     * @param context The publishing context with user information
     * @return true if content can be submitted for review, false otherwise
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
            
            // User permission validation for review submission
            if (!hasReviewSubmissionPermission(context.getUser())) {
                CMSLogger.logSecurityEvent(
                    context.getUser().getUsername(),
                    "unauthorized_review_submission",
                    "User attempted review submission without proper permissions"
                );
                return false;
            }
            
            // Content status validation
            ContentStatus status = content.getStatus();
            if (!isValidStatusForReview(status)) {
                CMSLogger.logValidationError(
                    "Invalid content status for review submission: " + status,
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Check if content is already in review
            if (isContentInReview(content.getId())) {
                CMSLogger.logValidationError(
                    "Content is already under review",
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // Validate reviewer availability
            if (!hasAvailableReviewers(content, context)) {
                CMSLogger.logValidationError(
                    "No qualified reviewers available for content type",
                    context.getUser().getUsername()
                );
                return false;
            }
            
            // All validations passed
            CMSLogger.logValidationSuccess(
                "Review-based publishing validation passed for content: " + content.getId(),
                context.getUser().getUsername()
            );
            
            return true;
            
        } catch (Exception e) {
            // Log validation errors but don't throw - return false for failed validation
            CMSLogger.logError(
                "Error during review-based publishing validation: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
            return false;
        }
    }
    
    /**
     * Returns the strategy name for identification purposes.
     * @return "Review-Based Publishing"
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Returns the priority level for this strategy.
     * Review-based publishing has normal priority (50) for quality balance.
     * @return 50 (normal priority)
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
        return "Implements a comprehensive review-based publishing workflow where content " +
               "must be approved by designated reviewers before publication. Ideal for " +
               "corporate environments, compliance-sensitive content, and collaborative " +
               "editorial processes. Supports configurable approval implementations, multi-stage " +
               "reviews, and comprehensive audit trails for quality assurance and compliance.";
    }
    
    /**
     * Indicates that this strategy supports batch processing.
     * Multiple content items can be submitted for review in batches.
     * @return true - batch processing is supported
     */
    @Override
    public boolean supportsBatchProcessing() {
        return true;
    }
    
    /**
     * Indicates that this strategy supports rollback operations.
     * Content can be withdrawn from review or unpublished after approval.
     * @return true - rollback (withdrawal) is supported
     */
    @Override
    public boolean supportsRollback() {
        return true;
    }
    
    /**
     * Returns the estimated processing time for review submission setup.
     * This includes validation, reviewer assignment, and notification overhead.
     * Note: This does not include the actual review time by human reviewers.
     * @param content The content to be submitted for review
     * @param context The publishing context
     * @return Estimated processing time in milliseconds (setup only)
     */
    @Override
    public long getEstimatedProcessingTime(Content content, PublishingContext context) {
        long baseTime = BASE_PROCESSING_TIME;
        
        // Adjust based on content complexity
        if (content != null && content.getBody() != null) {
            int contentLength = content.getBody().length();
            if (contentLength > 10000) {
                baseTime += 500; // Extra time for complex content validation
            }
        }
        
        // Adjust based on number of needed reviewers
        String contentType = determineContentType(content);
        ReviewConfiguration config = reviewConfigurations.get(contentType);
        if (config != null) {
            baseTime += config.getneededApprovals() * 200; // Time per reviewer assignment
        }
        
        // Adjust based on notification settings
        if (context != null && context.isSendNotifications()) {
            baseTime += 400; // Time for notification processing
        }
        
        return baseTime;
    }
    
    /**
     * Processes a review decision for content under review.
     * This method is called by reviewers to approve or reject content.
     *
     * @param contentId The ID of the content being reviewed
     * @param reviewerUser The user making the review decision
     * @param decision The review decision
     * @param comment Optional comment from the reviewer
     * @throws ContentManagementException If the review processing fails
     */
    public void processReviewDecision(String contentId, User reviewerUser, 
                                    ReviewDecision decision, String comment) 
            throws ContentManagementException {
        
        try {
            ReviewProcess reviewProcess = contentInReview.get(contentId);
            if (reviewProcess == null) {
                throw new ContentManagementException(
                    "Content not found in review",
                    "The specified content is not currently under review."
                );
            }
            
            // Validate reviewer permissions
            if (!reviewProcess.getAssignedReviewers().contains(reviewerUser.getUsername())) {
                throw new ContentManagementException(
                    "Unauthorized review attempt",
                    "You are not assigned as a reviewer for this content."
                );
            }
            
            // Process the review decision
            reviewProcess.addReviewDecision(reviewerUser.getUsername(), decision, comment);
            
            CMSLogger.logContentOperation(
                contentId,
                reviewerUser.getUsername(),
                "review_decision_made",
                String.format("Review decision: %s - %s", decision.name(), 
                             comment != null ? comment : "No comment")
            );
            
            // Check if all needed approvals are met
            if (decision == ReviewDecision.REJECTED || reviewProcess.hasAnyRejection()) {
                // Content rejected - update status and notify
                reviewProcess.setStatus(ReviewStatus.REJECTED);
                fireReviewDecisionEvent(contentId, reviewerUser, ContentEvent.EventType.CONTENT_REJECTED);
                
            } else if (reviewProcess.hasneededApprovals()) {
                // Content approved - publish automatically
                reviewProcess.setStatus(ReviewStatus.APPROVED);
                publishApprovedContent(contentId, reviewProcess);
                
            } else {
                // Still waiting for more approvals
                reviewProcess.setStatus(ReviewStatus.IN_REVIEW);
                fireReviewDecisionEvent(contentId, reviewerUser, ContentEvent.EventType.CONTENT_REVIEWED);
            }
            
        } catch (ContentManagementException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentManagementException(
                "Error processing review decision",
                "An error occurred while processing the review decision. Please try again.",
                e
            );
        }
    }
    
    /**
     * Returns the current review status for the specified content.
     * @param contentId The content ID to check
     * @return ReviewProcess if content is under review, null otherwise
     */
    public ReviewProcess getReviewProcess(String contentId) {
        return contentInReview.get(contentId);
    }
    
    /**
     * Returns a list of content items currently under review for the specified reviewer.
     * @param reviewerUsername The reviewer's username
     * @return List of content IDs assigned to the reviewer
     */
    public List<String> getAssignedReviews(String reviewerUsername) {
        return reviewerAssignments.getOrDefault(reviewerUsername, Collections.emptySet())
            .stream()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    // Private implementation methods
    
    private void executeReviewBasedPublishing(Content content, PublishingContext context) 
            throws ContentManagementException {
        
        try {
            // Determine content type and review configuration
            String contentType = determineContentType(content);
            ReviewConfiguration config = reviewConfigurations.getOrDefault(
                contentType, 
                new ReviewConfiguration(DEFAULT_needed_APPROVALS, 
                    Set.of(Role.EDITOR, Role.ADMINISTRATOR), false, 7)
            );
            
            // Create review process
            ReviewProcess reviewProcess = new ReviewProcess(
                content.getId(), 
                context.getUser().getUsername(),
                config.getneededApprovals()
            );
            
            // Assign reviewers
            assignReviewers(reviewProcess, content, context, config);
            
            // Update content status
            content.setStatus(ContentStatus.REVIEW, context.getUser().getId());
            
            // Store review process
            contentInReview.put(content.getId(), reviewProcess);
            
            // Update content metadata
            if (content.getMetadata() != null) {
                content.getMetadata().put("under_review", true);
                content.getMetadata().put("review_strategy", STRATEGY_NAME);
                content.getMetadata().put("submitted_by", context.getUser().getUsername());
                content.getMetadata().put("submission_date", System.currentTimeMillis());
                content.getMetadata().put("needed_approvals", config.getneededApprovals());
            }
            
            CMSLogger.logContentOperation(
                content.getId(),
                context.getUser().getUsername(),
                "content_submitted_for_review",
                String.format("Content submitted for review with %d needed approvals", 
                             config.getneededApprovals())
            );
            
        } catch (Exception e) {
            throw new ContentManagementException(
                "Failed to execute review-based publishing",
                "Unable to submit content for review workflow",
                e
            );
        }
    }
    
    private void assignReviewers(ReviewProcess reviewProcess, Content content, 
                               PublishingContext context, ReviewConfiguration config) {
        
        // Simple reviewer assignment algorithm
        // In a real implementation, this would be more sophisticated
        Set<Role> allowedRoles = config.getAllowedReviewerRoles();
        String submitterUsername = context.getUser().getUsername();
        
        // For demonstration, assign based on role hierarchy
        // This would typically involve a more complex reviewer selection algorithm
        if (allowedRoles.contains(Role.ADMINISTRATOR)) {
            reviewProcess.addReviewer("admin_reviewer"); // Mock reviewer
            updateReviewerAssignments("admin_reviewer", content.getId());
        }
        
        if (config.getneededApprovals() > 1 && allowedRoles.contains(Role.EDITOR)) {
            reviewProcess.addReviewer("editor_reviewer"); // Mock reviewer
            updateReviewerAssignments("editor_reviewer", content.getId());
        }
        
        CMSLogger.logContentOperation(
            content.getId(),
            context.getUser().getUsername(),
            "reviewers_assigned",
            "Reviewers assigned: " + reviewProcess.getAssignedReviewers()
        );
    }
    
    private void updateReviewerAssignments(String reviewerUsername, String contentId) {
        reviewerAssignments.computeIfAbsent(reviewerUsername, k -> ConcurrentHashMap.newKeySet())
                          .add(contentId);
    }
    
    private void publishApprovedContent(String contentId, ReviewProcess reviewProcess) 
            throws ContentManagementException {
        
        try {
            // This would typically involve retrieving the content and updating its status
            // For now, we'll just update the review process and fire events
            reviewProcess.setStatus(ReviewStatus.PUBLISHED);
            
            // Remove from active review tracking
            contentInReview.remove(contentId);
            
            // Clean up reviewer assignments
            for (String reviewer : reviewProcess.getAssignedReviewers()) {
                Set<String> assignments = reviewerAssignments.get(reviewer);
                if (assignments != null) {
                    assignments.remove(contentId);
                }
            }
            
            // Fire publication event
            ContentEvent event = ContentEvent.builder(ContentEvent.EventType.CONTENT_PUBLISHED, null)
                .metadata("content_id", contentId)
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("review_based_publication", true)
                .metadata("review_process_id", reviewProcess.getContentId())
                .build();
            
            contentSubject.notifyObservers(event);
            
            CMSLogger.logContentOperation(
                contentId,
                "system",
                "content_published_after_review",
                "Content published automatically after receiving needed approvals"
            );
            
        } catch (Exception e) {
            throw new ContentManagementException(
                "Failed to publish approved content",
                "Error occurred during automatic publication of approved content",
                e
            );
        }
    }
    
    private void fireReviewEvent(Content content, PublishingContext context, 
                               ContentEvent.EventType eventType) {
        try {
            ContentEvent event = ContentEvent.builder(eventType, content)
                .user(context.getUser())
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("review_based_publishing", true)
                .metadata("environment", context.getEnvironment())
                .build();
            
            contentSubject.notifyObservers(event);
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire review event for content: " + content.getId(),
                e,
                context.getUser().getUsername()
            );
        }
    }
    
    private void fireReviewDecisionEvent(String contentId, User reviewer, 
                                       ContentEvent.EventType eventType) {
        try {
            ContentEvent event = ContentEvent.builder(eventType, null)
                .user(reviewer)
                .metadata("content_id", contentId)
                .metadata("publishing_strategy", STRATEGY_NAME)
                .metadata("reviewer_decision", true)
                .build();
            
            contentSubject.notifyObservers(event);
            
        } catch (Exception e) {
            CMSLogger.logError(
                "Failed to fire review decision event for content: " + contentId,
                e,
                reviewer.getUsername()
            );
        }
    }
    
    private void initializeDefaultConfigurations() {
        // Default configuration for article content
        reviewConfigurations.put("article", new ReviewConfiguration(
            1, Set.of(Role.EDITOR, Role.ADMINISTRATOR), false, 3
        ));
        
        // Stricter configuration for page content
        reviewConfigurations.put("page", new ReviewConfiguration(
            2, Set.of(Role.ADMINISTRATOR), false, 7
        ));
        
        // Standard configuration for other content
        reviewConfigurations.put("default", new ReviewConfiguration(
            DEFAULT_needed_APPROVALS, Set.of(Role.EDITOR, Role.ADMINISTRATOR), false, 5
        ));
    }
    
    private String determineContentType(Content content) {
        // Simple content type determination
        // In a real implementation, this would be based on actual content type
        if (content.getClass().getSimpleName().toLowerCase().contains("article")) {
            return "article";
        } else if (content.getClass().getSimpleName().toLowerCase().contains("page")) {
            return "page";
        }
        return "default";
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
    
    private boolean hasReviewSubmissionPermission(User user) {
        Role role = user.getRole();
        return role == Role.AUTHOR || role == Role.EDITOR || role == Role.ADMINISTRATOR;
    }
    
    private boolean isValidStatusForReview(ContentStatus status) {
        return status == ContentStatus.DRAFT;
    }
    
    private boolean isContentInReview(String contentId) {
        ReviewProcess process = contentInReview.get(contentId);
        return process != null && (process.getStatus() == ReviewStatus.IN_REVIEW || 
                                  process.getStatus() == ReviewStatus.PENDING_REVIEW);
    }
    
    private boolean hasAvailableReviewers(Content content, PublishingContext context) {
        // In a real implementation, this would check actual reviewer availability
        // For demonstration, we'll assume reviewers are available
        return true;
    }
}