package edu.ai.lms.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlation_id";

    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = normalize(request.getHeader(HEADER_NAME));
        if (correlationId == null) correlationId = UUID.randomUUID().toString();

        response.setHeader(HEADER_NAME, correlationId);
        long startedAt = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            log.info("event=http.request.started correlation_id={} method={} path={}",
                    correlationId, request.getMethod(), request.getRequestURI());
            try {
                filterChain.doFilter(request, response);
                log.info("event=http.request.completed correlation_id={} method={} path={} status={} duration_ms={}",
                        correlationId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        elapsedMillis(startedAt));
            } catch (IOException | ServletException | RuntimeException exception) {
                log.error("event=http.request.failed correlation_id={} method={} path={} status={} duration_ms={} error_type={}",
                        correlationId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        elapsedMillis(startedAt), exception.getClass().getSimpleName(), exception);
                throw exception;
            }
        }
    }

    public static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return SAFE_VALUE.matcher(trimmed).matches() ? trimmed : null;
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
