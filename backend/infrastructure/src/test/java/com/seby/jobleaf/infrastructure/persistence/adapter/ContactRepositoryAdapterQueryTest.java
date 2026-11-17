package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.Contact;
import com.seby.jobleaf.domain.model.ContactSort;
import com.seby.jobleaf.domain.model.ContactStatus;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.infrastructure.persistence.entity.ContactEntity;
import com.seby.jobleaf.infrastructure.persistence.mapper.ContactPersistenceMapper;
import com.seby.jobleaf.infrastructure.persistence.repository.ContactJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the translation from the domain's sort / page types to Spring Data's {@link Sort} / {@link Pageable}
 * and back to a domain page.
 */
@ExtendWith(MockitoExtension.class)
class ContactRepositoryAdapterQueryTest {

    private final ContactEntity entity = new ContactEntity();
    private final Contact contact = mock(Contact.class);

    @Mock
    private ContactJpaRepository contactJpaRepository;

    @Mock
    private ContactPersistenceMapper mapper;

    @InjectMocks
    private ContactRepositoryAdapter adapter;

    @Test
    @DisplayName("Should sort by the requested field and then by id, so equal values keep a stable order")
    void toSort_addsIdTieBreaker() {
        Sort sort = ContactRepositoryAdapter.toSort(ContactSort.of("subject", "desc"));

        assertThat(sort).containsExactly(Sort.Order.desc("subject"), Sort.Order.asc("id"));
    }

    @Test
    @DisplayName("Should use the derived query for the plain list")
    void findAllByStatus_usesDerivedQuery() {
        when(contactJpaRepository.findByStatusOrderByCreatedAtAsc(ContactStatus.NEW)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(contact);

        assertThat(adapter.findAllByStatus(ContactStatus.NEW)).containsExactly(contact);
    }

    @Test
    @DisplayName("Should query with a Specification and the requested Sort")
    @SuppressWarnings("unchecked")
    void findAllByStatus_withSort_usesSpecification() {
        when(contactJpaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(contact);

        assertThat(adapter.findAllByStatus(ContactStatus.NEW, ContactSort.of("name", "asc"))).containsExactly(contact);
        verify(contactJpaRepository).findAll(any(Specification.class),
                eq(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))));
    }

    @Test
    @DisplayName("Should build the Pageable from the page query and copy the totals into the domain page")
    @SuppressWarnings("unchecked")
    void findAllByStatus_withPage_mapsPage() {
        Pageable expected = PageRequest.of(2, 5, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        when(contactJpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), expected, 11));
        when(mapper.toDomain(entity)).thenReturn(contact);

        var page = adapter.findAllByStatus(ContactStatus.NEW, ContactSort.DEFAULT, new PageQuery(2, 5));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(contactJpaRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(expected);
        assertThat(page.content()).containsExactly(contact);
        assertThat(page.pageNumber()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.last()).isTrue();
    }
}
