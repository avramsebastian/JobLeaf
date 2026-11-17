package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.exceptions.contact.ContactNotFoundException;
import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.in.contact.CloseContactUseCase;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link CloseContactUseCase}.
 */
@Service
@RequiredArgsConstructor
public class CloseContactService implements CloseContactUseCase {

    private static final Logger log = LoggerFactory.getLogger(CloseContactService.class);

    private final ContactRepositoryPort contactRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Contact closeContact(UUID id) {
        Objects.requireNonNull(id, "id");
        var contact = contactRepositoryPort.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot close contact {}: not found", id);
                    return new ContactNotFoundException(id);
                });

        if (contact.isClosed()) {
            log.info("Contact {} was already closed", id);
            return contact;
        }

        var closed = contactRepositoryPort.save(contact.close());
        log.info("Closed contact {}", id);
        return closed;
    }
}
