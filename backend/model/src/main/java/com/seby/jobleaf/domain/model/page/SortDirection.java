package com.seby.jobleaf.domain.model.page;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.util.Locale;

public enum SortDirection {
    ASC,
    DESC;

    /**
     * @param value "asc" or "desc" (any case); {@code null} or blank means {@link #ASC}
     * @throws DomainValidationException for any other value (400 for the caller)
     */
    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw DomainValidationException.invalidField("sortDir", "sortDir must be asc or desc");
        }
    }
}
