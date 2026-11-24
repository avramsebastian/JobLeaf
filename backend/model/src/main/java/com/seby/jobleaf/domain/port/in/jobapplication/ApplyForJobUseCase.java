package com.seby.jobleaf.domain.port.in.jobapplication;

import com.seby.jobleaf.domain.model.jobapplication.JobApplication;

import java.util.UUID;

/**
 * Inbound port for a job seeker applying for a job.
 */
public interface ApplyForJobUseCase {

    /**
     * @param userId      the logged-in user
     * @param coverLetter optional
     * @return the new (or reopened) application, status PENDING
     * @throws com.seby.jobleaf.domain.exceptions.user.JobSeekerAccessException             not a job seeker
     * @throws com.seby.jobleaf.domain.exceptions.job.JobNotFoundException                  job missing or not published
     * @throws com.seby.jobleaf.domain.exceptions.profile.ProfileIncompleteException        no profile with a resume
     * @throws com.seby.jobleaf.domain.exceptions.jobapplication.JobApplicationConflictException already applied,
     *                                                                                      or deadline passed
     */
    JobApplication applyForJob(UUID userId, UUID jobId, String coverLetter);
}
