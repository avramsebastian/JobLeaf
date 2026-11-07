package com.seby.jobleaf.infrastructure.web.security;

import java.security.Principal;
import java.util.UUID;

/**
 * The logged-in user, taken from a valid JWT and stored as the principal of the {@code SecurityContext}.
 * <p>
 * {@link #getName()} is the email, so {@code authentication.getName()} keeps returning the login identifier, while
 * {@link #id()} gives the user id for auditing and logs.
 */
public record AuthenticatedUser(UUID id, String email) implements Principal {

    /**
     * Request attribute holding the user, for code that runs after the security filter chain has already cleared
     * the {@code SecurityContext} (e.g. the request logging filter).
     */
    public static final String REQUEST_ATTRIBUTE = AuthenticatedUser.class.getName();

    @Override
    public String getName() {
        return email;
    }
}
