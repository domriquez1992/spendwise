package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Data-access layer for {@link Expense}. CRUD and pagination come for free from
 * {@link JpaRepository}; the custom query below performs a grouped aggregation
 * directly in the database rather than loading every row into memory.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Sums spend per category, optionally bounded by a date range.
     * A {@code null} bound means "unbounded" on that side.
     */
    @Query("""
            SELECT new com.domriquez.spendwise.expense.dto.CategorySummary(e.category, SUM(e.amount))
            FROM Expense e
            WHERE (:from IS NULL OR e.date >= :from)
              AND (:to IS NULL OR e.date <= :to)
            GROUP BY e.category
            ORDER BY e.category
            """)
    List<CategorySummary> summarizeByCategory(@Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
