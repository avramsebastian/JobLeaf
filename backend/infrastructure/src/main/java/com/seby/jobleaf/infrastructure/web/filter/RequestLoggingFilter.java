package com.seby.jobleaf.infrastructure.web.filter;

import com.seby.jobleaf.infrastructure.web.security.AuthenticatedUser;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Filter that logs every HTTP request and response for auditing and tracing (Create, Read, Update, Delete)
 * along with execution time and MDC trace identifiers for Grafana Loki / Tempo correlation.
 * <p>
 * Severity follows the outcome: 2xx/3xx at INFO, 4xx (client mistakes) at WARN, 5xx and exceptions at ERROR.
 * Only server failures mark the span as failed, following the OpenTelemetry HTTP server conventions.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    static final String USER_ID_MDC_KEY = "userId";
    static final String USER_ID_SPAN_ATTRIBUTE = "user.id";
    static final String ANONYMOUS = "anonymous";
    static final String MASK = "***";
    private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
            "token", "access_token", "refresh_token", "id_token", "code", "password", "secret",
            "api_key", "apikey", "key", "email");
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

        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String query = maskQuery(request.getQueryString());

        MDC.put(USER_ID_MDC_KEY, ANONYMOUS);
        Span span = Span.current();
        Throwable failure = null;
        try {
            log.debug("Incoming request: [{}] {}{}", method, uri, query);
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            String userId = resolveUserId(request);
            MDC.put(USER_ID_MDC_KEY, userId);
            span.setAttribute(USER_ID_SPAN_ATTRIBUTE, userId);
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            int status = response.getStatus();
            logCompletion(span, failure, method, uri, query, status, duration);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    /**
     * Logs the outcome at a severity that matches the HTTP status and mirrors server failures on the span.
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
            log.warn("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        } else {
            log.info("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        }
    }

    /**
     * @return the id of the user the JWT filter authenticated, or {@value #ANONYMOUS}. Only server-validated data is
     * used, never a client-supplied header.
     */
    static String resolveUserId(HttpServletRequest request) {
        return request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) instanceof AuthenticatedUser user
                ? user.id().toString()
                : ANONYMOUS;
    }

    /**
     * @return the query string prefixed with {@code ?}, with the values of sensitive parameters replaced by
     * {@value #MASK}, or an empty string when there is no query
     */
    static String maskQuery(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        return "?" + Arrays.stream(queryString.split("&"))
                .map(RequestLoggingFilter::maskParameter)
                .collect(Collectors.joining("&"));
    }

    private static String maskParameter(String parameter) {
        int separator = parameter.indexOf('=');
        if (separator < 0) {
            return parameter;
        }
        String name = parameter.substring(0, separator);
        return SENSITIVE_QUERY_PARAMS.contains(name.toLowerCase(Locale.ROOT)) ? name + "=" + MASK : parameter;
    }
}
