package com.seby.jobleaf.infrastructure.web.security.filter;

import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
import com.seby.jobleaf.infrastructure.web.security.jwt.JwtClaimNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies the signature and expiry, and puts the user into the
 * {@code SecurityContext}. Requests without a Bearer token pass through as anonymous, so the authorization rules
 * decide whether they are allowed.
 * <p>
 * Created with {@code new} in the security config on purpose: as a Spring bean it would also be registered as a
 * regular servlet filter and run twice.
 */
@Slf4j
public class JwtTokenValidatorFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_MDC_KEY = "userId";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String headerName;
    private final SecretKey signingKey;
    private final List<String> publicPaths;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtTokenValidatorFilter(String headerName, SecretKey signingKey, List<String> publicPaths,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.headerName = headerName;
        this.signingKey = signingKey;
        this.publicPaths = publicPaths;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(headerName);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(JwtClaimNames.ISSUER)
                    .build()
                    .parseSignedClaims(authHeader.substring(BEARER_PREFIX.length()).trim())
                    .getPayload();

            authenticate(request, claims);
        } catch (ExpiredJwtException exception) {
            reject(request, response, new CredentialsExpiredException("JWT expired", exception));
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Rejected JWT: {}", exception.getMessage());
            reject(request, response, new BadCredentialsException("Invalid JWT", exception));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        String email = claims.get(JwtClaimNames.EMAIL, String.class);
        if (email == null || email.isBlank()) {
            throw new JwtException("JWT has no " + JwtClaimNames.EMAIL + " claim");
        }
        if (claims.getSubject() == null) {
            throw new JwtException("JWT has no subject (user id)");
        }

        var user = new AuthenticatedUser(UUID.fromString(claims.getSubject()), email);
        String roles = claims.get(JwtClaimNames.ROLES, String.class);

        var authentication = new UsernamePasswordAuthenticationToken(user, null,
                AuthorityUtils.commaSeparatedStringToAuthorityList(roles == null ? "" : roles));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);

        MDC.put(USER_ID_MDC_KEY, user.id().toString());
    }

    /**
     * The filter runs before {@code ExceptionTranslationFilter}, so a thrown exception would become a 500.
     * Calling the entry point directly gives the same 401 problem response as every other authentication failure.
     */
    private void reject(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, exception);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return publicPaths.stream().anyMatch(publicPath -> pathMatcher.match(publicPath, path));
    }
}
