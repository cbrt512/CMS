package com.cms.patterns.composite;

import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a leaf node in the site hierarchy that wraps individual Content
 * objects.
 *
 * <p>
 * The ContentItem class implements the Leaf role in the Composite pattern,
 * representing
 * individual content pieces that cannot contain other components. This class
 * acts as an
 * adapter between the existing Content model objects and the SiteComponent
 * hierarchy,
 * enabling content items to participate in the composite structure while
 * maintaining
 * their specific content-related behaviors.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Composite Pattern - Leaf Class
 * <br>
 * This class serves as a Leaf in the Composite design pattern, implementing the
 * SiteComponent interface but throwing UnsupportedOperationException for
 * operations
 * that don't make sense for leaf nodes (add, remove, getChild). This shows
 * the proper leaf behavior in the Composite pattern.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Completes the Composite pattern
 * implementation
 * by providing the leaf nodes that represent individual content items in
 * the hierarchical structure. This allows treating individual content items
 * uniformly
 * with composite containers through the SiteComponent interface.
 * </p>
 *
 * <p>
 * <strong>Adapter Pattern Integration:</strong> This class also shows aspects
 * of the Adapter pattern by adapting the existing Content&lt;?&gt; objects to
 * work
 * within the SiteComponent hierarchy, providing a bridge between the content
 * model
 * and the composite structure.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Returns appropriate empty collections
 * for
 * methods that don't apply to leaf nodes, demonstrating proper defensive
 * programming
 * and consistent interface compliance.
 * </p>
 *
 * <p>
 * <strong>Usage Examples:</strong>
 * <ul>
 * <li><strong>Article Content:</strong> Wrapping ArticleContent objects in site
 * hierarchy</li>
 * <li><strong>Page Content:</strong> Integrating PageContent into category
 * structures</li>
 * <li><strong>Media Content:</strong> Including ImageContent/VideoContent in
 * media categories</li>
 * <li><strong>Mixed Hierarchies:</strong> Combining different content types in
 * unified trees</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Leaf Node Behavior:</strong> As a leaf node, ContentItem:
 * <ul>
 * <li>Cannot contain child components (throws
 * UnsupportedOperationException)</li>
 * <li>Returns empty collections for child-related operations</li>
 * <li>Counts as 1 item for recursive counting operations</li>
 * <li>Displays its own content information when display() is called</li>
 * </ul>
 * </p>
 *
 * @see com.cms.patterns.composite.SiteComponent
 * @see com.cms.patterns.composite.Category
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class ContentItem implements SiteComponent {

    /** The wrapped content object that this item represents */
    private final Content<?> content;

    /** Optional parent component reference for navigation */
    private SiteComponent parent;

    /** Display order within parent container for sorting */
    private int displayOrder;

    /** Whether this content item is currently visible in hierarchical displays */
    private boolean visibleInHierarchy;

    /**
     * Constructs a new ContentItem wrapping the specified Content object.
     *
     * <p>
     * <strong>Adapter Pattern:</strong> This constructor shows the Adapter pattern
     * by taking an existing Content object and adapting it to work within the
     * SiteComponent
     * hierarchy. The wrapped content maintains its original functionality while
     * gaining the
     * ability to participate in composite operations.
     * </p>
     *
     * <p>
     * <strong>Input Validation:</strong> Validates that the content object is not
     * null
     * and contains valid data before wrapping it in the composite structure.
     * </p>
     *
     * @param content The Content object to wrap, must not be null
     * @throws IllegalArgumentException if content is null
     */
    public ContentItem(Content<?> content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        this.content = content;
        this.displayOrder = 0;
        this.visibleInHierarchy = true;
    }

    /**
     * Throws UnsupportedOperationException as leaf nodes cannot contain children.
     *
     * <p>
     * <strong>Composite Pattern - Leaf Behavior:</strong> This method implements
     * the
     * proper leaf behavior in the Composite pattern. Leaf nodes represent terminal
     * elements that cannot contain other components, so attempting to add children
     * should result in an UnsupportedOperationException.
     * </p>
     *
     * <p>
     * <strong>Exception Shielding:</strong> Provides a clear, user-friendly error
     * message explaining why this operation is not supported on content items.
     * </p>
     *
     * @param component The component that cannot be added
     * @throws UnsupportedOperationException always, as content items cannot contain
     *                                       children
     */
    @Override
    public void add(SiteComponent component) {
        throw new UnsupportedOperationException(
                String.format("Cannot add components to content item '%s'. Content items are leaf nodes " +
                        "and cannot contain other components. Use a Category instead if you need " +
                        "to group content items.", content.getTitle()));
    }

    /**
     * Throws UnsupportedOperationException as leaf nodes have no children to
     * remove.
     *
     * <p>
     * <strong>Composite Pattern - Leaf Behavior:</strong> Leaf nodes have no
     * children,
     * so removal operations are not applicable and should throw
     * UnsupportedOperationException.
     * </p>
     *
     * @param component The component that cannot be removed
     * @throws UnsupportedOperationException always, as content items have no
     *                                       children
     */
    @Override
    public void remove(SiteComponent component) {
        throw new UnsupportedOperationException(
                String.format("Cannot remove components from content item '%s'. Content items are leaf nodes " +
                        "and do not contain other components.", content.getTitle()));
    }

    /**
     * Throws UnsupportedOperationException as leaf nodes have no children to access
     * by index.
     *
     * <p>
     * <strong>Composite Pattern - Leaf Behavior:</strong> Since leaf nodes cannot
     * contain
     * children, attempting to access children by index is not meaningful and should
     * throw
     * UnsupportedOperationException.
     * </p>
     *
     * @param index The index that cannot be accessed
     * @throws UnsupportedOperationException always, as content items have no
     *                                       children
     */
    @Override
    public SiteComponent getChild(int index) {
        throw new UnsupportedOperationException(
                String.format("Cannot get child at index %d from content item '%s'. Content items are leaf nodes " +
                        "and do not contain other components.", index, content.getTitle()));
    }

    /**
     * Displays this content item with appropriate formatting.
     *
     * <p>
     * <strong>Composite Pattern - Uniform Interface:</strong> This method shows
     * the uniform interface principle of the Composite pattern. Whether called on a
     * Category (composite) or ContentItem (leaf), the display() method works
     * consistently,
     * allowing clients to treat both types uniformly.
     * </p>
     *
     * <p>
     * <strong>Content Display:</strong> Shows relevant content information
     * including
     * title, type, status, and key metadata in a format that integrates well with
     * hierarchical displays from composite nodes.
     * </p>
     */
    @Override
    public void display() {
        String statusText = getStatusDisplayText();
        String typeText = content.getContentType().toUpperCase();

        System.out.printf("ContentItem: \"%s\" [%s] (%s)%n",
                content.getTitle(), statusText, typeText);

        // Show additional content details
        if (content.getBody() != null && !content.getBody().trim().isEmpty()) {
            String preview = content.getBody().length() > 100
                    ? content.getBody().substring(0, 100) + "..."
                    : content.getBody();
            System.out.printf("  Preview: %s%n", preview.replaceAll("\\n", " "));
        }

        // Show creation info
        System.out.printf("  Created: %s by %s%n",
                formatDateTime(content.getCreatedDate()),
                content.getCreatedBy());

        // Show modification info if different from creation
        if (!content.getCreatedDate().equals(content.getModifiedDate())) {
            System.out.printf("  Modified: %s by %s%n",
                    formatDateTime(content.getModifiedDate()),
                    content.getModifiedBy());
        }

        // Show tags if present
        if (content.getTags().length > 0) {
            System.out.printf("  Tags: %s%n", String.join(", ", content.getTags()));
        }
    }

    /**
     * Returns an empty list as leaf nodes have no children.
     *
     * <p>
     * <strong>Composite Pattern - Consistent Interface:</strong> Even though leaf
     * nodes
     * have no children, they still implement the getChildren() method to maintain
     * interface
     * consistency. Returning an empty list allows client code to iterate over
     * children
     * without special handling for leaf vs composite nodes.
     * </p>
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns a new empty ArrayList to
     * ensure
     * consistent behavior and prevent null pointer exceptions in client code.
     * </p>
     *
     * @return An empty List, never null
     */
    @Override
    public List<SiteComponent> getChildren() {
        return new ArrayList<>(); // Leaf nodes have no children
    }

    /**
     * Gets the name of this content item (the content title).
     *
     * <p>
     * <strong>Uniform Interface:</strong> Provides the content title as the
     * component
     * name, enabling consistent naming behavior across all SiteComponent
     * implementations.
     * </p>
     *
     * @return The content title, never null
     */
    @Override
    public String getName() {
        return content.getTitle();
    }

    /**
     * Gets the component type identifier for content items.
     *
     * @return "content" identifying this as a content item component
     */
    @Override
    public String getComponentType() {
        return "content";
    }

    /**
     * Returns 1 as leaf nodes count as a single item.
     *
     * <p>
     * <strong>Composite Pattern - Recursive Operations:</strong> This method shows
     * how the Composite pattern enables recursive operations across the entire
     * structure.
     * Leaf nodes contribute a count of 1, while composite nodes sum the counts of
     * all
     * their children plus themselves.
     * </p>
     *
     * @return 1, representing this single content item
     */
    @Override
    public int getItemCount() {
        return 1; // Leaf nodes count as 1 item
    }

    // ContentItem-specific methods providing access to wrapped Content

    /**
     * Gets the wrapped Content object.
     *
     * <p>
     * <strong>Adapter Access:</strong> Provides access to the underlying Content
     * object
     * for operations that require content-specific functionality not available
     * through
     * the SiteComponent interface.
     * </p>
     *
     * @return The wrapped Content object, never null
     */
    public Content<?> getContent() {
        return content;
    }

    /**
     * Gets the content type from the wrapped content.
     *
     * @return The content type (e.g., "article", "page", "image", "video")
     */
    public String getContentType() {
        return content.getContentType();
    }

    /**
     * Gets the content status from the wrapped content.
     *
     * @return The current content status
     */
    public ContentStatus getContentStatus() {
        return content.getStatus();
    }

    /**
     * Gets the content creation date.
     *
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedDate() {
        return content.getCreatedDate();
    }

    /**
     * Gets the content modification date.
     *
     * @return The last modification timestamp
     */
    public LocalDateTime getModifiedDate() {
        return content.getModifiedDate();
    }

    /**
     * Gets the content creator user ID.
     *
     * @return The ID of the user who created this content
     */
    public String getCreatedBy() {
        return content.getCreatedBy();
    }

    /**
     * Gets the content modifier user ID.
     *
     * @return The ID of the user who last modified this content
     */
    public String getModifiedBy() {
        return content.getModifiedBy();
    }

    /**
     * Gets the content tags.
     *
     * @return Array of tags associated with this content
     */
    public String[] getTags() {
        return content.getTags().clone(); // Defensive copy
    }

    /**
     * Checks if the content is published.
     *
     * @return true if content status is PUBLISHED
     */
    public boolean isPublished() {
        return ContentStatus.PUBLISHED.equals(content.getStatus());
    }

    /**
     * Checks if the content is a draft.
     *
     * @return true if content status is DRAFT
     */
    public boolean isDraft() {
        return ContentStatus.DRAFT.equals(content.getStatus());
    }

    /**
     * Gets the parent component of this content item.
     *
     * @return The parent component, or null if this is a root item
     */
    public SiteComponent getParent() {
        return parent;
    }

    /**
     * Sets the parent component (used internally by containers).
     *
     * @param parent The new parent component
     */
    void setParent(SiteComponent parent) {
        this.parent = parent;
    }

    /**
     * Gets the display order within the parent container.
     *
     * @return The display order
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Sets the display order within the parent container.
     *
     * @param displayOrder The new display order
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Checks if this content item is visible in hierarchical displays.
     *
     * @return true if visible in hierarchy displays
     */
    public boolean isVisibleInHierarchy() {
        return visibleInHierarchy;
    }

    /**
     * Sets visibility in hierarchical displays.
     *
     * @param visibleInHierarchy Whether to show this item in hierarchy displays
     */
    public void setVisibleInHierarchy(boolean visibleInHierarchy) {
        this.visibleInHierarchy = visibleInHierarchy;
    }

    /**
     * Gets a summary of this content item for quick display.
     *
     * @return A summary string containing key information
     */
    public String getSummary() {
        return String.format("%s: \"%s\" [%s] (%s)",
                getContentType().toUpperCase(),
                content.getTitle(),
                getStatusDisplayText(),
                formatDateTime(content.getModifiedDate()));
    }

    // Private utility methods

    /**
     * Formats a LocalDateTime for display.
     *
     * @param dateTime The datetime to format
     * @return Formatted datetime string
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.toString().replace('T', ' ').substring(0, 19);
    }

    /**
     * Gets display text for content status.
     *
     * @return User-friendly status text
     */
    private String getStatusDisplayText() {
        if (content.getStatus() == null) {
            return "UNKNOWN";
        }

        switch (content.getStatus()) {
            case DRAFT:
                return "DRAFT";
            case PUBLISHED:
                return "PUBLISHED";
            case ARCHIVED:
                return "ARCHIVED";
            case REJECTED:
                return "REJECTED";
            default:
                return content.getStatus().toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        ContentItem that = (ContentItem) obj;
        return content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public String toString() {
        return String.format("ContentItem{content='%s', type='%s', status='%s'}",
                content.getTitle(),
                content.getContentType(),
                content.getStatus());
    }
}
