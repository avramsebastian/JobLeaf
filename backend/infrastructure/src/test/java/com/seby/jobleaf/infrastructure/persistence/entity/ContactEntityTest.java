package com.seby.jobleaf.infrastructure.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactEntityTest {

    @Test
    @DisplayName("Should correctly set and get all fields via setters and getters")
    void shouldSetAndGetAllFields() {
        // Given
        ContactEntity entity = new ContactEntity();
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        // When
        entity.id = id;
        entity.setName("Bruce Wayne");
        entity.setEmail("bruce@wayne.enterprise");
        entity.setUserType("Employer");
        entity.setSubject("Executive Account");
        entity.setMessage("Inquiry regarding enterprise account.");
        entity.createdAt = createdAt;

        // Then
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Bruce Wayne");
        assertThat(entity.getEmail()).isEqualTo("bruce@wayne.enterprise");
        assertThat(entity.getUserType()).isEqualTo("Employer");
        assertThat(entity.getSubject()).isEqualTo("Executive Account");
        assertThat(entity.getMessage()).isEqualTo("Inquiry regarding enterprise account.");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should instantiate ContactEntity with all-arguments constructor")
    void shouldInstantiateWithAllArgsConstructor() {
        // Given
        String name = "Clark Kent";
        String email = "clark@dailyplanet.example";
        String userType = "jobseeker";
        String subject = "Journalist Role";
        String message = "Looking for journalism opportunities.";

        // When
        ContactEntity entity = new ContactEntity(name, email, userType, subject, message);

        // Then
        assertThat(entity.getName()).isEqualTo(name);
        assertThat(entity.getEmail()).isEqualTo(email);
        assertThat(entity.getUserType()).isEqualTo(userType);
        assertThat(entity.getSubject()).isEqualTo(subject);
        assertThat(entity.getMessage()).isEqualTo(message);
    }
}
