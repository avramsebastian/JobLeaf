package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable contact aggregate.
 */
public record Contact(
        UUID id,
        String name,
        String email,
        String userType,
        String subject,
        String message,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
    /**
     * Creates a new, not yet persisted contact. Timestamps stay empty: they are set by JPA auditing on insert.
     */
    public static Contact create(
            UUID id,
            String name,
            String email,
            String userType,
            String subject,
            String message,
            UUID createdBy
    ){
        return new Contact(id, name, email, userType, subject, message, null, createdBy, null, createdBy);
    }
}
