package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.CategorySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for {@link Expense}. CRUD and pagination come for free from
 * {@link JpaRepository}; the methods below add owner-scoped lookups so a user can only
 * ever reach their own rows, plus a grouped aggregation performed in the database.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** A page of the given user's expenses. */
    Page<Expense> findByOwnerUsername(String username, Pageable pageable);

    /**
     * A single expense by id, but only if it belongs to the given user. Returning empty for
     * someone else's id means the API reports 404 rather than revealing that the row exists.
     */
    Optional<Expense> findByIdAndOwnerUsername(Long id, String username);

    /**
     * Sums the given user's spend per category, optionally bounded by a date range.
     * A {@code null} bound means "unbounded" on that side.
     */
    @Query("""
            SELECT new com.domriquez.spendwise.expense.dto.CategorySummary(e.category, SUM(e.amount))
            FROM Expense e
            WHERE e.owner.username = :username
              AND (:from IS NULL OR e.date >= :from)
              AND (:to IS NULL OR e.date <= :to)
            GROUP BY e.category
            ORDER BY e.category
            """)
    List<CategorySummary> summarizeByCategory(@Param("username") String username,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /**
     * Total a single user has spent in one category over a half-open date range
     * {@code [from, to)}. Returns zero (never {@code null}) when there are no matching rows.
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.owner.username = :username
              AND e.category = :category
              AND e.date >= :from
              AND e.date < :to
            """)
    BigDecimal sumByOwnerAndCategoryInPeriod(@Param("username") String username,
                                             @Param("category") Category category,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
