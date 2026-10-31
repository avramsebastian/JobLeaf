package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.model.SystemActor;
import com.seby.jobleaf.domain.port.in.contact.CreateContactUseCase.CreateContactCommand;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class CreateContactServiceTest {

    @Mock
    private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private CreateContactService createContactService;

    @Captor
    private ArgumentCaptor<Contact> contactCaptor;

    @Test
    @DisplayName("Should build a new contact from the command, save it and return the persisted contact")
    void execute_buildsAndSavesContact() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateContactCommand command = new CreateContactCommand(
                userId, "John Doe", "john@example.com", "Employer", "Job Posting", "I have a question.");
        when(contactRepositoryPort.save(any(Contact.class))).then(returnsFirstArg());

        // When
        Contact result = createContactService.execute(command);

        // Then
        verify(contactRepositoryPort).save(contactCaptor.capture());
        Contact saved = contactCaptor.getValue();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.name()).isEqualTo("John Doe");
        assertThat(saved.email()).isEqualTo("john@example.com");
        assertThat(saved.userType()).isEqualTo("Employer");
        assertThat(saved.subject()).isEqualTo("Job Posting");
        assertThat(saved.message()).isEqualTo("I have a question.");
        assertThat(saved.createdBy()).isEqualTo(userId);
        assertThat(saved.updatedBy()).isEqualTo(userId);
        assertThat(saved.createdAt()).as("set by JPA auditing on insert").isNull();
        assertThat(saved.updatedAt()).as("set by JPA auditing on insert").isNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("Should return what the repository persisted, not the input contact")
    void execute_returnsPersistedContact() {
        // Given
        CreateContactCommand command = new CreateContactCommand(
                null, "John Doe", "john@example.com", "Employer", "Job Posting", "I have a question.");
        Contact persisted = new Contact(UUID.randomUUID(), "John Doe", "john@example.com", "Employer",
                "Job Posting", "I have a question.", Instant.now(), SystemActor.ID, Instant.now(), SystemActor.ID);
        when(contactRepositoryPort.save(any(Contact.class))).thenReturn(persisted);

        // When
        Contact result = createContactService.execute(command);

        // Then
        assertThat(result).isSameAs(persisted);
    }

    @Test
    @DisplayName("Should generate a different id for every contact")
    void execute_generatesUniqueIds() {
        // Given
        CreateContactCommand command = new CreateContactCommand(
                null, "John Doe", "john@example.com", "Employer", "Job Posting", "I have a question.");
        when(contactRepositoryPort.save(any(Contact.class))).then(returnsFirstArg());

        // When
        Contact first = createContactService.execute(command);
        Contact second = createContactService.execute(command);

        // Then
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("Should reject a null command without touching the repository")
    void execute_whenCommandIsNull_throws() {
        assertThatThrownBy(() -> createContactService.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command");
        verifyNoInteractions(contactRepositoryPort);
    }
}
