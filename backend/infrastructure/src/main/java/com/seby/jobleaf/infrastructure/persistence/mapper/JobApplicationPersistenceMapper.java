package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.jobapplication.Applicant;
import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.profile.ProfileDetails;
import com.seby.jobleaf.infrastructure.persistence.entity.JobApplicationEntity;
import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.repository.ProfileMetadataRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobApplicationPersistenceMapper {

    private final JobPersistenceMapper jobMapper;

    /**
     * Maps the application with its job (load the job in the same query, see JobApplicationJpaRepository).
     */
    public JobApplication toDomain(JobApplicationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new JobApplication(
                entity.getId(),
                entity.getUser().getId(),
                jobMapper.toDomain(entity.getJob()),
                entity.getAppliedAt(),
                entity.getStatus(),
                entity.getCoverLetter(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    /**
     * The user behind an application with their profile details.
     *
     * @param profile the profile without its files, or null if the user has no profile yet
     */
    public Applicant toApplicant(JobLeafUserEntity user, ProfileMetadataRow profile) {
        ProfileDetails details = profile == null ? null : new ProfileDetails(profile.jobTitle(), profile.location(),
                profile.experienceLevel(), profile.professionalBio(), profile.portfolioWebsite());
        return new Applicant(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobileNumber(),
                details,
                profile != null && profile.resumeName() != null,
                profile != null && profile.profilePictureName() != null
        );
    }

    /**
     * Copies what can change: applied date, status and cover letter (job seeker), status and notes (employer).
     * User and job are never changed here.
     */
    public void applyTo(JobApplication application, JobApplicationEntity entity) {
        entity.setAppliedAt(application.appliedAt());
        entity.setStatus(application.status());
        entity.setCoverLetter(application.coverLetter());
        entity.setNotes(application.notes());
    }
}
