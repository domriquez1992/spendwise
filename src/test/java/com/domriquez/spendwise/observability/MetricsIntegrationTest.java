package com.domriquez.spendwise.observability;

import com.domriquez.spendwise.AbstractIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the observability metrics pipeline: the Prometheus scrape endpoint is exposed, and the
 * custom domain counter is registered and incremented when an expense is created.
 */
class MetricsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void prometheusEndpointIsExposedAndCustomMetricIsRecorded() throws Exception {
        register("metrics-user", "password123");
        String token = login("metrics-user", "password123");
        createExpense(token);

        // The Prometheus scrape endpoint is reachable without authentication and renders metrics.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));

        // The custom domain counter was registered and incremented by the AFTER_COMMIT listener.
        Counter counter = meterRegistry.find("spendwise.expenses.created")
                .tag("category", "FOOD").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThanOrEqualTo(1.0);
    }
}
