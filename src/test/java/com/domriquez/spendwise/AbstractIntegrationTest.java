package com.domriquez.spendwise;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for full-stack integration tests. Boots the entire application against an in-memory
 * H2 database and a real, throwaway Kafka broker started with Testcontainers.
 *
 * <p>The broker is a single static instance started once and shared by every subclass (the
 * classic Testcontainers "singleton container" pattern). It is never explicitly stopped — the
 * Testcontainers runtime reaps it when the JVM exits — and because all subclasses share the same
 * Spring test configuration, the application context is cached and reused across them.
 *
 * <p>Consequently the database is shared across all integration tests, so each test uses distinct
 * usernames to stay independent without per-test rollback. (Rollback would also suppress the
 * {@code AFTER_COMMIT} event that drives the Kafka flow, so committing for real is intentional.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Version-matched to the kafka-clients shipped with Spring Boot 4 (Kafka 4.0).
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    protected MockMvc mockMvc;

    protected void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isCreated());
    }

    protected String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    /** Creates a 120.50 FOOD expense for the given user and returns its id. */
    protected int createExpense(String token) throws Exception {
        String body = """
                {"description": "Lunch", "amount": 120.50, "category": "FOOD", "date": "2026-06-16"}
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    protected static String credentials(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }
}
