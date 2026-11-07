package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.domain.model.AuthToken;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenCookiesTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private final AccessTokenCookies cookies =
            new AccessTokenCookies("JOBLEAF_ACCESS_TOKEN", true, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Should put the token in an HttpOnly, Secure, SameSite=Lax cookie for the API that expires with it")
    void create_buildsProtectedCookie() {
        ResponseCookie cookie = cookies.create(new AuthToken("aaa.bbb.ccc", NOW.plus(Duration.ofHours(1))));

        assertThat(cookie.getName()).isEqualTo("JOBLEAF_ACCESS_TOKEN");
        assertThat(cookie.getValue()).isEqualTo("aaa.bbb.ccc");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/jobleaf");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should build a deleting cookie (same name and path, max age 0) for logout")
    void clear_buildsExpiredCookie() {
        ResponseCookie cookie = cookies.clear();

        assertThat(cookie.getName()).isEqualTo("JOBLEAF_ACCESS_TOKEN");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getPath()).isEqualTo("/jobleaf");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("Should read the token from the request cookies, ignoring other cookies")
    void read_findsTokenCookie() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("XSRF-TOKEN", "csrf"), new Cookie("JOBLEAF_ACCESS_TOKEN", "aaa.bbb.ccc"));

        assertThat(cookies.read(request)).contains("aaa.bbb.ccc");
        assertThat(cookies.read(new MockHttpServletRequest())).isEmpty();
    }
}
