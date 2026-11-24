package com.seby.jobleaf.domain.port.out.savedjob;

import com.seby.jobleaf.domain.model.job.Job;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the jobs saved by users (the {@code saved_jobs} join table).
 */
public interface SavedJobRepositoryPort {

    boolean isSaved(UUID userId, UUID jobId);

    /**
     * Saves the job for the user. Both must exist.
     */
    void save(UUID userId, UUID jobId);

    /**
     * Removes the job from the user's saved jobs, if it is there.
     */
    void remove(UUID userId, UUID jobId);

    /**
     * @return the user's saved jobs, newest posted first
     */
    List<Job> findSavedJobs(UUID userId);
}
