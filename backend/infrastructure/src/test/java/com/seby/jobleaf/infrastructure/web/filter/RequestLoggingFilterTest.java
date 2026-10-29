package com.seby.jobleaf.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RequestLoggingFilter filter;

    @Test
    @DisplayName("Should log business request and invoke filter chain")
    void shouldLogRequestAndInvokeFilterChain() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/api/v1/companies");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("page=0&size=10");
        when(response.getStatus()).thenReturn(200);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should bypass detailed logging for actuator health endpoints")
    void shouldBypassLoggingForActuatorHealth() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should log completion even when filter chain throws an exception")
    void shouldLogCompletionWhenFilterChainThrows() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/api/v1/contacts");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn(null);
        when(response.getStatus()).thenReturn(500);
        doThrow(new ServletException("Database down")).when(filterChain).doFilter(request, response);

        // When / Then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("Database down");

        verify(filterChain).doFilter(request, response);
    }
}
