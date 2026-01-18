package com.cms.patterns.strategy;

import com.cms.core.model.Content;
import com.cms.core.exception.ContentManagementException;

/**
 * Strategy Pattern interface defining the contract for content publishing strategies.
 *
 * <p>The PublishingStrategy interface represents the core Strategy Pattern interface
 * that defines a family of algorithms for content publishing operations. Each concrete
 * implementation represents a different publishing approach that can be used
 * interchangeably based on business implementations, content type, or user preferences.</p>
 *
 * <p><strong>Design Pattern:</strong> Strategy Pattern  - This interface serves
 * as the Strategy interface in the Strategy Pattern implementation. It enables the
 * definition of a family of publishing algorithms, encapsulates each one, and makes
 * them interchangeable at runtime. The pattern lets the algorithm vary independently
 * from clients that use it.</p>
 *
 * <p><strong>Implementation:</strong> Implements the Strategy Pattern implementation
 * by providing a common interface for all concrete publishing strategies.
 * This design promotes loose coupling between the context (PublishingService) and
 * the specific algorithm implementations, enabling runtime strategy selection and
 * easy extension with new publishing strategies.</p>
 *
 * <p><strong>Key Benefits:</strong></p>
 * <ul>
 *   <li>Algorithm Encapsulation - Each publishing approach is self-contained</li>
 *   <li>Runtime Flexibility - Strategies can be changed dynamically</li>
 *   <li>Open/Closed Principle - Easy to add new strategies without modifying existing code</li>
 *   <li>Single Responsibility - Each strategy focuses on one publishing approach</li>
 *   <li>Testability - Strategies can be unit tested independently</li>
 * </ul>
 *
 * <p><strong>Integration with Other Patterns:</strong></p>
 * <ul>
 *   <li>Factory Pattern - Content validation and processing</li>
 *   <li>Observer Pattern - Publishing event notifications</li>
 *   <li>Composite Pattern - Hierarchical content publishing</li>
 *   <li>Iterator Pattern - Batch processing operations</li>
 *   <li>Exception Shielding - Proper error handling and user-friendly messages</li>
 * </ul>
 *
 * @author JavaCMS Development Team
 * @version 1.0
 * @since 1.0
 * @see PublishingContext For publishing metadata and configuration
 * @see PublishingService For the context class that uses strategies
 */
public interface PublishingStrategy {
    
    /**
     * Publishes content using this specific strategy's algorithm.
     *
     * <p>This method represents the core algorithm execution for each strategy.
     * Different implementations will handle publishing in various ways such as
     * immediate publishing, scheduled publishing, or review-based workflows.</p>
     *
     * <p><strong>Strategy Pattern Role:</strong> This is the primary algorithm method
     * that varies between different concrete strategy implementations. The method
     * signature remains consistent across all strategies, but the implementation
     * logic is specific to each publishing approach.</p>
     *
     * @param content The content to be published, must not be null
     * @param context The publishing context containing metadata and configuration
     * @throws ContentManagementException If publishing fails due to validation,
     *         permission issues, or other business rule violations. Uses exception
     *         shielding to provide user-friendly error messages while logging
     *         technical details for debugging.
     * @throws IllegalArgumentException If content or context parameters are null
     * @see PublishingContext For context parameter details
     */
    void publish(Content content, PublishingContext context) throws ContentManagementException;
    
    /**
     * Validates whether this strategy can successfully publish the given content.
     *
     * <p>This method performs pre-publication validation to determine if the
     * content meets the implementations for this specific publishing strategy.
     * Validation may include checking content status, user permissions,
     * scheduling constraints, or other strategy-specific implementations.</p>
     *
     * <p><strong>Strategy-Specific Validation:</strong> Each strategy implements
     * its own validation logic. For example, scheduled publishing might validate
     * the scheduled date, while review-based publishing might check approval status.</p>
     *
     * @param content The content to validate for publishing
     * @param context The publishing context containing metadata and user information
     * @return true if the content can be published using this strategy, false otherwise
     * @throws IllegalArgumentException If content or context parameters are null
     */
    boolean validate(Content content, PublishingContext context);
    
    /**
     * Returns the human-readable name of this publishing strategy.
     *
     * <p>This name is used for strategy identification in user interfaces,
     * logging, and administrative functions. It should be descriptive and
     * unique among all available strategies.</p>
     *
     * @return A non-empty string identifying this strategy
     */
    String getStrategyName();
    
    /**
     * Returns the execution priority of this strategy for automatic selection.
     *
     * <p>Priority is used when multiple strategies are available for the same
     * content. Higher priority values indicate preferred strategies. Priority
     * values are typically in the range of 1-100, with higher numbers indicating
     * higher priority.</p>
     *
     * <p><strong>Common Priority Ranges:</strong></p>
     * <ul>
     *   <li>90-100: Critical/Emergency publishing (immediate)</li>
     *   <li>70-89: High priority (scheduled, auto-publish)</li>
     *   <li>50-69: Normal priority (review-based)</li>
     *   <li>30-49: Low priority (batch processing)</li>
     *   <li>10-29: Background priority (maintenance operations)</li>
     * </ul>
     *
     * @return The priority value for this strategy (1-100 range recommended)
     */
    int getPriority();
    
    /**
     * Returns a detailed description of this strategy's publishing approach.
     *
     * <p>This description explains how the strategy works, what conditions
     * it's best suited for, and any special implementations or limitations.
     * Used for documentation, help systems, and strategy selection guidance.</p>
     *
     * @return A detailed description of the strategy's behavior and use cases
     */
    default String getDescription() {
        return "Publishing strategy: " + getStrategyName();
    }
    
    /**
     * Indicates whether this strategy supports batch processing of multiple content items.
     *
     * <p>Batch-capable strategies can process multiple content items in a single
     * operation, which is more efficient for large-scale publishing operations.
     * Non-batch strategies process items individually.</p>
     *
     * @return true if this strategy can handle batch processing, false otherwise
     */
    default boolean supportsBatchProcessing() {
        return false;
    }
    
    /**
     * Indicates whether this strategy supports rollback of published content.
     *
     * <p>Rollback-capable strategies can undo publishing operations, allowing
     * content to be returned to its previous state. This is useful for handling
     * publishing errors or reverting unwanted publications.</p>
     *
     * @return true if this strategy supports rollback operations, false otherwise
     */
    default boolean supportsRollback() {
        return false;
    }
    
    /**
     * Returns the estimated processing time for this strategy in milliseconds.
     *
     * <p>This estimate helps in planning and progress reporting for publishing
     * operations. Different strategies may have vastly different processing
     * times based on their complexity and external dependencies.</p>
     *
     * @param content The content to be published
     * @param context The publishing context
     * @return Estimated processing time in milliseconds, or -1 if unknown
     */
    default long getEstimatedProcessingTime(Content content, PublishingContext context) {
        return -1; // Unknown/not implemented
    }
}