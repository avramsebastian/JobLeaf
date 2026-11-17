package com.seby.jobleaf.domain.port.in.user;

import com.seby.jobleaf.domain.model.JobLeafUser;

/**
 * Inbound port for the admin looking up a user.
 */
public interface SearchUserUseCase {

    /**
     * @param email email address; upper/lower case and surrounding spaces do not matter
     * @throws com.seby.jobleaf.domain.exceptions.user.UserNotFoundException if no user has this email
     */
    JobLeafUser searchByEmail(String email);
}
