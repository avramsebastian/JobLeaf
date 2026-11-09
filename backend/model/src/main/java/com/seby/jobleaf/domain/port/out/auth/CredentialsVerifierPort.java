package com.seby.jobleaf.domain.port.out.auth;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;

/**
 * Outbound port that checks an email/password pair against the stored (hashed) credentials.
 */
public interface CredentialsVerifierPort {

    /**
     * @throws InvalidCredentialsException when they do not match
     */
    void verify(String email, String password);
}
