package com.domriquez.spendwise.exception;

/**
 * Thrown when registration is attempted with a username that already exists.
 * Mapped to HTTP 409 (Conflict) by {@link GlobalExceptionHandler}.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already taken: " + username);
    }
}
