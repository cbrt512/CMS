package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentValidationException;
import com.cms.core.model.ContentRenderingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

/**
 * Concrete implementation of Content for image-type content in the JavaCMS
 * system.
 *
 * <p>
 * This class represents image files, photos, graphics, and other visual
 * content.
 * Images have specific properties like dimensions, file size, alt text, and
 * format validation.
 * </p>
 *
 * <p>
 * <strong>Factory Pattern:</strong> This class is created by the
 * ContentFactory,
 * demonstrating how the Factory Pattern handles media content with
 * format-specific validation.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ImageContent extends Content<ImageContent> {

    private String fileName;
    private String filePath;
    private long fileSize;
    private int width;
    private int height;
    private String altText;
    private String caption;
    private boolean thumbnail;
    private String mimeType;
    private Map<String, Object> imageMetadata;

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public ImageContent(String title, String body, String createdBy, Map<String, Object> properties) {
        super(title, body, createdBy);
        this.imageMetadata = new HashMap<>();
        initializeImageProperties(properties);
    }

    @Override
    public String getContentType() {
        return "image";
    }

    @Override
    public boolean validate() throws ContentValidationException {
        if (getTitle() == null || getTitle().trim().isEmpty()) {
            throw ContentValidationException.requiredField("title");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw ContentValidationException.requiredField("fileName");
        }

        // Validate file extension
        String extension = getFileExtension(fileName).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw ContentValidationException.invalidFormat("fileName", fileName,
                    "Supported image formats: " + String.join(", ", SUPPORTED_FORMATS));
        }

        // Validate file size
        if (fileSize > MAX_FILE_SIZE) {
            throw new ContentValidationException(
                    String.format("Image file size %d bytes exceeds maximum of %d bytes", fileSize, MAX_FILE_SIZE),
                    String.format("Image file is too large (%.1f MB). Maximum size is %.1f MB.",
                            fileSize / (1024.0 * 1024.0), MAX_FILE_SIZE / (1024.0 * 1024.0)),
                    "fileSize", fileSize);
        }

        // Validate dimensions
        if (width <= 0 || height <= 0) {
            throw new ContentValidationException(
                    String.format("Invalid image dimensions: %dx%d", width, height),
                    "Image dimensions must be positive values",
                    "dimensions", width + "x" + height);
        }

        return true;
    }

    @Override
    public String render(String format, Map<String, Object> context) throws ContentRenderingException {
        if (context == null)
            context = new HashMap<>();

        switch (format.toUpperCase()) {
            case "HTML":
                return renderAsHtml(context);
            case "JSON":
                return renderAsJson(context);
            case "IMG_TAG":
                return renderAsImgTag(context);
            default:
                throw ContentRenderingException.unsupportedFormat(getContentType(), format);
        }
    }

    // Getters and setters
    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getAltText() {
        return altText;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setAltText(String altText, String modifiedBy) {
        this.altText = altText;
        updateModificationInfo(modifiedBy);
    }

    public void setCaption(String caption, String modifiedBy) {
        this.caption = caption;
        updateModificationInfo(modifiedBy);
    }

    private void initializeImageProperties(Map<String, Object> properties) {
        if (properties == null)
            return;

        this.fileName = getStringProperty(properties, "fileName", "");
        this.filePath = getStringProperty(properties, "filePath", "");
        this.fileSize = getLongProperty(properties, "fileSize", 0L);
        this.width = getIntProperty(properties, "width", 0);
        this.height = getIntProperty(properties, "height", 0);
        this.altText = getStringProperty(properties, "altText", "");
        this.caption = getStringProperty(properties, "caption", "");
        this.thumbnail = getBooleanProperty(properties, "thumbnail", false);
        this.mimeType = getStringProperty(properties, "mimeType", determineMimeType(fileName));
    }

    private void updateModificationInfo(String modifiedBy) {
        addMetadata("lastModifiedBy", modifiedBy, modifiedBy);
    }

    private String renderAsHtml(Map<String, Object> context) {
        StringBuilder html = new StringBuilder();
        html.append("<figure class=\"cms-image\">\n");
        html.append("  <img src=\"").append(filePath).append("\" alt=\"").append(escapeHtml(altText)).append("\"");
        html.append(" width=\"").append(width).append("\" height=\"").append(height).append("\" />\n");
        if (caption != null && !caption.isEmpty()) {
            html.append("  <figcaption>").append(escapeHtml(caption)).append("</figcaption>\n");
        }
        html.append("</figure>");
        return html.toString();
    }

    private String renderAsImgTag(Map<String, Object> context) {
        return String.format("<img src=\"%s\" alt=\"%s\" width=\"%d\" height=\"%d\" />",
                filePath, escapeHtml(altText), width, height);
    }

    private String renderAsJson(Map<String, Object> context) {
        return String.format(
                "{\n  \"id\": \"%s\",\n  \"type\": \"%s\",\n  \"fileName\": \"%s\",\n  \"width\": %d,\n  \"height\": %d,\n  \"fileSize\": %d\n}",
                getId(), getContentType(), fileName, width, height, fileSize);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private String determineMimeType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }

    // Helper methods
    private String getStringProperty(Map<String, Object> props, String key, String def) {
        Object value = props.get(key);
        return value instanceof String ? (String) value : def;
    }

    private boolean getBooleanProperty(Map<String, Object> props, String key, boolean def) {
        Object value = props.get(key);
        return value instanceof Boolean ? (Boolean) value : def;
    }

    private int getIntProperty(Map<String, Object> props, String key, int def) {
        Object value = props.get(key);
        return value instanceof Integer ? (Integer) value : def;
    }

    private long getLongProperty(Map<String, Object> props, String key, long def) {
        Object value = props.get(key);
        return value instanceof Long ? (Long) value : def;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
