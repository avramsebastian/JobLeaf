package com.seby.jobleaf.infrastructure.web.controller;

import com.seby.jobleaf.apis.generated.CsrfApi;
import com.seby.jobleaf.apis.generated.model.CsrfTokenResponse;
import com.seby.jobleaf.infrastructure.web.mapper.CsrfWebMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter implementing generated {@link CsrfApi}.
 * <p>
 * CSRF is purely an HTTP concern, so there is no use case or domain behind this endpoint: Spring Security's
 * {@code CsrfFilter} has already put the token into the request, and reading it makes
 * {@code CookieCsrfTokenRepository} write the {@code XSRF-TOKEN} cookie into this response.
 */
@RestController
@RequiredArgsConstructor
public class CsrfController implements CsrfApi {

    /** Spring injects a proxy that always points at the current request. */
    private final HttpServletRequest request;
    private final CsrfWebMapper csrfWebMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<CsrfTokenResponse> getCSRFToken() {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(csrfWebMapper.toCsrfTokenResponse(csrfToken));
    }
}
