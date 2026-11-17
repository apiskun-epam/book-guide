package com.bookshelf.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private FirebaseToken token;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(firebaseAuth, new AuthProperties(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void stubToken(String provider, Object authTime) {
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of("sign_in_provider", provider),
                "auth_time", authTime));
    }

    @Test
    void createSession_googleTokenWithFreshSignIn_returnsCookieAndUser() throws Exception {
        stubToken("google.com", NOW.minusSeconds(30).getEpochSecond());
        when(token.getUid()).thenReturn("uid-a");
        when(token.getEmail()).thenReturn("a@example.com");
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);
        when(firebaseAuth.createSessionCookie(eq("id-token"), any(SessionCookieOptions.class)))
                .thenReturn("session-cookie");

        AuthService.Session session = service.createSession("id-token");

        assertEquals("session-cookie", session.cookieValue());
        assertEquals("uid-a", session.user().uid());
    }

    @Test
    void createSession_nonGoogleProvider_isRejected() throws Exception {
        stubToken("password", NOW.getEpochSecond());
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        assertThrows(InvalidSessionException.class, () -> service.createSession("id-token"));
        verify(firebaseAuth, never()).createSessionCookie(anyString(), any());
    }

    @Test
    void createSession_staleSignIn_isRejected() throws Exception {
        stubToken("google.com", NOW.minus(Duration.ofHours(1)).getEpochSecond());
        when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(token);

        assertThrows(InvalidSessionException.class, () -> service.createSession("id-token"));
        verify(firebaseAuth, never()).createSessionCookie(anyString(), any());
    }

    @Test
    void createSession_invalidToken_isRejected() throws Exception {
        when(firebaseAuth.verifyIdToken("bad", true)).thenThrow(IllegalArgumentException.class);

        assertThrows(InvalidSessionException.class, () -> service.createSession("bad"));
    }

    @Test
    void verifySession_validCookie_returnsUserAndChecksRevocation() throws Exception {
        when(token.getUid()).thenReturn("uid-a");
        when(firebaseAuth.verifySessionCookie("cookie", true)).thenReturn(token);

        assertEquals("uid-a", service.verifySession("cookie").uid());
    }

    @Test
    void verifySession_revokedOrInvalidCookie_isRejected() throws Exception {
        when(firebaseAuth.verifySessionCookie("cookie", true)).thenThrow(IllegalArgumentException.class);

        assertThrows(InvalidSessionException.class, () -> service.verifySession("cookie"));
    }

    @Test
    void revokeSessions_revokesRefreshTokensForUid() throws Exception {
        service.revokeSessions("uid-a");

        verify(firebaseAuth).revokeRefreshTokens("uid-a");
    }
}
