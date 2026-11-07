package com.seby.jobleaf.infrastructure.web.security.jwt;

import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.filter.JwtTokenValidatorFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtTokenIssuerAdapterTest {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "test-secret-key-for-unit-tests-only-0123456789".getBytes(StandardCharsets.UTF_8));
    private static final JobLeafUser USER = new JobLeafUser(UUID.randomUUID(), "Admin", "admin@example.com", null,
            RoleName.ROLE_ADMIN, null, null, Instant.parse("2026-11-03T00:00:00Z"));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should sign a token with issuer, subject, email, roles and the configured validity")
    void issue_createsSignedTokenWithClaims() {
        // Given
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        var issuer = new JwtTokenIssuerAdapter(KEY, Duration.ofMinutes(30), Clock.fixed(now, ZoneOffset.UTC));

        // When
        AuthToken token = issuer.issue(USER);

        // Then
        assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        Claims claims = Jwts.parser().verifyWith(KEY).clock(() -> java.util.Date.from(now)).build()
                .parseSignedClaims(token.value()).getPayload();
        assertThat(claims.getIssuer()).isEqualTo("jobleaf");
        assertThat(claims.getSubject()).isEqualTo(USER.id().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("admin@example.com");
        assertThat(claims.get("name", String.class)).isEqualTo("Admin");
        assertThat(claims).as("a user without a mobile number has no mobileNumber claim").doesNotContainKey("mobileNumber");
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(now);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(token.expiresAt());
    }

    @Test
    @DisplayName("Should issue tokens that the JWT filter accepts (round trip login -> secured request)")
    void issuedToken_isAcceptedByValidatorFilter() throws Exception {
        // Given
        AuthToken token = new JwtTokenIssuerAdapter(KEY, Duration.ofHours(1)).issue(USER);
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        var filter = new JwtTokenValidatorFilter("Authorization", KEY, List.of(), entryPoint, new AccessTokenCookies("JOBLEAF_ACCESS_TOKEN", true));
        var request = new MockHttpServletRequest("GET", "/jobleaf/companies");
        request.addHeader("Authorization", "Bearer " + token.value());

        // When
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // Then
        verify(entryPoint, never()).commence(any(), any(), any());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("admin@example.com");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }
}
