package com.seby.jobleaf.domain.exceptions.user;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

import java.util.UUID;

/**
 * Only job seekers have a profile — maps to HTTP 403. The security filter checks the role in the token; this checks
 * the stored user, because the token can be up to an hour old.
 */
public class JobSeekerAccessException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.user.not.job.seeker";

    public JobSeekerAccessException(UUID userId) {
        super("User " + userId + " is not a job seeker");
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
