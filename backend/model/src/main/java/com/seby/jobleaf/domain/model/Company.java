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

    /**
     * A new, not yet persisted company without jobs (employers add them later). Timestamps stay empty: they are
     * set by JPA auditing on insert.
     */
    public static Company create(UUID id, CompanyDetails details, UUID createdBy) {
        return new Company(id, details.name(), details.logo(), details.industry(), details.size(), details.rating(),
                details.locations(), details.founded(), details.description(), details.employees(), details.website(),
                null, createdBy, null, createdBy, List.of());
    }

    /**
     * @return a copy with all details replaced; id, audit fields and jobs stay the same
     */
    public Company withDetails(CompanyDetails details) {
        return new Company(id, details.name(), details.logo(), details.industry(), details.size(), details.rating(),
                details.locations(), details.founded(), details.description(), details.employees(), details.website(),
                createdAt, createdBy, updatedAt, updatedBy, jobs);
    }
}
