package com.seby.jobleaf.domain.model.job;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * What an employer fills in for a new job. Validates itself, so an invalid job can not get past the domain even if a
 * caller skips the request validation of the web layer. Company, status and dates are not part of it: the
 * application decides those.
 */
public record JobDetails(
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
        Instant applicationDeadline,
        boolean featured,
        boolean urgent,
        boolean remote
) {

    public JobDetails {
        title = requireText(title, "title");
        location = requireText(location, "location");
        workType = requireText(workType, "workType");
        jobType = requireText(jobType, "jobType");
        category = requireText(category, "category");
        experienceLevel = requireText(experienceLevel, "experienceLevel");
        salaryCurrency = requireText(salaryCurrency, "salaryCurrency");
        salaryPeriod = requireText(salaryPeriod, "salaryPeriod");
        description = requireText(description, "description");
        requirements = requireItems(requirements, "requirements");
        benefits = requireItems(benefits, "benefits");

        if (salaryMin == null || salaryMax == null) {
            throw DomainValidationException.invalidField(salaryMin == null ? "salaryMin" : "salaryMax",
                    "salaryMin and salaryMax are required");
        }
        if (salaryMin.signum() < 0) {
            throw DomainValidationException.invalidField("salaryMin", "salaryMin can not be negative");
        }
        if (salaryMax.compareTo(salaryMin) < 0) {
            throw DomainValidationException.invalidField("salaryMax",
                    "salaryMax must be greater than or equal to salaryMin");
        }
        if (applicationDeadline == null) {
            throw DomainValidationException.invalidField("applicationDeadline", "applicationDeadline is required");
        }
        if (!applicationDeadline.isAfter(Instant.now())) {
            throw DomainValidationException.invalidField("applicationDeadline",
                    "applicationDeadline must be in the future");
        }
    }

    private static String requireText(String value, String field) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw DomainValidationException.invalidField(field, field + " is required");
        }
        return normalized;
    }

    /**
     * @return the trimmed items without blanks, as an immutable list; at least one is required
     */
    private static List<String> requireItems(List<String> values, String field) {
        List<String> items = values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        if (items.isEmpty()) {
            throw DomainValidationException.invalidField(field, field + " needs at least one item");
        }
        return items;
    }
}
