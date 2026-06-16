package com.domriquez.spendwise.expense.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spending summary: a breakdown per category and the overall total
 * for the requested (optional) date range.
 */
public record SummaryResponse(
        List<CategorySummary> categories,
        BigDecimal grandTotal
) {
}
