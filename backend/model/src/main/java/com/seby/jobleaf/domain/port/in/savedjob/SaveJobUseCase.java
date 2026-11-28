package com.seby.jobleaf.domain.port.in.savedjob;

import com.seby.jobleaf.domain.model.job.Job;

import java.util.UUID;

/**
 * Inbound port for a job seeker saving a job.
 */
public interface SaveJobUseCase {

    /**
     * Saves the job; saving an already saved job changes nothing.
     *
     * @param userId the logged-in user
     * @return the saved job
     * @throws com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException if the user is not a job seeker
     * @throws com.seby.jobleaf.domain.exceptions.job.JobNotFoundException     if the job does not exist or is not
     *                                                                          published
     */
    Job saveJob(UUID userId, UUID jobId);
}
