package com.seby.jobleaf.domain.model.jobapplication;

/**
 * Lifecycle of a job application. A job seeker creates it as {@link #PENDING} and can {@link #WITHDRAWN withdraw}
 * it; the other statuses are set by the employer.
 */
public enum ApplicationStatus {
    PENDING,
    REVIEWED,
    SHORTLISTED,
    INTERVIEWED,
    OFFERED,
    REJECTED,
    WITHDRAWN
}
