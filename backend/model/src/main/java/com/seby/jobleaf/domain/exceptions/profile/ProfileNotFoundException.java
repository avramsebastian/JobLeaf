package com.seby.jobleaf.domain.exceptions.profile;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

/**
 * No profile saved yet, or the requested file was never uploaded — maps to HTTP 404.
 */
public class ProfileNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.profile.not.found";

    private ProfileNotFoundException(String message) {
        super(MESSAGE_KEY, message);
    }

    public static ProfileNotFoundException noProfile() {
        return new ProfileNotFoundException("You have not created a profile yet");
    }

    public static ProfileNotFoundException noFile(String file) {
        return new ProfileNotFoundException("You have not uploaded a " + file + " yet");
    }
}
