package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.exceptions.auth.UserAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.model.AuthToken;
import com.seby.jobleaf.domain.model.JobLeafUser;
import com.seby.jobleaf.domain.model.RoleName;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginCommand;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase.LoginResult;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase.RegisterCommand;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import com.seby.jobleaf.infrastructure.web.mapper.CsrfWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.JobLeafEmailPwdAuthenticationProvider;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP requests through security (including CSRF), JSON binding, the controllers and the mappers; only
 * the use cases are mocked.
 * <p>
 * CSRF is done like the frontend does it (double submit: the same value in the XSRF-TOKEN cookie and the
 * X-XSRF-TOKEN header), not with spring-security-test's {@code csrf()}: that helper swaps the repository inside the
 * shared CsrfFilter for the rest of the test context, which would hide the real cookie behaviour.
 */
@WebMvcTest({AuthController.class, CsrfController.class})
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class,
        AuthWebMapper.class, CsrfWebMapper.class, AccessTokenCookies.class})
class AuthControllerWebTest {

    private static final String API_MEDIA_TYPE = "application/vnd.jobleaf+json;v=1.0";
    private static final String LOGIN_URL = "/jobleaf/auth/login/public";
    private static final String REGISTER_URL = "/jobleaf/auth/register/public";
    private static final String CSRF_URL = "/jobleaf/csrf-token/public";
    private static final String REGISTER_BODY = """
            {"name":"Jane Doe","email":"jane@example.com","mobileNumber":"+40712345678","password":"a-Long-unique-pass-42"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    /** Needed by the AuthenticationManager bean; the use cases are mocked, so it is never called here. */
    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    private static RequestPostProcessor csrfToken() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
            return request;
        };
    }

    private static LoginResult result(JobLeafUser user) {
        return new LoginResult(user, new AuthToken("aaa.bbb.ccc", Instant.parse("2026-11-03T13:00:00Z")));
    }

    @Test
    @DisplayName("Should log in without a token and return the user and the JWT")
    void login_withValidCredentials_returns200() throws Exception {
        // Given
        JobLeafUser user = new JobLeafUser(UUID.fromString("6f1c2b7e-3d4a-4e5f-9a8b-1c2d3e4f5a02"), "Admin",
                "admin@example.com", null, RoleName.ROLE_ADMIN, null, null, Instant.parse("2026-11-03T00:00:00Z"));
        when(loginUseCase.login(new LoginCommand("admin@example.com", "secret123"))).thenReturn(result(user));

        // When / Then
        mockMvc.perform(post(LOGIN_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.jwtToken").doesNotExist())
                .andExpect(cookie().value("JOBLEAF_ACCESS_TOKEN", "aaa.bbb.ccc"))
                .andExpect(cookie().httpOnly("JOBLEAF_ACCESS_TOKEN", true))
                .andExpect(cookie().secure("JOBLEAF_ACCESS_TOKEN", true))
                .andExpect(cookie().path("JOBLEAF_ACCESS_TOKEN", "/jobleaf"))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-03T13:00:00Z"))
                .andExpect(jsonPath("$.user.email").value("admin@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Should answer wrong credentials with a 401 problem and the generic message")
    void login_withWrongCredentials_returns401() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should reject an invalid email with a 400 before reaching the use case")
    void login_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(loginUseCase);
    }

    @Test
    @DisplayName("Should reject a POST whose header does not match the CSRF cookie")
    void login_withMismatchedCsrfToken_returns403() throws Exception {
        mockMvc.perform(post(LOGIN_URL).cookie(new Cookie("XSRF-TOKEN", "cookie-value")).header("X-XSRF-TOKEN", "other-value")
                        .contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loginUseCase);
    }

    @Test
    @DisplayName("Should reject a POST without a CSRF token with a 403 problem, before reaching the use case")
    void login_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value(containsString("CSRF token is missing or invalid")));

        verifyNoInteractions(loginUseCase);
    }

    @Test
    @DisplayName("Should register a new user, log them in and answer 201 with the user and the JWT")
    void register_withValidData_returns201() throws Exception {
        // Given
        JobLeafUser user = new JobLeafUser(UUID.randomUUID(), "Jane Doe", "jane@example.com", "+40712345678",
                RoleName.ROLE_JOB_SEEKER, null, null, Instant.parse("2026-11-07T10:00:00Z"));
        when(registerUseCase.register(
                new RegisterCommand("Jane Doe", "jane@example.com", "+40712345678", "a-Long-unique-pass-42")))
                .thenReturn(result(user));

        // When / Then
        mockMvc.perform(post(REGISTER_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.jwtToken").doesNotExist())
                .andExpect(cookie().value("JOBLEAF_ACCESS_TOKEN", "aaa.bbb.ccc"))
                .andExpect(cookie().httpOnly("JOBLEAF_ACCESS_TOKEN", true))
                .andExpect(jsonPath("$.user.role").value("ROLE_JOB_SEEKER"))
                .andExpect(jsonPath("$.user.mobileNumber").value("+40712345678"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Should ignore a role sent in the request: unknown fields are rejected, the role is set by the system")
    void register_withRoleInRequest_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content(REGISTER_BODY.replace("}", ",\"role\":\"ROLE_ADMIN\"}")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registerUseCase);
    }

    @Test
    @DisplayName("Should answer an already registered email with a 409 problem naming the field")
    void register_withExistingEmail_returns409() throws Exception {
        when(registerUseCase.register(any())).thenThrow(UserAlreadyExistsException.email());

        mockMvc.perform(post(REGISTER_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email").value("An account with this email already exists"));
    }

    @Test
    @DisplayName("Should answer a compromised password with a 400 problem on the password field")
    void register_withCompromisedPassword_returns400() throws Exception {
        when(registerUseCase.register(any()))
                .thenThrow(DomainValidationException.invalidField("password", "This password has appeared in a data breach"));

        mockMvc.perform(post(REGISTER_URL).with(csrfToken()).contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content(REGISTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("This password has appeared in a data breach"));
    }

    @Test
    @DisplayName("Should log out by telling the browser to delete the token cookie")
    void logout_clearsTokenCookie() throws Exception {
        mockMvc.perform(post("/jobleaf/auth/logout/public").with(csrfToken()).accept(API_MEDIA_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().maxAge("JOBLEAF_ACCESS_TOKEN", 0))
                .andExpect(cookie().path("JOBLEAF_ACCESS_TOKEN", "/jobleaf"));
    }

    @Test
    @DisplayName("Should return a CSRF token without login and set the same value in the XSRF-TOKEN cookie")
    void csrfToken_returnsTokenAndSetsCookie() throws Exception {
        MvcResult result = mockMvc.perform(get(CSRF_URL).accept(API_MEDIA_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();

        // Double submit: the cookie and the returned token must be the same value
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertThat(result.getResponse().getCookie("XSRF-TOKEN").getValue()).isEqualTo(token);
    }
}
