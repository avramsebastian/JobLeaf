package com.seby.jobleaf.domain.port.out.auth;

/**
 * Outbound port that turns a plain password into a one-way hash for storage.
 */
public interface PasswordHasherPort {

    String hash(String rawPassword);
}
