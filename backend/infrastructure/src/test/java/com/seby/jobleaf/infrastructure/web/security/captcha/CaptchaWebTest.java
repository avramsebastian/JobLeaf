package com.seby.jobleaf.infrastructure.web.security.captcha;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The captcha filter wired into the real security chain. Only the "no token" case: it is answered before any call
 * to Cloudflare, so the test needs no network.
 */
@WebMvcTest(AuthController.class)
@Import({JobLeafSecurityConfig.class, PathsConfig.class, ProblemDetailSecurityHandler.class, AuthWebMapper.class,
        AccessTokenCookies.class})
@TestPropertySource(properties = {"jobleaf.captcha.enabled=true", "jobleaf.captcha.secret=unused-in-this-test"})
class CaptchaWebTest {

    private static final String API_MEDIA_TYPE = "application/vnd.jobleaf+json;v=1.0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private JobLeafEmailPwdAuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("Should answer a login without captcha token with a 400 problem and never try the login")
    void loginWithoutCaptcha_returns400() throws Exception {
        mockMvc.perform(post("/jobleaf/auth/login/public")
                        .with(request -> {
                            request.setCookies(new Cookie("XSRF-TOKEN", "test-csrf-token"));
                            request.addHeader("X-XSRF-TOKEN", "test-csrf-token");
                            return request;
                        })
                        .contentType(API_MEDIA_TYPE).accept(API_MEDIA_TYPE)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"some-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Please confirm that you are human and try again."))
                .andExpect(jsonPath("$.errors").doesNotExist());

        verify(loginUseCase, never()).login(any());
    }
}
