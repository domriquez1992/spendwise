package com.domriquez.spendwise.exception;

/**
 * Thrown when an expense is requested by an id that does not exist.
 * Translated into an HTTP 404 by the {@code GlobalExceptionHandler}.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with id: " + id);
    }
}
