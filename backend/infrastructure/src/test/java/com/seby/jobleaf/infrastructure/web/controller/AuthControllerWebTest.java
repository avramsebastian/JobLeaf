package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.UserAccount;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.infrastructure.persistence.adapter.InMemoryUserAccountAdapter;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP requests through security, JSON binding, the controller and the mapper; only the use case is mocked.
 */
@WebMvcTest(AuthController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class,
        InMemoryUserAccountAdapter.class, AuthWebMapper.class})
class AuthControllerWebTest {

    private static final String API_MEDIA_TYPE = "application/vnd.jobleaf+json;v=1.0";
    private static final String LOGIN_URL = "/jobleaf/auth/login/public";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Test
    @DisplayName("Should log in without a token and return the user and the JWT")
    void login_withValidCredentials_returns200() throws Exception {
        // Given
        UserAccount user = new UserAccount(UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02"), "Admin",
                "admin@example.com", null, "ROLE_ADMIN", null, null, Instant.parse("2026-11-03T00:00:00Z"));
        when(loginUseCase.login(new LoginCommand("admin@example.com", "secret123")))
                .thenReturn(new LoginResult(user, new AuthToken("aaa.bbb.ccc", Instant.parse("2026-11-03T13:00:00Z"))));

        // When / Then
        mockMvc.perform(post(LOGIN_URL).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.jwtToken").value("aaa.bbb.ccc"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-03T13:00:00Z"))
                .andExpect(jsonPath("$.user.email").value("admin@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Should answer wrong credentials with a 401 problem and the generic message")
    void login_withWrongCredentials_returns401() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should reject an invalid email with a 400 before reaching the use case")
    void login_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(loginUseCase);
    }
}
