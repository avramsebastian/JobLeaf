package com.seby.jobleaf.domain.port.in.savedjob;

import java.util.UUID;

/**
 * Inbound port for a job seeker removing a job from the saved jobs.
 */
public interface UnsaveJobUseCase {

    /**
     * Removes the job from the saved jobs; removing a job that is not saved changes nothing.
     *
     * @param userId the logged-in user
     * @throws com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException if the user is not a job seeker
     */
    void unsaveJob(UUID userId, UUID jobId);
}
