package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.ContactWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private CreateContactUseCase createContactUseCase;

    @Mock
    private ContactWebMapper contactWebMapper;

    @InjectMocks
    private ContactController contactController;

    @Test
    @DisplayName("Should return 201 CREATED with CreateContactResponse")
    void createContact_returnsCreatedResponse() {
        // Given
        CreateContactRequest request = new CreateContactRequest();
        request.setName("Diana Prince");
        request.setEmail("diana@example.com");
        request.setUserType("jobseeker");
        request.setSubject("Interview Question");
        request.setMessage("Where can I find interview tips?");

        Contact domainToCreate = new Contact(
                null,
                "Diana Prince",
                "diana@example.com",
                "jobseeker",
                "Interview Question",
                "Where can I find interview tips?",
                null,
                null,
                null,
                null
        );

        Contact createdDomain = new Contact(
                UUID.randomUUID(),
                "Diana Prince",
                "diana@example.com",
                "jobseeker",
                "Interview Question",
                "Where can I find interview tips?",
                Instant.now(),
                null,
                null,
                null
        );

        CreateContactResponse expectedResponse = new CreateContactResponse();
        expectedResponse.setStatus("Request processed successfully!");

        when(contactWebMapper.toDomain(request)).thenReturn(domainToCreate);
        when(createContactUseCase.createContact(domainToCreate)).thenReturn(createdDomain);
        when(contactWebMapper.toCreateContactResponse(createdDomain)).thenReturn(expectedResponse);

        // When
        ResponseEntity<CreateContactResponse> response = contactController.createContact(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("Request processed successfully!");

        verify(contactWebMapper).toDomain(request);
        verify(createContactUseCase).createContact(domainToCreate);
        verify(contactWebMapper).toCreateContactResponse(createdDomain);
    }
}
