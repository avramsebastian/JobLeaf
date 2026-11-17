package com.seby.jobleaf.infrastructure.web.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Limits how often one client (IP address) may call the endpoints in {@link RateLimitRule#DEFAULTS}.
 * <p>
 * Every rule + IP gets its own Bucket4j token bucket: it starts full ({@code capacity} tokens), each request takes
 * one token, and tokens flow back in steadily over the {@code period}. An empty bucket means 429 Too Many Requests.
 * <p>
 * Kept simple on purpose: the buckets live in memory, so the limits are per backend instance and reset on restart,
 * and the map keeps one small entry per IP that ever called a limited endpoint.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final List<RateLimitRule> rules;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(List<RateLimitRule> rules, HandlerExceptionResolver handlerExceptionResolver) {
        this.rules = List.copyOf(rules);
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateLimitRule rule = findRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(rule.path() + "|" + clientIp, key -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + 999_999_999));
        log.warn("Rate limit reached for {} on {} {}, retry in {}s", clientIp, rule.method(), rule.path(),
                retryAfterSeconds);
        handlerExceptionResolver.resolveException(request, response, null,
                new RateLimitExceededException(retryAfterSeconds));
    }

    private RateLimitRule findRule(HttpServletRequest request) {
        for (RateLimitRule rule : rules) {
            if (rule.matches(request.getMethod(), request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Full bucket of {@code capacity} tokens, refilled greedily: e.g. 5 per minute means one token back every 12s,
     * not all 5 at once when the minute is over.
     */
    private static Bucket newBucket(RateLimitRule rule) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity()).refillGreedy(rule.capacity(), rule.period()))
                .build();
    }
}
