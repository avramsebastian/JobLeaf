package com.seby.jobleaf.domain.model.contact;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The fields contact messages may be sorted by. An allow-list: any other value is rejected instead of being handed
 * to the database.
 */
public enum ContactSortField {
    CREATED_AT("createdAt"),
    NAME("name"),
    EMAIL("email"),
    SUBJECT("subject"),
    USER_TYPE("userType"),
    STATUS("status");

    private final String fieldName;

    ContactSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @return the name used in the API and on the {@code Contact} model, e.g. "createdAt"
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * @param value API name like "createdAt"; {@code null} or blank means {@link #CREATED_AT}
     * @throws DomainValidationException if the field is not sortable (400 for the caller)
     */
    public static ContactSortField from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        return Arrays.stream(values())
                .filter(field -> field.fieldName.equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> DomainValidationException.invalidField("sortBy",
                        "sortBy must be one of " + Arrays.stream(values()).map(ContactSortField::fieldName)
                                .collect(Collectors.joining(", "))));
    }
}
