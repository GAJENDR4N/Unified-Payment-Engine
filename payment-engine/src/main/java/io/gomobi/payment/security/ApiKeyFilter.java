//package io.gomobi.payment.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.List;
//
///**
// * Validates an internal API key on inbound requests from the calling
// * gateway (not the provider callbacks - those are authenticated via each
// * provider's own signature scheme in the relevant adapter's
// * handleCallback). Disabled by default (empty expected key) so this
// * doesn't block local development; set payment.security.api-key to
// * enforce it.
// * This is a starting point, not a full auth layer - for anything beyond a
// * single shared secret between two trusted internal services, replace
// * this with proper mTLS or OAuth2 client-credentials between GoMobi
// * services.
// */
//@Slf4j
//@Order(10)
//@Component
//public class ApiKeyFilter extends OncePerRequestFilter {
//
//    private static final String API_KEY_HEADER = "X-Api-Key";
//    private static final List<String> EXCLUDED_PATHS = List.of(
//            "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
//    );
//
//    private final AntPathMatcher pathMatcher = new AntPathMatcher();
//
//    @Value("${payment.security.api-key:}")
//    private String expectedApiKey;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//            throws ServletException, IOException {
//
//        if (expectedApiKey == null || expectedApiKey.isBlank() || isExcluded(request.getRequestURI())) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        String providedKey = request.getHeader(API_KEY_HEADER);
//        if (expectedApiKey.equals(providedKey)) {
//            chain.doFilter(request, response);
//        } else {
//            log.warn("Rejected request to {} - missing/invalid API key", request.getRequestURI());
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write("{\"code\":\"" + io.gomobi.payment.exception.ErrorCode.AUTH_FAILED.getCode()
//                    + "\",\"message\":\"Missing or invalid API key\"}");
//        }
//    }
//
//    private boolean isExcluded(String uri) {
//        return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
//    }
//}
