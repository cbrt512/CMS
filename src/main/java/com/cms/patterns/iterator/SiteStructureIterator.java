package com.cms.patterns.iterator;

import com.cms.patterns.composite.SiteComponent;
import java.util.*;
import java.util.function.Predicate;

/**
 * Iterator implementation for traversing site hierarchy structures in JavaCMS.
 *
 * <p>
 * This class provides multiple traversal strategies for navigating through the
 * composite site structure created by the SiteComponent hierarchy. It supports
 * both
 * depth-first and breadth-first traversal algorithms, enabling different
 * iteration
 * patterns based on specific use cases and implementations.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Iterator Pattern Implementation
 * <br>
 * This class serves as a concrete iterator in the Iterator design pattern,
 * specifically designed to work with the Composite pattern hierarchy
 * (SiteComponent).
 * It shows the integration of multiple design patterns by providing
 * sequential access to composite structures without exposing internal
 * complexity.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the Iterator Pattern
 * by providing controlled sequential access to hierarchical site structures,
 * integrating seamlessly with the existing Composite Pattern implementation,
 * and
 * supporting multiple traversal strategies for different operational needs.
 * </p>
 *
 * <p>
 * <strong>Traversal Strategies:</strong>
 * <ul>
 * <li><strong>Depth-First:</strong> Explores each branch completely before
 * moving to siblings</li>
 * <li><strong>Breadth-First:</strong> Explores all nodes at current level
 * before going deeper</li>
 * <li><strong>Leaf-Only:</strong> Iterates only through leaf nodes (ContentItem
 * instances)</li>
 * <li><strong>Composite-Only:</strong> Iterates only through composite nodes
 * (Category, Site)</li>
 * <li><strong>Filtered:</strong> Applies custom predicates for selective
 * iteration</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li>Site crawling and indexing operations</li>
 * <li>Content structure validation and analysis</li>
 * <li>Backup and export operations</li>
 * <li>Site map generation and navigation</li>
 * <li>SEO analysis and optimization</li>
 * </ul>
 * </p>
 *
 * @see com.cms.patterns.composite.SiteComponent
 * @see com.cms.patterns.composite.Category
 * @see com.cms.patterns.composite.ContentItem
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class SiteStructureIterator implements Iterator<SiteComponent> {

    /** Enumeration of supported traversal strategies */
    public enum TraversalMode {
        /** Depth-first traversal - explore branches completely */
        DEPTH_FIRST,
        /** Breadth-first traversal - explore level by level */
        BREADTH_FIRST,
        /** Only iterate through leaf components */
        LEAVES_ONLY,
        /** Only iterate through composite components */
        COMPOSITES_ONLY
    }

    /** The root component to start iteration from */
    private final SiteComponent root;

    /** Traversal mode for this iterator */
    private final TraversalMode mode;

    /** Optional filter predicate */
    private final Predicate<SiteComponent> filter;

    /** Queue for managing traversal order */
    private final Deque<SiteComponent> traversalQueue;

    /** Set to track visited nodes (prevents infinite loops) */
    private final Set<SiteComponent> visitedNodes;

    /** Pre-computed list of components to iterate (for efficiency) */
    private final List<SiteComponent> components;

    /** Internal iterator for the computed list */
    private final Iterator<SiteComponent> internalIterator;

    /** Current depth in the hierarchy (for depth-aware operations) */
    private int currentDepth;

    /**
     * Creates a site structure iterator with depth-first traversal.
     *
     * <p>
     * This constructor creates an iterator that will traverse the site
     * hierarchy using depth-first strategy, visiting all components.
     * </p>
     *
     * @param root The root component to start iteration from
     * @throws IllegalArgumentException if root is null
     * @since 1.0
     */
    public SiteStructureIterator(SiteComponent root) {
        this(root, TraversalMode.DEPTH_FIRST, null);
    }

    /**
     * Creates a site structure iterator with specified traversal mode.
     *
     * <p>
     * This constructor allows selection of the traversal strategy
     * while iterating through all components that match the mode.
     * </p>
     *
     * @param root The root component to start iteration from
     * @param mode The traversal strategy to use
     * @throws IllegalArgumentException if root or mode is null
     * @since 1.0
     */
    public SiteStructureIterator(SiteComponent root, TraversalMode mode) {
        this(root, mode, null);
    }

    /**
     * Creates a site structure iterator with custom filtering.
     *
     * <p>
     * This constructor provides full control over traversal strategy
     * and component selection through custom filtering predicates.
     * </p>
     *
     * @param root   The root component to start iteration from
     * @param mode   The traversal strategy to use
     * @param filter Optional filter predicate (null for no filtering)
     * @throws IllegalArgumentException if root or mode is null
     * @since 1.0
     */
    public SiteStructureIterator(SiteComponent root, TraversalMode mode, Predicate<SiteComponent> filter) {
        if (root == null) {
            throw new IllegalArgumentException("Root component cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Traversal mode cannot be null");
        }

        this.root = root;
        this.mode = mode;
        this.filter = filter;
        this.traversalQueue = new ArrayDeque<>();
        this.visitedNodes = new HashSet<>();
        this.currentDepth = 0;

        // Pre-compute the traversal sequence for efficiency
        this.components = computeTraversalSequence();
        this.internalIterator = components.iterator();
    }

    /**
     * Factory method for depth-first traversal iterator.
     *
     * @param root The root component to traverse from
     * @return Iterator configured for depth-first traversal
     * @since 1.0
     */
    public static SiteStructureIterator depthFirst(SiteComponent root) {
        return new SiteStructureIterator(root, TraversalMode.DEPTH_FIRST);
    }

    /**
     * Factory method for breadth-first traversal iterator.
     *
     * @param root The root component to traverse from
     * @return Iterator configured for breadth-first traversal
     * @since 1.0
     */
    public static SiteStructureIterator breadthFirst(SiteComponent root) {
        return new SiteStructureIterator(root, TraversalMode.BREADTH_FIRST);
    }

    /**
     * Factory method for leaf-only traversal iterator.
     *
     * <p>
     * This iterator will only visit leaf components (ContentItem instances)
     * in the hierarchy, skipping all composite nodes.
     * </p>
     *
     * @param root The root component to traverse from
     * @return Iterator that visits only leaf components
     * @since 1.0
     */
    public static SiteStructureIterator leavesOnly(SiteComponent root) {
        return new SiteStructureIterator(root, TraversalMode.LEAVES_ONLY);
    }

    /**
     * Factory method for composite-only traversal iterator.
     *
     * <p>
     * This iterator will only visit composite components (Site, Category instances)
     * in the hierarchy, skipping all leaf nodes.
     * </p>
     *
     * @param root The root component to traverse from
     * @return Iterator that visits only composite components
     * @since 1.0
     */
    public static SiteStructureIterator compositesOnly(SiteComponent root) {
        return new SiteStructureIterator(root, TraversalMode.COMPOSITES_ONLY);
    }

    /**
     * Factory method for filtered traversal iterator.
     *
     * <p>
     * This method creates an iterator that applies a custom filter predicate
     * to determine which components should be included in the iteration.
     * </p>
     *
     * @param root   The root component to traverse from
     * @param filter Predicate for filtering components
     * @return Iterator with custom filtering applied
     * @since 1.0
     */
    public static SiteStructureIterator filtered(SiteComponent root, Predicate<SiteComponent> filter) {
        return new SiteStructureIterator(root, TraversalMode.DEPTH_FIRST, filter);
    }

    /**
     * Factory method for components by name pattern iterator.
     *
     * <p>
     * Creates an iterator that only includes components whose names match
     * the specified pattern (case-insensitive substring match).
     * </p>
     *
     * @param root        The root component to traverse from
     * @param namePattern Pattern to match against component names
     * @return Iterator filtered by name pattern
     * @since 1.0
     */
    public static SiteStructureIterator byNamePattern(SiteComponent root, String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Name pattern cannot be null or empty");
        }

        String pattern = namePattern.toLowerCase().trim();
        Predicate<SiteComponent> nameFilter = component -> component.getName() != null &&
                component.getName().toLowerCase().contains(pattern);

        return new SiteStructureIterator(root, TraversalMode.DEPTH_FIRST, nameFilter);
    }

    /**
     * Factory method for components by type iterator.
     *
     * <p>
     * Creates an iterator that only includes components of the specified class
     * type.
     * </p>
     *
     * @param root          The root component to traverse from
     * @param componentType The class type to filter by
     * @param <T>           The specific component type
     * @return Iterator filtered by component type
     * @since 1.0
     */
    public static <T extends SiteComponent> SiteStructureIterator byType(
            SiteComponent root, Class<T> componentType) {
        if (componentType == null) {
            throw new IllegalArgumentException("Component type cannot be null");
        }

        Predicate<SiteComponent> typeFilter = componentType::isInstance;
        return new SiteStructureIterator(root, TraversalMode.DEPTH_FIRST, typeFilter);
    }

    /**
     * Computes the complete traversal sequence based on the configured mode and
     * filter.
     *
     * <p>
     * This method pre-computes the entire iteration sequence for efficiency,
     * applying the traversal algorithm and any filtering criteria.
     * </p>
     *
     * @return List of components in traversal order
     * @since 1.0
     */
    private List<SiteComponent> computeTraversalSequence() {
        List<SiteComponent> result = new ArrayList<>();

        switch (mode) {
            case DEPTH_FIRST:
                depthFirstTraversal(root, result, 0);
                break;
            case BREADTH_FIRST:
                breadthFirstTraversal(result);
                break;
            case LEAVES_ONLY:
                depthFirstTraversal(root, result, 0, this::isLeafComponent);
                break;
            case COMPOSITES_ONLY:
                depthFirstTraversal(root, result, 0, this::isCompositeComponent);
                break;
        }

        return result;
    }

    /**
     * Performs depth-first traversal of the site structure.
     *
     * @param component Current component being processed
     * @param result    List to collect traversed components
     * @param depth     Current depth in the hierarchy
     * @since 1.0
     */
    private void depthFirstTraversal(SiteComponent component, List<SiteComponent> result, int depth) {
        depthFirstTraversal(component, result, depth, null);
    }

    /**
     * Performs depth-first traversal with additional filtering.
     *
     * @param component        Current component being processed
     * @param result           List to collect traversed components
     * @param depth            Current depth in the hierarchy
     * @param additionalFilter Additional filter to apply
     * @since 1.0
     */
    private void depthFirstTraversal(SiteComponent component, List<SiteComponent> result,
            int depth, Predicate<SiteComponent> additionalFilter) {
        if (component == null || visitedNodes.contains(component)) {
            return; // Prevent infinite loops
        }

        visitedNodes.add(component);

        // Apply filters
        boolean includeComponent = true;
        if (filter != null && !filter.test(component)) {
            includeComponent = false;
        }
        if (additionalFilter != null && !additionalFilter.test(component)) {
            includeComponent = false;
        }

        if (includeComponent) {
            result.add(component);
        }

        // Traverse children
        try {
            List<SiteComponent> children = component.getChildren();
            if (children != null) {
                for (SiteComponent child : children) {
                    depthFirstTraversal(child, result, depth + 1, additionalFilter);
                }
            }
        } catch (UnsupportedOperationException e) {
            // This is a leaf component - no children to traverse
        }
    }

    /**
     * Performs breadth-first traversal of the site structure.
     *
     * @param result List to collect traversed components
     * @since 1.0
     */
    private void breadthFirstTraversal(List<SiteComponent> result) {
        Queue<SiteComponent> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            SiteComponent current = queue.poll();
            if (current == null || visitedNodes.contains(current)) {
                continue; // Skip null or already visited components
            }

            visitedNodes.add(current);

            // Apply filter
            if (filter == null || filter.test(current)) {
                result.add(current);
            }

            // Add children to queue
            try {
                List<SiteComponent> children = current.getChildren();
                if (children != null) {
                    for (SiteComponent child : children) {
                        if (child != null && !visitedNodes.contains(child)) {
                            queue.offer(child);
                        }
                    }
                }
            } catch (UnsupportedOperationException e) {
                // This is a leaf component - no children to add
            }
        }
    }

    /**
     * Determines if a component is a leaf component.
     *
     * @param component Component to check
     * @return true if component is a leaf (cannot have children)
     * @since 1.0
     */
    private boolean isLeafComponent(SiteComponent component) {
        try {
            component.getChildren();
            return false; // If getChildren() works, it's a composite
        } catch (UnsupportedOperationException e) {
            return true; // If getChildren() throws exception, it's a leaf
        }
    }

    /**
     * Determines if a component is a composite component.
     *
     * @param component Component to check
     * @return true if component is a composite (can have children)
     * @since 1.0
     */
    private boolean isCompositeComponent(SiteComponent component) {
        return !isLeafComponent(component);
    }

    /**
     * Returns true if there are more components in the iteration.
     *
     * @return true if there are more components to iterate
     * @since 1.0
     */
    @Override
    public boolean hasNext() {
        return internalIterator.hasNext();
    }

    /**
     * Returns the next component in the iteration sequence.
     *
     * @return The next SiteComponent in traversal order
     * @throws NoSuchElementException if there are no more components
     * @since 1.0
     */
    @Override
    public SiteComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more components available in site structure");
        }
        return internalIterator.next();
    }

    /**
     * Remove operation is not supported for structural integrity.
     *
     * @throws UnsupportedOperationException always
     * @since 1.0
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove operation not supported. Use SiteComponent methods for structural modifications.");
    }

    /**
     * Returns the total number of components that will be iterated.
     *
     * @return The count of components in the traversal sequence
     * @since 1.0
     */
    public int size() {
        return components.size();
    }

    /**
     * Returns true if no components match the traversal criteria.
     *
     * @return true if the iteration sequence is empty
     * @since 1.0
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    /**
     * Returns the traversal mode being used by this iterator.
     *
     * @return The current traversal mode
     * @since 1.0
     */
    public TraversalMode getTraversalMode() {
        return mode;
    }

    /**
     * Returns the filter predicate being used (if any).
     *
     * @return The current filter predicate, or null if no filtering
     * @since 1.0
     */
    public Predicate<SiteComponent> getFilter() {
        return filter;
    }

    /**
     * Returns the root component of this traversal.
     *
     * @return The root component
     * @since 1.0
     */
    public SiteComponent getRoot() {
        return root;
    }

    /**
     * Returns a list containing all components in the traversal sequence.
     *
     * <p>
     * This method returns a copy of the pre-computed traversal sequence
     * without consuming the iterator state.
     * </p>
     *
     * @return List of all components in traversal order
     * @since 1.0
     */
    public List<SiteComponent> toList() {
        return new ArrayList<>(components);
    }

    /**
     * Creates a new iterator with an additional filter applied.
     *
     * <p>
     * This method enables filter chaining by creating a new iterator
     * that combines the current filter with an additional predicate.
     * </p>
     *
     * @param additionalFilter Additional filter to apply
     * @return New iterator with combined filters
     * @since 1.0
     */
    public SiteStructureIterator filter(Predicate<SiteComponent> additionalFilter) {
        if (additionalFilter == null) {
            return new SiteStructureIterator(root, mode, filter);
        }

        Predicate<SiteComponent> combinedFilter = filter == null ? additionalFilter : filter.and(additionalFilter);

        return new SiteStructureIterator(root, mode, combinedFilter);
    }

    /**
     * Returns a string representation of this iterator.
     *
     * @return String describing iterator configuration and state
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format("SiteStructureIterator{root='%s', mode=%s, size=%d, hasFilter=%s}",
                root.getName(), mode, components.size(), filter != null);
    }
}
