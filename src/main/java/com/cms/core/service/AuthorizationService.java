package com.cms.core.service;

import com.cms.core.model.User;
import com.cms.core.model.Role;
import com.cms.core.model.Content;
import com.cms.core.model.ContentStatus;
import com.cms.patterns.shield.CMSException;
import com.cms.patterns.shield.ExceptionShielder;
import com.cms.security.SecurityValidator;
import com.cms.util.LoggerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authorization service for role-based access control in the CMS application.
 *
 * <p>
 * This service implements comprehensive authorization logic for all CMS
 * operations,
 * providing role-based access control, resource-specific permissions, and
 * security
 * audit trails. It integrates with the security framework to prevent
 * unauthorized
 * access and supports security implementation.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong>
 * - Role-based permission validation
 * - Resource-specific access control
 * - Operation-level authorization checks
 * - Security audit logging for compliance
 * - Integration with SecurityValidator for additional validation
 * </p>
 *
 * <p>
 * <strong>Technologies Implemented:</strong>
 * - Security Implementation: Controlled access and authorization
 * - Collections Framework: Permission sets and role mappings
 * - Exception Shielding: Security-conscious error handling
 * - Logging: Comprehensive security audit trails
 * </p>
 *
 * <p>
 * <strong>Authorization Model:</strong>
 * - ADMIN: Full system access including user management
 * - EDITOR: Content management and publication permissions
 * - AUTHOR: Content creation and editing of own content
 * - VIEWER: Read-only access to published content
 * </p>
 *
 * @see com.cms.core.model.User.Role
 * @see com.cms.security.SecurityValidator
 * @see com.cms.patterns.shield.CMSException
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class AuthorizationService {

    // Permission definitions for different operations
    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            "CREATE_CONTENT", "UPDATE_CONTENT", "DELETE_CONTENT", "PUBLISH_CONTENT",
            "CREATE_USER", "UPDATE_USER", "DELETE_USER", "VIEW_USER",
            "SYSTEM_CONFIG", "VIEW_AUDIT", "MANAGE_SITE", "MANAGE_TEMPLATES");

    private static final Set<String> EDITOR_PERMISSIONS = Set.of(
            "CREATE_CONTENT", "UPDATE_CONTENT", "DELETE_CONTENT", "PUBLISH_CONTENT",
            "VIEW_USER", "MANAGE_SITE", "MANAGE_TEMPLATES");

    private static final Set<String> PUBLISHER_PERMISSIONS = Set.of(
            "CREATE_CONTENT", "UPDATE_CONTENT", "DELETE_CONTENT", "PUBLISH_CONTENT",
            "VIEW_USER", "MANAGE_SITE");

    private static final Set<String> AUTHOR_PERMISSIONS = Set.of(
            "CREATE_CONTENT", "UPDATE_OWN_CONTENT", "DELETE_OWN_CONTENT", "VIEW_CONTENT");

    private static final Set<String> GUEST_PERMISSIONS = Set.of(
            "VIEW_CONTENT");

    // Cache for permission checks to improve performance
    private final Map<String, Boolean> permissionCache;

    // Audit trail for security monitoring
    private final List<AuthorizationAudit> auditTrail;

    /**
     * Constructs a new AuthorizationService.
     */
    public AuthorizationService() {
        this.permissionCache = new ConcurrentHashMap<>();
        this.auditTrail = Collections.synchronizedList(new ArrayList<>());

        LoggerUtil.logInfo("AuthorizationService", "AuthorizationService initialized");
    }

    /**
     * Validates that a user has permission to perform a specific operation.
     *
     * @param user       The user requesting permission
     * @param operation  The operation being requested
     * @param resourceId Optional resource identifier for resource-specific checks
     * @throws SecurityException        if user lacks required permission
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void validateUserPermission(User user, String operation, String resourceId)
            throws SecurityException {

        LoggerUtil.logDebug("AuthorizationService",
                String.format("Validating permission: user=%s, operation=%s, resource=%s",
                        user.getUsername(), operation, resourceId));

        try {
            // Input validation
            if (user == null) {
                LoggerUtil.logWarn("AuthorizationService", "Permission check with null user");
                throw new SecurityException("Authentication required");
            }

            if (operation == null || operation.trim().isEmpty()) {
                throw new IllegalArgumentException("Operation cannot be null or empty");
            }

            // Check cache first for performance
            String cacheKey = generateCacheKey(user.getId(), operation, resourceId);
            Boolean cachedResult = permissionCache.get(cacheKey);
            if (cachedResult != null) {
                if (!cachedResult) {
                    auditAuthorizationFailure(user, operation, resourceId, "Cached denial");
                    throw new SecurityException("Access denied");
                }
                LoggerUtil.logDebug("AuthorizationService", "Permission granted from cache");
                return;
            }

            // Validate user account status
            SecurityValidator.validateAuthorization(user, operation, resourceId);

            // Check role-based permissions
            boolean hasPermission = checkRolePermission(user.getRole(), operation);

            // Additional resource-specific checks
            if (hasPermission && resourceId != null) {
                hasPermission = checkResourceSpecificPermission(user, operation, resourceId);
            }

            // Cache the result
            permissionCache.put(cacheKey, hasPermission);

            if (!hasPermission) {
                auditAuthorizationFailure(user, operation, resourceId, "Role-based denial");
                throw new SecurityException("Insufficient permissions for operation: " + operation);
            }

            // Audit successful authorization
            auditAuthorizationSuccess(user, operation, resourceId);

            LoggerUtil.logDebug("AuthorizationService", "Permission validation successful");

        } catch (SecurityException e) {
            LoggerUtil.logWarn("AuthorizationService",
                    "Authorization failed for user: " + user.getUsername() + ", operation: " + operation);
            throw e;
        } catch (Exception e) {
            LoggerUtil.logError("AuthorizationService", "Authorization check failed", e);
            auditAuthorizationFailure(user, operation, resourceId, "System error: " + e.getMessage());
            throw new SecurityException("Authorization check failed");
        }
    }

    /**
     * Validates content-specific access permissions.
     *
     * @param user       The user requesting access
     * @param content    The content being accessed
     * @param accessType The type of access requested (READ, UPDATE, DELETE,
     *                   PUBLISH)
     * @throws SecurityException if access is denied
     */
    public void validateContentAccess(User user, Content content, String accessType)
            throws SecurityException {

        LoggerUtil.logDebug("AuthorizationService",
                String.format("Validating content access: user=%s, content=%s, access=%s",
                        user.getUsername(), content.getId(), accessType));

        try {
            // Basic permission check for the operation type
            String operation = mapAccessTypeToOperation(accessType, content);
            validateUserPermission(user, operation, content.getId());

            // Content-specific authorization rules
            switch (accessType.toUpperCase()) {
                case "READ":
                    validateReadAccess(user, content);
                    break;
                case "UPDATE":
                    validateUpdateAccess(user, content);
                    break;
                case "DELETE":
                    validateDeleteAccess(user, content);
                    break;
                case "PUBLISH":
                    validatePublishAccess(user, content);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown access type: " + accessType);
            }

            LoggerUtil.logDebug("AuthorizationService", "Content access validation successful");

        } catch (SecurityException e) {
            LoggerUtil.logWarn("AuthorizationService",
                    String.format("Content access denied: user=%s, content=%s, access=%s",
                            user.getUsername(), content.getId(), accessType));
            throw e;
        } catch (Exception e) {
            LoggerUtil.logError("AuthorizationService", "Content access validation failed", e);
            throw new SecurityException("Content access validation failed");
        }
    }

    /**
     * Checks if a user has a specific role.
     *
     * @param user The user to check
     * @param role The role to verify
     * @return true if user has the specified role or higher
     */
    public boolean hasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        Role userRole = user.getRole();
        if (userRole == null) {
            return false;
        }

        // Role hierarchy: ADMIN > PUBLISHER > EDITOR > AUTHOR > GUEST
        switch (role) {
            case GUEST:
                return true; // All authenticated users have at least guest access
            case AUTHOR:
                return userRole != Role.GUEST;
            case EDITOR:
                return userRole == Role.EDITOR || userRole == Role.PUBLISHER || userRole == Role.ADMINISTRATOR;
            case PUBLISHER:
                return userRole == Role.PUBLISHER || userRole == Role.ADMINISTRATOR;
            case ADMINISTRATOR:
                return userRole == Role.ADMINISTRATOR;
            default:
                return false;
        }
    }

    /**
     * Gets all permissions for a user's role.
     *
     * @param user The user to get permissions for
     * @return Set of permission strings for the user's role
     */
    public Set<String> getUserPermissions(User user) {
        if (user == null || user.getRole() == null) {
            return Collections.emptySet();
        }

        return getRolePermissions(user.getRole());
    }

    /**
     * Clears the permission cache (useful for testing or after role changes).
     */
    public void clearPermissionCache() {
        permissionCache.clear();
        LoggerUtil.logInfo("AuthorizationService", "Permission cache cleared");
    }

    /**
     * Gets the authorization audit trail for security monitoring.
     *
     * @return List of authorization audit entries
     */
    public List<AuthorizationAudit> getAuditTrail() {
        return new ArrayList<>(auditTrail);
    }

    /**
     * Checks role-based permissions for an operation.
     */
    private boolean checkRolePermission(Role role, String operation) {
        Set<String> rolePermissions = getRolePermissions(role);

        // Direct permission check
        if (rolePermissions.contains(operation)) {
            return true;
        }

        // Check for ownership-based permissions
        if (operation.contains("_OWN_") && rolePermissions.contains(operation)) {
            return true;
        }

        return false;
    }

    /**
     * Gets permissions for a specific role.
     */
    private Set<String> getRolePermissions(Role role) {
        switch (role) {
            case ADMINISTRATOR:
                return ADMIN_PERMISSIONS;
            case PUBLISHER:
                return PUBLISHER_PERMISSIONS;
            case EDITOR:
                return EDITOR_PERMISSIONS;
            case AUTHOR:
                return AUTHOR_PERMISSIONS;
            case GUEST:
                return GUEST_PERMISSIONS;
            default:
                return Collections.emptySet();
        }
    }

    /**
     * Checks resource-specific permissions.
     */
    private boolean checkResourceSpecificPermission(User user, String operation, String resourceId) {
        // This is where you would implement resource-specific logic
        // For example, checking if a user owns a specific content item

        if (operation.contains("_OWN_") && resourceId != null) {
            // For ownership-based operations, we would need to check
            // if the user is the owner of the resource
            // This is simplified for the current implementation
            return true;
        }

        return true; // Allow by default for this implementation
    }

    /**
     * Maps access type strings to operation strings.
     */
    private String mapAccessTypeToOperation(String accessType, Content content) {
        switch (accessType.toUpperCase()) {
            case "READ":
                return "VIEW_CONTENT";
            case "UPDATE":
                return "UPDATE_CONTENT";
            case "DELETE":
                return "DELETE_CONTENT";
            case "PUBLISH":
                return "PUBLISH_CONTENT";
            default:
                throw new IllegalArgumentException("Unknown access type: " + accessType);
        }
    }

    /**
     * Validates read access to content.
     */
    private void validateReadAccess(User user, Content content) throws SecurityException {
        // Check if content is published or if user has edit rights
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            // Unpublished content requires higher permissions or ownership
            if (!hasRole(user, Role.EDITOR) &&
                    !Objects.equals(content.getCreatedBy(), user.getId())) {
                throw new SecurityException("Cannot access unpublished content");
            }
        }
    }

    /**
     * Validates update access to content.
     */
    private void validateUpdateAccess(User user, Content content) throws SecurityException {
        // Editors and admins can update any content
        if (hasRole(user, Role.EDITOR)) {
            return;
        }

        // Authors can only update their own content
        if (hasRole(user, Role.AUTHOR) &&
                Objects.equals(content.getCreatedBy(), user.getId())) {
            return;
        }

        throw new SecurityException("Cannot update this content");
    }

    /**
     * Validates delete access to content.
     */
    private void validateDeleteAccess(User user, Content content) throws SecurityException {
        // Only editors and admins can delete content
        if (!hasRole(user, Role.EDITOR)) {
            throw new SecurityException("Insufficient permissions to delete content");
        }

        // Additional business rules for deletion can be added here
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            // May require admin rights to delete published content
            if (!hasRole(user, Role.ADMINISTRATOR)) {
                throw new SecurityException("Cannot delete published content without admin rights");
            }
        }
    }

    /**
     * Validates publish access to content.
     */
    private void validatePublishAccess(User user, Content content) throws SecurityException {
        // Only editors and admins can publish content
        if (!hasRole(user, Role.EDITOR)) {
            throw new SecurityException("Insufficient permissions to publish content");
        }
    }

    /**
     * Generates cache key for permission caching.
     */
    private String generateCacheKey(String userId, String operation, String resourceId) {
        return String.format("%s:%s:%s", userId, operation, resourceId != null ? resourceId : "");
    }

    /**
     * Records successful authorization in audit trail.
     */
    private void auditAuthorizationSuccess(User user, String operation, String resourceId) {
        AuthorizationAudit audit = new AuthorizationAudit(
                user.getId(),
                user.getUsername(),
                operation,
                resourceId,
                true,
                null,
                System.currentTimeMillis());

        auditTrail.add(audit);

        LoggerUtil.logInfo("AuthorizationService",
                String.format("Authorization granted: user=%s, operation=%s",
                        user.getUsername(), operation));
    }

    /**
     * Records failed authorization in audit trail.
     */
    private void auditAuthorizationFailure(User user, String operation, String resourceId, String reason) {
        AuthorizationAudit audit = new AuthorizationAudit(
                user != null ? user.getId() : "unknown",
                user != null ? user.getUsername() : "unknown",
                operation,
                resourceId,
                false,
                reason,
                System.currentTimeMillis());

        auditTrail.add(audit);

        LoggerUtil.logWarn("AuthorizationService",
                String.format("Authorization denied: user=%s, operation=%s, reason=%s",
                        user != null ? user.getUsername() : "unknown", operation, reason));
    }

    /**
     * Authorization audit record for security monitoring.
     */
    public static class AuthorizationAudit {
        private final String userId;
        private final String username;
        private final String operation;
        private final String resourceId;
        private final boolean granted;
        private final String reason;
        private final long timestamp;

        public AuthorizationAudit(String userId, String username, String operation,
                String resourceId, boolean granted, String reason, long timestamp) {
            this.userId = userId;
            this.username = username;
            this.operation = operation;
            this.resourceId = resourceId;
            this.granted = granted;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        // Getters
        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getOperation() {
            return operation;
        }

        public String getResourceId() {
            return resourceId;
        }

        public boolean isGranted() {
            return granted;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("AuthAudit[user=%s, op=%s, granted=%s, time=%d]",
                    username, operation, granted, timestamp);
        }
    }
}
