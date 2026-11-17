package com.bookshelf.auth;

/**
 * Thrown when an ID token or session cookie cannot be accepted. The message is generic on
 * purpose and never contains token contents.
 */
public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException(String message) {
        super(message);
    }

    public InvalidSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
