package com.seby.jobleaf.domain.exceptions.user;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

import java.util.UUID;

/**
 * The user may not manage jobs (right now) — maps to HTTP 403.
 * <p>
 * The security filter already requires ROLE_EMPLOYER from the token, but the token can be up to an hour old. The
 * services check the user as stored now: still an employer, and assigned to a company.
 */
public class EmployerAccessException extends LocalizedException {

    public static final String NOT_EMPLOYER_MESSAGE_KEY = "error.employer.not.employer";
    public static final String NO_COMPANY_MESSAGE_KEY = "error.employer.no.company";

    private final String messageKey;

    private EmployerAccessException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public static EmployerAccessException notAnEmployer(UUID userId) {
        return new EmployerAccessException(NOT_EMPLOYER_MESSAGE_KEY, "User " + userId + " is not an employer");
    }

    public static EmployerAccessException noCompany(UUID userId) {
        return new EmployerAccessException(NO_COMPANY_MESSAGE_KEY,
                "You are not assigned to a company yet. Ask an admin to assign you to your company");
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
