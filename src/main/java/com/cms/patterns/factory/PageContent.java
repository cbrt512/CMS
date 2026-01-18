package com.cms.patterns.factory;

import com.cms.core.model.Content;
import com.cms.core.model.ContentValidationException;
import com.cms.core.model.ContentRenderingException;
import java.util.Map;
import java.util.HashMap;

/**
 * Concrete implementation of Content for page-type content in the JavaCMS
 * system.
 *
 * <p>
 * This class represents static pages, landing pages, and other non-blog
 * content.
 * Pages typically have layout templates, menu integration, and hierarchical
 * organization.
 * </p>
 *
 * <p>
 * <strong>Factory Pattern:</strong> This class is one of the concrete products
 * created by the ContentFactory, demonstrating the Factory Pattern's ability
 * to create different content types with shared interfaces but unique
 * behaviors.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class PageContent extends Content<PageContent> {

    private String layout;
    private boolean showInMenu;
    private int menuOrder;
    private String parentPageId;
    private Map<String, Object> pageMetadata;

    private static final int MAX_TITLE_LENGTH = 100;

    public PageContent(String title, String body, String createdBy, Map<String, Object> properties) {
        super(title, body, createdBy);
        this.pageMetadata = new HashMap<>();
        initializePageProperties(properties);
    }

    @Override
    public String getContentType() {
        return "page";
    }

    @Override
    public boolean validate() throws ContentValidationException {
        if (getTitle() == null || getTitle().trim().isEmpty()) {
            throw ContentValidationException.requiredField("title");
        }

        if (getTitle().length() > MAX_TITLE_LENGTH) {
            throw ContentValidationException.fieldTooLong("title", getTitle().length(), MAX_TITLE_LENGTH);
        }

        if (getBody() == null) {
            throw ContentValidationException.requiredField("body");
        }

        if (layout == null || layout.trim().isEmpty()) {
            throw ContentValidationException.requiredField("layout");
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
            default:
                throw ContentRenderingException.unsupportedFormat(getContentType(), format);
        }
    }

    // Getters and setters
    public String getLayout() {
        return layout;
    }

    public boolean isShowInMenu() {
        return showInMenu;
    }

    public int getMenuOrder() {
        return menuOrder;
    }

    public String getParentPageId() {
        return parentPageId;
    }

    public void setLayout(String layout, String modifiedBy) {
        this.layout = layout;
        updateModificationInfo(modifiedBy);
    }

    public void setShowInMenu(boolean showInMenu, String modifiedBy) {
        this.showInMenu = showInMenu;
        updateModificationInfo(modifiedBy);
    }

    public void setMenuOrder(int menuOrder, String modifiedBy) {
        this.menuOrder = menuOrder;
        updateModificationInfo(modifiedBy);
    }

    private void initializePageProperties(Map<String, Object> properties) {
        if (properties == null) {
            this.layout = "default";
            this.showInMenu = false;
            this.menuOrder = 0;
            return;
        }

        this.layout = getStringProperty(properties, "layout", "default");
        this.showInMenu = getBooleanProperty(properties, "showInMenu", false);
        this.menuOrder = getIntProperty(properties, "menuOrder", 0);
        this.parentPageId = getStringProperty(properties, "parentPageId", null);
    }

    private void updateModificationInfo(String modifiedBy) {
        addMetadata("lastModifiedBy", modifiedBy, modifiedBy);
    }

    private String renderAsHtml(Map<String, Object> context) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cms-page\" data-layout=\"").append(layout).append("\">\n");
        html.append("  <h1>").append(escapeHtml(getTitle())).append("</h1>\n");
        html.append("  <div class=\"page-content\">\n");
        html.append(getBody());
        html.append("\n  </div>\n");
        html.append("</div>");
        return html.toString();
    }

    private String renderAsJson(Map<String, Object> context) {
        return String.format(
                "{\n  \"id\": \"%s\",\n  \"type\": \"%s\",\n  \"title\": \"%s\",\n  \"layout\": \"%s\",\n  \"showInMenu\": %s\n}",
                getId(), getContentType(), escapeJson(getTitle()), layout, showInMenu);
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

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
