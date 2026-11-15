package com.seby.jobleaf.infrastructure.web.security.ratelimit;

import java.time.Duration;
import java.util.List;

/**
 * "At most {@code capacity} {@code method} requests to {@code path} per {@code period}, per client IP".
 */
public record RateLimitRule(String method, String path, int capacity, Duration period) {

    /**
     * The endpoints anyone can call without logging in, where abuse is cheap for an attacker:
     * guessing passwords, creating fake accounts, spamming the contact form.
     */
    public static final List<RateLimitRule> DEFAULTS = List.of(
            new RateLimitRule("POST", "/jobleaf/auth/login/public", 5, Duration.ofMinutes(1)),
            new RateLimitRule("POST", "/jobleaf/auth/register/public", 3, Duration.ofMinutes(10)),
            new RateLimitRule("POST", "/jobleaf/contacts/public", 3, Duration.ofMinutes(1))
    );

    public boolean matches(String requestMethod, String requestPath) {
        return method.equalsIgnoreCase(requestMethod) && path.equals(requestPath);
    }
}
