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
    public static final UUID SYSTEM_ACTOR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static Contact create(
            UUID id,
            String name,
            String email,
            String userType,
            String subject,
            String message,
            UUID createdBy,
            Instant now
    ){
        return new Contact(id, name, email, userType, subject, message, now, createdBy, now, createdBy);
    }
}
