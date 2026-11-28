package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.ReceivedApplication;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for an employer reading the applications received for one of their company's jobs.
 */
public interface ListReceivedApplicationsUseCase {

    /**
     * @param employerId the logged-in user
     * @return the job's applications with their applicants, newest first, without the withdrawn ones
     * @throws com.seby.jobleaf.domain.exceptions.user.EmployerAccessException if not an employer or no company
     * @throws com.seby.jobleaf.domain.exceptions.job.JobNotFoundException    if no such job in their company
     */
    List<ReceivedApplication> listReceivedApplications(UUID employerId, UUID jobId);
}
