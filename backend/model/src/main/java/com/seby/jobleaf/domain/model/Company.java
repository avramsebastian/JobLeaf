package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.List;
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
        Integer employees,
        String website,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        List<Job> jobs
) {
}
