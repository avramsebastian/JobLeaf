package com.seby.jobleaf.domain.exceptions.job;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;
import com.seby.jobleaf.domain.model.job.JobStatus;

/**
 * The requested status change is not allowed from the job's current status — maps to HTTP 409.
 */
public class InvalidJobStatusChangeException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.job.invalid.status.change";

    public InvalidJobStatusChangeException(JobStatus from, JobStatus to) {
        super("A job can not change from " + from + " to " + to);
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
