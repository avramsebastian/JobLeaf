package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.model.CreateContactRequest;
import com.seby.jobleaf.apis.generated.model.CreateContactResponse;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase.CreateContactCommand;
import com.seby.jobleaf.infrastructure.web.mapper.ContactWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @DisplayName("Should create the contact anonymously and return 201 Created with the success response")
    void createContact_returnsCreated() {
        // Given
        CreateContactRequest request = new CreateContactRequest();
        CreateContactCommand command = new CreateContactCommand(
                null, "John Doe", "john@example.com", "Employer", "Job Posting", "I have a question.");
        CreateContactResponse responseDto = new CreateContactResponse();
        responseDto.setStatus("Request processed successfully!");

        when(contactWebMapper.toCreateContactCommand(null, request)).thenReturn(command);
        when(contactWebMapper.toCreateContactResponse()).thenReturn(responseDto);

        // When
        ResponseEntity<CreateContactResponse> response = contactController.createContact(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(responseDto);
        verify(createContactUseCase).execute(command);
    }

    @Test
    @DisplayName("Should propagate use case failures and not build a success response")
    void createContact_whenUseCaseFails_propagatesException() {
        // Given
        CreateContactRequest request = new CreateContactRequest();
        CreateContactCommand command = new CreateContactCommand(
                null, "John Doe", "john@example.com", "Employer", "Job Posting", "I have a question.");
        when(contactWebMapper.toCreateContactCommand(null, request)).thenReturn(command);
        when(createContactUseCase.execute(any())).thenThrow(new IllegalStateException("Database down"));

        // When / Then
        assertThatThrownBy(() -> contactController.createContact(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database down");
        verify(contactWebMapper, never()).toCreateContactResponse();
    }
}
