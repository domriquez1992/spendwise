package com.domriquez.spendwise.expense.dto;

import com.domriquez.spendwise.expense.Category;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Outgoing representation of an expense returned by the API.
 */
public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        Category category,
        LocalDate date,
        Instant createdAt
) {
}
