package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.profile.Profile;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.domain.model.profile.ProfileFile;
import com.seby.jobleaf.domain.model.profile.ProfileFileType;
import com.seby.jobleaf.infrastructure.persistence.entity.ProfileEntity;
import com.seby.jobleaf.infrastructure.persistence.repository.ProfileMetadataRow;
import org.springframework.stereotype.Component;

@Component
public class ProfilePersistenceMapper {

    /**
     * @param includeBinaryData false: the files only carry name and type (content null), so the megabytes are not
     *                          passed up through the application; true: with the file contents
     */
    public Profile toDomain(ProfileEntity entity, boolean includeBinaryData) {
        if (entity == null) {
            return null;
        }
        return new Profile(
                entity.getId(),
                entity.getUser().getId(),
                details(entity.getJobTitle(), entity.getLocation(), entity.getExperienceLevel(),
                        entity.getProfessionalBio(), entity.getPortfolioWebsite()),
                file(entity.getProfilePictureName(), entity.getProfilePictureType(),
                        includeBinaryData ? entity.getProfilePicture() : null),
                file(entity.getResumeName(), entity.getResumeType(), includeBinaryData ? entity.getResume() : null),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    /**
     * A row from the query without file columns: always without file contents.
     */
    public Profile toDomain(ProfileMetadataRow row) {
        return new Profile(
                row.id(),
                row.userId(),
                details(row.jobTitle(), row.location(), row.experienceLevel(), row.professionalBio(),
                        row.portfolioWebsite()),
                file(row.profilePictureName(), row.profilePictureType(), null),
                file(row.resumeName(), row.resumeType(), null),
                row.createdAt(),
                row.createdBy(),
                row.updatedAt(),
                row.updatedBy()
        );
    }

    /**
     * Copies the details, and only the files that carry new content; files without content stay as stored.
     */
    public void applyTo(Profile profile, ProfileEntity entity) {
        ProfileDetails details = profile.details();
        entity.setJobTitle(details.jobTitle());
        entity.setLocation(details.location());
        entity.setExperienceLevel(details.experienceLevel());
        entity.setProfessionalBio(details.professionalBio());
        entity.setPortfolioWebsite(details.portfolioWebsite());

        ProfileFile picture = profile.profilePicture();
        if (picture != null && picture.hasContent()) {
            entity.setProfilePicture(picture.content());
            entity.setProfilePictureName(picture.name());
            entity.setProfilePictureType(picture.type().mediaType());
        }
        ProfileFile resume = profile.resume();
        if (resume != null && resume.hasContent()) {
            entity.setResume(resume.content());
            entity.setResumeName(resume.name());
            entity.setResumeType(resume.type().mediaType());
        }
    }

    private static ProfileDetails details(String jobTitle, String location, String experienceLevel, String bio,
                                          String portfolioWebsite) {
        return new ProfileDetails(jobTitle, location, experienceLevel, bio, portfolioWebsite);
    }

    /**
     * @return null when no file was uploaded yet
     */
    private static ProfileFile file(String name, String mediaType, byte[] content) {
        if (name == null || mediaType == null) {
            return null;
        }
        ProfileFileType type = ProfileFileType.fromMediaType(mediaType)
                .orElseThrow(() -> new IllegalStateException("Unknown stored file type " + mediaType));
        return new ProfileFile(name, type, content);
    }
}
