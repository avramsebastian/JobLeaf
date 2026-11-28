package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;

import java.util.UUID;

/**
 * Inbound port for a job seeker withdrawing an application.
 */
public interface WithdrawApplicationUseCase {

    /**
     * Sets the application to WITHDRAWN; withdrawing again changes nothing.
     *
     * @param userId the logged-in user
     * @return the withdrawn application
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationNotFoundException  no application
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException  already rejected
     */
    JobApplication withdrawApplication(UUID userId, UUID jobId);
}
