package com.bookshelf.auth;

import com.bookshelf.agents.BookSuggestionController;
import com.bookshelf.agents.BookSuggestionOrchestrator;
import com.bookshelf.books.BookDTO;
import com.bookshelf.books.CollectionController;
import com.bookshelf.books.CollectionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks the security chain over the real controllers, with Firebase mocked. */
@WebMvcTest(controllers = {AuthController.class, CollectionController.class, BookSuggestionController.class},
        properties = "app.cors.allowed-origins=http://localhost:3000")
@Import(SecurityConfig.class)
class AuthSecurityTest {

    private static final AuthenticatedUser USER_A = new AuthenticatedUser("uid-a", "a@example.com", "A", null);
    private static final AuthenticatedUser USER_B = new AuthenticatedUser("uid-b", "b@example.com", "B", null);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CollectionService collectionService;

    @MockBean
    private BookSuggestionOrchestrator orchestrator;

    private static Cookie cookie(String value) {
        return new Cookie("bookshelf_session", value);
    }

    @BeforeEach
    void setUp() {
        when(authService.verifySession("cookie-a")).thenReturn(USER_A);
        when(authService.verifySession("cookie-b")).thenReturn(USER_B);
        when(authService.verifySession("revoked")).thenThrow(new InvalidSessionException("Invalid session"));
    }

    @Test
    void collection_withoutSession_returns401WithStandardBody() throws Exception {
        mockMvc.perform(get("/api/collection"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/collection"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void collection_withRevokedSession_returns401() throws Exception {
        mockMvc.perform(get("/api/collection").cookie(cookie("revoked")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void collection_usesUidFromVerifiedSession_andIgnoresClientSuppliedUserId() throws Exception {
        when(collectionService.getUserCollection("uid-a")).thenReturn(List.of(new BookDTO()));

        mockMvc.perform(get("/api/collection").param("userId", "uid-b").cookie(cookie("cookie-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(collectionService).getUserCollection("uid-a");
        verify(collectionService, never()).getUserCollection("uid-b");
    }

    @Test
    void collection_userBNeverSeesUserAData() throws Exception {
        when(collectionService.getUserCollection("uid-a")).thenReturn(List.of(new BookDTO()));
        when(collectionService.getUserCollection("uid-b")).thenReturn(List.of());

        mockMvc.perform(get("/api/collection").cookie(cookie("cookie-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(collectionService, never()).getUserCollection("uid-a");
    }

    @Test
    void removeBook_scopesDeleteToAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/collection/5")
                        .cookie(cookie("cookie-b"))
                        .header("X-Requested-With", "bookshelf"))
                .andExpect(status().isNoContent());

        verify(collectionService).removeBookFromCollection("uid-b", 5);
        verify(collectionService, never()).removeBookFromCollection(eq("uid-a"), anyInt());
    }

    @Test
    void removeBook_withoutCsrfHeader_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/collection/5").cookie(cookie("cookie-a")))
                .andExpect(status().isForbidden());

        verify(collectionService, never()).removeBookFromCollection(any(), anyInt());
    }

    @Test
    void suggestions_withoutSession_returns401() throws Exception {
        mockMvc.perform(post("/api/suggestions")
                        .header("X-Requested-With", "bookshelf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"something light\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suggestions_runForAuthenticatedUidOnly() throws Exception {
        mockMvc.perform(post("/api/suggestions")
                        .cookie(cookie("cookie-a"))
                        .header("X-Requested-With", "bookshelf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"something light\"}"))
                .andExpect(status().isOk());

        verify(orchestrator).generateSuggestions("uid-a", "something light");
    }

    @Test
    void createSession_setsHardenedCookie() throws Exception {
        when(authService.createSession("id-token"))
                .thenReturn(new AuthService.Session("signed-cookie", USER_A));

        mockMvc.perform(post("/api/auth/session")
                        .header("X-Requested-With", "bookshelf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-a"))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("bookshelf_session=signed-cookie"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Strict"),
                        containsString("Path=/api"),
                        containsString("Max-Age=604800"))));
    }

    @Test
    void createSession_withRejectedToken_returns401WithoutCookie() throws Exception {
        when(authService.createSession("bad")).thenThrow(new InvalidSessionException("Could not sign in"));

        mockMvc.perform(post("/api/auth/session")
                        .header("X-Requested-With", "bookshelf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void createSession_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/session")
                        .header("X-Requested-With", "bookshelf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_revokesSessionsAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookie("cookie-a"))
                        .header("X-Requested-With", "bookshelf"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).revokeSessions("uid-a");
    }

    @Test
    void logout_withoutValidSession_stillClearsCookieAndRevokesNothing() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookie("revoked"))
                        .header("X-Requested-With", "bookshelf"))
                .andExpect(status().isNoContent());

        verify(authService, never()).revokeSessions(any());
    }

    @Test
    void me_returnsAuthenticatedProfile() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(cookie("cookie-b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-b"));
    }

    @Test
    void me_withoutSession_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
