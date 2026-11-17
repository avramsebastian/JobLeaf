package com.seby.jobleaf.domain.model;

import com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException;

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

    public boolean isEmployer() {
        return role == RoleName.ROLE_EMPLOYER;
    }

    public boolean isAdmin() {
        return role == RoleName.ROLE_ADMIN;
    }

    /**
     * Rule for promoting a user: an admin keeps the admin role (that would be a demotion), everyone else may
     * become an employer. An existing employer needs no change.
     *
     * @throws UserRoleConflictException for an admin
     */
    public void checkCanBecomeEmployer() {
        if (isAdmin()) {
            throw UserRoleConflictException.adminCanNotBecomeEmployer(id);
        }
    }

    /**
     * Rule for linking a user to a company: only employers work for a company.
     *
     * @throws UserRoleConflictException if the user is not an employer
     */
    public void checkCanBeAssignedToCompany() {
        if (!isEmployer()) {
            throw UserRoleConflictException.notAnEmployer(id, role);
        }
    }
}
