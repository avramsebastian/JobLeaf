package com.seby.jobleaf.infrastructure.persistence.adapter;

import com.seby.jobleaf.domain.model.UserAccount;
import com.seby.jobleaf.domain.port.out.auth.UserAccountPort;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * TEMPORARY test users until the users table exists; then a JPA adapter replaces this class.
 * <p>
 * One place for both views of a user: {@link UserAccountPort} (profile data for the application) and
 * {@link UserDetailsService} (email + password hash + role for Spring Security's credential check).
 */
@Component
public class InMemoryUserAccountAdapter implements UserAccountPort, UserDetailsService {

    private record StoredUser(UserAccount account, String passwordHash) {
    }

    private static final Instant SEEDED_AT = Instant.parse("2026-11-03T00:00:00Z");

    private static final List<StoredUser> USERS = List.of(
            new StoredUser(new UserAccount(UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a01"), "Madan",
                    "madan@example.com", null, "ROLE_JOB_SEEKER", null, null, SEEDED_AT),
                    "$2a$10$viS6XrG2FpiZXPQgP7.rQeBrG6TauRaybxsaNjNi.WCLCdIURzZCq"),
            new StoredUser(new UserAccount(UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02"), "Admin",
                    "admin@example.com", null, "ROLE_ADMIN", null, null, SEEDED_AT),
                    "$2a$10$CurDmUEPRQsX5AhEN1NSV.ejUteU0S3dj5XfjxOjaVFhCCTOuj8WG")
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return find(email).map(StoredUser::account);
    }

    /**
     * Spring Security calls the login identifier "username"; here it is the email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        StoredUser user = find(email).orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return User.withUsername(user.account().email())
                .password(user.passwordHash())
                .authorities(user.account().role())
                .build();
    }

    private static Optional<StoredUser> find(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return USERS.stream().filter(user -> user.account().email().equals(normalized)).findFirst();
    }
}
