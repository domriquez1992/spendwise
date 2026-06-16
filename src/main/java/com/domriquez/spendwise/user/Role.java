package com.domriquez.spendwise.user;

/**
 * Application roles. Every newly registered user is a {@link #USER}; {@link #ADMIN}
 * exists for role-based authorization introduced in a later iteration.
 */
public enum Role {
    USER,
    ADMIN
}
