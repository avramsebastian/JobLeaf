package com.seby.jobleaf.domain.exceptions.company;

import com.seby.jobleaf.domain.exceptions.domain.LocalizedException;

/**
 * Thrown when another company already has this name (names are unique) — maps to HTTP 409.
 */
public class CompanyAlreadyExistsException extends LocalizedException {

    public static final String MESSAGE_KEY = "error.company.already.exists";
    public static final String FIELD = "name";

    public CompanyAlreadyExistsException(String name) {
        super("A company named " + name + " already exists");
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    public String getField() {
        return FIELD;
    }
}
