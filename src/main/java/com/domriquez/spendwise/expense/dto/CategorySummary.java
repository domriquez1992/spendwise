package com.domriquez.spendwise.expense.dto;

import com.domriquez.spendwise.expense.Category;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Total amount spent within a single {@link Category}.
 * Populated directly by a JPQL constructor expression in the repository.
 *
 * <p>{@link Serializable} because it is nested inside {@link SummaryResponse}, which is cached
 * in Redis.
 */
public record CategorySummary(
        Category category,
        BigDecimal total
) implements Serializable {
}
