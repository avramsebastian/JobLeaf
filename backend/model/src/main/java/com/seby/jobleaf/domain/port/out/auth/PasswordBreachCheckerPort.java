package com.seby.jobleaf.domain.port.out.auth;

/**
 * Outbound port that tells whether a password is known from data breaches, so it must not be used.
 */
public interface PasswordBreachCheckerPort {

    boolean isCompromised(String rawPassword);
}
