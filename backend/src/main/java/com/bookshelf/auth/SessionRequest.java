package com.bookshelf.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {

    /** Firebase ID token from a Google sign-in. Never logged. */
    @ToString.Exclude
    @NotBlank
    @Size(max = 8192)
    private String idToken;
}
