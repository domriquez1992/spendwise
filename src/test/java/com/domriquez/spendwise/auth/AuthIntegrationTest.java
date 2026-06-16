package com.domriquez.spendwise.auth;

import com.domriquez.spendwise.AbstractIntegrationTest;
import com.domriquez.spendwise.user.Role;
import com.domriquez.spendwise.user.User;
import com.domriquez.spendwise.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the whole security stack against an in-memory H2 database: the real filter chain,
 * real JWT issuance and verification, BCrypt hashing, per-user data isolation, and role-based
 * method security. Nothing here is mocked.
 *
 * <p>Shared setup (the application context, MockMvc, and the register/login/createExpense
 * helpers) lives in {@link AbstractIntegrationTest}. Each test uses distinct usernames because
 * the test database is shared across methods.
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_login_andAccessProtectedEndpoint() throws Exception {
        register("alice", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("alice", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        // Protected endpoint rejects anonymous requests...
        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isUnauthorized());

        // ...and accepts a valid bearer token.
        mockMvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        register("bob", "correct-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("bob", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        register("carol", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("carol", "password123")))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotAccessEachOthersExpenses() throws Exception {
        register("owner1", "password123");
        register("owner2", "password123");
        String owner1Token = login("owner1", "password123");
        String owner2Token = login("owner2", "password123");

        int expenseId = createExpense(owner1Token);

        // The owner can read their own expense.
        mockMvc.perform(get("/api/v1/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + owner1Token))
                .andExpect(status().isOk());

        // Another user cannot — it does not exist as far as they are concerned (404, not 403).
        mockMvc.perform(get("/api/v1/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + owner2Token))
                .andExpect(status().isNotFound());

        // Nor can they delete it.
        mockMvc.perform(delete("/api/v1/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + owner2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminEndpoint_isForbiddenForUsersAndAllowedForAdmins() throws Exception {
        // A normal user is rejected by @PreAuthorize with 403.
        register("dave", "password123");
        String daveToken = login("dave", "password123");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + daveToken))
                .andExpect(status().isForbidden());

        // An admin (provisioned directly) is allowed through.
        userRepository.save(new User("root", passwordEncoder.encode("root-password"), Role.ADMIN));
        String adminToken = login("root", "root-password");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
