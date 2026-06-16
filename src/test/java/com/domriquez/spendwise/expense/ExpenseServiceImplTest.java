package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository repository;

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        // Real mapper, mocked repository: we exercise actual mapping logic
        // while controlling persistence behaviour.
        service = new ExpenseServiceImpl(repository, new ExpenseMapper());
    }

    @Test
    void create_persistsAndReturnsResponse() {
        ExpenseRequest request = new ExpenseRequest(
                "Lunch", new BigDecimal("120.50"), Category.FOOD, LocalDate.now());

        when(repository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        ExpenseResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.description()).isEqualTo("Lunch");
        assertThat(response.amount()).isEqualByComparingTo("120.50");
        assertThat(response.category()).isEqualTo(Category.FOOD);
        verify(repository).save(any(Expense.class));
    }

    @Test
    void getById_whenPresent_returnsResponse() {
        Expense entity = new Expense();
        entity.setId(7L);
        entity.setDescription("Train ticket");
        entity.setAmount(new BigDecimal("45.00"));
        entity.setCategory(Category.TRANSPORT);
        entity.setDate(LocalDate.now());

        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        ExpenseResponse response = service.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.category()).isEqualTo(Category.TRANSPORT);
    }

    @Test
    void getById_whenMissing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_whenMissing_throwsAndDoesNotDelete() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void summary_aggregatesGrandTotal() {
        List<CategorySummary> categoryTotals = List.of(
                new CategorySummary(Category.FOOD, new BigDecimal("100.00")),
                new CategorySummary(Category.TRANSPORT, new BigDecimal("55.50"))
        );
        when(repository.summarizeByCategory(any(), any())).thenReturn(categoryTotals);

        SummaryResponse summary = service.summary(null, null);

        assertThat(summary.categories()).hasSize(2);
        assertThat(summary.grandTotal()).isEqualByComparingTo("155.50");
    }
}
