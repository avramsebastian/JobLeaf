package com.seby.jobleaf.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

        log.info("Incoming request: [{}] {}{}", method, uri, query);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("Completed request: [{}] {}{} -> status={} [{}ms]", method, uri, query, status, duration);
        }
    }
}
