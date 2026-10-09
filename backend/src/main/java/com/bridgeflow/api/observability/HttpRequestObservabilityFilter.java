package com.bridgeflow.api.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestObservabilityFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern UUID_SEGMENT = Pattern.compile(
        "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
    );
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestObservabilityFilter.class);

    private final MeterRegistry meterRegistry;

    public HttpRequestObservabilityFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        var correlationId = correlationId(request.getHeader(CORRELATION_HEADER));
        var started = System.nanoTime();
        response.setHeader(CORRELATION_HEADER, correlationId);
        try (var ignored = MDC.putCloseable("correlationId", correlationId)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                var durationNanos = System.nanoTime() - started;
                var route = routeTemplate(request);
                var status = Integer.toString(response.getStatus());
                Timer.builder("bridgeflow.http.requests")
                    .description("BridgeFlow HTTP request duration")
                    .tag("method", request.getMethod())
                    .tag("uri", route)
                    .tag("status", status)
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
                LOGGER.info(
                    "http_request method={} route={} status={} durationMs={}",
                    request.getMethod(), route, status, TimeUnit.NANOSECONDS.toMillis(durationNanos)
                );
            }
        }
    }

    private String correlationId(String candidate) {
        if (candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()) return candidate;
        return UUID.randomUUID().toString();
    }

    private String routeTemplate(HttpServletRequest request) {
        var pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String route && !route.isBlank()) return route;
        return UUID_SEGMENT.matcher(request.getRequestURI()).replaceAll("/{id}");
    }
}
