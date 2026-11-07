package com.seby.jobleaf.infrastructure.web.security.adapter;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.port.out.auth.CredentialsVerifierPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Secondary adapter that implements {@link CredentialsVerifierPort} with Spring Security's
 * {@link AuthenticationManager}: it loads the user through the {@code UserDetailsService} and compares the password
 * with the stored Bcrypt hash.
 */
@Component
@RequiredArgsConstructor
public class SpringSecurityCredentialsAdapter implements CredentialsVerifierPort {

    private final AuthenticationManager authenticationManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(String email, String password) {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (AuthenticationException exception) {
            // Unknown email, wrong password, disabled or locked account: all answered the same way
            throw new InvalidCredentialsException();
        }
    }
}
