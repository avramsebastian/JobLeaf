package com.seby.jobleaf.infrastructure.web.security.captcha;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CaptchaFilterTest {

    private static final String LOGIN = "/jobleaf/auth/login/public";

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final List<String> verifiedTokens = new ArrayList<>();
    /** Accepts only the token "good" and remembers what it was asked. */
    private final CaptchaFilter filter = new CaptchaFilter(CaptchaFilter.PROTECTED_PATHS, (token, ip) -> {
        verifiedTokens.add(token);
        return "good".equals(token);
    }, resolver);

    private boolean send(String method, String path, String token) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        if (token != null) {
            request.addHeader(CaptchaFilter.TOKEN_HEADER, token);
        }
        var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain.getRequest() != null;
    }

    @Test
    @DisplayName("Should let a login with a verified token through")
    void validToken_passes() throws Exception {
        assertThat(send("POST", LOGIN, "good")).isTrue();
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject a token the verifier does not accept")
    void invalidToken_rejected() throws Exception {
        assertThat(send("POST", "/jobleaf/auth/register/public", "forged")).isFalse();
        verify(resolver).resolveException(any(), any(), isNull(), isA(CaptchaVerificationException.class));
    }

    @Test
    @DisplayName("Should reject a missing token without calling the verifier")
    void missingToken_rejectedWithoutVerifierCall() throws Exception {
        assertThat(send("POST", LOGIN, null)).isFalse();
        assertThat(verifiedTokens).isEmpty();
        verify(resolver).resolveException(any(), any(), isNull(), isA(CaptchaVerificationException.class));
    }

    @Test
    @DisplayName("Should not check other paths, or other methods on the protected paths")
    void otherRequests_notChecked() throws Exception {
        assertThat(send("POST", "/jobleaf/contacts/public", null)).isTrue();
        assertThat(send("OPTIONS", LOGIN, null)).isTrue();
        assertThat(verifiedTokens).isEmpty();
    }
}
