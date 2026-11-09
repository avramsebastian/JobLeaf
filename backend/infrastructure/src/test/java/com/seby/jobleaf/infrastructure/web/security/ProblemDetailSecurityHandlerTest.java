package com.seby.jobleaf.infrastructure.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProblemDetailSecurityHandlerTest {

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private ProblemDetailSecurityHandler securityHandler;

    @Test
    @DisplayName("Should hand authentication failures to the MVC exception resolver")
    void commence_delegatesToExceptionResolver() {
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Full authentication is required");

        securityHandler.commence(request, response, exception);

        verify(handlerExceptionResolver).resolveException(request, response, null, exception);
    }

    @Test
    @DisplayName("Should hand access denials to the MVC exception resolver")
    void handle_delegatesToExceptionResolver() {
        AccessDeniedException exception = new AccessDeniedException("Access Denied");

        securityHandler.handle(request, response, exception);

        verify(handlerExceptionResolver).resolveException(request, response, null, exception);
    }
}
