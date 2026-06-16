package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    @Test
    void create_withValidPayload_returns201() throws Exception {
        ExpenseResponse response = new ExpenseResponse(
                1L, "Lunch", new BigDecimal("120.50"), Category.FOOD, LocalDate.now(), Instant.now());
        when(service.create(any())).thenReturn(response);

        String body = """
                {
                  "description": "Lunch",
                  "amount": 120.50,
                  "category": "FOOD",
                  "date": "%s"
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    void create_withInvalidPayload_returns400() throws Exception {
        // Blank description and a negative amount both violate the constraints.
        String body = """
                {
                  "description": "",
                  "amount": -5,
                  "category": "FOOD",
                  "date": "2020-01-01"
                }
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").value("One or more fields are invalid"));
    }

    @Test
    void getById_whenFound_returns200() throws Exception {
        ExpenseResponse response = new ExpenseResponse(
                5L, "Rent", new BigDecimal("9000.00"), Category.HOUSING, LocalDate.now(), Instant.now());
        when(service.getById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/expenses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.category").value("HOUSING"));
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        when(service.getById(eq(99L))).thenThrow(new ExpenseNotFoundException(99L));

        mockMvc.perform(get("/api/v1/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Expense Not Found"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/1"))
                .andExpect(status().isNoContent());
    }
}
