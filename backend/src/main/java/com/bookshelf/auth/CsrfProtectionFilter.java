package com.bookshelf.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * CSRF defence for the cookie session, layered on top of SameSite=Strict: every state-changing
 * request must carry the custom {@code X-Requested-With} header (which a cross-site form cannot
 * set) and, when an Origin header is present, it must be an allowed frontend origin.
 */
@RequiredArgsConstructor
public class CsrfProtectionFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Requested-With";
    public static final String HEADER_VALUE = "bookshelf";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final List<String> allowedOrigins;
    private final JsonErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!SAFE_METHODS.contains(request.getMethod()) && !isTrusted(request)) {
            errorWriter.write(request, response, HttpStatus.FORBIDDEN, "Request rejected");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isTrusted(HttpServletRequest request) {
        if (!HEADER_VALUE.equals(request.getHeader(HEADER_NAME))) {
            return false;
        }
        String origin = request.getHeader("Origin");
        return origin == null || allowedOrigins.contains(origin);
    }
}
