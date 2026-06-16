package com.domriquez.spendwise.audit;

import com.domriquez.spendwise.AbstractIntegrationTest;
import com.domriquez.spendwise.expense.ExpenseSummaryCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the two polyglot-persistence features added in this iteration, run
 * against real MongoDB and Redis instances started by Testcontainers (see
 * {@link AbstractIntegrationTest}):
 *
 * <ul>
 *   <li>domain actions are recorded to the MongoDB audit log, and
 *   <li>the per-user spending summary is cached in Redis and evicted when the user writes.
 * </ul>
 */
class AuditAndCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void auditLog_recordsRegistrationLoginAndExpenseCreation() throws Exception {
        String user = "audituser";
        register(user, "password123");
        String token = login(user, "password123");
        createExpense(token);

        // The audit listener fires on AFTER_COMMIT synchronously, so the entries are already
        // persisted by the time the requests above have returned — no polling required.
        List<AuditEventType> types = auditEventRepository.findByUsernameOrderByTimestampDesc(user)
                .stream()
                .map(AuditEvent::getType)
                .toList();
        assertThat(types).contains(
                AuditEventType.USER_REGISTERED,
                AuditEventType.LOGIN_SUCCESS,
                AuditEventType.EXPENSE_CREATED);

        // The owner-scoped endpoint exposes the same trail.
        mockMvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", hasItem("EXPENSE_CREATED")));
    }

    @Test
    void summary_isCachedPerUserAndEvictedOnWrite() throws Exception {
        String user = "cacheuser";
        register(user, "password123");
        String token = login(user, "password123");
        createExpense(token);

        Cache cache = cacheManager.getCache(ExpenseSummaryCache.SUMMARY_CACHE);
        assertThat(cache).isNotNull();
        // Nothing is cached until the summary is first read.
        assertThat(cache.get(user)).isNull();

        // First read populates the cache, round-tripping the value through Redis.
        mockMvc.perform(get("/api/v1/expenses/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(cache.get(user)).isNotNull();

        // A subsequent write evicts the owner's cached summary, so the next read recomputes.
        createExpense(token);
        assertThat(cache.get(user)).isNull();
    }
}
