package com.seby.jobleaf.infrastructure.web.security;

import com.seby.jobleaf.domain.port.in.company.ReadCompanyUseCase;
import com.seby.jobleaf.infrastructure.web.controller.CompanyController;
import com.seby.jobleaf.infrastructure.web.mapper.CompanyWebMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs requests through the real security filter chain (no database) to check access rules and security headers.
 */
@WebMvcTest(CompanyController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class})
class SecurityHeadersTest {

    private static final String API_ACCEPT = "application/vnd.jobleaf+json;v=1.0";

    @Autowired
    private MockMvc mockMvc;

    /** Needed by the AuthenticationManager bean; the login itself is not exercised here. */
    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private ReadCompanyUseCase readCompanyUseCase;

    @MockitoBean
    private CompanyWebMapper companyWebMapper;

    private static void assertSecurityHeaders(ResultActions result) throws Exception {
        result.andExpect(header().string("Content-Security-Policy", JobLeafSecurityConfig.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", JobLeafSecurityConfig.PERMISSIONS_POLICY))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Should serve public endpoints without authentication and with the security headers")
    void publicEndpoint_returnsOkWithSecurityHeaders() throws Exception {
        when(readCompanyUseCase.listAllCompanies()).thenReturn(List.of());
        when(companyWebMapper.toListCompaniesResponse(List.of())).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get("/jobleaf/companies/public").accept(API_ACCEPT))
                .andExpect(status().isOk());

        assertSecurityHeaders(result);
    }

    @Test
    @DisplayName("Should answer secured endpoints without credentials with a 401 problem and the security headers")
    void securedEndpoint_withoutCredentials_returns401Problem() throws Exception {
        ResultActions result = mockMvc.perform(get("/jobleaf/companies").accept(API_ACCEPT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://jobleaf.local/problems/unauthorized"));

        assertSecurityHeaders(result);
    }

    @Autowired
    private SecretKey jwtSigningKey;

    private String token(Instant expiresAt) {
        return Jwts.builder().issuer("jobleaf").subject("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02").claim("email", "admin@example.com").claim("roles", "ROLE_ADMIN")
                .expiration(Date.from(expiresAt)).signWith(jwtSigningKey).compact();
    }

    @Test
    @DisplayName("Should let a request with a valid JWT past security (404 because no secured endpoint exists yet)")
    void securedPath_withValidToken_passesSecurity() throws Exception {
        mockMvc.perform(get("/jobleaf/companies").accept(API_ACCEPT)
                        .header("Authorization", "Bearer " + token(Instant.now().plusSeconds(600))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should answer an expired JWT with a 401 problem saying it expired")
    void securedPath_withExpiredToken_returns401Problem() throws Exception {
        mockMvc.perform(get("/jobleaf/companies").accept(API_ACCEPT)
                        .header("Authorization", "Bearer " + token(Instant.now().minusSeconds(60))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value("The access token has expired, please log in again"));
    }

    @Test
    @DisplayName("Should answer a tampered JWT with a 401 problem, not a 500")
    void securedPath_withInvalidToken_returns401Problem() throws Exception {
        mockMvc.perform(get("/jobleaf/companies").accept(API_ACCEPT)
                        .header("Authorization", "Bearer " + token(Instant.now().plusSeconds(600)) + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("The access token is invalid"));
    }

    @Test
    @DisplayName("Should not delete the CSRF cookie on authenticated requests (stateless: no per-request rotation)")
    void authenticatedRequest_keepsCsrfCookie() throws Exception {
        when(readCompanyUseCase.listAllCompanies()).thenReturn(List.of());
        when(companyWebMapper.toListCompaniesResponse(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/jobleaf/companies/public").accept(API_ACCEPT)
                        .cookie(new jakarta.servlet.http.Cookie("JOBLEAF_ACCESS_TOKEN", token(Instant.now().plusSeconds(600))),
                                new jakarta.servlet.http.Cookie("XSRF-TOKEN", "csrf-value")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("Should deny paths outside the API that are not explicitly opened")
    void unlistedPath_isDenied() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
