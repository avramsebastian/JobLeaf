package com.seby.jobleaf.domain.port.out.job;

import com.seby.jobleaf.domain.model.job.Job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for storing and reading jobs.
 */
public interface JobRepositoryPort {

    /**
     * @return all jobs of the company, every status, newest first
     */
    List<Job> findAllByCompanyId(UUID companyId);

    Optional<Job> findById(UUID id);

    /**
     * Stores a new job for its company ({@code job.companyId()}, which must exist).
     */
    Job create(Job job);

    /**
     * Stores the job's status and posted date; nothing else changes.
     */
    Job updateStatus(Job job);
}
