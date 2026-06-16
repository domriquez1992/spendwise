package com.domriquez.spendwise.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the username of the currently authenticated user from the security context.
 *
 * <p>Injecting this small component into services — rather than calling the static
 * {@link SecurityContextHolder} directly in business logic — keeps the service layer free of
 * security plumbing and makes it trivial to substitute a fixed user in unit tests.
 */
@Component
public class CurrentUserProvider {

    public String requireCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return authentication.getName();
    }
}
