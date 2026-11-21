package com.seby.jobleaf.domain.exceptions.job;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

import java.util.UUID;

/**
 * No such job — or one of another company: employers get the same 404 for both, so they can not probe which job
 * ids exist elsewhere.
 */
public class JobNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.job.not.found";

    public JobNotFoundException(UUID id) {
        super(MESSAGE_KEY, "Job with id " + id + " not found", id);
    }
}
