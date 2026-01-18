package com.cms.core.model;

/**
 * Enumeration representing user roles in the JavaCMS system for authorization
 * control.
 *
 * <p>
 * This enum defines the hierarchical role structure used for role-based access
 * control (RBAC)
 * throughout the content management system. Each role has specific permissions
 * and capabilities,
 * with some roles inheriting permissions from others.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Provides the foundation for
 * authorization decisions
 * throughout the CMS, ensuring users can only access and modify content
 * according to their
 * assigned roles. Supports the principle of least privilege by defining minimal
 * necessary
 * permissions for each role.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Used in HashSet&lt;Role&gt;
 * collections within User
 * objects, providing proper enum usage in Collections Framework.
 * </p>
 *
 * @see com.cms.core.model.User
 * @see com.cms.security.AuthorizationService
 * @since 1.0
 * @author Otman Hmich S007924
 */
public enum Role {

    /**
     * Guest role - read-only access to published content.
     *
     * <p>
     * Guests can view published content but cannot create, edit, or manage
     * any content or system settings. This is the default role for anonymous users.
     * </p>
     */
    GUEST("Guest", "Read-only access to published content", 1),

    /**
     * Author role - can create and edit own content.
     *
     * <p>
     * Authors can create new content, edit their own content, and submit
     * content for review. They cannot publish content directly or edit others'
     * content.
     * </p>
     */
    AUTHOR("Author", "Can create and edit own content", 2),

    /**
     * Editor role - can edit any content and review submissions.
     *
     * <p>
     * Editors can edit any content in the system, review submissions from authors,
     * and publish approved content. They have broader content management
     * permissions.
     * </p>
     */
    EDITOR("Editor", "Can edit any content and review submissions", 3),

    /**
     * Publisher role - can publish and unpublish content.
     *
     * <p>
     * Publishers have the authority to make content live or take it down.
     * They can manage the publishing workflow and control what content is visible
     * to the public.
     * </p>
     */
    PUBLISHER("Publisher", "Can publish and unpublish content", 4),

    /**
     * Administrator role - full system access and user management.
     *
     * <p>
     * Administrators have complete system access including user management,
     * system configuration, and all content operations. This is the highest
     * privilege role.
     * </p>
     */
    ADMINISTRATOR("Administrator", "Full system access and user management", 5);

    /** Human-readable display name for this role */
    private final String displayName;

    /** Description of the role's permissions and responsibilities */
    private final String description;

    /** Numeric hierarchy level - higher numbers indicate more privileges */
    private final int hierarchyLevel;

    /**
     * Private constructor for enum constants.
     *
     * @param displayName    Human-readable name for display purposes
     * @param description    Detailed explanation of the role's permissions
     * @param hierarchyLevel Numeric level indicating privilege hierarchy
     */
    Role(String displayName, String description, int hierarchyLevel) {
        this.displayName = displayName;
        this.description = description;
        this.hierarchyLevel = hierarchyLevel;
    }

    /**
     * Returns the human-readable display name for this role.
     *
     * @return The display name, suitable for user interfaces
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a detailed description of this role's permissions.
     *
     * @return The role description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the hierarchy level of this role.
     *
     * <p>
     * Higher numbers indicate more privileges. This can be used for
     * authorization checks where any role at or above a certain level
     * should be granted access.
     * </p>
     *
     * @return The hierarchy level (1-5)
     */
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    /**
     * Checks if this role has at least the same privileges as the specified role.
     *
     * <p>
     * Used for hierarchical authorization checks. For example, an ADMINISTRATOR
     * role has at least the same privileges as an EDITOR role.
     * </p>
     *
     * @param requiredRole The minimum required role level
     * @return true if this role meets or exceeds the required privilege level
     */
    public boolean hasAtLeastPrivilegeOf(Role requiredRole) {
        if (requiredRole == null) {
            return true;
        }
        return this.hierarchyLevel >= requiredRole.hierarchyLevel;
    }

    /**
     * Checks if this role can create content.
     *
     * @return true if this role has content creation privileges
     */
    public boolean canCreateContent() {
        return this.hierarchyLevel >= AUTHOR.hierarchyLevel;
    }

    /**
     * Checks if this role can edit any content (not just own content).
     *
     * @return true if this role can edit any content in the system
     */
    public boolean canEditAnyContent() {
        return this.hierarchyLevel >= EDITOR.hierarchyLevel;
    }

    /**
     * Checks if this role can publish content.
     *
     * @return true if this role has publishing privileges
     */
    public boolean canPublishContent() {
        return this.hierarchyLevel >= PUBLISHER.hierarchyLevel;
    }

    /**
     * Checks if this role has administrative privileges.
     *
     * @return true if this role has system administration privileges
     */
    public boolean canAdministerSystem() {
        return this == ADMINISTRATOR;
    }

    /**
     * Checks if this role can manage users.
     *
     * @return true if this role can create, modify, or deactivate users
     */
    public boolean canManageUsers() {
        return this == ADMINISTRATOR;
    }

    /**
     * Returns all roles that have at least the privilege level of the specified
     * role.
     *
     * <p>
     * Useful for authorization queries where you need to find all roles
     * that should be granted access to a particular resource.
     * </p>
     *
     * @param minimumRole The minimum required role
     * @return Array of roles that meet or exceed the minimum privilege level
     */
    public static Role[] getRolesWithAtLeastPrivilegeOf(Role minimumRole) {
        if (minimumRole == null) {
            return values();
        }

        return java.util.Arrays.stream(values())
                .filter(role -> role.hasAtLeastPrivilegeOf(minimumRole))
                .toArray(Role[]::new);
    }

    /**
     * Finds a role by its display name.
     *
     * <p>
     * Case-insensitive search for finding roles by their display names.
     * Useful for parsing role information from external sources.
     * </p>
     *
     * @param displayName The display name to search for
     * @return The matching role, or null if not found
     */
    public static Role findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }

        for (Role role : values()) {
            if (role.displayName.equalsIgnoreCase(displayName.trim())) {
                return role;
            }
        }
        return null;
    }
}
