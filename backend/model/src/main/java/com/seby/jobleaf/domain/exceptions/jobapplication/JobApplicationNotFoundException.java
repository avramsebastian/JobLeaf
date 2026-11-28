package com.seby.jobleaf.domain.exceptions.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

import java.util.UUID;

/**
 * The application does not exist (or not for this user / company) — maps to HTTP 404.
 */
public class JobApplicationNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.application.not.found";

    /**
     * The job seeker has no application for this job.
     */
    public JobApplicationNotFoundException(UUID jobId) {
        super(MESSAGE_KEY, "You have not applied for job " + jobId, jobId);
    }

    private JobApplicationNotFoundException(String message, UUID applicationId) {
        super(MESSAGE_KEY, message, applicationId);
    }

    /**
     * No application with this id for the employer's company.
     */
    public static JobApplicationNotFoundException byId(UUID applicationId) {
        return new JobApplicationNotFoundException("Job application " + applicationId + " not found", applicationId);
    }
}
