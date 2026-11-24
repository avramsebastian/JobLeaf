package com.seby.jobleaf.domain.exceptions.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

import java.util.UUID;

/**
 * The job seeker has no application for this job — maps to HTTP 404.
 */
public class JobApplicationNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.application.not.found";

    public JobApplicationNotFoundException(UUID jobId) {
        super(MESSAGE_KEY, "You have not applied for job " + jobId, jobId);
    }
}
