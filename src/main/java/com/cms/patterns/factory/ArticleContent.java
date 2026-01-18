package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentValidationException;
import com.cms.core.model.ContentRenderingException;
import java.util.Map;
import java.util.HashMap;

/**
 * Concrete implementation of Content for article-type content in the JavaCMS
 * system.
 *
 * <p>
 * This class represents blog posts, news articles, and other editorial content.
 * It extends the abstract Content class and provides article-specific
 * validation,
 * rendering, and management capabilities. Articles typically include features
 * like
 * categories, comments, and publication scheduling.
 * </p>
 *
 * <p>
 * <strong>Factory Pattern:</strong> This class is one of the concrete products
 * created by the ContentFactory. It shows how the Factory Pattern
 * enables different content types to share a common interface while providing
 * type-specific behavior and validation.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Serves as a concrete implementation of the
 * Factory Pattern, showing how different content types can be instantiated
 * through a common factory interface while maintaining their unique
 * characteristics and validation rules.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Uses Map for storing article-specific
 * metadata and shows proper handling of article properties through
 * Collections Framework operations.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ArticleContent extends Content<ArticleContent> {

    /** The article category for organization */
    private String category;

    /** Whether comments are allowed on this article */
    private boolean allowComments;

    /** Whether this article is featured */
    private boolean featured;

    /** The article excerpt/summary */
    private String excerpt;

    /** Reading time estimate in minutes */
    private int readingTimeMinutes;

    /** SEO-friendly slug for URLs */
    private String slug;

    /** Keywords for search optimization */
    private String keywords;

    /** Article-specific metadata */
    private Map<String, Object> articleMetadata;

    /** Minimum word count for articles */
    private static final int MIN_WORD_COUNT = 10;

    /** Maximum title length for articles */
    private static final int MAX_TITLE_LENGTH = 200;

    /** Maximum excerpt length */
    private static final int MAX_EXCERPT_LENGTH = 500;

    /**
     * Constructs a new ArticleContent with the specified properties.
     *
     * <p>
     * <strong>Factory Pattern Integration:</strong> This constructor is called
     * by the ContentFactory when creating article-type content. It initializes
     * article-specific properties while delegating common content initialization
     * to the parent Content class.
     * </p>
     *
     * @param title      The article title, must not be null or empty
     * @param body       The article content body, must not be null
     * @param createdBy  The ID of the user creating this article
     * @param properties Additional properties for article configuration
     * @throws IllegalArgumentException if needed parameters are invalid
     */
    public ArticleContent(String title, String body, String createdBy, Map<String, Object> properties) {
        super(title, body, createdBy);

        this.articleMetadata = new HashMap<>();

        // Initialize article-specific properties from the properties map
        initializeArticleProperties(properties);

        // Generate automatic properties
        generateAutomaticProperties();
    }

    /**
     * Returns the content type identifier for articles.
     *
     * <p>
     * <strong>Factory Pattern:</strong> This method is used by the ContentFactory
     * and other parts of the system to identify this content as an article type.
     * </p>
     *
     * @return The string "article" as the content type identifier
     */
    @Override
    public String getContentType() {
        return "article";
    }

    /**
     * Validates article-specific content implementations.
     *
     * <p>
     * <strong>Content Validation:</strong> Implements article-specific validation
     * rules including word count, title length, category implementations, and other
     * editorial standards for article content.
     * </p>
     *
     * @return true if the article passes all validation checks
     * @throws ContentValidationException if validation fails with specific error
     *                                    details
     */
    @Override
    public boolean validate() throws ContentValidationException {
        // Basic title validation
        if (getTitle() == null || getTitle().trim().isEmpty()) {
            throw ContentValidationException.requiredField("title");
        }

        if (getTitle().length() > MAX_TITLE_LENGTH) {
            throw ContentValidationException.fieldTooLong("title", getTitle().length(), MAX_TITLE_LENGTH);
        }

        // Body validation
        if (getBody() == null || getBody().trim().isEmpty()) {
            throw ContentValidationException.requiredField("body");
        }

        // Word count validation
        int wordCount = countWords(getBody());
        if (wordCount < MIN_WORD_COUNT) {
            throw new ContentValidationException(
                    String.format("Article body has only %d words, minimum needed is %d", wordCount, MIN_WORD_COUNT),
                    String.format("Articles must contain at least %d words. Your article currently has %d words.",
                            MIN_WORD_COUNT, wordCount),
                    "body",
                    wordCount);
        }

        // Category validation
        if (category == null || category.trim().isEmpty()) {
            throw ContentValidationException.requiredField("category");
        }

        // Excerpt validation (if provided)
        if (excerpt != null && excerpt.length() > MAX_EXCERPT_LENGTH) {
            throw ContentValidationException.fieldTooLong("excerpt", excerpt.length(), MAX_EXCERPT_LENGTH);
        }

        // Slug validation (if provided)
        if (slug != null && !isValidSlug(slug)) {
            throw ContentValidationException.invalidFormat("slug", slug,
                    "URL-friendly format (lowercase letters, numbers, hyphens only)");
        }

        return true;
    }

    /**
     * Renders the article content in the specified format.
     *
     * <p>
     * <strong>Content Rendering:</strong> Provides article-specific rendering
     * logic for different output formats. Articles can be rendered as HTML for
     * web display, JSON for API responses, or plain text for search indexing.
     * </p>
     *
     * @param format  The desired output format (HTML, JSON, TEXT, RSS)
     * @param context Additional rendering context and parameters
     * @return The rendered article content as a string
     * @throws ContentRenderingException if rendering fails or format is unsupported
     */
    @Override
    public String render(String format, Map<String, Object> context) throws ContentRenderingException {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Render format cannot be null or empty");
        }

        if (context == null) {
            context = new HashMap<>();
        }

        try {
            switch (format.toUpperCase()) {
                case "HTML":
                    return renderAsHtml(context);
                case "JSON":
                    return renderAsJson(context);
                case "TEXT":
                    return renderAsText(context);
                case "RSS":
                    return renderAsRss(context);
                case "EXCERPT":
                    return renderExcerpt(context);
                default:
                    throw ContentRenderingException.unsupportedFormat(getContentType(), format);
            }
        } catch (Exception e) {
            throw ContentRenderingException.templateError(getContentType(), format, e.getMessage());
        }
    }

    // Article-specific getter methods

    /**
     * Returns the article category.
     *
     * @return The category string, may be null if not set
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns whether comments are allowed on this article.
     *
     * @return true if comments are allowed, false otherwise
     */
    public boolean isAllowComments() {
        return allowComments;
    }

    /**
     * Returns whether this article is marked as featured.
     *
     * @return true if featured, false otherwise
     */
    public boolean isFeatured() {
        return featured;
    }

    /**
     * Returns the article excerpt/summary.
     *
     * @return The excerpt string, may be null if not set
     */
    public String getExcerpt() {
        return excerpt;
    }

    /**
     * Returns the estimated reading time in minutes.
     *
     * @return The reading time estimate in minutes
     */
    public int getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    /**
     * Returns the SEO-friendly slug for this article.
     *
     * @return The URL slug, may be null if not set
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Returns the article keywords for SEO.
     *
     * @return The keywords string, may be null if not set
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Returns article-specific metadata.
     *
     * @return A defensive copy of the article metadata map
     */
    public Map<String, Object> getArticleMetadata() {
        return new HashMap<>(articleMetadata);
    }

    // Article-specific setter methods

    /**
     * Sets the article category.
     *
     * @param category   The category to set, must not be null or empty
     * @param modifiedBy The ID of the user making this change
     * @throws IllegalArgumentException if category is invalid
     */
    public void setCategory(String category, String modifiedBy) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        this.category = category.trim();
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets whether comments are allowed on this article.
     *
     * @param allowComments true to allow comments, false to disable
     * @param modifiedBy    The ID of the user making this change
     */
    public void setAllowComments(boolean allowComments, String modifiedBy) {
        this.allowComments = allowComments;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the featured status of this article.
     *
     * @param featured   true to mark as featured, false otherwise
     * @param modifiedBy The ID of the user making this change
     */
    public void setFeatured(boolean featured, String modifiedBy) {
        this.featured = featured;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the article excerpt.
     *
     * @param excerpt    The excerpt text, may be null to clear
     * @param modifiedBy The ID of the user making this change
     * @throws IllegalArgumentException if excerpt exceeds maximum length
     */
    public void setExcerpt(String excerpt, String modifiedBy) {
        if (excerpt != null && excerpt.length() > MAX_EXCERPT_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Excerpt cannot exceed %d characters (current: %d)",
                            MAX_EXCERPT_LENGTH, excerpt.length()));
        }
        this.excerpt = excerpt != null ? excerpt.trim() : null;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Sets the SEO keywords for this article.
     *
     * @param keywords   The keywords string, may be null to clear
     * @param modifiedBy The ID of the user making this change
     */
    public void setKeywords(String keywords, String modifiedBy) {
        this.keywords = keywords != null ? keywords.trim() : null;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Adds article-specific metadata.
     *
     * @param key        The metadata key, must not be null or empty
     * @param value      The metadata value, must not be null
     * @param modifiedBy The ID of the user making this change
     */
    public void addArticleMetadata(String key, Object value, String modifiedBy) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Metadata value cannot be null");
        }

        articleMetadata.put(key, value);
        updateModificationInfo(modifiedBy);
    }

    // Private utility methods

    /**
     * Initializes article-specific properties from the properties map.
     *
     * @param properties The properties map containing article configuration
     */
    private void initializeArticleProperties(Map<String, Object> properties) {
        if (properties == null) {
            // Set default values
            this.category = "uncategorized";
            this.allowComments = true;
            this.featured = false;
            return;
        }

        // Extract article-specific properties
        this.category = getStringProperty(properties, "category", "uncategorized");
        this.allowComments = getBooleanProperty(properties, "allowComments", true);
        this.featured = getBooleanProperty(properties, "featured", false);
        this.excerpt = getStringProperty(properties, "excerpt", null);
        this.keywords = getStringProperty(properties, "keywords", null);
        this.slug = getStringProperty(properties, "slug", null);

        // Copy additional metadata
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!isStandardProperty(entry.getKey())) {
                articleMetadata.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Generates automatic properties like reading time and slug.
     */
    private void generateAutomaticProperties() {
        // Calculate reading time (average 200 words per minute)
        int wordCount = countWords(getBody());
        this.readingTimeMinutes = Math.max(1, wordCount / 200);

        // Generate slug if not provided
        if (this.slug == null || this.slug.trim().isEmpty()) {
            this.slug = generateSlugFromTitle(getTitle());
        }
    }

    /**
     * Updates modification tracking information.
     */
    private void updateModificationInfo(String modifiedBy) {
        // This would typically call a method in the parent class
        // For now, we'll add the metadata
        addMetadata("lastModifiedBy", modifiedBy, modifiedBy);
    }

    /**
     * Counts words in the given text.
     *
     * @param text The text to count words in
     * @return The number of words
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Validates if a slug is in the correct format.
     *
     * @param slug The slug to validate
     * @return true if the slug is valid
     */
    private boolean isValidSlug(String slug) {
        return slug != null && slug.matches("^[a-z0-9-]+$");
    }

    /**
     * Generates a URL-friendly slug from a title.
     *
     * @param title The title to convert to a slug
     * @return A URL-friendly slug
     */
    private String generateSlugFromTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "untitled";
        }

        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Helper method to get string property with default value.
     */
    private String getStringProperty(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * Helper method to get boolean property with default value.
     */
    private boolean getBooleanProperty(Map<String, Object> properties, String key, boolean defaultValue) {
        Object value = properties.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    /**
     * Checks if a property key is a standard article property.
     */
    private boolean isStandardProperty(String key) {
        return "title".equals(key) || "body".equals(key) || "category".equals(key) ||
                "allowComments".equals(key) || "featured".equals(key) || "excerpt".equals(key) ||
                "keywords".equals(key) || "slug".equals(key);
    }

    // Rendering methods

    /**
     * Renders article as HTML.
     */
    private String renderAsHtml(Map<String, Object> context) {
        StringBuilder html = new StringBuilder();
        html.append("<article class=\"cms-article\">\n");
        html.append("  <header>\n");
        html.append("    <h1>").append(escapeHtml(getTitle())).append("</h1>\n");

        if (excerpt != null && !excerpt.isEmpty()) {
            html.append("    <p class=\"excerpt\">").append(escapeHtml(excerpt)).append("</p>\n");
        }

        html.append("    <div class=\"article-meta\">\n");
        html.append("      <span class=\"category\">").append(escapeHtml(category)).append("</span>\n");
        html.append("      <span class=\"reading-time\">").append(readingTimeMinutes).append(" min read</span>\n");
        html.append("    </div>\n");
        html.append("  </header>\n");
        html.append("  <div class=\"content\">\n");
        html.append(getBody()); // Assuming body is already HTML
        html.append("\n  </div>\n");
        html.append("</article>");

        return html.toString();
    }

    /**
     * Renders article as JSON.
     */
    private String renderAsJson(Map<String, Object> context) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(getId()).append("\",\n");
        json.append("  \"type\": \"").append(getContentType()).append("\",\n");
        json.append("  \"title\": \"").append(escapeJson(getTitle())).append("\",\n");
        json.append("  \"body\": \"").append(escapeJson(getBody())).append("\",\n");
        json.append("  \"category\": \"").append(escapeJson(category)).append("\",\n");
        json.append("  \"featured\": ").append(featured).append(",\n");
        json.append("  \"allowComments\": ").append(allowComments).append(",\n");
        json.append("  \"readingTime\": ").append(readingTimeMinutes).append(",\n");

        if (excerpt != null) {
            json.append("  \"excerpt\": \"").append(escapeJson(excerpt)).append("\",\n");
        }

        if (slug != null) {
            json.append("  \"slug\": \"").append(escapeJson(slug)).append("\",\n");
        }

        json.append("  \"createdDate\": \"").append(getCreatedDate()).append("\",\n");
        json.append("  \"status\": \"").append(getStatus()).append("\"\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Renders article as plain text.
     */
    private String renderAsText(Map<String, Object> context) {
        StringBuilder text = new StringBuilder();
        text.append(getTitle()).append("\n");
        text.append("=".repeat(getTitle().length())).append("\n\n");

        if (excerpt != null && !excerpt.isEmpty()) {
            text.append(excerpt).append("\n\n");
        }

        text.append("Category: ").append(category).append("\n");
        text.append("Reading time: ").append(readingTimeMinutes).append(" minutes\n\n");

        // Strip HTML tags from body for plain text
        String plainBody = getBody().replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        text.append(plainBody);

        return text.toString();
    }

    /**
     * Renders article as RSS item.
     */
    private String renderAsRss(Map<String, Object> context) {
        StringBuilder rss = new StringBuilder();
        rss.append("    <item>\n");
        rss.append("      <title><![CDATA[").append(getTitle()).append("]]></title>\n");
        rss.append("      <description><![CDATA[");
        if (excerpt != null && !excerpt.isEmpty()) {
            rss.append(excerpt);
        } else {
            // Use first 200 characters of body
            String bodyText = getBody().replaceAll("<[^>]+>", "");
            rss.append(bodyText.length() > 200 ? bodyText.substring(0, 200) + "..." : bodyText);
        }
        rss.append("]]></description>\n");
        rss.append("      <category>").append(escapeXml(category)).append("</category>\n");
        rss.append("      <pubDate>").append(getCreatedDate()).append("</pubDate>\n");
        rss.append("      <guid>").append(getId()).append("</guid>\n");
        rss.append("    </item>");

        return rss.toString();
    }

    /**
     * Renders article excerpt.
     */
    private String renderExcerpt(Map<String, Object> context) {
        if (excerpt != null && !excerpt.isEmpty()) {
            return excerpt;
        }

        // Generate excerpt from body (first 200 characters)
        String bodyText = getBody().replaceAll("<[^>]+>", "").trim();
        if (bodyText.length() <= 200) {
            return bodyText;
        }

        // Find last complete word within 200 characters
        String truncated = bodyText.substring(0, 200);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0) {
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + "...";
    }

    // Utility methods for escaping content in different formats

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
