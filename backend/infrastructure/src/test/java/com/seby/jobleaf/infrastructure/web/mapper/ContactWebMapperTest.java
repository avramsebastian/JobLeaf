package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactWebMapperTest {

    private ContactWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContactWebMapper();
    }

    @Test
    @DisplayName("Should map CreateContactRequest to domain Contact")
    void toDomain_mapsRequestToDomain() {
        // Given
        CreateContactRequest request = new CreateContactRequest();
        request.setName("Charlie Brown");
        request.setEmail("charlie@example.com");
        request.setUserType("Employer");
        request.setSubject("Billing Question");
        request.setMessage("Can you provide an invoice?");

        // When
        Contact domain = mapper.toDomain(request);

        // Then
        assertThat(domain).isNotNull();
        assertThat(domain.name()).isEqualTo("Charlie Brown");
        assertThat(domain.email()).isEqualTo("charlie@example.com");
        assertThat(domain.userType()).isEqualTo("Employer");
        assertThat(domain.subject()).isEqualTo("Billing Question");
        assertThat(domain.message()).isEqualTo("Can you provide an invoice?");
        assertThat(domain.id()).isNull();
    }

    @Test
    @DisplayName("Should return null when request is null")
    void toDomain_whenRequestIsNull_returnsNull() {
        // Given
        CreateContactRequest request = null;

        // When
        Contact domain = mapper.toDomain(request);

        // Then
        assertThat(domain).isNull();
    }

    @Test
    @DisplayName("Should map domain Contact to CreateContactResponse with success status")
    void toCreateContactResponse_returnsSuccessResponse() {
        // Given
        Contact domain = new Contact(
                UUID.randomUUID(),
                "Charlie Brown",
                "charlie@example.com",
                "Employer",
                "Billing",
                "Message content",
                Instant.now(),
                null,
                null,
                null
        );

        // When
        CreateContactResponse response = mapper.toCreateContactResponse(domain);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Request processed successfully!");
    }
}
