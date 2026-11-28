package com.seby.jobleaf.domain.port.in.savedjob;

import com.seby.jobleaf.domain.model.job.Job;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for a job seeker listing the saved jobs.
 */
public interface ListSavedJobsUseCase {

    /**
     * @param userId the logged-in user
     * @return the saved jobs, newest posted first (also jobs closed after saving, with their status)
     * @throws com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException if the user is not a job seeker
     */
    List<Job> listSavedJobs(UUID userId);
}
