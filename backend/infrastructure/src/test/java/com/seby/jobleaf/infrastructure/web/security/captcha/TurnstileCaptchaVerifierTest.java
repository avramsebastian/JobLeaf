package com.seby.jobleaf.infrastructure.web.security.captcha;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Talks to a fake Cloudflare ({@link MockRestServiceServer}) instead of the real one.
 */
class TurnstileCaptchaVerifierTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer cloudflare = MockRestServiceServer.bindTo(builder).build();
    private final TurnstileCaptchaVerifier verifier = new TurnstileCaptchaVerifier("the-secret", builder);

    @Test
    @DisplayName("Should send secret, token and IP as a form and accept success=true")
    void verify_success() {
        cloudflare.expect(requestTo(TurnstileCaptchaVerifier.SITEVERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "secret", "the-secret", "response", "token-123", "remoteip", "10.0.0.1")))
                .andRespond(withSuccess("{\"success\":true,\"error-codes\":[]}", MediaType.APPLICATION_JSON));

        assertThat(verifier.verify("token-123", "10.0.0.1")).isTrue();
        cloudflare.verify();
    }

    @Test
    @DisplayName("Should refuse when Cloudflare says success=false (e.g. a reused token)")
    void verify_rejected() {
        cloudflare.expect(requestTo(TurnstileCaptchaVerifier.SITEVERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
                        MediaType.APPLICATION_JSON));

        assertThat(verifier.verify("used-token", "10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("Should refuse (fail closed) when Cloudflare cannot be reached")
    void verify_cloudflareDown() {
        cloudflare.expect(requestTo(TurnstileCaptchaVerifier.SITEVERIFY_URL)).andRespond(withServerError());

        assertThat(verifier.verify("token-123", "10.0.0.1")).isFalse();
    }
}
