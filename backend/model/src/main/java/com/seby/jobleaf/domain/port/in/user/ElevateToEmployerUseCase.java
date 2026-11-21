package com.seby.jobleaf.domain.port.in.user;

import com.seby.jobleaf.domain.model.user.JobLeafUser;

import java.util.UUID;

/**
 * Inbound port for the admin giving a user the employer role.
 */
public interface ElevateToEmployerUseCase {

    /**
     * Makes the user an employer. A user who already is one is returned unchanged.
     *
     * @return the user with role ROLE_EMPLOYER
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException     if no user has this id
     * @throws com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException if the user is an admin
     */
    JobLeafUser elevateToEmployer(UUID userId);
}
