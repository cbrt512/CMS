package com.cms.patterns.iterator;

import com.cms.core.model.UserSession;
import com.cms.core.model.Role;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Iterator implementation for managing and traversing user sessions in JavaCMS.
 *
 * <p>
 * This class provides controlled sequential access to active user sessions with
 * support for filtering by user roles, activity status, session duration, and
 * custom criteria. It implements the Iterator Pattern to enable systematic
 * session management while maintaining security and performance considerations.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Iterator Pattern Implementation
 * <br>
 * This class serves as a concrete iterator in the Iterator design pattern,
 * specifically designed for processing UserSession objects in administrative
 * and monitoring scenarios. It shows advanced iterator functionality with
 * security-aware filtering, time-based selection, and real-time session
 * tracking.
 * </p>
 *
 * <p>
 * <strong>Implementation:</strong> Implements the Iterator Pattern
 * by providing controlled sequential access to user session collections,
 * integrating with existing User and UserSession model classes, and supporting
 * sophisticated session management for administrative and security operations.
 * </p>
 *
 * <p>
 * <strong>Session Management Features:</strong>
 * <ul>
 * <li><strong>Activity-Based Filtering:</strong> Filter by active, idle, or
 * expired sessions</li>
 * <li><strong>Role-Based Access:</strong> Filter sessions by user roles and
 * permissions</li>
 * <li><strong>Time-Based Queries:</strong> Filter by session duration, last
 * activity, creation time</li>
 * <li><strong>Security Monitoring:</strong> Track suspicious activities and
 * security events</li>
 * <li><strong>Performance Optimization:</strong> Efficient iteration over large
 * session collections</li>
 * <li><strong>Thread Safety:</strong> Safe for concurrent session management
 * operations</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage Scenarios:</strong>
 * <ul>
 * <li>Administrative session monitoring and management</li>
 * <li>Security auditing and anomaly detection</li>
 * <li>Performance monitoring and resource optimization</li>
 * <li>User activity analysis and reporting</li>
 * <li>Automated session cleanup and maintenance</li>
 * </ul>
 * </p>
 *
 * @see com.cms.core.model.UserSession
 * @see com.cms.core.model.User
 * @see com.cms.core.model.Role
 * @since 1.0
 * @author Otman Hmich S007924
 */
public class UserSessionIterator implements Iterator<UserSession> {

    /** Enumeration of session activity states */
    public enum ActivityState {
        /** Session is currently active */
        ACTIVE,
        /** Session is idle but not expired */
        IDLE,
        /** Session has expired */
        EXPIRED,
        /** Session is in any valid state (active or idle) */
        VALID,
        /** All sessions regardless of state */
        ALL
    }

    /** Enumeration of session selection modes */
    public enum SelectionMode {
        /** Select all sessions */
        ALL_SESSIONS,
        /** Select only active sessions */
        ACTIVE_ONLY,
        /** Select sessions by role */
        BY_ROLE,
        /** Select sessions by activity state */
        BY_ACTIVITY,
        /** Select sessions by duration */
        BY_DURATION,
        /** Select with custom filtering */
        FILTERED
    }

    /** The source collection of sessions */
    private final List<UserSession> sourceSessions;

    /** Current selection mode */
    private final SelectionMode mode;

    /** Optional filter predicate */
    private final Predicate<UserSession> filter;

    /** Pre-filtered session list for efficient iteration */
    private final List<UserSession> filteredSessions;

    /** Internal iterator for the filtered sessions */
    private final Iterator<UserSession> internalIterator;

    /** Session activity tracking map */
    private final Map<String, LocalDateTime> lastActivityMap;

    /** Session timeout duration (default 30 minutes) */
    private Duration sessionTimeout;

    /** Maximum idle time before considering session idle (default 5 minutes) */
    private Duration idleThreshold;

    /**
     * Creates a user session iterator for all sessions.
     *
     * <p>
     * This constructor creates an iterator that will traverse all user
     * sessions in the provided collection without any filtering.
     * </p>
     *
     * @param sessions Collection of user sessions to iterate over
     * @throws IllegalArgumentException if sessions collection is null
     * @since 1.0
     */
    public UserSessionIterator(Collection<UserSession> sessions) {
        this(sessions, SelectionMode.ALL_SESSIONS, null);
    }

    /**
     * Creates a user session iterator with specified selection mode.
     *
     * <p>
     * This constructor allows selection of filtering mode to control
     * which sessions are included in the iteration sequence.
     * </p>
     *
     * @param sessions Collection of user sessions to iterate over
     * @param mode     Selection mode for session filtering
     * @throws IllegalArgumentException if sessions or mode is null
     * @since 1.0
     */
    public UserSessionIterator(Collection<UserSession> sessions, SelectionMode mode) {
        this(sessions, mode, null);
    }

