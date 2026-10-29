package com.seby.jobleaf.domain.port.out.contact;

import com.seby.jobleaf.domain.model.Contact;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting {@link Contact} aggregates.
 *
 * <p>
 * Implementations live in the infrastructure layer (e.g. JPA adapters).
 */
public interface ContactRepositoryPort {

    /**
     * Persists the given contact aggregate.
     *
     * @param contact domain state to store
     * @return the persisted contact
     */
    Contact save(Contact contact);

    /**
     * Retrieves the contact based on the id or empty if not found.
     *
     * @param id contact identifier
     * @return the contact with the given identifier.
     */
    Optional<Contact> findById(UUID id);
}
