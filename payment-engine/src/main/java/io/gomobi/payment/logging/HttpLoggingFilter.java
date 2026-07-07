package io.gomobi.payment.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Structured, one-request/one-response log pair for every inbound HTTP
 * call: what came in, what went out, and how long it took - in a
 * consistent key=value shape that's easy to parse from log-aggregation
 * tooling (ELK/Loki/Splunk).
 *
 * Bodies are captured via Spring's ContentCachingRequest/ResponseWrapper
 * rather than manually buffering streams, so the actual controller/Jackson
 * body-read still works unmodified. Health/docs endpoints are excluded to
 * keep the access log focused on business traffic.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            log.info("event=INCOMING_REQUEST method={} uri={} query={}",
                    request.getMethod(), request.getRequestURI(), request.getQueryString());

            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - start;

            String responseBody = extractBody(wrappedResponse.getContentAsByteArray());
            String requestBody = extractBody(wrappedRequest.getContentAsByteArray());
            if (!requestBody.isBlank()) {
                log.debug("event=INCOMING_REQUEST_BODY method={} uri={} requestBody={}",
                        request.getMethod(), request.getRequestURI(), requestBody);
            }

            log.info("event=OUTGOING_RESPONSE method={} uri={} status={} durationMs={} responseBody={}",
                    request.getMethod(), request.getRequestURI(), wrappedResponse.getStatus(), durationMs, responseBody);

            // Must copy the buffered body back into the real response, or the client gets nothing.
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String extractBody(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private boolean isExcluded(String uri) {
        return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
