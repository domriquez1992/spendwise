package com.domriquez.spendwise.expense.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Spending summary: a breakdown per category and the overall total
 * for the requested (optional) date range.
 *
 * <p>Implements {@link Serializable} because instances are cached in Redis. The auto-configured
 * {@code RedisCacheManager} serializes values with JDK serialization by default, which keeps the
 * cache independent of the application's JSON stack.
 */
public record SummaryResponse(
        List<CategorySummary> categories,
        BigDecimal grandTotal
) implements Serializable {
}
