package com.seby.jobleaf.domain.exceptions.contact;

import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;

import java.util.UUID;

public class ContactNotFoundException extends ResourceNotFoundException {

    public static final String MESSAGE_KEY = "error.contact.not.found";

    public ContactNotFoundException(UUID id) {
        super(MESSAGE_KEY, "Contact with id " + id + " not found", id);
    }
}
