package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Application service implementing {@link CreateContactUseCase}.
 */
@Service
@RequiredArgsConstructor
public class CreateContactService implements CreateContactUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateContactService.class);

    private final ContactRepositoryPort contactRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Contact execute(CreateContactCommand command) {
        Objects.requireNonNull(command, "command");

        UUID contactId = UUID.randomUUID();

        Contact contact = Contact.create(
                contactId,
                command.name(),
                command.email(),
                command.userType(),
                command.subject(),
                command.message(),
                command.userId()
        );

        var savedContact = contactRepositoryPort.save(contact);
        log.info("Created contact with id {}", savedContact.id());
        return savedContact;
    }
}
