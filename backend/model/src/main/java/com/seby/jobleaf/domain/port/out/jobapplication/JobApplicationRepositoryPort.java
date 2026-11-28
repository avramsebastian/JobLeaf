package com.seby.jobleaf.domain.port.out.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;
import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;

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
     * @return the job's applications with their applicants, newest first, without the withdrawn ones
     */
    List<ReceivedApplication> findReceivedByJob(UUID jobId);

    /**
     * @return the application (any status) with its job and its applicant
     */
    Optional<ReceivedApplication> findReceivedById(UUID applicationId);

    /**
     * Creates the application, or updates status, applied date, cover letter and notes of an existing one.
     */
    JobApplication save(JobApplication application);
}
