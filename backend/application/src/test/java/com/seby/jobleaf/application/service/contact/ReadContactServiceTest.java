package com.seby.jobleaf.application.service.contact;

import com.seby.jobleaf.domain.model.contact.Contact;
import com.seby.jobleaf.domain.model.contact.ContactSort;
import com.seby.jobleaf.domain.model.contact.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.port.out.contact.ContactRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadContactServiceTest {

    private final Contact contact = mock(Contact.class);

    @Mock
    private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private ReadContactService service;

    @Test
    @DisplayName("Should only ask the repository for NEW contacts")
    void fetchNewContacts_readsNewOnly() {
        when(contactRepositoryPort.findAllByStatus(ContactStatus.NEW)).thenReturn(List.of(contact));

        assertThat(service.fetchNewContacts()).containsExactly(contact);
    }

    @Test
    @DisplayName("Should pass the sort on to the repository")
    void fetchNewContacts_withSort() {
        var sort = ContactSort.of("name", "desc");
        when(contactRepositoryPort.findAllByStatus(ContactStatus.NEW, sort)).thenReturn(List.of(contact));

        assertThat(service.fetchNewContacts(sort)).containsExactly(contact);
    }

    @Test
    @DisplayName("Should pass sort and page on to the repository and return its page")
    void fetchNewContacts_withPage() {
        var page = new PageQuery(1, 5);
        var result = new PageResult<>(List.of(contact), 1, 5, 6, 2);
        when(contactRepositoryPort.findAllByStatus(ContactStatus.NEW, ContactSort.DEFAULT, page)).thenReturn(result);

        assertThat(service.fetchNewContacts(ContactSort.DEFAULT, page)).isSameAs(result);
    }
}
