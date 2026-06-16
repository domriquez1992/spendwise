package com.domriquez.spendwise.expense.dto;

import com.domriquez.spendwise.expense.Category;

import java.math.BigDecimal;

/**
 * Total amount spent within a single {@link Category}.
 * Populated directly by a JPQL constructor expression in the repository.
 */
public record CategorySummary(
        Category category,
        BigDecimal total
) {
}
