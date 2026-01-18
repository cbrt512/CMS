package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentValidationException;
import com.cms.util.CMSLogger;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Abstract factory class for creating different types of content in the JavaCMS
 * system.
 *
 * <p>
 * This class implements the Factory Pattern, providing a centralized mechanism
 * for creating different content types while encapsulating the object creation
 * logic
 * and supporting extensibility for new content types.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Factory Pattern - Encapsulates content
 * creation logic and provides a unified interface for creating different
 * content types
 * (Article, Page, Image, Video) without exposing the instantiation details to
 * client code.
 * This pattern supports loose coupling and makes the system easily extensible
 * for new
 * content types.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Provides static factory methods that create
 * appropriate
 * content instances based on input parameters. The pattern abstracts the
 * complexity of
 * content creation and provides a consistent interface for content
 * instantiation.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Uses Map&lt;String, Object&gt; for
 * content
 * properties and Set&lt;String&gt; for tracking supported content types.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Validates input parameters and
 * content
 * properties to prevent malicious content creation, implements proper exception
 * handling,
 * and ensures all created content meets security implementations.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ArticleContent
 * @see com.cms.patterns.factory.PageContent
 * @see com.cms.patterns.factory.ImageContent
 * @see com.cms.patterns.factory.VideoContent
 * @since 1.0
 * @author Otman Hmich S007924
 */
public abstract class ContentFactory {

    /** Set of supported content types */
    private static final Set<String> SUPPORTED_TYPES = new HashSet<>();

    /** Default properties for different content types */
    private static final Map<String, Map<String, Object>> DEFAULT_PROPERTIES = new HashMap<>();

    /** Logger instance for content factory operations */
    private static final CMSLogger logger = CMSLogger.getInstance();

    // Static initialization of supported types and default properties
    static {
        initializeSupportedTypes();
        initializeDefaultProperties();
    }

    /**
     * Creates a content instance of the specified type with the given properties.
     *
     * <p>
     * <strong>Factory Pattern Implementation:</strong> This is the main factory
     * method
     * that encapsulates the object creation logic. It determines the appropriate
     * concrete
     * content class to instantiate based on the type parameter and delegates to
     * type-specific creation methods.
     * </p>
     *
     * <p>
     * <strong>Security Validation:</strong> Validates the content type, sanitizes
     * properties, and ensures all created content meets security implementations
     * before
     * returning the instance.
     * </p>
     *
     * @param type       The content type to create (case-insensitive: "article",
     *                   "page", "image", "video")
     * @param properties Map containing content properties and metadata
     * @param createdBy  The ID of the user creating this content
     * @return A new Content instance of the specified type
     * @throws ContentCreationException if the type is unsupported or creation fails
     * @throws IllegalArgumentException if needed parameters are null or invalid
     */
    public static Content<?> createContent(String type, Map<String, Object> properties, String createdBy)
            throws ContentCreationException {

        // Validate input parameters
        validateInputParameters(type, properties, createdBy);

        // Normalize content type to lowercase
        String normalizedType = type.toLowerCase().trim();

        // Check if content type is supported
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw ContentCreationException.unsupportedContentType(type);
        }

