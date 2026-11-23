package com.seby.jobleaf.domain.model.profile;

import java.time.Instant;
import java.util.UUID;

/**
 * A job seeker's profile: the details plus two optional files. It is filled in two steps (details first, files
 * later), so {@code profilePicture} and {@code resume} can be null.
 */
public record Profile(
        UUID id,
        UUID userId,
        ProfileDetails details,
        ProfileFile profilePicture,
        ProfileFile resume,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    /**
     * A new, not yet stored profile with only the details. Timestamps are set by JPA auditing.
     */
    public static Profile create(UUID id, UUID userId, ProfileDetails details) {
        return new Profile(id, userId, details, null, null, null, userId, null, userId);
    }

    public Profile withDetails(ProfileDetails newDetails) {
        return new Profile(id, userId, newDetails, profilePicture, resume, createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * @param newPicture the uploaded picture, or null to keep the stored one
     */
    public Profile withProfilePicture(ProfileFile newPicture) {
        return newPicture == null ? this
                : new Profile(id, userId, details, newPicture, resume, createdAt, createdBy, updatedAt, updatedBy);
    }

    /**
     * @param newResume the uploaded resume, or null to keep the stored one
     */
    public Profile withResume(ProfileFile newResume) {
        return newResume == null ? this
                : new Profile(id, userId, details, profilePicture, newResume, createdAt, createdBy, updatedAt, updatedBy);
    }
}
