package com.cms.core.model;

import com.cms.patterns.composite.SiteComponent;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a website or site container in the JavaCMS system.
 *
 * <p>
 * The Site class serves as the top-level container for all content, users, and
 * configuration
 * within a single content management system instance. It implements the root
 * node of the
 * Composite pattern hierarchy and provides centralized management for site-wide
 * settings,
 * content organization, and user management.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> This class serves as the root component in
 * the Composite
 * pattern implementation that will be fully implemented in Phase 2. It acts as
 * the
 * top-level container that can hold categories, sections, and individual
 * content items.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Extensively uses Java Collections
 * Framework
 * with ConcurrentHashMap for thread-safe content storage, HashMap for metadata,
 * TreeMap for
 * ordered content organization, and various List and Set implementations for
 * different
 * collection needs.
 * </p>
 *
 * <p>
 * <strong>Generics:</strong> Provides comprehensive Generics usage with
 * type-safe collections, parameterized methods, and generic utility operations
 * throughout
 * the content management functionality.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Provides the foundation for content management
 * operations and serves as the integration point for all major CMS components
 * including
 * user management, content storage, and system configuration.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.core.model.User
 * @see com.cms.patterns.composite.SiteComponent
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class Site implements SiteComponent {

    /** Unique identifier for this site */
    private final String id;

    /** The site name/title */
    private String name;

    /** Brief description of the site */
    private String description;

    /** The site's base URL */
    private String baseUrl;

    /** Site creation timestamp */
    private final LocalDateTime createdDate;

    /** Last modification timestamp */
    private LocalDateTime lastModified;

    /** User who last modified the site */
    private String lastModifiedBy;

    /** Site-wide metadata and configuration settings */
    private Map<String, Object> metadata;

    /** Thread-safe storage for all content in the site */
    private Map<String, Content<?>> contentStorage;

    /** Registered users of this site */
    private Map<String, User> userRegistry;

    /** Content organized by type for efficient retrieval */
    private Map<String, List<Content<?>>> contentByType;

    /** Content organized by status for workflow management */
    private Map<ContentStatus, Set<String>> contentByStatus;

    /** Content organized by creation date for chronological access */
    private TreeMap<LocalDateTime, Set<String>> contentByDate;

    /** Tags mapped to content IDs for categorization */
    private Map<String, Set<String>> contentByTag;

    /** Active user sessions for session management */
    private Map<String, UserSession> activeSessions;

    /** Site configuration properties */
    private Properties siteConfiguration;

    /** Indicates if the site is currently active/published */
    private boolean active;

    /** Default content type for new content creation */
    private String defaultContentType;

    /**
     * Root-level components (categories and content items) for composite pattern
     */
    private List<SiteComponent> rootComponents;

    /**
     * Constructs a new Site with the specified basic information.
     *
     * <p>
     * <strong>Collections Framework:</strong> Initializes all the various
     * collection
     * types used throughout the CMS, demonstrating comprehensive Collections
     * Framework
     * usage with different collection interfaces and implementations.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> All collections are properly parameterized with
     * appropriate generic types for type safety and compile-time checking.
     * </p>
     *
     * @param name        The site name, must not be null or empty
     * @param description Brief description of the site
     * @param baseUrl     The site's base URL, must be valid format
     * @param createdBy   The ID of the user creating this site
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public Site(String name, String description, String baseUrl, String createdBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Site name cannot be null or empty");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by user ID cannot be null or empty");
        }

        this.id = UUID.randomUUID().toString();
        this.name = sanitizeInput(name);
        this.description = description != null ? sanitizeInput(description) : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.createdDate = LocalDateTime.now();
        this.lastModified = this.createdDate;
        this.lastModifiedBy = createdBy;
        this.active = true;
        this.defaultContentType = "article";

        // Initialize all collection-based storage systems
        initializeCollections();

        // Initialize default configuration
        initializeDefaultConfiguration();
    }

    /**
     * Initializes all collection-based storage systems.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses initialization of various
     * collection types including ConcurrentHashMap for thread safety, HashMap for
     * general purpose mapping, TreeMap for ordered storage, and different Set and
     * List implementations for specific use cases.
     * </p>
     */
    private void initializeCollections() {
        // Thread-safe storage for high-concurrency scenarios
        this.contentStorage = new ConcurrentHashMap<>();
        this.userRegistry = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();

        // General purpose collections
        this.metadata = new HashMap<>();
        this.contentByType = new HashMap<>();
        this.contentByTag = new HashMap<>();

        // Ordered collections for status and date-based organization
        this.contentByStatus = new EnumMap<>(ContentStatus.class);
        this.contentByDate = new TreeMap<>();

        // Initialize content status sets
        for (ContentStatus status : ContentStatus.values()) {
            contentByStatus.put(status, new HashSet<>());
        }

        // Initialize root components for composite pattern
        this.rootComponents = new ArrayList<>();
    }

    /**
     * Initializes default site configuration.
     */
    private void initializeDefaultConfiguration() {
        this.siteConfiguration = new Properties();
        this.siteConfiguration.setProperty("site.theme", "default");
        this.siteConfiguration.setProperty("site.language", "en");
        this.siteConfiguration.setProperty("site.timezone", "UTC");
        this.siteConfiguration.setProperty("content.default.status", "DRAFT");
        this.siteConfiguration.setProperty("content.auto.publish", "false");
    }

    // Content Management Methods

    /**
     * Adds content to the site's content management system.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses multiple collection operations
     * including Map.put(), Set.add(), List.add(), and TreeMap operations for
     * organizing
     * content across multiple indexing schemes.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Uses parameterized types throughout for type-safe
     * content operations and collection management.
     * </p>
     *
     * @param content The content to add, must not be null
     * @param addedBy The ID of the user adding the content
     * @throws IllegalArgumentException   if content is null or addedBy is invalid
     * @throws ContentManagementException if content cannot be added
     */
    public void addContent(Content<?> content, String addedBy) throws ContentManagementException {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (addedBy == null || addedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Added by user ID cannot be null or empty");
        }

        try {
            // Validate content before adding
            if (!content.validate()) {
                throw new ContentManagementException(
                        String.format("Content validation failed for content ID: %s", content.getId()),
                        "The content could not be added because it contains invalid information.");
            }

            String contentId = content.getId();
            String contentType = content.getContentType();

            // Add to primary storage
            contentStorage.put(contentId, content);

            // Index by content type
            contentByType.computeIfAbsent(contentType, k -> new ArrayList<>()).add(content);

            // Index by status
            contentByStatus.get(content.getStatus()).add(contentId);

            // Index by creation date
            contentByDate.computeIfAbsent(content.getCreatedDate(), k -> new HashSet<>()).add(contentId);

            // Index by tags
            for (String tag : content.getTags()) {
                contentByTag.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(contentId);
            }

            updateModificationInfo(addedBy);

        } catch (ContentValidationException e) {
            throw new ContentManagementException(
                    "Content validation failed: " + e.getMessage(),
                    e.getUserMessage(),
                    e);
        }
    }

    /**
     * Retrieves content by its unique ID.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Map.get() operation with
     * generic return type handling.
     * </p>
     *
     * @param contentId The unique content identifier
     * @return The content if found, null otherwise
     */
    public Content<?> getContent(String contentId) {
        if (contentId == null || contentId.trim().isEmpty()) {
            return null;
        }
        return contentStorage.get(contentId);
    }

    /**
     * Retrieves all content of a specific type.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns defensive copy of List to
     * prevent
     * external modification while providing access to type-filtered content.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Maintains type safety through parameterized return
     * type.
     * </p>
     *
     * @param contentType The type of content to retrieve
     * @return A new List containing all content of the specified type, never null
     */
    public List<Content<?>> getContentByType(String contentType) {
        if (contentType == null) {
            return new ArrayList<>();
        }

        List<Content<?>> typeContent = contentByType.get(contentType);
        return typeContent != null ? new ArrayList<>(typeContent) : new ArrayList<>();
    }

    /**
     * Retrieves all content with a specific status.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Set operations and stream
     * processing for filtering and collecting content based on status.
     * </p>
     *
     * @param status The content status to filter by
     * @return A new List containing all content with the specified status
     */
    public List<Content<?>> getContentByStatus(ContentStatus status) {
        if (status == null) {
            return new ArrayList<>();
        }

        Set<String> contentIds = contentByStatus.get(status);
        return contentIds.stream()
                .map(contentStorage::get)
                .filter(Objects::nonNull)
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Searches for content containing the specified text in title or body.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses stream operations with
     * filtering and collection operations for text-based content search.
     * </p>
     *
     * <p>
     * <strong>Generics:</strong> Uses parameterized stream operations and
     * collectors
     * for type-safe search result handling.
     * </p>
     *
     * @param searchText The text to search for (case-insensitive)
     * @return A List of matching content, ordered by relevance
     */
    public List<Content<?>> searchContent(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerSearchText = searchText.toLowerCase();

        return contentStorage.values().stream()
                .filter(content -> content.getTitle().toLowerCase().contains(lowerSearchText) ||
                        content.getBody().toLowerCase().contains(lowerSearchText))
                .sorted((c1, c2) -> {
                    // Simple relevance scoring - title matches rank higher
                    boolean c1TitleMatch = c1.getTitle().toLowerCase().contains(lowerSearchText);
                    boolean c2TitleMatch = c2.getTitle().toLowerCase().contains(lowerSearchText);

                    if (c1TitleMatch && !c2TitleMatch)
                        return -1;
                    if (!c1TitleMatch && c2TitleMatch)
                        return 1;

                    // Secondary sort by modification date (newest first)
                    return c2.getModifiedDate().compareTo(c1.getModifiedDate());
                })
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Retrieves content by tag.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Set-to-Stream-to-List
     * conversion with filtering operations.
     * </p>
     *
     * @param tag The tag to search for (case-insensitive)
     * @return A List of content tagged with the specified tag
     */
    public List<Content<?>> getContentByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> contentIds = contentByTag.get(tag.toLowerCase());
        if (contentIds == null) {
            return new ArrayList<>();
        }

        return contentIds.stream()
                .map(contentStorage::get)
                .filter(Objects::nonNull)
                .sorted((c1, c2) -> c2.getModifiedDate().compareTo(c1.getModifiedDate()))
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Removes content from the site.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses removal operations across
     * multiple collection types to maintain consistency in all indexing schemes.
     * </p>
     *
     * @param contentId The ID of the content to remove
     * @param removedBy The ID of the user removing the content
     * @return true if content was removed, false if not found
     */
    public boolean removeContent(String contentId, String removedBy) {
        if (contentId == null || removedBy == null) {
            return false;
        }

        Content<?> content = contentStorage.remove(contentId);
        if (content == null) {
            return false;
        }

        // Remove from all indexes
        String contentType = content.getContentType();
        List<Content<?>> typeList = contentByType.get(contentType);
        if (typeList != null) {
            typeList.removeIf(c -> c.getId().equals(contentId));
        }

        // Remove from status index
        for (Set<String> statusSet : contentByStatus.values()) {
            statusSet.remove(contentId);
        }

        // Remove from date index
        contentByDate.values().forEach(dateSet -> dateSet.remove(contentId));

        // Remove from tag indexes
        for (String tag : content.getTags()) {
            Set<String> tagSet = contentByTag.get(tag.toLowerCase());
            if (tagSet != null) {
                tagSet.remove(contentId);
                if (tagSet.isEmpty()) {
                    contentByTag.remove(tag.toLowerCase());
                }
            }
        }

        updateModificationInfo(removedBy);
        return true;
    }

    // User Management Methods

    /**
     * Registers a new user with the site.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses ConcurrentHashMap for
     * thread-safe
     * user registration operations.
     * </p>
     *
     * @param user         The user to register, must not be null
     * @param registeredBy The ID of the user performing the registration
     * @throws IllegalArgumentException if user is null or registeredBy is invalid
     * @throws UserManagementException  if user cannot be registered
     */
    public void registerUser(User user, String registeredBy) throws UserManagementException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (registeredBy == null || registeredBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Registered by user ID cannot be null or empty");
        }

        // Check for duplicate username or email
        for (User existingUser : userRegistry.values()) {
            if (existingUser.getUsername().equalsIgnoreCase(user.getUsername())) {
                throw UserManagementException.duplicateUsername(user.getUsername());
            }
            if (existingUser.getEmail().equalsIgnoreCase(user.getEmail())) {
                throw UserManagementException.duplicateEmail(user.getEmail());
            }
        }

        userRegistry.put(user.getId(), user);
        updateModificationInfo(registeredBy);
    }

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The unique user identifier
     * @return The user if found, null otherwise
     */
    public User getUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return userRegistry.get(userId);
    }

    /**
     * Finds a user by their username.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses stream operations for
     * searching collections with custom criteria.
     * </p>
     *
     * @param username The username to search for (case-insensitive)
     * @return The user if found, null otherwise
     */
    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        return userRegistry.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all registered users.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns defensive copy to prevent
     * external modification of the user registry.
     * </p>
     *
     * @return A new List containing all registered users
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(userRegistry.values());
    }

    // Configuration and Metadata Methods

    /**
     * Sets a metadata property for the site.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Map operations for
     * dynamic metadata management.
     * </p>
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
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        metadata.put(sanitizeInput(key), value);
        updateModificationInfo(modifiedBy);
    }

    /**
     * Gets a metadata property value.
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

    /**
     * Returns a copy of all metadata.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns defensive copy of metadata
     * map.
     * </p>
     *
     * @return A new HashMap containing all metadata entries
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }

    // Site Statistics Methods

    /**
     * Gets the total number of content items.
     *
     * @return The total content count
     */
    public int getTotalContentCount() {
        return contentStorage.size();
    }

    /**
     * Gets the number of published content items.
     *
     * @return The published content count
     */
    public int getPublishedContentCount() {
        return contentByStatus.get(ContentStatus.PUBLISHED).size();
    }

    /**
     * Gets content statistics by type.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses map transformation operations
     * and stream collectors for generating statistics.
     * </p>
     *
     * @return A Map with content types as keys and counts as values
     */
    public Map<String, Integer> getContentStatsByType() {
        return contentByType.entrySet().stream()
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().size()),
                        HashMap::putAll);
    }

    /**
     * Gets all unique tags in the system.
     *
     * <p>
     * <strong>Collections Framework:</strong> Uses Set operations for
     * collecting unique values from map keys.
     * </p>
     *
     * @return A Set of all unique tags
     */
    public Set<String> getAllTags() {
        return new HashSet<>(contentByTag.keySet());
    }

    // Getter methods

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public boolean isActive() {
        return active;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    // Setter methods with validation

    public void setName(String name, String modifiedBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Site name cannot be null or empty");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.name = sanitizeInput(name);
        updateModificationInfo(modifiedBy);
    }

    public void setDescription(String description, String modifiedBy) {
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.description = description != null ? sanitizeInput(description) : "";
        updateModificationInfo(modifiedBy);
    }

    public void setActive(boolean active, String modifiedBy) {
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user ID cannot be null or empty");
        }

        this.active = active;
        updateModificationInfo(modifiedBy);
    }

    // SiteComponent Interface Implementation (Composite Pattern)

    /**
     * Adds a root-level component (Category or ContentItem) to this site.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> This method implements the
     * composite behavior for the Site class as the root node of the composite
     * hierarchy.
     * It allows adding both Categories and ContentItems as direct children of the
     * site.
     * </p>
     *
     * <p>
     * <strong>Exception Shielding:</strong> Wraps any technical exceptions in
     * ContentManagementException with user-friendly messages.
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
            // Check for duplicate names at root level
            for (SiteComponent existing : rootComponents) {
                if (existing.getName().equalsIgnoreCase(component.getName())) {
                    throw new ContentManagementException(
                            String.format("Duplicate root component name '%s' in site %s",
                                    component.getName(), this.name),
                            String.format("A component named '%s' already exists at the root level of this site.",
                                    component.getName()));
                }
            }

            rootComponents.add(component);
            updateModificationInfo("system"); // Will be updated when user context is available

        } catch (Exception e) {
            if (e instanceof ContentManagementException) {
                throw e;
            }
            throw new ContentManagementException(
                    String.format("Failed to add root component to site %s: %s", this.name, e.getMessage()),
                    "Unable to add the component to this site. Please try again.",
                    e);
        }
    }

    /**
     * Removes a root-level component from this site.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Provides removal
     * capability
     * for root-level components, maintaining the dynamic nature of the composite
     * structure.
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
            boolean removed = rootComponents.remove(component);
            if (removed) {
                updateModificationInfo("system"); // Will be updated when user context is available
            }

        } catch (Exception e) {
            throw new ContentManagementException(
                    String.format("Failed to remove root component from site %s: %s", this.name, e.getMessage()),
                    "Unable to remove the component from this site. Please try again.",
                    e);
        }
    }

    /**
     * Retrieves the root-level child component at the specified index.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Provides indexed access to
     * root-level components in the site hierarchy.
     * </p>
     *
     * @param index The zero-based index of the child to retrieve
     * @return The root component at the specified index
     * @throws IndexOutOfBoundsException if index is negative or >=
     *                                   rootComponents.size()
     */
    @Override
    public SiteComponent getChild(int index) {
        if (index < 0 || index >= rootComponents.size()) {
            throw new IndexOutOfBoundsException(
                    String.format("Index %d is out of bounds. Site '%s' has %d root components.",
                            index, name, rootComponents.size()));
        }
        return rootComponents.get(index);
    }

    /**
     * Displays this site and recursively displays all root components.
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Provides the uniform
     * treatment principle by implementing the same display() method that works on
     * both composite and leaf nodes in the hierarchy.
     * </p>
     */
    @Override
    public void display() {
        String statusText = active ? "ACTIVE" : "INACTIVE";

        System.out.printf("Site: %s (%d root components) [%s]%n",
                name, rootComponents.size(), statusText);

        if (!description.isEmpty()) {
            System.out.printf("  Description: %s%n", description);
        }

        System.out.printf("  URL: %s%n", baseUrl);
        System.out.printf("  Total Content: %d items%n", getTotalContentCount());
        System.out.printf("  Users: %d registered%n", userRegistry.size());
        System.out.printf("  Created: %s by %s%n", createdDate.toString().replace('T', ' '), lastModifiedBy);

        if (!rootComponents.isEmpty()) {
            System.out.println("  Root Components:");
            for (SiteComponent component : rootComponents) {
                component.display();
            }
        }
    }

    /**
     * Retrieves all root-level components of this site.
     *
     * <p>
     * <strong>Collections Framework:</strong> Returns a defensive copy to prevent
     * external modification of the root components structure.
     * </p>
     *
     * @return A new List containing all root components, never null
     */
    @Override
    public List<SiteComponent> getChildren() {
        return new ArrayList<>(rootComponents);
    }

    /**
     * Gets the component type identifier for sites.
     *
     * @return "site" identifying this as a site component
     */
    @Override
    public String getComponentType() {
        return "site";
    }

    /**
     * Gets the total count of all items in the site hierarchy (recursive).
     *
     * <p>
     * <strong>Composite Pattern Implementation:</strong> Recursively counts all
     * components in the site hierarchy, demonstrating the recursive nature of
     * composite operations.
     * </p>
     *
     * @return Total count including this site and all descendants
     */
    @Override
    public int getItemCount() {
        int count = 1; // Count this site

        for (SiteComponent component : rootComponents) {
            count += component.getItemCount(); // Recursive counting
        }

        return count;
    }

    // Additional Site-specific methods for composite pattern integration

    /**
     * Gets all root-level categories.
     *
     * @return List of root categories, never null
     */
    public List<SiteComponent> getRootCategories() {
        return rootComponents.stream()
                .filter(component -> "category".equals(component.getComponentType()))
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Gets all root-level content items.
     *
     * @return List of root content items, never null
     */
    public List<SiteComponent> getRootContentItems() {
        return rootComponents.stream()
                .filter(component -> "content".equals(component.getComponentType()))
                .collect(ArrayList::new, List::add, List::addAll);
    }

    /**
     * Finds a root component by name (case-insensitive).
     *
     * @param name The name to search for
     * @return The first matching root component, or null if not found
     */
    public SiteComponent findRootComponentByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return rootComponents.stream()
                .filter(component -> component.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    // Private utility methods

    /**
     * Updates modification tracking information.
     */
    private void updateModificationInfo(String modifiedBy) {
        this.lastModifiedBy = modifiedBy;
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Sanitizes input strings to prevent XSS attacks.
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        return input.replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<.*?>", "")
                .trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Site site = (Site) obj;
        return Objects.equals(id, site.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Site{id='%s', name='%s', active=%s, contentCount=%d, userCount=%d}",
                id, name, active, getTotalContentCount(), userRegistry.size());
    }
}
