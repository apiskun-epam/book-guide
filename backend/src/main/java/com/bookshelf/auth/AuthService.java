package com.bookshelf.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Exchanges Google-backed Firebase ID tokens for server-side session cookies and verifies
 * those cookies on later requests. Token contents are never logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    static final String GOOGLE_PROVIDER = "google.com";

    private final FirebaseAuth firebaseAuth;
    private final AuthProperties properties;
    private final Clock clock;

    public record Session(String cookieValue, AuthenticatedUser user) {
    }

    /**
     * Verifies a freshly issued ID token and creates a session cookie for it.
     *
     * @throws InvalidSessionException if the token is invalid, revoked, stale, or not from Google sign-in
     */
    public Session createSession(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            requireGoogleProvider(token);
            requireRecentSignIn(token);

            SessionCookieOptions options = SessionCookieOptions.builder()
                    .setExpiresIn(properties.getSessionDuration().toMillis())
                    .build();
            String cookie = firebaseAuth.createSessionCookie(idToken, options);
            return new Session(cookie, toUser(token));
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            log.warn("Rejected ID token while creating a session: {}", e.getClass().getSimpleName());
            throw new InvalidSessionException("Could not sign in", e);
        }
    }

    /**
     * Verifies a session cookie, including a revocation check, and returns the identity it carries.
     *
     * @throws InvalidSessionException if the cookie is invalid, expired or revoked
     */
    public AuthenticatedUser verifySession(String sessionCookie) {
        try {
            return toUser(firebaseAuth.verifySessionCookie(sessionCookie, true));
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new InvalidSessionException("Invalid session", e);
        }
    }

    /** Invalidates every session of the user (used by logout). */
    public void revokeSessions(String uid) {
        try {
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to revoke sessions: {}", e.getAuthErrorCode());
            throw new InvalidSessionException("Could not revoke session", e);
        }
    }

    private void requireGoogleProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        String provider = firebaseClaim instanceof Map<?, ?> map && map.get("sign_in_provider") instanceof String s
                ? s : null;
        if (!GOOGLE_PROVIDER.equals(provider)) {
            throw new InvalidSessionException("Unsupported sign-in provider");
        }
    }

    private void requireRecentSignIn(FirebaseToken token) {
        Object authTime = token.getClaims().get("auth_time");
        if (!(authTime instanceof Number seconds)) {
            throw new InvalidSessionException("Missing sign-in time");
        }
        Instant signedInAt = Instant.ofEpochSecond(seconds.longValue());
        if (signedInAt.plus(properties.getMaxSignInAge()).isBefore(clock.instant())) {
            throw new InvalidSessionException("Sign-in too old");
        }
    }

    private AuthenticatedUser toUser(FirebaseToken token) {
        return new AuthenticatedUser(token.getUid(), token.getEmail(), token.getName(), token.getPicture());
    }
}
