package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.domain.model.AuthToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Creates, reads and clears the cookie that carries the JWT to and from the browser.
 * <p>
 * {@code HttpOnly}: JavaScript cannot read it, so an XSS cannot steal the token. {@code Secure}: HTTPS only.
 * {@code SameSite=Lax}: not sent on requests started by other sites. {@code Path=/jobleaf}: only sent to the API.
 * Because the browser sends it automatically, every state-changing request is also CSRF protected.
 */
@Component
public class AccessTokenCookies {

    static final String PATH = "/jobleaf";
    static final String SAME_SITE = "Lax";

    private final String name;
    private final boolean secure;
    private final Clock clock;

    @Autowired
    public AccessTokenCookies(@Value("${jobleaf.jwt.cookie.name}") String name,
                              @Value("${jobleaf.jwt.cookie.secure}") boolean secure) {
        this(name, secure, Clock.systemUTC());
    }

    AccessTokenCookies(String name, boolean secure, Clock clock) {
        this.name = name;
        this.secure = secure;
        this.clock = clock;
    }

    /**
     * @return a cookie holding the token, expiring together with it
     */
    public ResponseCookie create(AuthToken token) {
        Duration maxAge = Duration.between(clock.instant(), token.expiresAt());
        return base(token.value()).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    /**
     * @return a cookie with the same name and path that tells the browser to delete it (logout)
     */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE)
                .path(PATH);
    }
}
