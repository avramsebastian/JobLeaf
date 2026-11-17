package com.seby.jobleaf.infrastructure.web.security.captcha;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Requires a valid Turnstile token in the {@value #TOKEN_HEADER} header for the POSTs in {@link #PROTECTED_PATHS}.
 */
public class CaptchaFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Captcha-Token";
    public static final List<String> PROTECTED_PATHS = List.of(
            "/jobleaf/auth/login/public",
            "/jobleaf/auth/register/public"
    );

    private final List<String> protectedPaths;
    /** (token, client IP) → valid? Normally {@link TurnstileCaptchaVerifier#verify}, a lambda in tests. */
    private final BiPredicate<String, String> verifier;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public CaptchaFilter(List<String> protectedPaths, BiPredicate<String, String> verifier,
                         HandlerExceptionResolver handlerExceptionResolver) {
        this.protectedPaths = List.copyOf(protectedPaths);
        this.verifier = verifier;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !protectedPaths.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        // Missing token: answer right away, no need to ask Cloudflare
        if (token == null || token.isBlank() || !verifier.test(token, request.getRemoteAddr())) {
            handlerExceptionResolver.resolveException(request, response, null, new CaptchaVerificationException());
            return;
        }
        chain.doFilter(request, response);
    }
}
