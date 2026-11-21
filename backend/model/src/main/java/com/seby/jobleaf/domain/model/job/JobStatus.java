package com.seby.jobleaf.domain.model.job;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.job.InvalidJobStatusChangeException;

/**
 * Lifecycle of a job: created as {@link #DRAFT}, published as {@link #ACTIVE} (only these are shown to job
 * seekers), {@link #CLOSED} when no longer open. A closed job can be reopened.
 */
public enum JobStatus {
    DRAFT,
    ACTIVE,
    CLOSED;

    /**
     * @throws DomainValidationException for an unknown value
     */
    public static JobStatus from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainValidationException.invalidField("status", "status must be DRAFT, ACTIVE or CLOSED");
        }
    }

    /**
     * The one forbidden move: back to DRAFT once the job was published or closed (job seekers may already have seen
     * or applied to it). Every other change, including to the same status, is allowed.
     *
     * @throws InvalidJobStatusChangeException when going back to DRAFT
     */
    public void checkCanChangeTo(JobStatus target) {
        if (target == DRAFT && this != DRAFT) {
            throw new InvalidJobStatusChangeException(this, target);
        }
    }
}
