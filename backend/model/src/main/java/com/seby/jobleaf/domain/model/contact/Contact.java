package com.seby.jobleaf.domain.model.contact;

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
        ContactStatus status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
    /**
     * Creates a new, not yet persisted contact with status {@link ContactStatus#NEW}. Timestamps stay empty: they are
     * set by JPA auditing on insert.
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
        return new Contact(id, name, email, userType, subject, message, ContactStatus.NEW, null, createdBy, null,
                createdBy);
    }

    public boolean isClosed() {
        return status == ContactStatus.CLOSED;
    }

    /**
     * @return a copy with status {@link ContactStatus#CLOSED}; who changed it and when is filled in by JPA auditing
     */
    public Contact close() {
        return new Contact(id, name, email, userType, subject, message, ContactStatus.CLOSED, createdAt, createdBy,
                updatedAt, updatedBy);
    }
}
