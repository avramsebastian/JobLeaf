package com.seby.jobleaf.domain.exceptions.user;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.user.not.found";

    private UserNotFoundException(String message) {
        super(MESSAGE_KEY, message);
    }

    public static UserNotFoundException byId(UUID id) {
        return new UserNotFoundException("User with id " + id + " not found");
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("No user with email " + email);
    }
}
