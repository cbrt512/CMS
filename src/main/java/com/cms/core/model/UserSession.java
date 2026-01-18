package com.cms.core.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an active user session in the JavaCMS system.
 *
 * <p>
 * This class manages user session information including session tracking,
 * timeout
 * management, and security monitoring. It supports the authentication and
 * authorization
 * system by maintaining session state and providing session-based security
 * controls.
 * </p>
 *
 * <p>
 * <strong>Security Implementation:</strong> Implements secure session
 * management with
 * automatic timeout, session ID generation, and activity tracking to prevent
 * session
 * hijacking and ensure proper session lifecycle management.
 * </p>
 *
 * <p>
 * <strong>Collections Framework:</strong> Used in ConcurrentHashMap&lt;String,
 * UserSession&gt;
 * collections within Site objects for thread-safe session management.
 * </p>
 *
 * @see com.cms.core.model.User
 * @see com.cms.core.model.Site
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class UserSession {

    /** Unique session identifier */
    private final String sessionId;

    /** The user associated with this session */
    private final User user;

    /** Timestamp when the session was created */
    private final LocalDateTime createdTime;

    /** Timestamp of the last activity in this session */
    private LocalDateTime lastActivityTime;

    /** The IP address from which the session was initiated */
    private final String ipAddress;

    /** The user agent string from the client */
    private final String userAgent;

    /** Whether the session is currently active */
    private boolean active;

    /** Session timeout duration in minutes */
    private int timeoutMinutes;

    /** Default session timeout in minutes (30 minutes) */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    /**
     * Constructs a new UserSession for the specified user.
     *
     * <p>
     * <strong>Security Implementation:</strong> Generates a cryptographically
     * secure
     * session ID and initializes session tracking with the current timestamp and
     * client information.
     * </p>
     *
     * @param user      The user for whom this session is created, must not be null
     * @param ipAddress The IP address of the client, may be null
     * @param userAgent The user agent string from the client, may be null
     * @throws IllegalArgumentException if user is null
     */
    public UserSession(User user, String ipAddress, String userAgent) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        this.sessionId = generateSecureSessionId();
        this.user = user;
        this.createdTime = LocalDateTime.now();
        this.lastActivityTime = this.createdTime;
        this.ipAddress = ipAddress != null ? ipAddress : "unknown";
        this.userAgent = userAgent != null ? userAgent : "unknown";
        this.active = true;
        this.timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
    }

    /**
     * Updates the last activity time to the current time.
     *
     * <p>
     * This method should be called on every user action to maintain accurate
     * session timeout tracking and prevent premature session expiration.
     * </p>
     */
    public void updateActivity() {
        if (active) {
            this.lastActivityTime = LocalDateTime.now();
        }
    }

    /**
     * Checks if the session has expired based on the configured timeout.
     *
     * <p>
     * <strong>Security Implementation:</strong> Automatically determines session
     * expiration based on inactivity timeout to prevent abandoned sessions from
     * remaining active indefinitely.
     * </p>
     *
     * @return true if the session has expired, false otherwise
     */
    public boolean isExpired() {
        if (!active) {
            return true;
        }

        LocalDateTime expirationTime = lastActivityTime.plusMinutes(timeoutMinutes);
        return LocalDateTime.now().isAfter(expirationTime);
    }

    /**
     * Checks if the session is currently valid (active and not expired).
     *
     * @return true if the session is valid, false otherwise
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Invalidates the session, making it inactive.
     *
     * <p>
     * <strong>Security Implementation:</strong> Provides explicit session
     * termination
     * for logout operations and security-related session invalidation.
     * </p>
     */
    public void invalidate() {
        this.active = false;
    }

    /**
     * Sets a custom timeout for this session.
     *
     * @param timeoutMinutes The timeout duration in minutes, must be positive
     * @throws IllegalArgumentException if timeoutMinutes is not positive
     */
    public void setTimeout(int timeoutMinutes) {
        if (timeoutMinutes <= 0) {
            throw new IllegalArgumentException("Timeout minutes must be positive");
        }
        this.timeoutMinutes = timeoutMinutes;
    }

    /**
     * Gets the remaining time before this session expires.
     *
     * @return The remaining minutes before expiration, or 0 if expired/inactive
     */
    public long getRemainingMinutes() {
        if (!active) {
            return 0;
        }

        LocalDateTime expirationTime = lastActivityTime.plusMinutes(timeoutMinutes);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(expirationTime)) {
            return 0;
        }

        // Calculate remaining minutes (simplified calculation)
        return java.time.Duration.between(now, expirationTime).toMinutes();
    }

    /**
     * Gets the total session duration in minutes.
     *
     * @return The total duration of this session in minutes
     */
    public long getSessionDurationMinutes() {
        LocalDateTime endTime = active ? LocalDateTime.now() : lastActivityTime;
        return java.time.Duration.between(createdTime, endTime).toMinutes();
    }

    // Getter methods

    /**
     * Returns the unique session identifier.
     *
     * @return The session ID, never null
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the user associated with this session.
     *
     * @return The user, never null
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the session creation timestamp.
     *
     * @return The creation time, never null
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    /**
     * Alias for getCreatedTime() for compatibility.
     *
     * @return The session creation time, never null
     */
    public LocalDateTime getCreationTime() {
        return createdTime;
    }

    /**
     * Returns the last activity timestamp.
     *
     * @return The last activity time, never null
     */
    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Returns the client IP address.
     *
     * @return The IP address string, never null
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the client user agent string.
     *
     * @return The user agent string, never null
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns whether the session is currently active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the session timeout duration in minutes.
     *
     * @return The timeout duration in minutes
     */
    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    // Private utility methods

    /**
     * Generates a cryptographically secure session ID.
     *
     * <p>
     * <strong>Security Implementation:</strong> Uses UUID.randomUUID() which
     * provides
     * cryptographically strong random session identifiers to prevent session
     * prediction
     * or collision attacks.
     * </p>
     *
     * @return A secure session ID string
     */
    private String generateSecureSessionId() {
        return "session-" + UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        UserSession session = (UserSession) obj;
        return Objects.equals(sessionId, session.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return String.format("UserSession{sessionId='%s', user='%s', active=%s, duration=%d min}",
                sessionId, user.getUsername(), active, getSessionDurationMinutes());
    }
}
