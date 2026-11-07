package com.seby.jobleaf.infrastructure.web.security.adapter;

import com.seby.jobleaf.domain.port.out.auth.PasswordBreachCheckerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.stereotype.Component;

/**
 * Secondary adapter that implements {@link PasswordBreachCheckerPort} with Spring Security's
 * {@link CompromisedPasswordChecker} (Have I Been Pwned).
 * <p>
 * Only the first 5 characters of the password's SHA-1 hash are sent to the service (k-anonymity): the password
 * itself never leaves the server.
 */
@Component
@RequiredArgsConstructor
public class CompromisedPasswordCheckerAdapter implements PasswordBreachCheckerPort {

    private final CompromisedPasswordChecker compromisedPasswordChecker;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompromised(String rawPassword) {
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(rawPassword);
        return decision.isCompromised();
    }
}
