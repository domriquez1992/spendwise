package com.domriquez.spendwise.expense.dto;

import com.domriquez.spendwise.expense.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Incoming payload for creating or updating an expense.
 * Kept separate from the {@code Expense} entity so the API contract and the
 * persistence model can evolve independently and internal fields are never exposed.
 */
public record ExpenseRequest(

        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "category is required")
        Category category,

        @NotNull(message = "date is required")
        @PastOrPresent(message = "date cannot be in the future")
        LocalDate date
) {
}
