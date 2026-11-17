package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.infrastructure.web.security.captcha.CaptchaFilter;
import com.seby.jobleaf.infrastructure.web.security.captcha.TurnstileCaptchaVerifier;
import com.seby.jobleaf.infrastructure.web.security.filter.JwtTokenValidatorFilter;
import com.seby.jobleaf.infrastructure.web.security.ratelimit.RateLimitFilter;
import com.seby.jobleaf.infrastructure.web.security.ratelimit.RateLimitRule;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Stateless REST security: no login page, no HTTP Basic, no session. Unauthenticated and forbidden requests get
 * the same {@code application/problem+json} responses as every other error.
 */
@Configuration
@EnableWebSecurity
public class JobLeafSecurityConfig {

    /**
     * Only resources from this server may load, nothing may embed the responses in a frame. Strict enough for the
     * JSON API, while still letting Swagger UI load its own scripts and inline styles.
     */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    /** Browser features the application never uses, so injected scripts cannot use them either. */
    static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    /**
     * Key used to sign and verify (filter) JWTs.
     */
    @Bean
    public SecretKey jwtSigningKey(@Value("${jobleaf.jwt.secret}") String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    SecurityFilterChain customSecurityFilterChain(HttpSecurity http,
                                                  @Qualifier("publicPaths") List<String> publicPaths,
                                                  @Qualifier("adminPaths") List<String> adminPaths,
                                                  @Qualifier("securedPaths") List<String> securedPaths,
                                                  @Qualifier("infrastructurePaths") List<String> infrastructurePaths,
                                                  ProblemDetailSecurityHandler problemDetailSecurityHandler,
                                                  SecretKey jwtSigningKey,
                                                  AccessTokenCookies accessTokenCookies,
                                                  @Value("${jobleaf.jwt.header:Authorization}") String jwtHeader,
                                                  @Value("${jobleaf.rate-limit.enabled:true}") boolean rateLimitEnabled,
                                                  @Value("${jobleaf.captcha.enabled:true}") boolean captchaEnabled,
                                                  @Value("${jobleaf.captcha.secret:}") String captchaSecret,
                                                  @Qualifier("handlerExceptionResolver")
                                                  HandlerExceptionResolver handlerExceptionResolver) {
        CaptchaFilter captchaFilter = captchaFilter(captchaEnabled, captchaSecret, handlerExceptionResolver);
        return http.csrf(csrfConfig -> csrfConfig
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> {
                    requests.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll();
                    infrastructurePaths.forEach(path -> requests.requestMatchers(path).permitAll());
                    publicPaths.forEach(path -> requests.requestMatchers(path).permitAll());
                    adminPaths.forEach(path -> requests.requestMatchers(path).hasRole("ADMIN"));
                    securedPaths.forEach(path -> requests.requestMatchers(path).authenticated());
                    requests.anyRequest().denyAll();
                })
                // Spring already sends X-Content-Type-Options, X-Frame-Options, Cache-Control and HSTS (on HTTPS)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY)))
                .addFilterBefore(new RateLimitFilter(rateLimitEnabled ? RateLimitRule.DEFAULTS : List.of(),
                        handlerExceptionResolver), CsrfFilter.class)
                .addFilterAfter(captchaFilter, CsrfFilter.class)
                .addFilterBefore(new JwtTokenValidatorFilter(jwtHeader, jwtSigningKey, publicPaths,
                        problemDetailSecurityHandler, accessTokenCookies), BasicAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemDetailSecurityHandler)
                        .accessDeniedHandler(problemDetailSecurityHandler))
                .build();
    }

    /**
     * Turnstile check on login and register. Off (no protected paths) when {@code jobleaf.captcha.enabled=false},
     * e.g. in tests; when on, a missing secret stops the application from starting.
     */
    private static CaptchaFilter captchaFilter(boolean enabled, String secret, HandlerExceptionResolver resolver) {
        if (!enabled) {
            return new CaptchaFilter(List.of(), (token, ip) -> true, resolver);
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jobleaf.captcha.secret (TURNSTILE_SECRET_KEY) is not set");
        }
        var verifier = new TurnstileCaptchaVerifier(secret);
        return new CaptchaFilter(CaptchaFilter.PROTECTED_PATHS, verifier::verify, resolver);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Checks email + password for the login with JobLeaf's own provider, which reads the {@code users} table.
     */
    @Bean
    public AuthenticationManager authenticationManager(JobLeafEmailPwdAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }
}
