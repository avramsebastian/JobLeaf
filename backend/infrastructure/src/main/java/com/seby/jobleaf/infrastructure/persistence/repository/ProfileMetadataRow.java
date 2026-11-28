package com.seby.jobleaf.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * A profile row without the two file columns, filled directly by a JPQL constructor query
 * ({@link ProfileJpaRepository#findMetadataByUserId}), so the file contents are never read from the database.
 */
public record ProfileMetadataRow(
        UUID id,
        UUID userId,
        String jobTitle,
        String location,
        String experienceLevel,
        String professionalBio,
        String portfolioWebsite,
        String profilePictureName,
        String profilePictureType,
        String resumeName,
        String resumeType,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
