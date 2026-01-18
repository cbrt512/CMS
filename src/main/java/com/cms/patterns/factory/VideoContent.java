package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentValidationException;
import com.cms.core.model.ContentRenderingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

/**
 * Concrete implementation of Content for video-type content in the JavaCMS
 * system.
 *
 * <p>
 * This class represents video files and multimedia content with properties like
 * duration, resolution, playback controls, and format validation.
 * </p>
 *
 * <p>
 * <strong>Factory Pattern:</strong> This class is created by the
 * ContentFactory,
 * completing the set of concrete products that demonstrate the Factory
 * Pattern's
 * extensibility for different content types.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class VideoContent extends Content<VideoContent> {

    private String fileName;
    private String filePath;
    private long fileSize;
    private int duration; // in seconds
    private int width;
    private int height;
    private boolean autoplay;
    private boolean controls;
    private String posterImage;
    private String mimeType;
    private Map<String, Object> videoMetadata;

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "mp4", "webm", "ogg", "avi", "mov", "mkv");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public VideoContent(String title, String body, String createdBy, Map<String, Object> properties) {
        super(title, body, createdBy);
        this.videoMetadata = new HashMap<>();
        initializeVideoProperties(properties);
    }

    @Override
    public String getContentType() {
        return "video";
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
                    "Supported video formats: " + String.join(", ", SUPPORTED_FORMATS));
        }

        // Validate file size
        if (fileSize > MAX_FILE_SIZE) {
            throw new ContentValidationException(
                    String.format("Video file size %d bytes exceeds maximum of %d bytes", fileSize, MAX_FILE_SIZE),
                    String.format("Video file is too large (%.1f MB). Maximum size is %.1f MB.",
                            fileSize / (1024.0 * 1024.0), MAX_FILE_SIZE / (1024.0 * 1024.0)),
                    "fileSize", fileSize);
        }

        // Validate duration
        if (duration <= 0) {
            throw new ContentValidationException(
                    String.format("Invalid video duration: %d seconds", duration),
                    "Video duration must be a positive value",
                    "duration", duration);
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
            case "VIDEO_TAG":
                return renderAsVideoTag(context);
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

    public int getDuration() {
        return duration;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public boolean hasControls() {
        return controls;
    }

    public String getPosterImage() {
        return posterImage;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setAutoplay(boolean autoplay, String modifiedBy) {
        this.autoplay = autoplay;
        updateModificationInfo(modifiedBy);
    }

    public void setControls(boolean controls, String modifiedBy) {
        this.controls = controls;
        updateModificationInfo(modifiedBy);
    }

    public void setPosterImage(String posterImage, String modifiedBy) {
        this.posterImage = posterImage;
        updateModificationInfo(modifiedBy);
    }

    private void initializeVideoProperties(Map<String, Object> properties) {
        if (properties == null)
            return;

        this.fileName = getStringProperty(properties, "fileName", "");
        this.filePath = getStringProperty(properties, "filePath", "");
        this.fileSize = getLongProperty(properties, "fileSize", 0L);
        this.duration = getIntProperty(properties, "duration", 0);
        this.width = getIntProperty(properties, "width", 0);
        this.height = getIntProperty(properties, "height", 0);
        this.autoplay = getBooleanProperty(properties, "autoplay", false);
        this.controls = getBooleanProperty(properties, "controls", true);
        this.posterImage = getStringProperty(properties, "posterImage", "");
        this.mimeType = getStringProperty(properties, "mimeType", determineMimeType(fileName));
    }

    private void updateModificationInfo(String modifiedBy) {
        addMetadata("lastModifiedBy", modifiedBy, modifiedBy);
    }

    private String renderAsHtml(Map<String, Object> context) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cms-video\">\n");
        html.append("  <h3>").append(escapeHtml(getTitle())).append("</h3>\n");
        html.append(renderAsVideoTag(context));
        if (getBody() != null && !getBody().isEmpty()) {
            html.append("\n  <div class=\"video-description\">").append(getBody()).append("</div>\n");
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderAsVideoTag(Map<String, Object> context) {
        StringBuilder video = new StringBuilder();
        video.append("<video");
        if (width > 0 && height > 0) {
            video.append(" width=\"").append(width).append("\" height=\"").append(height).append("\"");
        }
        if (controls)
            video.append(" controls");
        if (autoplay)
            video.append(" autoplay");
        if (posterImage != null && !posterImage.isEmpty()) {
            video.append(" poster=\"").append(posterImage).append("\"");
        }
        video.append(">\n");
        video.append("  <source src=\"").append(filePath).append("\" type=\"").append(mimeType).append("\">\n");
        video.append("  Your browser does not support the video tag.\n");
        video.append("</video>");
        return video.toString();
    }

    private String renderAsJson(Map<String, Object> context) {
        return String.format(
                "{\n  \"id\": \"%s\",\n  \"type\": \"%s\",\n  \"fileName\": \"%s\",\n  \"duration\": %d,\n  \"width\": %d,\n  \"height\": %d,\n  \"fileSize\": %d\n}",
                getId(), getContentType(), fileName, duration, width, height, fileSize);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private String determineMimeType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        switch (ext) {
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "ogg":
                return "video/ogg";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "mkv":
                return "video/x-matroska";
            default:
                return "video/mp4"; // default fallback
        }
    }

    public String getFormattedDuration() {
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
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