    /**
     * Creates a user session iterator with custom filtering.
     *
     * <p>
     * This constructor provides full control over session selection
     * through custom filtering predicates and selection modes.
     * </p>
     *
     * @param sessions Collection of user sessions to iterate over
     * @param mode     Selection mode for session filtering
     * @param filter   Optional filter predicate (null for no additional filtering)
     * @throws IllegalArgumentException if sessions or mode is null
     * @since 1.0
     */
    public UserSessionIterator(Collection<UserSession> sessions, SelectionMode mode,
            Predicate<UserSession> filter) {
        if (sessions == null) {
            throw new IllegalArgumentException("Sessions collection cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Selection mode cannot be null");
        }

        // Create defensive copy for thread safety
        this.sourceSessions = new ArrayList<>(sessions);
        this.mode = mode;
        this.filter = filter;
        this.lastActivityMap = new ConcurrentHashMap<>();

        // Default timeout settings
        this.sessionTimeout = Duration.ofMinutes(30);
        this.idleThreshold = Duration.ofMinutes(5);

        // Initialize activity tracking
        updateActivityTracking();

        // Pre-filter sessions for efficient iteration
        this.filteredSessions = filterSessionsByMode();
        this.internalIterator = filteredSessions.iterator();
    }

    /**
     * Factory method for iterating all sessions.
     *
     * @param sessions Collection of sessions to iterate
     * @return Iterator for all sessions
     * @since 1.0
     */
    public static UserSessionIterator allSessions(Collection<UserSession> sessions) {
        return new UserSessionIterator(sessions, SelectionMode.ALL_SESSIONS);
    }

    /**
     * Factory method for iterating only active sessions.
     *
     * <p>
     * Active sessions are those that have recent activity and haven't expired.
     * </p>
     *
     * @param sessions Collection of sessions to filter
     * @return Iterator for active sessions only
     * @since 1.0
     */
    public static UserSessionIterator activeSessions(Collection<UserSession> sessions) {
        return new UserSessionIterator(sessions, SelectionMode.ACTIVE_ONLY);
    }

    /**
     * Factory method for iterating sessions by user role.
     *
     * @param sessions Collection of sessions to filter
     * @param role     User role to filter by
     * @return Iterator for sessions with specified role
     * @since 1.0
     */
    public static UserSessionIterator byRole(Collection<UserSession> sessions, Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        Predicate<UserSession> roleFilter = session -> session.getUser() != null &&
                session.getUser().getRole() == role;

        return new UserSessionIterator(sessions, SelectionMode.FILTERED, roleFilter);
    }

    /**
     * Factory method for iterating sessions by activity state.
     *
     * @param sessions      Collection of sessions to filter
     * @param activityState Activity state to filter by
     * @return Iterator for sessions with specified activity state
     * @since 1.0
     */
    public static UserSessionIterator byActivityState(Collection<UserSession> sessions,
            ActivityState activityState) {
        if (activityState == null) {
            throw new IllegalArgumentException("Activity state cannot be null");
        }

        UserSessionIterator iterator = new UserSessionIterator(sessions, SelectionMode.BY_ACTIVITY);
        return iterator.filter(session -> iterator.getSessionActivityState(session) == activityState);
    }

    /**
     * Factory method for iterating sessions by minimum duration.
     *
     * @param sessions    Collection of sessions to filter
     * @param minDuration Minimum session duration
     * @return Iterator for sessions exceeding minimum duration
     * @since 1.0
     */
    public static UserSessionIterator byMinDuration(Collection<UserSession> sessions, Duration minDuration) {
        if (minDuration == null || minDuration.isNegative()) {
            throw new IllegalArgumentException("Minimum duration must be non-negative");
        }

        Predicate<UserSession> durationFilter = session -> {
            Duration sessionDuration = Duration.between(session.getCreationTime(), LocalDateTime.now());
            return sessionDuration.compareTo(minDuration) >= 0;
        };

        return new UserSessionIterator(sessions, SelectionMode.FILTERED, durationFilter);
    }

    /**
     * Factory method for iterating expired sessions.
     *
     * @param sessions Collection of sessions to filter
     * @return Iterator for expired sessions only
     * @since 1.0
     */
    public static UserSessionIterator expiredSessions(Collection<UserSession> sessions) {
        return byActivityState(sessions, ActivityState.EXPIRED);
    }

