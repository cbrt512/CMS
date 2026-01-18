package com.cms.patterns.composite;

import java.util.List;

/**
 * Component interface for the Composite pattern implementation in JavaCMS.
 *
 * <p>
 * This interface defines the common operations that can be performed on both
 * composite objects (containers like Site and Category) and leaf objects
 * (individual ContentItem instances) in the site hierarchy. This creates a
 * uniform interface for manipulating both individual content and collections
 * of content.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Composite Pattern - Component Interface
 * <br>
 * This interface serves as the Component in the Composite design pattern,
 * establishing the contract that both Composite (Site, Category) and Leaf
 * (ContentItem) classes must implement. This allows clients to treat individual
 * objects and compositions uniformly.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the Composite Pattern
 * by providing the foundational component interface that enables
 * hierarchical content organization where clients can work with tree structures
 * of content without knowing whether they're dealing with individual content
 * items or collections.
 * </p>
 *
 * <p>
 * <strong>Pattern Benefits:</strong>
 * <ul>
 * <li><strong>Uniform Treatment:</strong> Clients can work with single content
 * items
 * and collections through the same interface</li>
 * <li><strong>Recursive Composition:</strong> Enables unlimited nesting of
 * categories
 * and content hierarchies</li>
 * <li><strong>Simplified Client Code:</strong> Client code doesn't need to
 * distinguish
 * between composite and leaf objects</li>
 * <li><strong>Flexible Structure:</strong> Easy to add new types of components
 * without
 * breaking existing client code</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * 
 * <pre>{@code
 * // Create site hierarchy
 * SiteComponent site = new Site("My Blog", "A personal blog", "https://myblog.com", "admin");
 * SiteComponent blogCategory = new Category("Blog Posts", "Article collection");
 * SiteComponent articleItem = new ContentItem(someArticleContent);
 *
 * // Build hierarchy using uniform interface
 * site.add(blogCategory);
 * blogCategory.add(articleItem);
 *
 * // Display entire hierarchy recursively
 * site.display(); // Works uniformly on all components
 * }</pre>
 *
 * <p>
 * <strong>Security Considerations:</strong> Implementations should validate all
 * input parameters and handle exceptions appropriately through the Exception
 * Shielding pattern to prevent system failures and provide user-friendly error
 * messages.
 * </p>
 *
 * @see com.cms.core.model.Site
 * @see com.cms.patterns.composite.Category
 * @see com.cms.patterns.composite.ContentItem
 * @see com.cms.core.model.Content
 * @since 1.0
 * @author Otman Hmich S007924
 */
public interface SiteComponent {

    /**
     * Adds a child component to this component.
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong>
     * <ul>
     * <li><strong>Composite nodes (Site, Category):</strong> Should add the
     * component
     * to their internal collection of children</li>
     * <li><strong>Leaf nodes (ContentItem):</strong> Should throw
     * UnsupportedOperationException as leaves cannot contain children</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Exception Shielding:</strong> Implementations should catch any
     * technical
     * exceptions and convert them to user-friendly messages while logging the
     * technical
     * details for debugging purposes.
     * </p>
     *
     * @param component The component to add as a child, must not be null
     * @throws IllegalArgumentException                      if component is null
     * @throws UnsupportedOperationException                 if called on a leaf
     *                                                       component
     * @throws com.cms.core.model.ContentManagementException if the component cannot
     *                                                       be added
     */
    void add(SiteComponent component) throws com.cms.core.model.ContentManagementException;

    /**
     * Removes a child component from this component.
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong>
     * <ul>
     * <li><strong>Composite nodes (Site, Category):</strong> Should remove the
     * component
     * from their internal collection if it exists</li>
     * <li><strong>Leaf nodes (ContentItem):</strong> Should throw
     * UnsupportedOperationException as leaves have no children to remove</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Safety Considerations:</strong> Should handle cases where the
     * component
     * to remove doesn't exist gracefully without throwing exceptions.
     * </p>
     *
     * @param component The component to remove, must not be null
     * @throws IllegalArgumentException                      if component is null
     * @throws UnsupportedOperationException                 if called on a leaf
     *                                                       component
     * @throws com.cms.core.model.ContentManagementException if removal fails
     */
    void remove(SiteComponent component) throws com.cms.core.model.ContentManagementException;

