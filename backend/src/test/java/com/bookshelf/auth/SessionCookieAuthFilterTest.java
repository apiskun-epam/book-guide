package com.bookshelf.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionCookieAuthFilterTest {

    @Mock
    private AuthService authService;

    @Mock
    private FilterChain chain;

    private SessionCookieAuthFilter filter;
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/collection");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        filter = new SessionCookieAuthFilter(authService, new AuthProperties());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noCookie_leavesRequestUnauthenticated() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void validCookie_setsPrincipalFromVerifiedUid() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser("uid-a", "a@example.com", "A", null);
        when(authService.verifySession("good")).thenReturn(user);
        request.setCookies(new Cookie("bookshelf_session", "good"));

        filter.doFilter(request, response, chain);

        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidOrRevokedCookie_leavesRequestUnauthenticated() throws Exception {
        when(authService.verifySession("bad")).thenThrow(new InvalidSessionException("Invalid session"));
        request.setCookies(new Cookie("bookshelf_session", "bad"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void cookieWithOtherName_isIgnored() throws Exception {
        request.setCookies(new Cookie("other", "value"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
