package com.seby.jobleaf.domain.model.profile;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;

import java.util.Locale;

import static com.seby.jobleaf.domain.util.TextUtility.normalizeNullableText;

/**
 * The text part of a profile (step one of filling it in). Validates itself.
 */
public record ProfileDetails(
        String jobTitle,
        String location,
        String experienceLevel,
        String professionalBio,
        String portfolioWebsite
) {

    public ProfileDetails {
        jobTitle = requireText(jobTitle, "jobTitle");
        location = requireText(location, "location");
        experienceLevel = requireText(experienceLevel, "experienceLevel");
        professionalBio = requireText(professionalBio, "professionalBio");
        portfolioWebsite = normalizeNullableText(portfolioWebsite);
        if (portfolioWebsite != null && !portfolioWebsite.toLowerCase(Locale.ROOT).matches("^https?://.+")) {
            // Only web links: a "javascript:" link shown on the profile page would run code when clicked
            throw DomainValidationException.invalidField("portfolioWebsite",
                    "portfolioWebsite must start with http:// or https://");
        }
    }

    private static String requireText(String value, String field) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw DomainValidationException.invalidField(field, field + " is required");
        }
        return normalized;
    }
}
