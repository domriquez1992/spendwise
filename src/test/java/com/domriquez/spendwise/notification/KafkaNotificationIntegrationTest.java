package com.domriquez.spendwise.notification;

import com.domriquez.spendwise.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the event-driven pipeline end to end against a real Kafka broker: creating an expense
 * publishes an event, the {@code @KafkaListener} consumes it asynchronously, and — because the
 * spend is over the (test) budget limit — a notification is persisted and readable via the API.
 */
class KafkaNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void creatingAnOverBudgetExpense_producesABudgetNotification() throws Exception {
        register("kafkauser", "password123");
        String token = login("kafkauser", "password123");

        // 120.50 in FOOD exceeds the test budget limit (100), so the consumer should react to
        // the published event by saving a notification — but only after the event makes a full
        // round trip through Kafka, hence the poll rather than an immediate assertion.
        createExpense(token);

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> notificationRepository.countByOwnerUsername("kafkauser") > 0);

        // The owner can read the notification back through the API.
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].message").value(containsString("FOOD")));
    }
}
