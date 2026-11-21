package com.seby.jobleaf.domain.port.in.job;

import com.seby.jobleaf.domain.model.job.Job;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for an employer listing the jobs of their company.
 */
public interface ListEmployerJobsUseCase {

    /**
     * @param employerId the logged-in user
     * @return all jobs of the employer's company, every status, newest first
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException     if the user does not exist anymore
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException  if not an employer or no company
     */
    List<Job> listJobs(UUID employerId);
}
