package com.domriquez.spendwise.observability;

import com.domriquez.spendwise.event.ExpenseCreatedEvent;
import com.domriquez.spendwise.expense.Category;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Map;

/**
 * Custom business metrics for the expense domain.
 *
 * <p>Spring Boot already auto-instruments HTTP requests, the JVM, the datasource pool, and caches.
 * This component adds domain-level meters those generic metrics cannot express: how many expenses
 * are created (broken down by category) and the distribution of their amounts.
 *
 * <p>The per-category counters are <strong>registered eagerly at startup</strong>, one per
 * {@link Category}, so every series is present (at zero) in the very first Prometheus scrape rather
 * than only appearing after the first expense of that category. Registering meters up front is the
 * recommended Micrometer practice: it avoids gaps in dashboards and alerts and makes the meters
 * deterministically scrapeable. The {@code category} tag has bounded cardinality (the seven-value
 * enum), so it is safe as a Prometheus label.
 *
 * <p>The counters are then incremented from an {@link ExpenseCreatedEvent} listener bound to
 * {@link TransactionPhase#AFTER_COMMIT}, mirroring the Kafka and audit listeners, so a meter moves
 * only for an expense whose transaction actually committed -- never for one that was rolled back.
 * Keeping instrumentation in a separate listener leaves the service free of cross-cutting concerns
 * and avoids changing its constructor (and the unit test that builds it directly). Amount is
 * recorded as a {@code double} purely for the metric histogram; authoritative monetary values
 * remain {@code BigDecimal} everywhere else.
 */
@Component
public class ExpenseMetrics {

    private static final String CREATED_COUNTER = "spendwise.expenses.created";
    private static final String AMOUNT_SUMMARY = "spendwise.expense.amount";

    private final Map<Category, Counter> createdByCategory = new EnumMap<>(Category.class);
    private final DistributionSummary amountSummary;

    public ExpenseMetrics(MeterRegistry registry) {
        for (Category category : Category.values()) {
            createdByCategory.put(category, Counter.builder(CREATED_COUNTER)
                    .description("Number of expenses created, by category")
                    .tag("category", category.name())
                    .register(registry));
        }
        this.amountSummary = DistributionSummary.builder(AMOUNT_SUMMARY)
                .description("Distribution of created expense amounts")
                .register(registry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        createdByCategory.get(event.category()).increment();
        amountSummary.record(event.amount().doubleValue());
    }
}
