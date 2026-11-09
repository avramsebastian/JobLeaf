package com.seby.jobleaf.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered user who can log in. The password is not part of it: checking credentials is the job of the
 * {@code CredentialsVerifierPort}, so the hash never travels through the application.
 */
public record JobLeafUser(
        UUID id,
        String name,
        String email,
        String mobileNumber,
        RoleName role,
        UUID companyId,
        String companyName,
        Instant createdAt
) {
}
