package com.seby.jobleaf.domain.port.out.auth;

import com.seby.jobleaf.domain.model.UserAccount;

import java.util.Optional;

/**
 * Outbound port for reading user accounts.
 */
public interface UserAccountPort {

    /**
     * @param email email address, compared case-insensitively
     */
    Optional<UserAccount> findByEmail(String email);
}
