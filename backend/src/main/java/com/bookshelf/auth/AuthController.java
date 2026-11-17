package com.bookshelf.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_PATH = "/api";

    private final AuthService authService;
    private final AuthProperties properties;

    /** Exchanges a Google-backed Firebase ID token for an HttpOnly session cookie. */
    @PostMapping("/session")
    public ResponseEntity<AuthenticatedUser> createSession(@Valid @RequestBody SessionRequest request) {
        AuthService.Session session;
        try {
            session = authService.createSession(request.getIdToken());
        } catch (InvalidSessionException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not sign in");
        }
        ResponseCookie cookie = buildCookie(session.cookieValue(), properties.getSessionDuration());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.user());
    }

    /** Returns the signed-in user; used by the SPA to restore its state on load. */
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUser> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(user);
    }

    /**
     * Revokes the user's sessions server-side and clears the cookie. Always clears the cookie,
     * even when the session was already invalid.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            authService.revokeSessions(user.uid());
        }
        ResponseCookie cleared = buildCookie("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .build();
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
