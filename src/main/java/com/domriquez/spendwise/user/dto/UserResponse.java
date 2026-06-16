package com.domriquez.spendwise.user.dto;

import com.domriquez.spendwise.user.Role;

/**
 * Public view of a user. Deliberately omits the password hash so it can never be
 * serialized over the wire.
 */
public record UserResponse(Long id, String username, Role role) {
}
