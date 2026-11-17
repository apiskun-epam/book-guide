package com.bookshelf.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsrfProtectionFilterTest {

    @Mock
    private FilterChain chain;

    private final CsrfProtectionFilter filter = new CsrfProtectionFilter(
            List.of("http://localhost:3000"), new JsonErrorWriter(new ObjectMapper()));
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private MockHttpServletRequest post() {
        return new MockHttpServletRequest("POST", "/api/collection");
    }

    @Test
    void getRequest_passesWithoutHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/collection");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void postWithoutCustomHeader_isForbidden() throws Exception {
        MockHttpServletRequest request = post();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postFromForeignOrigin_isForbidden() throws Exception {
        MockHttpServletRequest request = post();
        request.addHeader("X-Requested-With", "bookshelf");
        request.addHeader("Origin", "https://evil.example");

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void postFromAllowedOriginWithHeader_passes() throws Exception {
        MockHttpServletRequest request = post();
        request.addHeader("X-Requested-With", "bookshelf");
        request.addHeader("Origin", "http://localhost:3000");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
