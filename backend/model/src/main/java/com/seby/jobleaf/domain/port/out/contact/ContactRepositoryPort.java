package com.seby.jobleaf.domain.port.out.contact;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.model.ContactSort;
import com.seby.jobleaf.domain.model.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;

import java.util.List;
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

    /**
     * @return all contacts with the given status, oldest first
     */
    List<Contact> findAllByStatus(ContactStatus status);

    /**
     * @return all contacts with the given status in the given order
     */
    List<Contact> findAllByStatus(ContactStatus status, ContactSort sort);

    /**
     * @return one page of the contacts with the given status in the given order
     */
    PageResult<Contact> findAllByStatus(ContactStatus status, ContactSort sort, PageQuery page);
}
