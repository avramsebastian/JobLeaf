package com.seby.jobleaf.domain.model.contact;

import com.seby.jobleaf.domain.model.page.SortDirection;

import java.util.Objects;

/**
 * How to order contact messages.
 */
public record ContactSort(ContactSortField field, SortDirection direction) {

    /** Oldest first: the message waiting the longest is handled first. */
    public static final ContactSort DEFAULT = new ContactSort(ContactSortField.CREATED_AT, SortDirection.ASC);

    public ContactSort {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(direction, "direction");
    }

    /**
     * Builds the sort from raw request values, validating both.
     */
    public static ContactSort of(String sortBy, String sortDir) {
        return new ContactSort(ContactSortField.from(sortBy), SortDirection.from(sortDir));
    }
}
