package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A user who can log in. The password is not part of it: checking credentials is the job of the
 * {@code CredentialsVerifierPort}, so the hash never travels through the application.
 *
 * @param role one of {@code ROLE_JOB_SEEKER}, {@code ROLE_EMPLOYER}, {@code ROLE_ADMIN}
 */
public record UserAccount(
        UUID id,
        String name,
        String email,
        String mobileNumber,
        String role,
        UUID companyId,
        String companyName,
        Instant createdAt
) {
}