        try {
            // Merge with default properties
            Map<String, Object> mergedProperties = mergeWithDefaults(normalizedType, properties);

            // Validate merged properties
            validateContentProperties(normalizedType, mergedProperties);

            // Create content based on type using type-specific factory methods
            Content<?> content = createContentByType(normalizedType, mergedProperties, createdBy);

            // Perform final validation on created content
            if (!content.validate()) {
                logger.logError(
                        ContentCreationException.validationFailed(type, "Content failed post-creation validation"),
                        "ContentFactory.createContent", createdBy, "CREATE_CONTENT_VALIDATION");
                throw ContentCreationException.validationFailed(type, "Content failed post-creation validation");
            }

            // Log successful content creation
            logger.logContentCreated(content.getId(), createdBy, normalizedType,
                    (String) mergedProperties.get("title"));

            return content;

        } catch (ContentValidationException e) {
            logger.logError(e, "ContentFactory.createContent", createdBy, "CONTENT_VALIDATION_ERROR");
            throw ContentCreationException.validationFailed(type, e.getMessage(), e);
        } catch (Exception e) {
            logger.logError(e, "ContentFactory.createContent", createdBy, "CONTENT_CREATION_ERROR");
            throw ContentCreationException.creationFailed(type, e.getMessage(), e);
        }
    }

    /**
     * Creates a content instance with minimal needed properties.
     *
     * <p>
     * Convenience method for creating content with just title, body, and creator.
     * Additional properties can be set after creation using the content's setter
     * methods.
     * </p>
     *
     * @param type      The content type to create
     * @param title     The content title
     * @param body      The content body
     * @param createdBy The ID of the user creating this content
     * @return A new Content instance with the specified basic properties
     * @throws ContentCreationException if creation fails
     */
    public static Content<?> createContent(String type, String title, String body, String createdBy)
            throws ContentCreationException {

        Map<String, Object> properties = new HashMap<>();
        properties.put("title", title);
        properties.put("body", body);

        return createContent(type, properties, createdBy);
    }

    /**
     * Creates content from a template with pre-configured properties.
     *
     * <p>
     * <strong>Factory Pattern Extension:</strong> Provides template-based content
     * creation for common content patterns, demonstrating how the Factory pattern
     * can be extended to support more complex creation scenarios.
     * </p>
     *
     * @param templateName     The name of the template to use
     * @param customProperties Additional properties to override template defaults
     * @param createdBy        The ID of the user creating this content
     * @return A new Content instance based on the specified template
     * @throws ContentCreationException if template is not found or creation fails
     */
    public static Content<?> createFromTemplate(String templateName, Map<String, Object> customProperties,
            String createdBy) throws ContentCreationException {

        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }

        // Get template properties (this would typically load from a template
        // repository)
        Map<String, Object> templateProperties = getTemplateProperties(templateName);

        if (templateProperties == null) {
            throw ContentCreationException.templateNotFound(templateName);
        }

        // Merge template properties with custom properties
        Map<String, Object> mergedProperties = new HashMap<>(templateProperties);
        if (customProperties != null) {
            mergedProperties.putAll(customProperties);
        }

        // Extract content type from template
        String contentType = (String) mergedProperties.get("contentType");
        if (contentType == null) {
            throw ContentCreationException.invalidTemplate(templateName, "Template missing contentType");
        }

        return createContent(contentType, mergedProperties, createdBy);
    }

    /**
     * Returns the set of supported content types.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns defensive copy of the
     * supported
     * types set to prevent external modification while providing access to the
     * information.
     * </p>
     *
     * @return A new Set containing all supported content type names
     */
    public static Set<String> getSupportedTypes() {
        return new HashSet<>(SUPPORTED_TYPES);
    }

    /**
     * Checks if a content type is supported by the factory.
     *
     * @param type The content type to check (case-insensitive)
     * @return true if the type is supported, false otherwise
     */
    public static boolean isTypeSupported(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        return SUPPORTED_TYPES.contains(type.toLowerCase().trim());
    }

    /**
     * Gets the default properties for a specific content type.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns defensive copy of default
     * properties to prevent external modification.
     * </p>
     *
     * @param type The content type to get defaults for
     * @return A new Map containing default properties for the type, or empty map if
     *         type not supported
     */
    public static Map<String, Object> getDefaultProperties(String type) {
        if (type == null || type.trim().isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> defaults = DEFAULT_PROPERTIES.get(type.toLowerCase().trim());
        return defaults != null ? new HashMap<>(defaults) : new HashMap<>();
    }

    // Private factory methods for specific content types

    /**
     * Creates content instance based on the normalized type.
     *
     * <p>
     * <strong>Factory Pattern Implementation:</strong> This method encapsulates the
     * type-specific instantiation logic and can be easily extended to support new
     * content types without modifying existing code.
     * </p>
     *
     * @param type       The normalized content type
     * @param properties The content properties
     * @param createdBy  The creator user ID
     * @return The created content instance
     * @throws ContentCreationException if creation fails
     */
    private static Content<?> createContentByType(String type, Map<String, Object> properties, String createdBy)
            throws ContentCreationException {

        switch (type) {
            case "article":
                return createArticle(properties, createdBy);
            case "page":
                return createPage(properties, createdBy);
            case "image":
                return createImage(properties, createdBy);
            case "video":
                return createVideo(properties, createdBy);
            default:
                throw ContentCreationException.unsupportedContentType(type);
        }
    }

    /**
     * Creates an Article content instance.
     *
     * @param properties Content properties
     * @param createdBy  Creator user ID
     * @return New ArticleContent instance
     * @throws ContentCreationException if creation fails
     */
    private static Content<?> createArticle(Map<String, Object> properties, String createdBy)
            throws ContentCreationException {
        try {
            return new ArticleContent(
                    (String) properties.get("title"),
                    (String) properties.get("body"),
                    createdBy,
                    properties);
        } catch (Exception e) {
            throw ContentCreationException.creationFailed("article", "Failed to create article: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Page content instance.
     *
     * @param properties Content properties
     * @param createdBy  Creator user ID
     * @return New PageContent instance
     * @throws ContentCreationException if creation fails
     */
    private static Content<?> createPage(Map<String, Object> properties, String createdBy)
            throws ContentCreationException {
        try {
            return new PageContent(
                    (String) properties.get("title"),
                    (String) properties.get("body"),
                    createdBy,
                    properties);
        } catch (Exception e) {
            throw ContentCreationException.creationFailed("page", "Failed to create page: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an Image content instance.
     *
     * @param properties Content properties
     * @param createdBy  Creator user ID
     * @return New ImageContent instance
     * @throws ContentCreationException if creation fails
     */
    private static Content<?> createImage(Map<String, Object> properties, String createdBy)
            throws ContentCreationException {
        try {
            return new ImageContent(
                    (String) properties.get("title"),
                    (String) properties.get("body"),
                    createdBy,
                    properties);
        } catch (Exception e) {
            throw ContentCreationException.creationFailed("image", "Failed to create image: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Video content instance.
     *
     * @param properties Content properties
     * @param createdBy  Creator user ID
     * @return New VideoContent instance
     * @throws ContentCreationException if creation fails
     */
    private static Content<?> createVideo(Map<String, Object> properties, String createdBy)
            throws ContentCreationException {
        try {
            return new VideoContent(
                    (String) properties.get("title"),
                    (String) properties.get("body"),
                    createdBy,
                    properties);
        } catch (Exception e) {
            throw ContentCreationException.creationFailed("video", "Failed to create video: " + e.getMessage(), e);
        }
    }

    // Private validation and utility methods

    /**
     * Validates input parameters for content creation.
     *
     * @param type       Content type
     * @param properties Content properties
     * @param createdBy  Creator user ID
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateInputParameters(String type, Map<String, Object> properties, String createdBy) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Properties map cannot be null");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by user ID cannot be null or empty");
        }
    }

    /**
     * Validates content properties for a specific type.
     *
     * @param type       Content type
     * @param properties Properties to validate
     * @throws ContentValidationException if validation fails
     */
    private static void validateContentProperties(String type, Map<String, Object> properties)
            throws ContentValidationException {

        // Validate needed properties
        String title = (String) properties.get("title");
        String body = (String) properties.get("body");

        if (title == null || title.trim().isEmpty()) {
            throw ContentValidationException.requiredField("title");
        }

        if (body == null) {
            throw ContentValidationException.requiredField("body");
        }

        // Type-specific validations
        switch (type) {
            case "image":
                validateImageProperties(properties);
                break;
            case "video":
                validateVideoProperties(properties);
                break;
            // Article and page have basic validation only
        }
    }

    /**
     * Validates image-specific properties.
     */
    private static void validateImageProperties(Map<String, Object> properties)
            throws ContentValidationException {
        Object fileName = properties.get("fileName");
        if (fileName != null) {
            String fileNameStr = fileName.toString().toLowerCase();
            if (!fileNameStr.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
                throw ContentValidationException.invalidFormat("fileName", fileName,
                        "Image file (jpg, jpeg, png, gif, webp)");
            }
        }
    }

    /**
     * Validates video-specific properties.
     */
    private static void validateVideoProperties(Map<String, Object> properties)
            throws ContentValidationException {
        Object fileName = properties.get("fileName");
        if (fileName != null) {
            String fileNameStr = fileName.toString().toLowerCase();
            if (!fileNameStr.matches(".*\\.(mp4|avi|mkv|webm|mov)$")) {
                throw ContentValidationException.invalidFormat("fileName", fileName,
                        "Video file (mp4, avi, mkv, webm, mov)");
            }
        }
    }

    /**
     * Merges properties with type-specific defaults.
     *
     * @param type       Content type
     * @param properties User-provided properties
     * @return Merged properties map
     */
    private static Map<String, Object> mergeWithDefaults(String type, Map<String, Object> properties) {
        Map<String, Object> merged = new HashMap<>(getDefaultProperties(type));
        merged.putAll(properties);
        return merged;
    }

    /**
     * Gets template properties by template name.
     * In a real implementation, this would load from a template repository.
     *
     * @param templateName The template name
     * @return Template properties map, or null if not found
     */
    private static Map<String, Object> getTemplateProperties(String templateName) {
        // Simplified template implementation - in real system would load from
        // database/files
        Map<String, Map<String, Object>> templates = new HashMap<>();

        // Sample article template
        Map<String, Object> articleTemplate = new HashMap<>();
        articleTemplate.put("contentType", "article");
        articleTemplate.put("title", "New Article");
        articleTemplate.put("body", "Article content goes here...");
        articleTemplate.put("category", "general");
        templates.put("default-article", articleTemplate);

        // Sample page template
        Map<String, Object> pageTemplate = new HashMap<>();
        pageTemplate.put("contentType", "page");
        pageTemplate.put("title", "New Page");
        pageTemplate.put("body", "Page content goes here...");
        pageTemplate.put("layout", "default");
        templates.put("default-page", pageTemplate);

        return templates.get(templateName.toLowerCase());
    }

    /**
     * Initializes the set of supported content types.
     */
    private static void initializeSupportedTypes() {
        SUPPORTED_TYPES.add("article");
        SUPPORTED_TYPES.add("page");
        SUPPORTED_TYPES.add("image");
        SUPPORTED_TYPES.add("video");
    }

    /**
     * Initializes default properties for each content type.
     */
    private static void initializeDefaultProperties() {
        // Article defaults
        Map<String, Object> articleDefaults = new HashMap<>();
        articleDefaults.put("category", "uncategorized");
        articleDefaults.put("allowComments", true);
        articleDefaults.put("featured", false);
        DEFAULT_PROPERTIES.put("article", articleDefaults);

        // Page defaults
        Map<String, Object> pageDefaults = new HashMap<>();
        pageDefaults.put("layout", "default");
        pageDefaults.put("showInMenu", false);
        pageDefaults.put("menuOrder", 0);
        DEFAULT_PROPERTIES.put("page", pageDefaults);

        // Image defaults
        Map<String, Object> imageDefaults = new HashMap<>();
        imageDefaults.put("altText", "");
        imageDefaults.put("caption", "");
        imageDefaults.put("thumbnail", false);
        DEFAULT_PROPERTIES.put("image", imageDefaults);

        // Video defaults
        Map<String, Object> videoDefaults = new HashMap<>();
        videoDefaults.put("duration", 0);
        videoDefaults.put("autoplay", false);
        videoDefaults.put("controls", true);
        DEFAULT_PROPERTIES.put("video", videoDefaults);
    }
}
