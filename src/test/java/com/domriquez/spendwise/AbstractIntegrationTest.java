package com.domriquez.spendwise;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for full-stack integration tests. Boots the entire application against an in-memory
 * H2 database plus three real, throwaway services started with Testcontainers: Kafka, MongoDB, and
 * Redis. All three are static singletons started once and shared by every subclass (the classic
 * Testcontainers "singleton container" pattern); the runtime reaps them when the JVM exits.
 *
 * <p>MongoDB and Redis are wired with Spring Boot's {@code @ServiceConnection}: Boot reads each
 * started container's real, host-reachable address and registers a {@code ConnectionDetails} bean
 * that takes priority over any {@code spring.data.*} property. This is more robust than overriding
 * connection properties by hand, which is sensitive to how the container's address is resolved.
 * Kafka keeps a {@code @DynamicPropertySource} override, which only needs the bootstrap-servers
 * string.
 *
 * <p>The database is shared across all integration tests, so each test uses distinct usernames to
 * stay independent without per-test rollback. (Rollback would also suppress the {@code AFTER_COMMIT}
 * events that drive the Kafka and audit flows, so committing for real is intentional.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Version-matched to the kafka-clients shipped with Spring Boot 4 (Kafka 4.0).
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

    // Document store for the audit log.
    @ServiceConnection
    static final MongoDBContainer MONGO =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    // Cache backing store. Redis has no dedicated Testcontainers module, so a core GenericContainer
    // runs the official image; the name hint tells @ServiceConnection it is a Redis service.
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        KAFKA.start();
        MONGO.start();
        REDIS.start();
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
