package com.seby.jobleaf.infrastructure.web.security.jwt;

import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.UserAccount;
import com.seby.jobleaf.domain.port.out.auth.TokenIssuerPort;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Secondary adapter that implements {@link TokenIssuerPort} with signed JWTs (HS256), using the same key as
 * {@code JwtTokenValidatorFilter}.
 */
@Component
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final SecretKey signingKey;
    private final Duration validity;
    private final Clock clock;

    @Autowired
    public JwtTokenIssuerAdapter(SecretKey jwtSigningKey, @Value("${jobleaf.jwt.expiration}") Duration validity) {
        this(jwtSigningKey, validity, Clock.systemUTC());
    }

    JwtTokenIssuerAdapter(SecretKey signingKey, Duration validity, Clock clock) {
        this.signingKey = signingKey;
        this.validity = validity;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthToken issue(UserAccount user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(validity);
        String token = Jwts.builder()
                .issuer(JwtClaimNames.ISSUER)
                .subject(user.id().toString())
                .claim(JwtClaimNames.EMAIL, user.email())
                .claim(JwtClaimNames.ROLES, user.role())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new AuthToken(token, expiresAt);
    }
}
