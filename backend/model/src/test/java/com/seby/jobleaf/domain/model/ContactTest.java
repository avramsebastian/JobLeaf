package com.seby.jobleaf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTest {

    @Test
    @DisplayName("Should create a new contact with the actor set and timestamps left for JPA auditing")
    void create_setsActorAndLeavesTimestampsEmpty() {
        // Given
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        // When
        Contact contact = Contact.create(id, "John", "john@example.com", "Employer", "Subject", "Hello there", actor);

        // Then
        assertThat(contact.id()).isEqualTo(id);
        assertThat(contact.name()).isEqualTo("John");
        assertThat(contact.email()).isEqualTo("john@example.com");
        assertThat(contact.userType()).isEqualTo("Employer");
        assertThat(contact.subject()).isEqualTo("Subject");
        assertThat(contact.message()).isEqualTo("Hello there");
        assertThat(contact.createdBy()).isEqualTo(actor);
        assertThat(contact.updatedBy()).isEqualTo(actor);
        assertThat(contact.createdAt()).isNull();
        assertThat(contact.updatedAt()).isNull();
    }
}
