package com.seby.jobleaf.domain.model.contact;

import com.seby.jobleaf.domain.model.user.SystemActor;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.page.PageQuery;
import com.seby.jobleaf.domain.model.page.PageResult;
import com.seby.jobleaf.domain.model.page.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactSortAndPageTest {

    @Test
    @DisplayName("Should build the sort from API values, any case for the direction")
    void contactSort_fromApiValues() {
        assertThat(ContactSort.of("userType", "DESC"))
                .isEqualTo(new ContactSort(ContactSortField.USER_TYPE, SortDirection.DESC));
    }

    @Test
    @DisplayName("Should fall back to createdAt asc when nothing is given")
    void contactSort_defaults() {
        assertThat(ContactSort.of(null, " ")).isEqualTo(ContactSort.DEFAULT);
    }

    @Test
    @DisplayName("Should reject a field that is not on the allow-list")
    void contactSort_unknownField_rejected() {
        assertThatThrownBy(() -> ContactSort.of("password", "asc"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("sortBy must be one of createdAt, name, email, subject, userType, status");
    }

    @Test
    @DisplayName("Should reject an unknown direction")
    void sortDirection_unknown_rejected() {
        assertThatThrownBy(() -> SortDirection.from("up")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should default to page 0 of size 10 and validate the bounds")
    void pageQuery_defaultsAndBounds() {
        assertThat(PageQuery.of(null, null)).isEqualTo(new PageQuery(0, 10));
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PageQuery(0, 101)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("Should know whether it is the first or last page and map its content")
    void pageResult_firstLastAndMap() {
        var page = new PageResult<>(List.of(1, 2), 1, 2, 5, 3);

        assertThat(page.first()).isFalse();
        assertThat(page.last()).isFalse();
        assertThat(new PageResult<>(List.of(), 0, 10, 0, 0).last()).as("an empty result is its own last page").isTrue();

        var mapped = page.map(String::valueOf);
        assertThat(mapped.content()).containsExactly("1", "2");
        assertThat(mapped.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create contacts as NEW and close them without changing anything else")
    void contact_createAndClose() {
        var contact = Contact.create(UUID.randomUUID(), "Jane", "jane@example.com", "Employer", "Subject",
                "Message", SystemActor.ID);

        assertThat(contact.status()).isEqualTo(ContactStatus.NEW);
        assertThat(contact.isClosed()).isFalse();

        var closed = contact.close();
        assertThat(closed.isClosed()).isTrue();
        assertThat(closed).usingRecursiveComparison().ignoringFields("status").isEqualTo(contact);
    }
}
