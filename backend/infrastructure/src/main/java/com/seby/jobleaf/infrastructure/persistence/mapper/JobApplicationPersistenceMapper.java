package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.infrastructure.persistence.entity.JobApplicationEntity;
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
     * Copies what a job seeker can change; user, job and the employer's notes are never changed here.
     */
    public void applyTo(JobApplication application, JobApplicationEntity entity) {
        entity.setAppliedAt(application.appliedAt());
        entity.setStatus(application.status());
        entity.setCoverLetter(application.coverLetter());
    }
}
