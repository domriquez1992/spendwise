package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Computes a user's spending summary and caches the common, unfiltered view in Redis.
 *
 * <p>Caching lives in its own bean on purpose. Spring's caching is applied by a proxy around the
 * bean, so a {@code @Cacheable} method invoked through {@code this} (a self-invocation) would
 * bypass the cache entirely. Keeping it here means {@link ExpenseServiceImpl} calls it as a
 * separate bean and the interception actually happens.
 *
 * <p>Only the default summary (no date filter) is cached, keyed by username — this is the hot path
 * a dashboard hits repeatedly. Date-filtered summaries are computed on demand and not cached
 * (see the {@code condition}), which keeps the cache key simple and eviction precise: a single
 * {@link #evict(String)} for the user clears their entry after any write.
 */
@Component
public class ExpenseSummaryCache {

    /** Cache name shared by the {@code @Cacheable}/{@code @CacheEvict} annotations and tests. */
    public static final String SUMMARY_CACHE = "expense-summary";

    private final ExpenseRepository repository;

    public ExpenseSummaryCache(ExpenseRepository repository) {
        this.repository = repository;
    }

    /**
     * Sums the user's spend per category and the grand total. The result is cached only when no
     * date range is supplied (the repeatedly-read default view); filtered queries always execute.
     */
    @Cacheable(
            cacheNames = SUMMARY_CACHE,
            key = "#username",
            condition = "#from == null && #to == null")
    public SummaryResponse summarize(String username, LocalDate from, LocalDate to) {
        List<CategorySummary> categories = repository.summarizeByCategory(username, from, to);
        BigDecimal grandTotal = categories.stream()
                .map(CategorySummary::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SummaryResponse(categories, grandTotal);
    }

    /**
     * Drops the user's cached summary. Called after any expense write so the next read recomputes
     * from current data. Evicting eagerly (rather than after commit) is safe: a redundant eviction
     * only causes a cache miss, whereas a stale entry would serve wrong data.
     */
    @CacheEvict(cacheNames = SUMMARY_CACHE, key = "#username")
    public void evict(String username) {
        // Body intentionally empty: the @CacheEvict annotation performs the work.
    }
}
