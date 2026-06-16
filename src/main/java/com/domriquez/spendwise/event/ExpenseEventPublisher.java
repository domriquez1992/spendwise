package com.domriquez.spendwise.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the in-process Spring event to Kafka.
 *
 * <p>The relay is bound to {@link TransactionPhase#AFTER_COMMIT}: the Kafka message is only sent
 * once the database transaction that created the expense has successfully committed. This avoids
 * the classic dual-write hazard where a consumer reacts to an event for data that was later rolled
 * back (or that it cannot yet read).
 */
@Component
public class ExpenseEventPublisher {

    /** Topic carrying expense-created events. */
    public static final String TOPIC = "expense.created";

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventPublisher.class);

    private final KafkaTemplate<String, ExpenseCreatedEvent> kafkaTemplate;

    public ExpenseEventPublisher(KafkaTemplate<String, ExpenseCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        // Key by username so all of a user's events land on the same partition (per-user ordering).
        kafkaTemplate.send(TOPIC, event.ownerUsername(), event);
        log.debug("Published ExpenseCreatedEvent for expense {} (user {})",
                event.expenseId(), event.ownerUsername());
    }
}
