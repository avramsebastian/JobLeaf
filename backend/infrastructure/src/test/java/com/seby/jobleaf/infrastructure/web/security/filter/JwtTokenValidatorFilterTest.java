package com.seby.jobleaf.infrastructure.web.security.filter;

import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtTokenValidatorFilterTest {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "test-secret-key-for-unit-tests-only-0123456789".getBytes(StandardCharsets.UTF_8));
    private static final SecretKey OTHER_KEY = Keys.hmacShaKeyFor(
            "another-secret-key-that-did-not-sign-the-token".getBytes(StandardCharsets.UTF_8));

    private static final UUID USER_ID = UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02");

    private AuthenticationEntryPoint entryPoint;
    private JwtTokenValidatorFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        entryPoint = mock(AuthenticationEntryPoint.class);
        filter = new JwtTokenValidatorFilter("Authorization", KEY, List.of("/jobleaf/companies/public"), entryPoint,
                new AccessTokenCookies("JOBLEAF_ACCESS_TOKEN", true));
        request = new MockHttpServletRequest("GET", "/jobleaf/companies");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private static String token(SecretKey key, String email, String roles, Instant expiresAt) {
        var builder = Jwts.builder().issuer("jobleaf").subject(USER_ID.toString()).claim("email", email).expiration(Date.from(expiresAt)).signWith(key);
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }

    private static String validToken() {
        return token(KEY, "admin@example.com", "ROLE_ADMIN,ROLE_USER", Instant.now().plusSeconds(600));
    }

    private AuthenticationException rejection() throws Exception {
        ArgumentCaptor<AuthenticationException> captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(entryPoint).commence(any(), any(), captor.capture());
        assertThat(chain.getRequest()).as("request must not continue").isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        return captor.getValue();
    }

    @Test
    @DisplayName("Should authenticate the user with the roles from a valid Bearer token")
    void validToken_authenticatesUser() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer " + validToken());

        // When
        filter.doFilter(request, response, chain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin@example.com");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(MDC.get("userId")).isEqualTo(USER_ID.toString());
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(USER_ID, "admin@example.com"));
        assertThat(request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE))
                .isEqualTo(new AuthenticatedUser(USER_ID, "admin@example.com"));
        assertThat(chain.getRequest()).as("request continues").isNotNull();
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    @DisplayName("Should authenticate with no authorities when the token has no roles claim")
    void tokenWithoutRoles_authenticatesWithoutAuthorities() throws Exception {
        request.addHeader("Authorization", "Bearer " + token(KEY, "madan@example.com", null, Instant.now().plusSeconds(600)));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Should authenticate a browser request that carries the token in the HttpOnly cookie")
    void tokenInCookie_authenticatesUser() throws Exception {
        request.setCookies(new jakarta.servlet.http.Cookie("JOBLEAF_ACCESS_TOKEN", validToken()));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new AuthenticatedUser(USER_ID, "admin@example.com"));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Should reject a tampered token in the cookie on a secured path")
    void tamperedTokenInCookie_isRejected() throws Exception {
        request.setCookies(new jakarta.servlet.http.Cookie("JOBLEAF_ACCESS_TOKEN", validToken() + "x"));

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should let requests without an Authorization header through as anonymous")
    void noHeader_continuesUnauthenticated() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    @DisplayName("Should ignore non-Bearer Authorization headers instead of failing")
    void nonBearerHeader_continuesUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Should reject an expired token with CredentialsExpiredException")
    void expiredToken_isRejected() throws Exception {
        request.addHeader("Authorization",
                "Bearer " + token(KEY, "admin@example.com", "ROLE_ADMIN", Instant.now().minusSeconds(60)));

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(CredentialsExpiredException.class);
    }

    @Test
    @DisplayName("Should reject a token signed with another key")
    void tokenWithWrongSignature_isRejected() throws Exception {
        request.addHeader("Authorization",
                "Bearer " + token(OTHER_KEY, "admin@example.com", "ROLE_ADMIN", Instant.now().plusSeconds(600)));

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject a token whose payload was changed")
    void tamperedToken_isRejected() throws Exception {
        String[] parts = validToken().split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"email\":\"admin@example.com\",\"roles\":\"ROLE_ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer " + parts[0] + "." + forgedPayload + "." + parts[2]);

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject a value that is not a JWT")
    void garbageToken_isRejected() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject a token without an email claim")
    void tokenWithoutEmail_isRejected() throws Exception {
        String token = Jwts.builder().issuer("jobleaf").subject(USER_ID.toString()).claim("roles", "ROLE_ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(KEY).compact();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should identify the user on a public path when the token is valid")
    void publicPath_withValidToken_authenticatesUser() throws Exception {
        MockHttpServletRequest publicRequest = new MockHttpServletRequest("GET", "/jobleaf/companies/public");
        publicRequest.addHeader("Authorization", "Bearer " + validToken());

        filter.doFilter(publicRequest, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new AuthenticatedUser(USER_ID, "admin@example.com"));
        assertThat(MDC.get("userId")).isEqualTo(USER_ID.toString());
    }

    @Test
    @DisplayName("Should ignore a broken token on a public path and continue anonymously instead of answering 401")
    void publicPath_withInvalidToken_continuesAnonymously() throws Exception {
        MockHttpServletRequest publicRequest = new MockHttpServletRequest("GET", "/jobleaf/companies/public");
        publicRequest.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(publicRequest, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    @DisplayName("Should ignore an expired token on a public path, so a stale login cannot break public pages")
    void publicPath_withExpiredToken_continuesAnonymously() throws Exception {
        MockHttpServletRequest publicRequest = new MockHttpServletRequest("GET", "/jobleaf/companies/public");
        publicRequest.addHeader("Authorization",
                "Bearer " + token(KEY, "admin@example.com", "ROLE_ADMIN", Instant.now().minusSeconds(60)));

        filter.doFilter(publicRequest, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject a correctly signed token that was not issued by JobLeaf")
    void tokenFromOtherIssuer_isRejected() throws Exception {
        String token = Jwts.builder().issuer("someone-else").subject(USER_ID.toString()).claim("email", "admin@example.com")
                .claim("roles", "ROLE_ADMIN").expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(KEY).compact();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject a token without a subject (user id)")
    void tokenWithoutSubject_isRejected() throws Exception {
        String token = Jwts.builder().issuer("jobleaf").claim("email", "admin@example.com").claim("roles", "ROLE_ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(KEY).compact();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject a token whose subject is not a user id")
    void tokenWithMalformedSubject_isRejected() throws Exception {
        String token = Jwts.builder().issuer("jobleaf").subject("not-a-uuid").claim("email", "admin@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(KEY).compact();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, chain);

        assertThat(rejection()).isInstanceOf(BadCredentialsException.class);
    }
}
