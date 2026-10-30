package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.ContactPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.ContactJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter that implements {@link ContactRepositoryPort} using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ContactRepositoryAdapter implements ContactRepositoryPort {

    private final ContactJpaRepository contactJpaRepository;
    private final ContactPersistenceMapper mapper;

    @Override
    public Contact save(Contact contact) {
        ContactEntity saved = contactJpaRepository.save(mapper.toEntity(contact));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contact> findById(UUID id) {
        return contactJpaRepository.findById(id).map(mapper::toDomain);
    }
}
