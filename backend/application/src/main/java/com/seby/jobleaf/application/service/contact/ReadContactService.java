package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactSort;
import com.seby.jobleaf.domain.model.contact.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.port.in.contact.ReadContactUseCase;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application service implementing {@link ReadContactUseCase}: the admin only sees messages that are still NEW.
 */
@Service
@RequiredArgsConstructor
public class ReadContactService implements ReadContactUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReadContactService.class);

    private final ContactRepositoryPort contactRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contact> fetchNewContacts() {
        var contacts = contactRepositoryPort.findAllByStatus(ContactStatus.NEW);
        log.info("Listed {} new contact messages", contacts.size());
        return contacts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contact> fetchNewContacts(ContactSort sort) {
        Objects.requireNonNull(sort, "sort");
        var contacts = contactRepositoryPort.findAllByStatus(ContactStatus.NEW, sort);
        log.info("Listed {} new contact messages sorted by {} {}", contacts.size(), sort.field().fieldName(),
                sort.direction());
        return contacts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Contact> fetchNewContacts(ContactSort sort, PageQuery page) {
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(page, "page");
        var result = contactRepositoryPort.findAllByStatus(ContactStatus.NEW, sort, page);
        log.info("Listed page {} (size {}) of new contact messages sorted by {} {}: {} of {} in total",
                page.pageNumber(), page.pageSize(), sort.field().fieldName(), sort.direction(),
                result.content().size(), result.totalElements());
        return result;
    }
}
