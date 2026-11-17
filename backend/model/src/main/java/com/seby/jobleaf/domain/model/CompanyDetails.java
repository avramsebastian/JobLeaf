package com.seby.jobleaf.domain.model;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.time.Year;
import java.time.ZoneOffset;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * Everything an admin can set on a company (all fields except id, audit fields and jobs). Used to create a company
 * and to update one. Validates itself, so an invalid company can never get past the domain, even if a caller skips
 * the request validation of the web layer.
 */
public record CompanyDetails(
        String name,
        String logo,
        String industry,
        String size,
        Double rating,
        String locations,
        Integer founded,
        String description,
        Integer employees,
        String website
) {

    public static final int MIN_FOUNDED = 1900;
    public static final double MAX_RATING = 5.0;
    /** A new company has no reviews yet. */
    public static final double DEFAULT_RATING = 0.0;

    public CompanyDetails {
        name = requireText(name, "name", "Name");
        logo = requireText(logo, "logo", "Logo");
        industry = requireText(industry, "industry", "Industry");
        size = requireText(size, "size", "Size");
        locations = requireText(locations, "locations", "Locations");
        description = requireText(description, "description", "Description");
        website = requireText(website, "website", "Website");

        rating = rating == null ? DEFAULT_RATING : rating;
        if (rating < 0 || rating > MAX_RATING) {
            throw DomainValidationException.invalidField("rating",
                    rating < 0 ? "Rating must be at least 0" : "Rating must be at most 5");
        }
        if (founded == null) {
            throw DomainValidationException.invalidField("founded", "Founded can not be empty");
        }
        if (founded < MIN_FOUNDED) {
            throw DomainValidationException.invalidField("founded", "Founded must be 1900 or later");
        }
        if (founded > Year.now(ZoneOffset.UTC).getValue()) {
            throw DomainValidationException.invalidField("founded", "Founded can not be in the future");
        }
        if (employees != null && employees < 1) {
            throw DomainValidationException.invalidField("employees",
                    "Employees must be greater than or equal to 1");
        }
    }

    private static String requireText(String value, String field, String label) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw DomainValidationException.invalidField(field, label + " can not be empty");
        }
        return normalized;
    }
}
