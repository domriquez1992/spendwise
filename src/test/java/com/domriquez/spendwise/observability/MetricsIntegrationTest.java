package com.domriquez.spendwise.observability;

import com.domriquez.spendwise.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the observability metrics pipeline end to end: creating an expense increments the custom
 * domain counter, and the Prometheus scrape endpoint (reachable without authentication) renders it.
 */
class MetricsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void prometheusEndpointExposesCustomDomainMetric() throws Exception {
        // Drive one real, committed expense creation so the AFTER_COMMIT metrics listener fires.
        register("metrics-user", "password123");
        String token = login("metrics-user", "password123");
        createExpense(token);

        // The scrape endpoint is permitted without a token. It must render a baseline JVM meter
        // (proving the Prometheus registry is active) and our custom counter. Asserting the base
        // name (the Prometheus exposition appends _total to counters) keeps this robust.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("spendwise_expenses_created")));
    }
}
