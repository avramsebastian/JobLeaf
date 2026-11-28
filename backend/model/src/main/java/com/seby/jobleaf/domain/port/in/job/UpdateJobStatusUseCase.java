package com.seby.jobleaf.domain.port.in.job;

import com.seby.jobleaf.domain.model.job.Job;
import com.seby.jobleaf.domain.model.job.JobStatus;

import java.util.UUID;

/**
 * Inbound port for an employer publishing, closing or reopening one of their company's jobs.
 */
public interface UpdateJobStatusUseCase {

    /**
     * @param employerId the logged-in user
     * @return the job with the new status (unchanged if it already had it)
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException       if not an employer or no company
     * @throws com.seby.jobleaf.domain.exceptions.job.JobNotFoundException          if no such job in their company
     * @throws com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException when going back to DRAFT
     */
    Job updateStatus(UUID employerId, UUID jobId, JobStatus status);
}
