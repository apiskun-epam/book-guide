package com.bookshelf.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** Name of the HttpOnly session cookie. */
    private String cookieName = "bookshelf_session";

    /** Session lifetime. Firebase allows 5 minutes to 14 days. */
    private Duration sessionDuration = Duration.ofDays(7);

    /** Set the Secure flag on the cookie. Enable whenever the app is served over HTTPS. */
    private boolean cookieSecure = false;

    /** Max age of the Google sign-in (auth_time) accepted when creating a session. */
    private Duration maxSignInAge = Duration.ofMinutes(5);
}
