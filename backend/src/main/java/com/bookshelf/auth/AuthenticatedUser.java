package com.bookshelf.auth;

/**
 * The verified identity behind a request. {@code uid} is the Firebase UID and is the only
 * value used to scope user data.
 */
public record AuthenticatedUser(String uid, String email, String name, String picture) {
}
