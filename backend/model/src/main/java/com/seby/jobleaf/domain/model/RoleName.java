package com.seby.jobleaf.domain.model;

/**
 * The roles a user can have. The constant names are also the Spring Security authorities, the values stored in the
 * {@code roles} table and the values the frontend checks.
 */
public enum RoleName {
    ROLE_JOB_SEEKER,
    ROLE_EMPLOYER,
    ROLE_ADMIN;

    /**
     * Role every new account gets at registration. Only an admin can change it later.
     */
    public static final RoleName DEFAULT_FOR_NEW_USERS = ROLE_JOB_SEEKER;
}
