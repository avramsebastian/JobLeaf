package com.seby.jobleaf.infrastructure.web.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private static final String LOGIN = "/jobleaf/auth/login/public";

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final RateLimitFilter filter = new RateLimitFilter(
            List.of(new RateLimitRule("POST", LOGIN, 2, Duration.ofMinutes(1))), resolver);

    /** @return the response; {@code passed[0]} tells whether the request reached the next filter */
    private MockHttpServletResponse send(String method, String path, String ip, boolean[] passed) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        passed[0] = chain.getRequest() != null;
        return response;
    }

    @Test
    @DisplayName("Should let requests through until the bucket is empty, then reject with retry-after")
    void limitedEndpoint_rejectsWhenBucketIsEmpty() throws Exception {
        boolean[] passed = new boolean[1];

        assertThat(send("POST", LOGIN, "10.0.0.1", passed).getHeader("X-RateLimit-Remaining")).isEqualTo("1");
        assertThat(passed[0]).isTrue();
        assertThat(send("POST", LOGIN, "10.0.0.1", passed).getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(passed[0]).isTrue();

        send("POST", LOGIN, "10.0.0.1", passed);

        assertThat(passed[0]).as("third request stops at the filter").isFalse();
        var exception = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), exception.capture());
        assertThat(exception.getValue()).isInstanceOf(RateLimitExceededException.class);
        // capacity 2 per minute refills one token every 30 seconds
        assertThat(((RateLimitExceededException) exception.getValue()).getRetryAfterSeconds()).isBetween(1L, 30L);
    }

    @Test
    @DisplayName("Should count every IP separately")
    void otherIp_hasItsOwnBucket() throws Exception {
        boolean[] passed = new boolean[1];
        send("POST", LOGIN, "10.0.0.1", passed);
        send("POST", LOGIN, "10.0.0.1", passed);

        send("POST", LOGIN, "10.0.0.2", passed);

        assertThat(passed[0]).isTrue();
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should not limit other paths or other methods on the same path")
    void unlistedRequests_areNotLimited() throws Exception {
        boolean[] passed = new boolean[1];
        for (int i = 0; i < 5; i++) {
            send("GET", "/jobleaf/companies/public", "10.0.0.1", passed);
            assertThat(passed[0]).isTrue();
            send("OPTIONS", LOGIN, "10.0.0.1", passed);
            assertThat(passed[0]).as("CORS preflights must never be counted").isTrue();
        }
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }
}
