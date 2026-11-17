package com.bookshelf.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates a request from the session cookie. The UID in the security context comes only
 * from the verified cookie. A missing or invalid cookie leaves the request unauthenticated and
 * the security entry point answers 401.
 */
@RequiredArgsConstructor
public class SessionCookieAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final AuthProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cookieValue = readCookie(request);
        if (cookieValue != null) {
            try {
                AuthenticatedUser user = authService.verifySession(cookieValue);
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidSessionException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
