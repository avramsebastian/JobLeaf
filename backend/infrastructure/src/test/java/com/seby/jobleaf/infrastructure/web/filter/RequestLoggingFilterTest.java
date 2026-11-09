package com.seby.jobleaf.infrastructure.web.filter;

import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(UUID.randomUUID(), "admin@example.com");

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
        when(request.getRequestURI()).thenReturn("/jobleaf/companies/public");
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

    @Test
    @DisplayName("Should use the id of the user authenticated by the JWT filter")
    void resolveUserId_whenAuthenticated_returnsUserId() {
        when(request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE)).thenReturn(USER);

        assertThat(RequestLoggingFilter.resolveUserId(request)).isEqualTo(USER.id().toString());
    }

    @Test
    @DisplayName("Should be anonymous when no user was authenticated")
    void resolveUserId_whenNotAuthenticated_returnsAnonymous() {
        assertThat(RequestLoggingFilter.resolveUserId(request)).isEqualTo("anonymous");
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
    @DisplayName("Should log as anonymous until the user is authenticated and clear the MDC afterwards")
    void shouldStartAnonymousAndClearMdcAfterwards() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies/public");
        when(request.getMethod()).thenReturn("GET");
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
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should ignore a client-supplied X-User-Id header, which anyone could fake")
    void shouldNotTrustUserIdHeader() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/jobleaf/companies/public");
        when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getHeader("X-User-Id")).thenReturn("admin");
        when(response.getStatus()).thenReturn(200);

        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build()) {
            Span span = tracerProvider.get("test").spanBuilder("request").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                // When
                filter.doFilter(request, response, filterChain);

                // Then
                span.end();
                assertThat(((ReadableSpan) span).toSpanData().getAttributes().get(AttributeKey.stringKey("user.id")))
                        .isEqualTo("anonymous");
            }
        }
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
        when(request.getRequestURI()).thenReturn("/jobleaf/companies/public");
        when(request.getMethod()).thenReturn("GET");
        // lenient: OncePerRequestFilter also reads its own "already filtered" attribute from the same mock
        lenient().when(request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE)).thenReturn(USER);
        when(response.getStatus()).thenReturn(200);

        try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build()) {
            Span span = tracerProvider.get("test").spanBuilder("request").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                // When
                filter.doFilter(request, response, filterChain);

                // Then
                span.end();
                SpanData data = ((ReadableSpan) span).toSpanData();
                assertThat(data.getAttributes().get(AttributeKey.stringKey("user.id"))).isEqualTo(USER.id().toString());
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
        verify(request, never()).getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
    }
}
