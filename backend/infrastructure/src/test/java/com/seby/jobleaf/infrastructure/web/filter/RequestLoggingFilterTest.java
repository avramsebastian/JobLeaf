package com.seby.jobleaf.infrastructure.web.filter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Test
    @DisplayName("Should mark the current span as failed for server error responses")
    void shouldMarkSpanAsErrorForServerErrorResponses() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(response.getStatus()).thenReturn(503);

        assertThat(spanStatusAfterFilter()).isEqualTo(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should not mark the span as failed for client error responses")
    void shouldKeepSpanStatusUnsetForClientErrors() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/does-not-exist");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(response.getStatus()).thenReturn(404);

        assertThat(spanStatusAfterFilter()).isEqualTo(StatusCode.UNSET);
    }

    private StatusCode spanStatusAfterFilter() throws ServletException, IOException {
        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build()) {
            Span span = tracerProvider.get("test").spanBuilder("request").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                filter.doFilter(request, response, filterChain);
                span.end();
                return ((ReadableSpan) span).toSpanData().getStatus().getStatusCode();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin\" OR 1=1", "user id with spaces", "<script>"})
    @DisplayName("Should fall back to anonymous when the user id header contains unsafe characters")
    void resolveUserId_whenUnsafe_returnsAnonymous(String headerValue) {
        when(request.getHeader("X-User-Id")).thenReturn(headerValue);

        assertThat(filter.resolveUserId(request)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should fall back to anonymous when the user id header is too long")
    void resolveUserId_whenTooLong_returnsAnonymous() {
        when(request.getHeader("X-User-Id")).thenReturn("a".repeat(65));

        assertThat(filter.resolveUserId(request)).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should accept and trim a safe user id")
    void resolveUserId_whenSafe_returnsTrimmedValue() {
        when(request.getHeader("X-User-Id")).thenReturn(" 00000000-0000-0000-0000-000000000001 ");

        assertThat(filter.resolveUserId(request)).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("Should mask the values of sensitive query parameters, case-insensitively")
    void maskQuery_masksSensitiveParameters() {
        assertThat(RequestLoggingFilter.maskQuery("page=0&token=abc123&Email=john@example.com&size=10"))
                .isEqualTo("?page=0&token=***&Email=***&size=10");
    }

    @Test
    @DisplayName("Should keep non-sensitive and value-less parameters unchanged")
    void maskQuery_keepsOtherParameters() {
        assertThat(RequestLoggingFilter.maskQuery("page=0&debug&sort=name")).isEqualTo("?page=0&debug&sort=name");
    }

    @Test
    @DisplayName("Should return an empty string when there is no query")
    void maskQuery_whenNoQuery_returnsEmpty() {
        assertThat(RequestLoggingFilter.maskQuery(null)).isEmpty();
        assertThat(RequestLoggingFilter.maskQuery("")).isEmpty();
    }

    @Test
    @DisplayName("Should expose the user id from the header in the MDC during the request and clear it afterwards")
    void shouldPutUserIdInMdcDuringRequestAndClearItAfterwards() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Id")).thenReturn("user-42");
        when(response.getStatus()).thenReturn(200);
        AtomicReference<String> userIdDuringChain = new AtomicReference<>();
        doAnswer(invocation -> {
            userIdDuringChain.set(MDC.get("userId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(userIdDuringChain.get()).isEqualTo("user-42");
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should fall back to anonymous when the user id header is blank")
    void shouldUseAnonymousWhenUserIdHeaderIsBlank() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Id")).thenReturn("  ");
        when(response.getStatus()).thenReturn(200);
        AtomicReference<String> userIdDuringChain = new AtomicReference<>();
        doAnswer(invocation -> {
            userIdDuringChain.set(MDC.get("userId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(userIdDuringChain.get()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should clear the MDC user id even when the filter chain throws")
    void shouldClearMdcWhenFilterChainThrows() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/contacts");
        when(request.getMethod()).thenReturn("POST");
        when(response.getStatus()).thenReturn(200);
        doThrow(new IllegalStateException("boom")).when(filterChain).doFilter(request, response);

        // When / Then
        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should record the user id on the span and leave the status unset for successful responses")
    void shouldTagSpanWithUserAndKeepStatusUnsetOnSuccess() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Id")).thenReturn("user-42");
        when(response.getStatus()).thenReturn(200);

        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build()) {
            Span span = tracerProvider.get("test").spanBuilder("request").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                // When
                filter.doFilter(request, response, filterChain);

                // Then
                span.end();
                SpanData data = ((ReadableSpan) span).toSpanData();
                assertThat(data.getAttributes().get(AttributeKey.stringKey("user.id"))).isEqualTo("user-42");
                assertThat(data.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
            }
        }
    }

    @Test
    @DisplayName("Should mark the span as failed with the exception message when the filter chain throws")
    void shouldMarkSpanAsErrorWhenFilterChainThrows() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/contacts");
        when(request.getMethod()).thenReturn("POST");
        when(response.getStatus()).thenReturn(200);
        doThrow(new ServletException("Database down")).when(filterChain).doFilter(request, response);

        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build()) {
            Span span = tracerProvider.get("test").spanBuilder("request").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                // When
                assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                        .isInstanceOf(ServletException.class);

                // Then
                span.end();
                SpanData data = ((ReadableSpan) span).toSpanData();
                assertThat(data.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
                assertThat(data.getStatus().getDescription()).isEqualTo("Database down");
            }
        }
    }

    @Test
    @DisplayName("Should bypass logging and MDC for the Prometheus scrape endpoint")
    void shouldBypassPrometheusEndpoint() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/prometheus");
        AtomicReference<String> userIdDuringChain = new AtomicReference<>("not-called");
        doAnswer(invocation -> {
            userIdDuringChain.set(MDC.get("userId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(userIdDuringChain.get()).isNull();
        verify(request, never()).getHeader("X-User-Id");
    }
}
