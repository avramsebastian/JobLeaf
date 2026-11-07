package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.infrastructure.persistence.entity.JobLeafUserEntity;
import com.seby.jobleaf.infrastructure.persistence.repository.JobLeafUserJpaRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Checks email + password against JobLeaf's own {@code users} table, without Spring Security's
 * {@code UserDetailsService}/{@code User}.
 * <p>
 * Unknown email and wrong password both end in the same {@link BadCredentialsException}, and both cost one BCrypt
 * comparison, so neither the answer nor the response time tells an attacker which emails are registered.
 */
@Component
public class JobLeafEmailPwdAuthenticationProvider implements AuthenticationProvider {

    static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final JobLeafUserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    /** Compared against when the email is unknown, so that case takes as long as a wrong password. */
    private final String dummyPasswordHash;

    public JobLeafEmailPwdAuthenticationProvider(JobLeafUserJpaRepository userJpaRepository,
                                                 PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("timing-attack-protection-dummy-password");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName() == null ? "" : authentication.getName().trim().toLowerCase(Locale.ROOT);
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        Optional<JobLeafUserEntity> user = userJpaRepository.findWithRoleByEmail(email);
        if (user.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new BadCredentialsException(BAD_CREDENTIALS_MESSAGE);
        }
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new BadCredentialsException(BAD_CREDENTIALS_MESSAGE);
        }

        // The password is not kept in the result (credentials = null)
        return UsernamePasswordAuthenticationToken.authenticated(user.get().getEmail(), null,
                AuthorityUtils.createAuthorityList(user.get().getRole().getName().name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
