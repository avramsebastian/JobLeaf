package com.seby.jobleaf.domain.exceptions.auth;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

/**
 * Thrown when the email/password pair is wrong — maps to HTTP 401.
 * <p>
 * The message is the same whether the email is unknown or the password is wrong, so callers cannot find out which
 * emails are registered.
 */
public class InvalidCredentialsException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.auth.invalid.credentials";
    public static final String MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