    /**
     * Retrieves the child component at the specified index.
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong>
     * <ul>
     * <li><strong>Composite nodes (Site, Category):</strong> Should return the
     * child
     * at the specified index from their internal collection</li>
     * <li><strong>Leaf nodes (ContentItem):</strong> Should throw
     * UnsupportedOperationException as leaves have no children</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Index Safety:</strong> Implementations should validate the index
     * range
     * and throw appropriate exceptions for invalid indices.
     * </p>
     *
     * @param index The zero-based index of the child to retrieve
     * @return The child component at the specified index, never null
     * @throws IndexOutOfBoundsException     if index is negative or >=
     *                                       getChildren().size()
     * @throws UnsupportedOperationException if called on a leaf component
     */
    SiteComponent getChild(int index);

    /**
     * Displays this component and recursively displays all its children.
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong>
     * <ul>
     * <li><strong>Composite nodes (Site, Category):</strong> Should display their
     * own
     * information then recursively call display() on all children with appropriate
     * indentation to show hierarchy</li>
     * <li><strong>Leaf nodes (ContentItem):</strong> Should display their content
     * information with appropriate formatting</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Display Format:</strong> Should use consistent formatting and
     * indentation
     * to clearly show the hierarchical structure. Consider including component
     * type,
     * name/title, and summary information.
     * </p>
     *
     * <p>
     * <strong>Usage Example Output:</strong>
     * </p>
     * 
     * <pre>
     * Site: My Blog (3 items)
     *   Category: Blog Posts (2 items)
     *     ContentItem: "Welcome to My Blog" [PUBLISHED]
     *     ContentItem: "About This Site" [DRAFT]
     *   Category: Pages (1 item)
     *     ContentItem: "Contact Us" [PUBLISHED]
     * </pre>
     */
    void display();

    /**
     * Retrieves all direct children of this component.
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong>
     * <ul>
     * <li><strong>Composite nodes (Site, Category):</strong> Should return a
     * defensive
     * copy of their children list to prevent external modification</li>
     * <li><strong>Leaf nodes (ContentItem):</strong> Should return an empty list as
     * leaves have no children</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns List&lt;SiteComponent&gt;
     * demonstrating proper generic usage and providing a standard collection
     * interface
     * for working with children.
     * </p>
     *
     * <p>
     * <strong>Defensive Programming:</strong> The returned list should be a copy to
     * prevent clients from modifying the internal structure directly. Modifications
     * should go through add() and remove() methods for proper validation and
     * consistency.
     * </p>
     *
     * @return A new List containing all direct children of this component, never
     *         null.
     *         Empty list if this component has no children (including leaf nodes).
     */
    List<SiteComponent> getChildren();

    /**
     * Gets the name or title of this component for display purposes.
     *
     * <p>
     * <strong>Component Identification:</strong> Provides a human-readable
     * identifier
     * for this component that can be used in displays, logging, and user
     * interfaces.
     * </p>
     *
     * <p>
     * <strong>Implementation Guidelines:</strong>
     * <ul>
     * <li><strong>Site:</strong> Should return the site name</li>
     * <li><strong>Category:</strong> Should return the category name</li>
     * <li><strong>ContentItem:</strong> Should return the content title</li>
     * </ul>
     * </p>
     *
     * @return The display name of this component, never null
     */
    String getName();

    /**
     * Gets the type of this component for identification and processing.
     *
     * <p>
     * <strong>Type Discrimination:</strong> Allows client code to identify the
     * specific type of component when uniform handling isn't sufficient and
     * type-specific operations are needed.
     * </p>
     *
     * <p>
     * <strong>Standard Types:</strong>
     * <ul>
     * <li><code>"site"</code> - Root site container</li>
     * <li><code>"category"</code> - Category/section container</li>
     * <li><code>"content"</code> - Individual content item</li>
     * </ul>
     * </p>
     *
     * @return The type identifier of this component, never null
     */
    String getComponentType();

    /**
     * Gets the total count of all items contained within this component
     * (recursive).
     *
     * <p>
     * <strong>Recursive Counting:</strong>
     * <ul>
     * <li><strong>Composite nodes:</strong> Should count all descendants
     * recursively,
     * not just direct children</li>
     * <li><strong>Leaf nodes:</strong> Should return 1 (representing
     * themselves)</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Usage:</strong> Useful for displaying statistics, calculating storage
     * implementations, and providing users with information about the size of
     * hierarchies.
     * </p>
     *
     * @return Total count of all items in this component's subtree (including
     *         itself)
     */
    int getItemCount();
}
