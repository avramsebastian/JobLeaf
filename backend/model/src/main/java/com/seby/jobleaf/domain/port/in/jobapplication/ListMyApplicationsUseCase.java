package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for a job seeker listing their applications.
 */
public interface ListMyApplicationsUseCase {

    /**
     * @param userId the logged-in user
     * @return all applications with their job, newest first (withdrawn ones included)
     */
    List<JobApplication> listMyApplications(UUID userId);
}
