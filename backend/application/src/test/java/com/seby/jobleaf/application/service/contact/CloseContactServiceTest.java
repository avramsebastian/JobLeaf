package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.exceptions.contact.ContactNotFoundException;
import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactStatus;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseContactServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private CloseContactService service;

    private static Contact contact(ContactStatus status) {
        return new Contact(ID, "Jane", "jane@example.com", "Employer", "Subject", "Message", status,
                Instant.parse("2026-11-15T10:00:00Z"), UUID.randomUUID(), null, null);
    }

    @Test
    @DisplayName("Should save the contact with status CLOSED")
    void closeContact_savesClosedContact() {
        when(contactRepositoryPort.findById(ID)).thenReturn(Optional.of(contact(ContactStatus.NEW)));
        when(contactRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Contact result = service.closeContact(ID);

        var saved = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ContactStatus.CLOSED);
        assertThat(saved.getValue().createdAt()).as("other fields are kept").isEqualTo(contact(ContactStatus.NEW).createdAt());
        assertThat(result.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Should not save again when the contact is already closed")
    void closeContact_alreadyClosed_isIdempotent() {
        when(contactRepositoryPort.findById(ID)).thenReturn(Optional.of(contact(ContactStatus.CLOSED)));

        assertThat(service.closeContact(ID).isClosed()).isTrue();
        verify(contactRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ContactNotFoundException for an unknown id")
    void closeContact_unknownId_throwsNotFound() {
        when(contactRepositoryPort.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closeContact(ID))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessageContaining(ID.toString());
        verify(contactRepositoryPort, never()).save(any());
    }
}
