package com.seby.jobleaf.domain.port.out.auth;

import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;

import java.util.Optional;

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
     * Data needed to create a user. The password is passed separately and only as a hash.
     */
    record NewUser(String name, String email, String mobileNumber, RoleName role) {
    }
}
