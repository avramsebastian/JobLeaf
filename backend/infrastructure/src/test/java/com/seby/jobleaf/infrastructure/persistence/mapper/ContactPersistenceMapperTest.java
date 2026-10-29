package com.seby.jobleaf.infrastructure.persistence.mapper;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactPersistenceMapperTest {

    private ContactPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContactPersistenceMapper();
    }

    @Test
    @DisplayName("Should map ContactEntity to domain Contact with all fields")
    void toDomain_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-10T11:00:00Z");

        ContactEntity entity = new ContactEntity();
        entity.id = id;
        entity.setName("Alice Smith");
        entity.setEmail("alice@example.com");
        entity.setUserType("jobseeker");
        entity.setSubject("Application status");
        entity.setMessage("Can I get an update?");
        entity.createdAt = createdAt;

        // When
        Contact domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("Alice Smith");
        assertThat(domain.email()).isEqualTo("alice@example.com");
        assertThat(domain.userType()).isEqualTo("jobseeker");
        assertThat(domain.subject()).isEqualTo("Application status");
        assertThat(domain.message()).isEqualTo("Can I get an update?");
        assertThat(domain.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should return null when mapping null ContactEntity to domain")
    void toDomain_whenEntityIsNull_returnsNull() {
        // Given
        ContactEntity entity = null;

        // When
        Contact domain = mapper.toDomain(entity);

        // Then
        assertThat(domain).isNull();
    }

    @Test
    @DisplayName("Should map domain Contact to ContactEntity with all fields")
    void toEntity_mapsAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-10T11:00:00Z");

        Contact domain = new Contact(
                id,
                "Bob Johnson",
                "bob@example.com",
                "other",
                "Partnership Inquiry",
                "Interested in partnering with JobLeaf.",
                createdAt,
                null,
                null,
                null
        );

        // When
        ContactEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Bob Johnson");
        assertThat(entity.getEmail()).isEqualTo("bob@example.com");
        assertThat(entity.getUserType()).isEqualTo("other");
        assertThat(entity.getSubject()).isEqualTo("Partnership Inquiry");
        assertThat(entity.getMessage()).isEqualTo("Interested in partnering with JobLeaf.");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should return null when mapping null domain Contact to entity")
    void toEntity_whenDomainIsNull_returnsNull() {
        // Given
        Contact domain = null;

        // When
        Contact entity = mapper.toDomain(domain != null ? mapper.toEntity(domain) : null);

        // Then
        assertThat(entity).isNull();
    }
}
