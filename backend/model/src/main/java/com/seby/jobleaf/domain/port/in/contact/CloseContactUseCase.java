package com.seby.jobleaf.domain.port.in.contact;

import com.seby.jobleaf.domain.model.Contact;

import java.util.UUID;

/**
 * Inbound port for the admin closing a handled contact message.
 */
public interface CloseContactUseCase {

    /**
     * Sets the message to CLOSED. Closing an already closed message changes nothing.
     *
     * @return the closed contact
     * @throws com.seby.jobleaf.domain.exceptions.contact.ContactNotFoundException if no contact has this id
     */
    Contact closeContact(UUID id);
}
