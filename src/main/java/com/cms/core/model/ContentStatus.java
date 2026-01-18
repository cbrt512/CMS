package com.cms.core.model;

/**
 * Enumeration representing the various states of content in the JavaCMS
 * lifecycle.
 *
 * <p>
 * This enum defines the standard content workflow states that content can
 * transition through
 * from creation to publication and eventual archival. The status progression
 * follows a typical
 * content management workflow: DRAFT → REVIEW → PUBLISHED → ARCHIVED.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Supports the Factory Pattern by providing
 * type-safe status
 * constants that the ContentFactory and concrete content implementations use to
 * maintain
 * consistent state management across all content types.
 * </p>
 *
 * <p>
 * <strong>Purpose:</strong> Provides proper enum usage as part of core Java
 * language features and provides type safety for content state management.
 * </p>
 *
 * @see com.cms.core.model.Content
 * @see com.cms.patterns.factory.ContentFactory
 * @since 1.0
 * @author Otman Hmich S007924
 */
public enum ContentStatus {

    /**
     * Content is in draft state - being created or edited but not ready for review.
     *
     * <p>
     * This is the initial state for all new content. Content in DRAFT state is
     * only visible to the author and content administrators.
     * </p>
     */
    DRAFT("Draft", "Content is being created or edited"),

    /**
     * Content is pending review - submitted for approval but not yet published.
     *
     * <p>
     * Content in REVIEW state is submitted by authors for editorial review
     * and approval. It's visible to reviewers and content administrators.
     * </p>
     */
    REVIEW("Under Review", "Content is pending editorial review"),

    /**
     * Content is published and publicly available.
     *
     * <p>
     * Content in PUBLISHED state is live and visible to all users. This is
     * the target state for most content in the system.
     * </p>
     */
    PUBLISHED("Published", "Content is live and publicly available"),

    /**
     * Content has been archived - no longer actively used but preserved.
     *
     * <p>
     * Content in ARCHIVED state is no longer publicly visible but is
     * preserved in the system for historical purposes or potential restoration.
     * </p>
     */
    ARCHIVED("Archived", "Content is archived and no longer active"),

    /**
     * Content has been rejected during review process.
     *
     * <p>
     * Content in REJECTED state was submitted for review but did not meet
     * publication standards. Authors can revise and resubmit such content.
     * </p>
     */
    REJECTED("Rejected", "Content was rejected during review");

    /** Human-readable display name for this status */
    private final String displayName;

    /** Detailed description of what this status means */
    private final String description;

    /**
     * Private constructor for enum constants.
     *
     * @param displayName Human-readable name for display purposes
     * @param description Detailed explanation of the status meaning
     */
    ContentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name for this status.
     *
     * @return The display name, suitable for user interfaces
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a detailed description of what this status represents.
     *
     * @return The status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if content with this status is publicly visible.
     *
     * <p>
     * Only PUBLISHED content is considered publicly visible. All other
     * states represent various stages of content development or archival.
     * </p>
     *
     * @return true if content with this status should be publicly visible
     */
    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }

    /**
     * Checks if content with this status can be edited.
     *
     * <p>
     * Content in DRAFT, REVIEW, and REJECTED states can typically be edited.
     * PUBLISHED content may have restrictions, and ARCHIVED content is usually
     * read-only.
     * </p>
     *
     * @return true if content with this status can be modified
     */
    public boolean isEditable() {
        return this == DRAFT || this == REVIEW || this == REJECTED;
    }

    /**
     * Returns valid next states for content workflow transitions.
     *
     * <p>
     * Defines the allowed state transitions in the content workflow:
     * <ul>
     * <li>DRAFT → REVIEW, PUBLISHED, ARCHIVED</li>
     * <li>REVIEW → PUBLISHED, REJECTED, DRAFT</li>
     * <li>PUBLISHED → ARCHIVED, DRAFT</li>
     * <li>REJECTED → DRAFT, REVIEW</li>
     * <li>ARCHIVED → DRAFT</li>
     * </ul>
     * </p>
     *
     * @return Array of valid next states for transition validation
     */
    public ContentStatus[] getValidNextStates() {
        switch (this) {
            case DRAFT:
                return new ContentStatus[] { REVIEW, PUBLISHED, ARCHIVED };
            case REVIEW:
                return new ContentStatus[] { PUBLISHED, REJECTED, DRAFT };
            case PUBLISHED:
                return new ContentStatus[] { ARCHIVED, DRAFT };
            case REJECTED:
                return new ContentStatus[] { DRAFT, REVIEW };
            case ARCHIVED:
                return new ContentStatus[] { DRAFT };
            default:
                return new ContentStatus[0];
        }
    }

    /**
     * Validates if a status transition is allowed.
     *
     * <p>
     * Checks if the current status can transition to the specified target status
     * according to the defined content workflow rules.
     * </p>
     *
     * @param targetStatus The desired target status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(ContentStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        ContentStatus[] validStates = getValidNextStates();
        for (ContentStatus validState : validStates) {
            if (validState == targetStatus) {
                return true;
            }
        }
        return false;
    }
}