    /**
     * Factory method for iterating idle sessions.
     *
     * @param sessions Collection of sessions to filter
     * @return Iterator for idle sessions only
     * @since 1.0
     */
    public static UserSessionIterator idleSessions(Collection<UserSession> sessions) {
        return byActivityState(sessions, ActivityState.IDLE);
    }

    /**
     * Factory method for iterating sessions by username.
     *
     * @param sessions Collection of sessions to filter
     * @param username Username to filter by
     * @return Iterator for sessions belonging to specified user
     * @since 1.0
     */
    public static UserSessionIterator byUsername(Collection<UserSession> sessions, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String targetUsername = username.trim();
        Predicate<UserSession> usernameFilter = session -> session.getUser() != null &&
                targetUsername.equals(session.getUser().getUsername());

        return new UserSessionIterator(sessions, SelectionMode.FILTERED, usernameFilter);
    }

    /**
     * Updates the activity tracking for all sessions.
     *
     * <p>
     * This method refreshes the internal activity tracking to ensure
     * accurate determination of session states.
     * </p>
     *
     * @since 1.0
     */
    private void updateActivityTracking() {
        LocalDateTime now = LocalDateTime.now();
        for (UserSession session : sourceSessions) {
            // Update activity tracking based on session's last activity
            lastActivityMap.put(session.getSessionId(), session.getLastActivityTime());
        }
    }

    /**
     * Filters sessions based on the selection mode.
     *
     * @return Filtered list of sessions
     * @since 1.0
     */
    private List<UserSession> filterSessionsByMode() {
        List<UserSession> filtered = new ArrayList<>();

        for (UserSession session : sourceSessions) {
            if (shouldIncludeSession(session)) {
                filtered.add(session);
            }
        }

        // Sort by creation time for consistent iteration order
        filtered.sort(Comparator.comparing(UserSession::getCreationTime));

        return filtered;
    }

    /**
     * Determines if a session should be included based on selection criteria.
     *
     * @param session Session to evaluate
     * @return true if session should be included
     * @since 1.0
     */
    private boolean shouldIncludeSession(UserSession session) {
        // Apply selection mode filtering
        boolean modeMatch = switch (mode) {
            case ALL_SESSIONS -> true;
            case ACTIVE_ONLY -> getSessionActivityState(session) == ActivityState.ACTIVE;
            case BY_ROLE, BY_ACTIVITY, BY_DURATION, FILTERED -> true; // Custom filter applied separately
        };

        if (!modeMatch) {
            return false;
        }

        // Apply custom filter if present
        return filter == null || filter.test(session);
    }

    /**
     * Determines the current activity state of a session.
     *
     * @param session Session to evaluate
     * @return Current activity state
     * @since 1.0
     */
    private ActivityState getSessionActivityState(UserSession session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastActivity = session.getLastActivityTime();
        LocalDateTime creationTime = session.getCreationTime();

        // Check if session has expired
        Duration sessionAge = Duration.between(creationTime, now);
        if (sessionAge.compareTo(sessionTimeout) > 0) {
            return ActivityState.EXPIRED;
        }

        // Check if session is idle
        Duration idleTime = Duration.between(lastActivity, now);
        if (idleTime.compareTo(idleThreshold) > 0) {
            return ActivityState.IDLE;
        }

        return ActivityState.ACTIVE;
    }

    /**
     * Returns true if there are more sessions to iterate.
     *
     * @return true if more sessions are available
     * @since 1.0
     */
    @Override
    public boolean hasNext() {
        return internalIterator.hasNext();
    }

