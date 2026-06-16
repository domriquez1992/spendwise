package com.domriquez.spendwise.notification;

import com.domriquez.spendwise.event.ExpenseCreatedEvent;
import com.domriquez.spendwise.event.ExpenseEventPublisher;
import com.domriquez.spendwise.expense.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consumes {@link ExpenseCreatedEvent}s and raises a budget notification when a user's spending
 * in a category for the month exceeds the configured limit. Running this asynchronously off a
 * Kafka topic keeps the write path (creating an expense) fast and decoupled from this reaction.
 */
@Component
public class ExpenseEventListener {

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventListener.class);

    private final ExpenseRepository expenseRepository;
    private final NotificationRepository notificationRepository;
    private final BigDecimal monthlyCategoryLimit;

    public ExpenseEventListener(ExpenseRepository expenseRepository,
                                NotificationRepository notificationRepository,
                                @Value("${app.budget.monthly-category-limit}") BigDecimal monthlyCategoryLimit) {
        this.expenseRepository = expenseRepository;
        this.notificationRepository = notificationRepository;
        this.monthlyCategoryLimit = monthlyCategoryLimit;
    }

    @KafkaListener(topics = ExpenseEventPublisher.TOPIC)
    @Transactional
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        LocalDate monthStart = event.date().withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);

        BigDecimal monthlyTotal = expenseRepository.sumByOwnerAndCategoryInPeriod(
                event.ownerUsername(), event.category(), monthStart, nextMonth);

        if (monthlyTotal.compareTo(monthlyCategoryLimit) > 0) {
            String message = "You have spent %s on %s so far this month, over your budget of %s."
                    .formatted(monthlyTotal, event.category(), monthlyCategoryLimit);
            notificationRepository.save(new Notification(event.ownerUsername(), message));
            log.info("Budget exceeded: user={} category={} total={}",
                    event.ownerUsername(), event.category(), monthlyTotal);
        }
    }
}
