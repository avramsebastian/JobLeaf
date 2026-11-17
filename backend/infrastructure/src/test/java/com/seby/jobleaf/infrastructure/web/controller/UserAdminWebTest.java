package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserNotFoundException;
import com.seby.jobleaf.domain.exceptions.user.UserRoleConflictException;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.user.AssignCompanyToEmployerUseCase;
import com.seby.jobleaf.domain.port.in.user.ElevateToEmployerUseCase;
import com.seby.jobleaf.domain.port.in.user.SearchUserUseCase;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.JobLeafEmailPwdAuthenticationProvider;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin user endpoints through the real security chain and mapper.
 */
@WebMvcTest(UserController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AccessTokenCookies.class,
        AuthWebMapper.class})
class UserAdminWebTest {

    private static final String API = "application/vnd.jobleaf+json;v=1.0";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COMPANY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @MockitoBean
    private SearchUserUseCase searchUserUseCase;

    @MockitoBean
    private ElevateToEmployerUseCase elevateToEmployerUseCase;

    @MockitoBean
    private AssignCompanyToEmployerUseCase assignCompanyToEmployerUseCase;

    private String bearer(String role) {
        return "Bearer " + Jwts.builder().issuer("jobleaf").subject(UUID.randomUUID().toString())
                .claim("email", "someone@example.com").claim("roles", role)
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(jwtSigningKey).compact();
    }

    private RequestPostProcessor as(String role) {
        return request -> {
            request.addHeader("Authorization", bearer(role));
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    private static JobLeafUser user(RoleName role, UUID companyId, String companyName) {
        return new JobLeafUser(USER_ID, "Jane Doe", "jane@example.com", "+40712345678", role, companyId, companyName,
                Instant.parse("2026-11-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Should only let admins search users")
    void search_nonAdmin_rejected() throws Exception {
        mockMvc.perform(get("/jobleaf/users/search/admin").param("email", "jane@example.com").accept(API))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/jobleaf/users/search/admin").param("email", "jane@example.com").accept(API)
                        .with(as("ROLE_EMPLOYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should find a user by email and return the user fields")
    void search_found() throws Exception {
        when(searchUserUseCase.searchByEmail("jane@example.com")).thenReturn(user(RoleName.ROLE_JOB_SEEKER, null, null));

        mockMvc.perform(get("/jobleaf/users/search/admin").param("email", "jane@example.com").accept(API)
                        .with(as("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.role").value("ROLE_JOB_SEEKER"))
                .andExpect(jsonPath("$.mobileNumber").value("+40712345678"));
    }

    @Test
    @DisplayName("Should answer an unknown email with 404 and an invalid one with 400")
    void search_notFoundAndInvalid() throws Exception {
        when(searchUserUseCase.searchByEmail("nobody@example.com"))
                .thenThrow(UserNotFoundException.byEmail("nobody@example.com"));

        mockMvc.perform(get("/jobleaf/users/search/admin").param("email", "nobody@example.com").accept(API)
                        .with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/jobleaf/users/search/admin").param("email", "not-an-email").accept(API)
                        .with(as("ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
        verify(searchUserUseCase, never()).searchByEmail("not-an-email");
    }

    @Test
    @DisplayName("Should elevate a user and answer 409 for an admin")
    void elevate() throws Exception {
        when(elevateToEmployerUseCase.elevateToEmployer(USER_ID)).thenReturn(user(RoleName.ROLE_EMPLOYER, null, null));
        mockMvc.perform(patch("/jobleaf/users/{userId}/role/employer/admin", USER_ID).accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYER"));

        UUID adminId = UUID.randomUUID();
        when(elevateToEmployerUseCase.elevateToEmployer(adminId))
                .thenThrow(UserRoleConflictException.adminCanNotBecomeEmployer(adminId));
        mockMvc.perform(patch("/jobleaf/users/{userId}/role/employer/admin", adminId).accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User " + adminId + " is an admin and can not be changed to employer"));
    }

    @Test
    @DisplayName("Should reject the PATCH without the CSRF token")
    void elevate_withoutCsrf_rejected() throws Exception {
        mockMvc.perform(patch("/jobleaf/users/{userId}/role/employer/admin", USER_ID).accept(API)
                        .header("Authorization", bearer("ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        verify(elevateToEmployerUseCase, never()).elevateToEmployer(any());
    }

    @Test
    @DisplayName("Should assign a company, answer 409 for a non-employer and 404 for an unknown company")
    void assignCompany() throws Exception {
        String url = "/jobleaf/users/{userId}/company/{companyId}/admin";
        when(assignCompanyToEmployerUseCase.assignCompany(USER_ID, COMPANY_ID))
                .thenReturn(user(RoleName.ROLE_EMPLOYER, COMPANY_ID, "DerpCorp"));
        mockMvc.perform(patch(url, USER_ID, COMPANY_ID).accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.companyName").value("DerpCorp"));

        UUID seeker = UUID.randomUUID();
        when(assignCompanyToEmployerUseCase.assignCompany(seeker, COMPANY_ID))
                .thenThrow(UserRoleConflictException.notAnEmployer(seeker, RoleName.ROLE_JOB_SEEKER));
        mockMvc.perform(patch(url, seeker, COMPANY_ID).accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isConflict());

        UUID unknownCompany = UUID.randomUUID();
        when(assignCompanyToEmployerUseCase.assignCompany(USER_ID, unknownCompany))
                .thenThrow(new CompanyNotFoundException("Company with id " + unknownCompany + " not found"));
        mockMvc.perform(patch(url, USER_ID, unknownCompany).accept(API).with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }
}
