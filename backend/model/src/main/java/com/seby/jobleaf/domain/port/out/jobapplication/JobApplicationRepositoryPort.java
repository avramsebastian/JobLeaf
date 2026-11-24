package com.seby.jobleaf.domain.port.out.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for storing and reading job applications.
 */
public interface JobApplicationRepositoryPort {

    /**
     * @return the user's application for the job (any status), with its job
     */
    Optional<JobApplication> findByUserAndJob(UUID userId, UUID jobId);

    /**
     * @return all applications of the user with their jobs, newest first
     */
    List<JobApplication> findAllByUser(UUID userId);

    /**
     * Creates the application, or updates status, applied date and cover letter of an existing one.
     */
    JobApplication save(JobApplication application);
}
