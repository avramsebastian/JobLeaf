package com.seby.jobleaf.infrastructure.web.security.captcha;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * Asks Cloudflare whether a Turnstile token (made by the widget in the browser) is valid.
 * <p>
 * Tokens are single-use and expire after 5 minutes; Cloudflare enforces both, so a replayed token fails here.
 */
@Slf4j
public class TurnstileCaptchaVerifier {

    static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String secret;
    private final RestClient restClient;

    public TurnstileCaptchaVerifier(String secret) {
        this(secret, RestClient.builder().requestFactory(timeouts()));
    }

    TurnstileCaptchaVerifier(String secret, RestClient.Builder restClientBuilder) {
        this.secret = secret;
        this.restClient = restClientBuilder.build();
    }

    /**
     * @return true only if Cloudflare confirms the token. Network problems count as "not verified" (fail closed):
     * if Cloudflare is unreachable nobody can log in, but a bot can never get in by breaking the check.
     */
    public boolean verify(String token, String remoteIp) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("secret", secret);
        form.add("response", token);
        form.add("remoteip", remoteIp);
        try {
            SiteverifyResponse response = restClient.post()
                    .uri(SITEVERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteverifyResponse.class);
            if (response == null || !response.success()) {
                log.info("Captcha rejected for {}: {}", remoteIp, response == null ? "empty answer" : response.errorCodes());
                return false;
            }
            return true;
        } catch (RestClientException e) {
            log.error("Captcha verification failed, Cloudflare not reachable", e);
            return false;
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }

    record SiteverifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
