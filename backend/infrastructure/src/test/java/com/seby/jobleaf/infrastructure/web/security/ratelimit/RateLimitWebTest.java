package com.seby.jobleaf.infrastructure.web.security.ratelimit;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.port.in.auth.LoginUseCase;
import com.seby.jobleaf.domain.port.in.auth.RegisterUseCase;
import com.seby.jobleaf.infrastructure.web.controller.AuthController;
import com.seby.jobleaf.infrastructure.web.mapper.AuthWebMapper;
import com.seby.jobleaf.infrastructure.web.security.AccessTokenCookies;
import com.seby.jobleaf.infrastructure.web.security.JobLeafEmailPwdAuthenticationProvider;
import com.seby.jobleaf.infrastructure.web.security.JobLeafSecurityConfig;
import com.seby.jobleaf.infrastructure.web.security.PathsConfig;
import com.seby.jobleaf.infrastructure.web.security.ProblemDetailSecurityHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The rate limit through the real filter chain: the 6th login within a minute gets a 429 problem with Retry-After,
 * and never reaches the controller.
 */
@WebMvcTest(AuthController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AuthWebMapper.class,
        AccessTokenCookies.class})
@TestPropertySource(properties = "jobleaf.rate-limit.enabled=true")
class RateLimitWebTest {

    private static final String API_MEDIA_TYPE = "application/vnd.jobleaf+json;v=1.0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/jobleaf/auth/login/public")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
                    request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
                    return request;
                })
                .contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                .content("{\"email\":\"admin@example.com\",\"password\":\"wrong-password\"}"));
    }

    @Test
    @DisplayName("Should answer the 6th login per minute from one IP with a 429 problem and Retry-After")
    void sixthLogin_returns429() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        for (int attempt = 1; attempt <= 5; attempt++) {
            login().andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(5 - attempt)));
        }

        login().andExpect(status().isTooManyRequests())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.type").value("https://jobleaf.local/problems/too-many-requests"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Too many requests")));

        verify(loginUseCase, times(5)).login(any());
    }
}
