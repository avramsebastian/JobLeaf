package com.seby.jobleaf.domain.port.in.job;

import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobDetails;

import java.util.UUID;

/**
 * Inbound port for an employer creating a job.
 */
public interface CreateJobUseCase {

    /**
     * Creates the job in status DRAFT for the employer's own company.
     *
     * @param employerId the logged-in user
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException     if the user does not exist anymore
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException  if not an employer or no company
     */
    Job createJob(UUID employerId, JobDetails details);
}
