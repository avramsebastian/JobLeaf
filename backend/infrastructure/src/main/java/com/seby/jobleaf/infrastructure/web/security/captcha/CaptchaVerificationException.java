package com.seby.jobleaf.infrastructure.web.security.captcha;

/**
 * The captcha token was missing or Cloudflare did not accept it; answered with 400.
 */
public class CaptchaVerificationException extends RuntimeException {

    public CaptchaVerificationException() {
        super("Please confirm that you are human and try again.");
    }
}
