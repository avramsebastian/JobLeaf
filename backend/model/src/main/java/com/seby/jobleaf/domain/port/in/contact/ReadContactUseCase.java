package com.seby.jobleaf.domain.port.in.contact;

import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactSort;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;

import java.util.List;

/**
 * Inbound port for the admin reading the contact messages that still need handling (status NEW).
 */
public interface ReadContactUseCase {

    /**
     * @return all new contact messages, oldest first
     */
    List<Contact> fetchNewContacts();

    /**
     * @return all new contact messages in the given order
     */
    List<Contact> fetchNewContacts(ContactSort sort);

    /**
     * @return one page of new contact messages in the given order
     */
    PageResult<Contact> fetchNewContacts(ContactSort sort, PageQuery page);
}
