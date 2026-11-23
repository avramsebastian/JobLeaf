package com.seby.jobleaf.infrastructure.persistence.repository;

import com.seby.jobleaf.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {

    /**
     * The whole entity, file contents included (needed to update it or to send a file).
     */
    Optional<ProfileEntity> findByUser_Id(UUID userId);

    /**
     * Only the columns without files: {@code select new ...} builds the record straight from the selected columns,
     * so the SQL never selects {@code profile_picture} or {@code resume}.
     */
    @Query("""
            select new com.seby.jobleaf.infrastructure.persistence.repository.ProfileMetadataRow(
                p.id, p.user.id, p.jobTitle, p.location, p.experienceLevel, p.professionalBio, p.portfolioWebsite,
                p.profilePictureName, p.profilePictureType, p.resumeName, p.resumeType,
                p.createdAt, p.createdBy, p.updatedAt, p.updatedBy)
            from ProfileEntity p
            where p.user.id = :userId""")
    Optional<ProfileMetadataRow> findMetadataByUserId(@Param("userId") UUID userId);
}
