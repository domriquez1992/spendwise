package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test for the summary aggregation. The {@code @Cacheable} annotation is a no-op without a
 * Spring proxy, so calling the method directly here exercises the plain aggregation logic: the
 * per-category totals are passed through and the grand total is their sum.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseSummaryCacheTest {

    private static final String USERNAME = "alice";

    @Mock
    private ExpenseRepository repository;

    private ExpenseSummaryCache summaryCache;

    @BeforeEach
    void setUp() {
        summaryCache = new ExpenseSummaryCache(repository);
    }

    @Test
    void summarize_sumsCategoryTotalsIntoGrandTotal() {
        List<CategorySummary> categoryTotals = List.of(
                new CategorySummary(Category.FOOD, new BigDecimal("100.00")),
                new CategorySummary(Category.TRANSPORT, new BigDecimal("55.50"))
        );
        when(repository.summarizeByCategory(eq(USERNAME), any(), any())).thenReturn(categoryTotals);

        SummaryResponse summary = summaryCache.summarize(USERNAME, null, null);

        assertThat(summary.categories()).hasSize(2);
        assertThat(summary.grandTotal()).isEqualByComparingTo("155.50");
    }
}
