package com.seby.jobleaf.domain.exceptions.profile;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

/**
 * Applying needs a profile with a resume (the employer has to see one) — maps to HTTP 409 with the field "profile",
 * so the frontend can send the job seeker to the profile page.
 */
public class ProfileIncompleteException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.profile.incomplete";
    public static final String FIELD = "profile";

    public ProfileIncompleteException() {
        super("Please complete your profile and upload your resume before applying for jobs");
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    public String getField() {
        return FIELD;
    }
}
