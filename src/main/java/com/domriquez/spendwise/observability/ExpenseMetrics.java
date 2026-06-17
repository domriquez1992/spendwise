package com.domriquez.spendwise.observability;

import com.domriquez.spendwise.event.ExpenseCreatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Custom business metrics for the expense domain.
 *
 * <p>Spring Boot already auto-instruments HTTP requests, the JVM, the datasource pool, and caches.
 * This component adds domain-level meters those generic metrics cannot express: how many expenses
 * are created (broken down by category) and the distribution of their amounts.
 *
 * <p>It listens to {@link ExpenseCreatedEvent} at {@link TransactionPhase#AFTER_COMMIT}, mirroring
 * the Kafka and audit listeners, so a meter is recorded only for an expense whose transaction
 * actually committed -- never for one that was rolled back. Keeping instrumentation in a separate
 * listener leaves the service free of cross-cutting concerns and avoids changing its constructor
 * (and the unit test that builds it directly).
 *
 * <p>The {@code category} tag has bounded cardinality (the seven-value {@code Category} enum), so it
 * is safe as a Prometheus label. Amount is recorded as a {@code double} purely for the metric
 * histogram; the authoritative monetary values remain {@code BigDecimal} everywhere else.
 */
@Component
public class ExpenseMetrics {

    private static final String CREATED_COUNTER = "spendwise.expenses.created";
    private static final String AMOUNT_SUMMARY = "spendwise.expense.amount";

    private final MeterRegistry registry;
    private final DistributionSummary amountSummary;

    public ExpenseMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.amountSummary = DistributionSummary.builder(AMOUNT_SUMMARY)
                .description("Distribution of created expense amounts")
                .register(registry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Counter.builder(CREATED_COUNTER)
                .description("Number of expenses created, by category")
                .tag("category", event.category().name())
                .register(registry)
                .increment();
        amountSummary.record(event.amount().doubleValue());
    }
}