    /**
     * Returns the next session in the iteration.
     *
     * @return Next UserSession in the sequence
     * @throws NoSuchElementException if no more sessions are available
     * @since 1.0
     */
    @Override
    public UserSession next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more user sessions available");
        }
        return internalIterator.next();
    }

    /**
     * Remove operation is not supported for session integrity.
     *
     * @throws UnsupportedOperationException always
     * @since 1.0
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove operation not supported. Use session management methods for session termination.");
    }

    /**
     * Returns the total number of sessions that will be iterated.
     *
     * @return Total session count in filtered iteration
     * @since 1.0
     */
    public int size() {
        return filteredSessions.size();
    }

    /**
     * Returns true if no sessions match the filtering criteria.
     *
     * @return true if iteration is empty
     * @since 1.0
     */
    public boolean isEmpty() {
        return filteredSessions.isEmpty();
    }

    /**
     * Returns the current selection mode.
     *
     * @return The selection mode being used
     * @since 1.0
     */
    public SelectionMode getSelectionMode() {
        return mode;
    }

    /**
     * Returns the filter predicate being used (if any).
     *
     * @return The filter predicate or null
     * @since 1.0
     */
    public Predicate<UserSession> getFilter() {
        return filter;
    }

    /**
     * Returns the session timeout duration.
     *
     * @return Session timeout duration
     * @since 1.0
     */
    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Sets the session timeout duration.
     *
     * @param timeout New timeout duration
     * @throws IllegalArgumentException if timeout is null or negative
     * @since 1.0
     */
    public void setSessionTimeout(Duration timeout) {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("Session timeout must be non-negative");
        }
        this.sessionTimeout = timeout;
    }

    /**
     * Returns the idle threshold duration.
     *
     * @return Idle threshold duration
     * @since 1.0
     */
    public Duration getIdleThreshold() {
        return idleThreshold;
    }

    /**
     * Sets the idle threshold duration.
     *
     * @param threshold New idle threshold duration
     * @throws IllegalArgumentException if threshold is null or negative
     * @since 1.0
     */
    public void setIdleThreshold(Duration threshold) {
        if (threshold == null || threshold.isNegative()) {
            throw new IllegalArgumentException("Idle threshold must be non-negative");
        }
        this.idleThreshold = threshold;
    }

    /**
     * Returns statistics about the session collection.
     *
     * @return Map containing session statistics
     * @since 1.0
     */
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalSessions = sourceSessions.size();
        int activeSessions = 0;
        int idleSessions = 0;
        int expiredSessions = 0;
        Map<Role, Integer> roleDistribution = new HashMap<>();

        for (UserSession session : sourceSessions) {
            ActivityState state = getSessionActivityState(session);
            switch (state) {
                case ACTIVE -> activeSessions++;
                case IDLE -> idleSessions++;
                case EXPIRED -> expiredSessions++;
            }

            if (session.getUser() != null) {
                Role role = session.getUser().getRole();
                roleDistribution.merge(role, 1, Integer::sum);
            }
        }

        stats.put("totalSessions", totalSessions);
        stats.put("activeSessions", activeSessions);
        stats.put("idleSessions", idleSessions);
        stats.put("expiredSessions", expiredSessions);
        stats.put("filteredSessions", filteredSessions.size());
        stats.put("roleDistribution", roleDistribution);

        return stats;
    }

    /**
     * Returns a list of all sessions in the current iteration.
     *
     * <p>
     * This method returns a copy of the filtered session list
     * without consuming the iterator state.
     * </p>
     *
     * @return List of sessions in iteration order
     * @since 1.0
     */
    public List<UserSession> toList() {
        return new ArrayList<>(filteredSessions);
    }

    /**
     * Creates a new iterator with additional filtering applied.
     *
     * @param additionalFilter Additional filter predicate
     * @return New iterator with combined filters
     * @since 1.0
     */
    public UserSessionIterator filter(Predicate<UserSession> additionalFilter) {
        if (additionalFilter == null) {
            return new UserSessionIterator(sourceSessions, mode, filter);
        }

        Predicate<UserSession> combinedFilter = filter == null ? additionalFilter : filter.and(additionalFilter);

        return new UserSessionIterator(sourceSessions, mode, combinedFilter);
    }

    /**
     * Returns sessions grouped by their activity state.
     *
     * @return Map of activity states to session lists
     * @since 1.0
     */
    public Map<ActivityState, List<UserSession>> groupByActivityState() {
        Map<ActivityState, List<UserSession>> grouped = new HashMap<>();

        for (ActivityState state : ActivityState.values()) {
            grouped.put(state, new ArrayList<>());
        }

        for (UserSession session : filteredSessions) {
            ActivityState state = getSessionActivityState(session);
            grouped.get(state).add(session);
            grouped.get(ActivityState.ALL).add(session);

            if (state == ActivityState.ACTIVE || state == ActivityState.IDLE) {
                grouped.get(ActivityState.VALID).add(session);
            }
        }

        return grouped;
    }

    /**
     * Returns sessions grouped by user role.
     *
     * @return Map of roles to session lists
     * @since 1.0
     */
    public Map<Role, List<UserSession>> groupByRole() {
        Map<Role, List<UserSession>> grouped = new HashMap<>();

        for (UserSession session : filteredSessions) {
            if (session.getUser() != null) {
                Role role = session.getUser().getRole();
                grouped.computeIfAbsent(role, k -> new ArrayList<>()).add(session);
            }
        }

        return grouped;
    }

    /**
     * Returns a string representation of the session iterator state.
     *
     * @return String describing iterator configuration and statistics
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format("UserSessionIterator{mode=%s, totalSessions=%d, filteredSessions=%d, hasFilter=%s}",
                mode, sourceSessions.size(), filteredSessions.size(), filter != null);
    }
}
