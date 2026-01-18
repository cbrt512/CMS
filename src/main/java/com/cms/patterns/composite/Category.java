package com.cms.patterns.composite;

import com.cms.core.model.ContentManagementException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a category in the site hierarchy that can contain subcategories
 * and content items.
 *
 * <p>
 * The Category class implements the Composite role in the Composite pattern,
 * serving as an
 * intermediate container that can hold both other categories (forming a tree
 * structure) and
 * individual content items. This enables the creation of hierarchical content
 * organization
 * systems with unlimited nesting depth.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Composite Pattern - Composite Class
 * <br>
 * This class serves as a Composite in the Composite design pattern,
 * implementing the
 * SiteComponent interface to provide container functionality. It can hold
 * collections of
 * other SiteComponent objects (both Categories and ContentItems), enabling
 * recursive
 * composition and hierarchical content organization.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the core Composite pattern
 * by providing a container class that can manage hierarchical structures of
 * content. This allows for flexible content organization where categories can
 * contain
 * both subcategories and individual content items.
 * </p>
 *
 * <p>
 * <strong>Collections Framework Usage:</strong> Extensively uses Java
 * Collections Framework
 * through CopyOnWriteArrayList for thread-safe concurrent access, HashMap for
 * metadata
 * storage, and various collection operations for managing child components.
 * </p>
 *
 * <p>
 * <strong>Generics Integration:</strong> Uses parameterized collections
 * throughout
 * (List&lt;SiteComponent&gt;, Map&lt;String, Object&gt;) for type safety and
 * compile-time
 * checking, contributing to the Generics implementation.
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li><strong>Blog Organization:</strong> "Articles" category containing
 * "Technology",
 * "Lifestyle" subcategories</li>
 * <li><strong>Documentation Structure:</strong> "User Guide" with
 * "Installation",
 * "Configuration" sections</li>
 * <li><strong>Media Library:</strong> "Images" with "Photos", "Graphics",
 * "Screenshots"
 * subcategories</li>
 * <li><strong>Product Catalog:</strong> "Electronics" with "Computers", "Mobile
 * Devices"
 * categories</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> Uses CopyOnWriteArrayList for thread-safe
 * operations
 * on the children collection, ensuring safe concurrent access in multi-threaded
 * environments.
 * </p>
 *
 * @see com.cms.patterns.composite.SiteComponent
 * @see com.cms.patterns.composite.ContentItem
 * @see com.cms.core.model.Site
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class Category implements SiteComponent {

    /** Unique identifier for this category */
    private final String id;

    /** Display name of this category */
    private String name;

    /** Description of this category's purpose and content */
    private String description;

    /** Thread-safe list of child components (subcategories and content items) */
    private final List<SiteComponent> children;

    /** Additional metadata for this category */
    private final Map<String, Object> metadata;

    /** Timestamp when this category was created */
    private final LocalDateTime createdDate;

    /** Timestamp of last modification */
    private LocalDateTime lastModified;

    /** User who created this category */
    private final String createdBy;

    /** User who last modified this category */
    private String lastModifiedBy;

    /** Parent component reference for navigation (optional) */
    private SiteComponent parent;

    /** Display order within parent for sorting */
    private int displayOrder;

    /** Whether this category is currently active/visible */
    private boolean active;

    /**
     * Constructs a new Category with the specified name and description.
     *
     * <p>
     * <strong>Collections Framework:</strong> Initializes CopyOnWriteArrayList for
     * thread-safe concurrent access and HashMap for metadata storage, demonstrating
     * proper collections usage in concurrent environments.
     * </p>
     *
     * <p>
     * <strong>Input Validation:</strong> Performs comprehensive input validation
     * and
     * sanitization to prevent security vulnerabilities and ensure data integrity.
     * </p>
     *
     * @param name        The display name for this category, must not be null or
     *                    empty
     * @param description A description of this category's purpose, may be null
     * @param createdBy   The ID of the user creating this category, must not be
     *                    null
     * @throws IllegalArgumentException if name is null/empty or createdBy is
     *                                  null/empty
     */
    public Category(String name, String description, String createdBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by user ID cannot be null or empty");
        }

        this.id = UUID.randomUUID().toString();
        this.name = sanitizeInput(name);
        this.description = description != null ? sanitizeInput(description) : "";
        this.createdBy = createdBy;
        this.createdDate = LocalDateTime.now();
        this.lastModified = this.createdDate;
        this.lastModifiedBy = createdBy;
        this.active = true;
        this.displayOrder = 0;

        // Initialize thread-safe collections
        this.children = new CopyOnWriteArrayList<>();
        this.metadata = new HashMap<>();

        // Set default metadata
        this.metadata.put("category.type", "general");
        this.metadata.put("category.created", this.createdDate);
        this.metadata.put("category.version", "1.0");
    }

    /**
     * Convenience constructor for simple category creation.
     *
     * @param name      The display name for this category
     * @param createdBy The ID of the user creating this category
     */
    public Category(String name, String createdBy) {
        this(name, null, createdBy);
    }

    /**
     * Adds a child component to this category.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> This method implements the
     * core
     * composite behavior by allowing this category to contain other SiteComponent
     * objects,
     * enabling the recursive tree structure that defines the Composite pattern.
     * </p>
     *
     * <p>
     * <strong>Exception Shielding:</strong> Catches technical exceptions and
     * converts
     * them to user-friendly ContentManagementException messages while logging
     * technical
     * details for debugging.
     * </p>
     *
     * <p>
     * <strong>Thread Safety:</strong> Uses thread-safe CopyOnWriteArrayList
     * operations
     * to ensure safe concurrent access when multiple threads are modifying the
     * category
     * structure.
     * </p>
     *
     * @param component The component to add (Category or ContentItem), must not be
     *                  null
     * @throws IllegalArgumentException   if component is null
     * @throws ContentManagementException if the component cannot be added
     */
    @Override
    public void add(SiteComponent component) throws ContentManagementException {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }

        try {
            // Prevent circular references
            if (component == this || wouldCreateCircularReference(component)) {
                throw new ContentManagementException(
                        String.format("Adding component %s would create circular reference in category %s",
                                component.getName(), this.name),
                        "Cannot add this item because it would create a circular reference in the category structure.");
            }

            // Check for duplicate names at same level
            for (SiteComponent child : children) {
                if (child.getName().equalsIgnoreCase(component.getName())) {
                    throw new ContentManagementException(
                            String.format("Duplicate component name '%s' in category %s",
                                    component.getName(), this.name),
                            String.format("A component named '%s' already exists in this category.",
                                    component.getName()));
                }
            }

            // Add the component
            children.add(component);

            // Set parent reference if component supports it
            if (component instanceof Category) {
                ((Category) component).parent = this;
            }

            updateModificationInfo("system"); // Will be updated when user context is available

        } catch (Exception e) {
            if (e instanceof ContentManagementException) {
                throw e;
            }
            throw new ContentManagementException(
                    String.format("Failed to add component to category %s: %s", this.name, e.getMessage()),
                    "Unable to add the item to this category. Please try again.",
                    e);
        }
    }

    /**
     * Removes a child component from this category.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Provides the removal
     * capability
     * needed by the Composite pattern, allowing dynamic modification of the tree
     * structure.
     * </p>
     *
     * <p>
     * <strong>Safe Removal:</strong> Handles cases where the component doesn't
     * exist
     * gracefully without throwing exceptions, and properly updates parent
     * references.
     * </p>
     *
     * @param component The component to remove, must not be null
     * @throws IllegalArgumentException   if component is null
     * @throws ContentManagementException if removal fails
     */
    @Override
    public void remove(SiteComponent component) throws ContentManagementException {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }

        try {
            boolean removed = children.remove(component);

            if (removed) {
                // Clear parent reference if component supports it
                if (component instanceof Category) {
                    ((Category) component).parent = null;
                }

                updateModificationInfo("system"); // Will be updated when user context is available
            }

            // Note: Not throwing exception if component wasn't found - this is safe removal
            // behavior

        } catch (Exception e) {
            throw new ContentManagementException(
                    String.format("Failed to remove component from category %s: %s", this.name, e.getMessage()),
                    "Unable to remove the item from this category. Please try again.",
                    e);
        }
    }

    /**
     * Retrieves the child component at the specified index.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Provides indexed access to
     * child
     * components, enabling clients to iterate through and access specific elements
     * in the
     * composite structure.
     * </p>
     *
     * @param index The zero-based index of the child to retrieve
     * @return The child component at the specified index
     * @throws IndexOutOfBoundsException if index is negative or >= children.size()
     */
    @Override
    public SiteComponent getChild(int index) {
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException(
                    String.format("Index %d is out of bounds. Category '%s' has %d children.",
                            index, name, children.size()));
        }
        return children.get(index);
    }

    /**
     * Displays this category and recursively displays all its children.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Implements the recursive
     * display
     * behavior that shows the uniform treatment of composite and leaf objects -
     * this method works the same whether called on a Category or ContentItem.
     * </p>
     *
     * <p>
     * <strong>Hierarchical Display:</strong> Uses indentation to show the tree
     * structure
     * and calls display() on all children to demonstrate the recursive nature of
     * the pattern.
     * </p>
     */
    @Override
    public void display() {
        display(0);
    }

    /**
     * Internal method for displaying with specific indentation level.
     *
     * @param indentLevel The level of indentation (0 = root level)
     */
    private void display(int indentLevel) {
        String indent = "  ".repeat(indentLevel);
        String statusText = active ? "ACTIVE" : "INACTIVE";

        System.out.printf("%sCategory: %s (%d items) [%s]%n",
                indent, name, getItemCount() - 1, statusText); // -1 to exclude self

        if (!description.isEmpty()) {
            System.out.printf("%s  Description: %s%n", indent, description);
        }

        // Display metadata if present
        if (!metadata.isEmpty()) {
            System.out.printf("%s  Metadata: %s%n", indent, formatMetadata());
        }

        // Recursively display all children
        for (SiteComponent child : children) {
            if (child instanceof Category) {
                ((Category) child).display(indentLevel + 1);
            } else {
                child.display();
                // Add indentation for content items
                String childOutput = captureChildDisplay(child);
                String[] lines = childOutput.split("\\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        System.out.printf("%s  %s%n", indent, line.trim());
                    }
                }
            }
        }
    }

    /**
     * Retrieves all direct children of this category.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns a defensive copy of the
     * children
     * list to prevent external modification while providing access to child
     * components.
     * </p>
     *
     * <p>
     * <strong>Thread Safety:</strong> The underlying CopyOnWriteArrayList ensures
     * that
     * the returned copy represents a consistent snapshot even in concurrent
     * environments.
     * </p>
     *
     * @return A new List containing all direct children, never null
     */
    @Override
    public List<SiteComponent> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Gets the name of this category.
     *
     * @return The category name, never null
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the component type identifier.
     *
     * @return "category" identifying this as a category component
     */
    @Override
    public String getComponentType() {
        return "category";
    }

    /**
     * Gets the total count of all items contained within this category (recursive).
     *
     * <p>
     * <strong>Composite Pattern Behavior:</strong> Recursively counts all
     * descendants,
     * demonstrating how the Composite pattern enables uniform operations across the
     * entire tree structure.
     * </p>
     *
     * @return Total count including this category and all descendants
     */
    @Override
    public int getItemCount() {
        int count = 1; // Count this category

        for (SiteComponent child : children) {
            count += child.getItemCount(); // Recursive counting
        }

        return count;
    }

    // Additional Category-specific methods

    /**
     * Gets the description of this category.
     *
     * @return The category description, never null (empty string if not set)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this category.
     *
     * @param description The new description, may be null
     * @param modifiedBy  The ID of the user making the change
     */
    public void setDescription(String description, String modifiedBy) {
        this.description = description != null ? sanitizeInput(description) : "";
        updateModificationInfo(modifiedBy);
    }

    /**
     * Gets the unique identifier of this category.
     *
     * @return The category ID, never null
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the creation date of this category.
     *
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the last modification date.
     *
     * @return The last modification timestamp
     */
    public LocalDateTime getLastModified() {
        return lastModified;
    }

    /**
     * Gets the parent component of this category.
     *
     * @return The parent component, or null if this is a root category
     */
    public SiteComponent getParent() {
        return parent;
    }

    /**
     * Checks if this category is currently active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active status of this category.
     *
     * @param active     The new active status
     * @param modifiedBy The ID of the user making the change
     */
    public void setActive(boolean active, String modifiedBy) {
        this.active = active;
        updateModificationInfo(modifiedBy);
    }

    /**
     * Gets all subcategories (direct children that are Category instances).
     *
     * @return List of direct subcategories, never null
     */
    public List<Category> getSubcategories() {
        return children.stream()
                .filter(child -> child instanceof Category)
                .map(child -> (Category) child)
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Gets all content items (direct children that are ContentItem instances).
     *
     * @return List of direct content items, never null
     */
    public List<SiteComponent> getContentItems() {
        return children.stream()
                .filter(child -> "content".equals(child.getComponentType()))
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Finds a child component by name (case-insensitive).
     *
     * @param name The name to search for
     * @return The first matching child, or null if not found
     */
    public SiteComponent findChildByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return children.stream()
                .filter(child -> child.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets metadata for this category.
     *
     * @param key        The metadata key, must not be null or empty
     * @param value      The metadata value, must not be null
     * @param modifiedBy The ID of the user making the change
     */
    public void setMetadata(String key, Object value, String modifiedBy) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata key cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Metadata value cannot be null");
        }

        metadata.put(sanitizeInput(key), value);
        updateModificationInfo(modifiedBy);
    }

    /**
     * Gets metadata value by key.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return metadata.get(key);
    }

    // Private utility methods

    /**
     * Checks if adding a component would create a circular reference.
     *
     * @param component The component to check
     * @return true if adding would create a circular reference
     */
    private boolean wouldCreateCircularReference(SiteComponent component) {
        // Walk up the parent chain to see if we'd create a cycle
        SiteComponent current = this.parent;
        while (current != null) {
            if (current == component) {
                return true;
            }
            if (current instanceof Category) {
                current = ((Category) current).parent;
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * Updates modification tracking information.
     *
     * @param modifiedBy The ID of the user making the change
     */
    private void updateModificationInfo(String modifiedBy) {
        this.lastModifiedBy = modifiedBy;
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Sanitizes input strings to prevent XSS attacks.
     *
     * @param input The input string to sanitize
     * @return The sanitized string
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        return input.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<.*?>", "")
                .trim();
    }

    /**
     * Formats metadata for display purposes.
     *
     * @return Formatted metadata string
     */
    private String formatMetadata() {
        if (metadata.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Captures display output from a child component.
     * This is a simplified implementation - in a real system you might use proper
     * output capturing.
     *
     * @param child The child component
     * @return Captured display output
     */
    private String captureChildDisplay(SiteComponent child) {
        // For now, return a simple representation
        return String.format("    %s: %s [%s]",
                child.getComponentType().toUpperCase(),
                child.getName(),
                child.getItemCount() + " items");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Category category = (Category) obj;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Category{id='%s', name='%s', children=%d, active=%s}",
                id, name, children.size(), active);
    }
}
