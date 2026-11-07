package com.seby.jobleaf.domain.exceptions.auth;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

/**
 * Thrown when registering with an email or mobile number that another user already has — maps to HTTP 409.
 */
public class UserAlreadyExistsException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.auth.user.already.exists";

    private final String field;

    private UserAlreadyExistsException(String field, String message) {
        super(message);
        this.field = field;
    }

    public static UserAlreadyExistsException email() {
        return new UserAlreadyExistsException("email", "An account with this email already exists");
    }

    public static UserAlreadyExistsException mobileNumber() {
        return new UserAlreadyExistsException("mobileNumber", "An account with this mobile number already exists");
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    @Override
    public Object[] getArgs() {
        return new Object[]{field};
    }

    /**
     * @return name of the conflicting field, as exposed in the API
     */
    public String getField() {
        return field;
    }
}
