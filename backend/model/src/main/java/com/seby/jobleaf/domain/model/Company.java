package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable company aggregate.
 */
public record Company(
        UUID id,
        String name,
        String logo,
        String industry,
        String size,
        Double rating,
        String locations,
        int founded,
        String description,
        int employees,
        String website,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
