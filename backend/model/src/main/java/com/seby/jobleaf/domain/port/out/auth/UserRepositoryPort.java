package com.seby.jobleaf.domain.port.out.auth;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for reading and creating users.
 */
public interface UserRepositoryPort {

    /**
     * @param email email address, compared case-insensitively
     */
    Optional<JobLeafUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Stores a new user with an already hashed password.
     *
     * @return the stored user, with its generated id and creation time
     */
    JobLeafUser create(NewUser user, String passwordHash);

    /**
     * @return the user with role and company loaded, or empty if no user has this id
     */
    Optional<JobLeafUser> findById(UUID id);

    /**
     * Stores a new role for the user.
     *
     * @return the updated user
     */
    JobLeafUser updateRole(UUID userId, RoleName role);

    /**
     * Links the user to the company (replacing a previous one). The company must exist.
     *
     * @return the updated user, with company id and name
     */
    JobLeafUser assignCompany(UUID userId, UUID companyId);

    /**
     * Data needed to create a user. The password is passed separately and only as a hash.
     */
    record NewUser(String name, String email, String mobileNumber, RoleName role) {
    }
}
