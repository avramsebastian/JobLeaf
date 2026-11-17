package com.seby.jobleaf.domain.exceptions.user;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;
import com.seby.jobleaf.domain.model.RoleName;

import java.util.UUID;

/**
 * The user's current role does not allow the requested change — maps to HTTP 409 (the request is fine, the state
 * of the user is not).
 */
public class UserRoleConflictException extends LocalizedException {

    public static final String ADMIN_MESSAGE_KEY = "error.user.admin.can.not.become.employer";
    public static final String NOT_EMPLOYER_MESSAGE_KEY = "error.user.not.employer";

    private final String messageKey;

    private UserRoleConflictException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public static UserRoleConflictException adminCanNotBecomeEmployer(UUID userId) {
        return new UserRoleConflictException(ADMIN_MESSAGE_KEY,
                "User " + userId + " is an admin and can not be changed to employer");
    }

    public static UserRoleConflictException notAnEmployer(UUID userId, RoleName role) {
        return new UserRoleConflictException(NOT_EMPLOYER_MESSAGE_KEY,
                "Only employers can be assigned to a company, user " + userId + " has " + role);
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
