package com.seby.jobleaf.domain.exceptions.jobapplication;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

import java.util.UUID;

/**
 * The application can not be created or changed in its current state — maps to HTTP 409.
 */
public class JobApplicationConflictException extends LocalizedException {

    public static final String ALREADY_APPLIED_KEY = "error.application.already.applied";
    public static final String ALREADY_REJECTED_KEY = "error.application.already.rejected";
    public static final String DEADLINE_PASSED_KEY = "error.application.deadline.passed";
    public static final String WITHDRAWN_KEY = "error.application.withdrawn";

    private final String messageKey;

    private JobApplicationConflictException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public static JobApplicationConflictException alreadyApplied(UUID jobId) {
        return new JobApplicationConflictException(ALREADY_APPLIED_KEY, "You have already applied for job " + jobId);
    }

    public static JobApplicationConflictException alreadyRejected(UUID jobId) {
        return new JobApplicationConflictException(ALREADY_REJECTED_KEY,
                "Your application for job " + jobId + " was already rejected and can not be withdrawn");
    }

    public static JobApplicationConflictException withdrawn(UUID applicationId) {
        return new JobApplicationConflictException(WITHDRAWN_KEY,
                "Application " + applicationId + " was withdrawn by the job seeker and can not be changed");
    }

    public static JobApplicationConflictException deadlinePassed(UUID jobId) {
        return new JobApplicationConflictException(DEADLINE_PASSED_KEY,
                "The application deadline of job " + jobId + " has passed");
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
