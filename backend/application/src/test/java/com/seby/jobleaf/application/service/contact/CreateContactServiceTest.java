package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateContactServiceTest {

    @Mock
    private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private CreateContactService createContactService;

    @Test
    @DisplayName("Should save contact via repository port and return persisted aggregate")
    void createContact_savesAndReturnsContact() {
        // Given
        UUID contactId = UUID.randomUUID();
        Contact inputContact = new Contact(
                null,
                "John Doe",
                "john@example.com",
                "Employer",
                "Job Posting",
                "I have a question about posting jobs.",
                null,
                null,
                null,
                null
        );

        Contact persistedContact = new Contact(
                contactId,
                "John Doe",
                "john@example.com",
                "Employer",
                "Job Posting",
                "I have a question about posting jobs.",
                Instant.now(),
                null,
                null,
                null
        );

        when(contactRepositoryPort.save(inputContact)).thenReturn(persistedContact);

        // When
        Contact result = createContactService.createContact(inputContact);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(contactId);
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.userType()).isEqualTo("Employer");
        assertThat(result.subject()).isEqualTo("Job Posting");
        assertThat(result.message()).isEqualTo("I have a question about posting jobs.");
        verify(contactRepositoryPort).save(inputContact);
    }
}
