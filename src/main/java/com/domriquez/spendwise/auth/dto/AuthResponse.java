package com.domriquez.spendwise.auth.dto;

/**
 * Returned on successful login. {@code tokenType} is always "Bearer": the client should send the
 * token back in the {@code Authorization: Bearer <token>} header on subsequent requests.
 */
public record AuthResponse(String token, String tokenType, String username) {
}
