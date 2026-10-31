package com.seby.jobleaf.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable job posting belonging to a {@link Company}.
 */
public record Job(
        UUID id,
        UUID companyId,
        String title,
        String location,
        String workType,
        String jobType,
        String category,
        String experienceLevel,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        String description,
        List<String> requirements,
        List<String> benefits,
        Instant postedDate,
        Instant applicationDeadline,
        Integer applicationsCount,
        boolean featured,
        boolean urgent,
        boolean remote,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
