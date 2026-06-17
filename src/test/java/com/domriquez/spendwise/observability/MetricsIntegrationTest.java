package com.domriquez.spendwise.observability;

import com.domriquez.spendwise.AbstractIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the observability metrics pipeline end to end: creating an expense records the custom
 * domain counter, that counter is held by the same registry the Actuator scrapes, and the
 * Prometheus scrape endpoint (reachable without authentication) renders it.
 */
class MetricsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void prometheusEndpointExposesCustomDomainMetric() throws Exception {
        register("metrics-user", "password123");
        String token = login("metrics-user", "password123");
        createExpense(token);

        Counter counter = meterRegistry.find("spendwise.expenses.created").counter();
        String diag = "registry=" + meterRegistry.getClass().getName()
                + " counterRegistered=" + (counter != null)
                + " count=" + (counter != null ? counter.count() : -1.0);

        // The custom counter must live in the registry that backs the scrape endpoint.
        assertThat(counter).as("custom counter not found in MeterRegistry -- " + diag).isNotNull();

        // ...and the Prometheus endpoint must render it (base name; the exposition appends _total),
        // alongside a baseline JVM meter proving the registry is being scraped.
        String body = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body)
                .as("Prometheus scrape missing the custom counter -- " + diag)
                .contains("jvm_memory_used_bytes")
                .contains("spendwise_expenses_created");
    }
}
