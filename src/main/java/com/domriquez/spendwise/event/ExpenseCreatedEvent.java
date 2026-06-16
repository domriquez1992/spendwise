package com.domriquez.spendwise.event;

import com.domriquez.spendwise.expense.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Emitted when an expense is created. Used both as an in-process Spring application event
 * (published by the service) and as the JSON payload sent to Kafka, so downstream consumers
 * react asynchronously without coupling to the expense service.
 */
public record ExpenseCreatedEvent(
        Long expenseId,
        String ownerUsername,
        Category category,
        BigDecimal amount,
        LocalDate date) {
}
