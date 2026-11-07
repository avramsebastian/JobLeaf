package com.seby.jobleaf.domain.model;

import java.time.Instant;

/**
 * A signed access token (JWT) and the moment it stops being accepted.
 */
public record AuthToken(String value, Instant expiresAt) {
}
