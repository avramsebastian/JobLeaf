package com.seby.jobleaf.infrastructure.web.security.adapter;

import com.seby.jobleaf.domain.port.out.auth.PasswordHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Secondary adapter that implements {@link PasswordHasherPort} with the application's {@link PasswordEncoder}
 * (BCrypt), the same one the login uses to compare passwords.
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     */
    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
