package com.seby.jobleaf.infrastructure.web.security.jwt;

import lombok.experimental.UtilityClass;

/**
 * Claim names shared by the token issuer (login) and the token validator filter, so both always agree.
 */
@UtilityClass
public class JwtClaimNames {

    /** User's email: the principal name, since users log in with their email. */
    public static final String EMAIL = "email";

    /** Comma-separated authorities, e.g. {@code ROLE_ADMIN}. */
    public static final String ROLES = "roles";

    /** Issuer of every JobLeaf token. */
    public static final String ISSUER = "jobleaf";
}
