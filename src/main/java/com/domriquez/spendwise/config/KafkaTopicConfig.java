package com.domriquez.spendwise.config;

import com.domriquez.spendwise.event.ExpenseEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics the application owns. Spring's {@code KafkaAdmin} (auto-configured
 * when spring-kafka is on the classpath) creates any {@link NewTopic} beans on startup if they
 * do not already exist. A single partition and replica suit a single-broker dev/demo setup.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic expenseCreatedTopic() {
        return TopicBuilder.name(ExpenseEventPublisher.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
