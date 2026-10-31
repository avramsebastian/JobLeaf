package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.ContactPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.ContactJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactRepositoryAdapterTest {

    @Mock
    private ContactJpaRepository contactJpaRepository;

    @Mock
    private ContactPersistenceMapper mapper;

    @InjectMocks
    private ContactRepositoryAdapter adapter;

    @Test
    @DisplayName("Should save contact aggregate and return domain model")
    void save_mapsToEntitySavesAndMapsToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        Contact domainContact = new Contact(
                id,
                "Jane Doe",
                "jane@example.com",
                "JobSeeker",
                "Support",
                "Need help logging in",
                Instant.now(),
                null,
                null,
                null
        );

        ContactEntity entityToSave = new ContactEntity();
        ContactEntity savedEntity = new ContactEntity();
        savedEntity.setId(id);

        when(mapper.toEntity(domainContact)).thenReturn(entityToSave);
        when(contactJpaRepository.save(entityToSave)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(domainContact);

        // When
        Contact result = adapter.save(domainContact);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Jane Doe");
        verify(mapper).toEntity(domainContact);
        verify(contactJpaRepository).save(entityToSave);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    @DisplayName("Should find contact by id and map to domain")
    void findById_whenFound_returnsMappedContact() {
        // Given
        UUID id = UUID.randomUUID();
        Contact domainContact = new Contact(
                id,
                "Jane Doe",
                "jane@example.com",
                "JobSeeker",
                "Support",
                "Need help logging in",
                Instant.now(),
                null,
                null,
                null
        );
        ContactEntity entity = new ContactEntity();
        entity.setId(id);

        when(contactJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainContact);

        // When
        Optional<Contact> result = adapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(domainContact);
        verify(contactJpaRepository).findById(id);
        verify(mapper).toDomain(entity);
    }
}
