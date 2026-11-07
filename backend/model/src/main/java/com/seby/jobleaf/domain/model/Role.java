package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A role a user can have, as stored in the {@code roles} table.
 */
public record Role(
        UUID id,
        RoleName name,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
