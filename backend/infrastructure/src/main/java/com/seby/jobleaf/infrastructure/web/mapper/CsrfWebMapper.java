package com.seby.jobleaf.infrastructure.web.mapper;

import com.seby.jobleaf.apis.generated.model.CsrfTokenResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

/**
 * Maps Spring Security's {@link CsrfToken} to the OpenAPI CSRF DTO.
 */
@Component
public class CsrfWebMapper {

    /**
     * Calling {@link CsrfToken#getToken()} loads the (deferred) token, which is what makes the cookie appear.
     */
    public CsrfTokenResponse toCsrfTokenResponse(CsrfToken csrfToken) {
        var response = new CsrfTokenResponse();
        response.setToken(csrfToken.getToken());
        response.setHeaderName(csrfToken.getHeaderName());
        return response;
    }
}
