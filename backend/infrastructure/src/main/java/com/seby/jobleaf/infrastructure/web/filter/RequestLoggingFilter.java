package com.seby.jobleaf.infrastructure.web.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that logs every HTTP request and response for auditing and tracing (Create, Read, Update, Delete)
 * along with execution time and MDC trace identifiers for Grafana Loki / Tempo correlation.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String USER_ID_MDC_KEY = "userId";
    private static final String USER_ID_HEADER = "X-User-Id"; // TODO: replace with JWT principal once auth lands
    private static final int CLIENT_ERROR_STATUS = 400;
    private static final int SERVER_ERROR_STATUS = 500;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/actuator/health") || uri.startsWith("/actuator/prometheus"))) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String userId = resolveUserId(request);

        MDC.put(USER_ID_MDC_KEY, userId);
        Span span = Span.current();
        span.setAttribute("user.id", userId);
        Throwable failure = null;
        try {
            log.info("Incoming request: [{}] {}{}", method, uri, query);
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            logCompletion(span, failure, method, uri, query, status, duration);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    /**
     * Logs the outcome at a severity that matches the HTTP status and mirrors it on the span.
     * Spans are exported to Tempo independently of the log level, but Tempo only marks a span as
     * failed when its status is set to {@link StatusCode#ERROR}.
     */
    private void logCompletion(Span span, Throwable failure, String method, String uri, String query,
                               int status, long duration) {
        if (failure != null) {
            span.setStatus(StatusCode.ERROR, failure.getMessage());
            log.error("Completed request: [{}] {}{} -> failed [{}ms]", method, uri, query, duration, failure);
        } else if (status >= SERVER_ERROR_STATUS) {
            span.setStatus(StatusCode.ERROR, "HTTP " + status);
            log.error("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        } else if (status >= CLIENT_ERROR_STATUS) {
            span.setStatus(StatusCode.ERROR, "HTTP " + status);
            log.error("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        } else {
            log.info("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        }
    }

    private String resolveUserId(HttpServletRequest request) {
        String headerValue = request.getHeader(USER_ID_HEADER);
        return headerValue != null && !headerValue.isBlank() ? headerValue : "anonymous";
    }
}