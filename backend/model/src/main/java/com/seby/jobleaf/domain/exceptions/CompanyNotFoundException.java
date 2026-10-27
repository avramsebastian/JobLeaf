package com.seby.jobleaf.domain.exceptions;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

/**
 * Thrown when a referenced company identifier does not match any persisted company.
 */
public class CompanyNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.company.not.found";

    public CompanyNotFoundException(String message) {
        super(MESSAGE_KEY, message);
    }
}
