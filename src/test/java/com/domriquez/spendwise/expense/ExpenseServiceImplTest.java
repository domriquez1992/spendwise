package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.event.ExpenseCreatedEvent;
import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import com.domriquez.spendwise.security.CurrentUserProvider;
import com.domriquez.spendwise.user.Role;
import com.domriquez.spendwise.user.User;
import com.domriquez.spendwise.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    private static final String USERNAME = "alice";

    @Mock
    private ExpenseRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        // Real mapper, mocked collaborators: we exercise the actual mapping logic while
        // controlling persistence and the "who is logged in" decision.
        service = new ExpenseServiceImpl(
                repository, new ExpenseMapper(), userRepository, currentUserProvider, eventPublisher);
        when(currentUserProvider.requireCurrentUsername()).thenReturn(USERNAME);
    }

    @Test
    void create_assignsOwnerAndReturnsResponse() {
        ExpenseRequest request = new ExpenseRequest(
                "Lunch", new BigDecimal("120.50"), Category.FOOD, LocalDate.now());
        User owner = new User(USERNAME, "hash", Role.USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(owner));
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
        // Creating an expense raises a domain event (relayed to Kafka after commit).
        verify(eventPublisher).publishEvent(any(ExpenseCreatedEvent.class));
    }

    @Test
    void getById_whenOwnedByCurrentUser_returnsResponse() {
        Expense entity = new Expense();
        entity.setId(7L);
        entity.setDescription("Train ticket");
        entity.setAmount(new BigDecimal("45.00"));
        entity.setCategory(Category.TRANSPORT);
        entity.setDate(LocalDate.now());
        when(repository.findByIdAndOwnerUsername(7L, USERNAME)).thenReturn(Optional.of(entity));

        ExpenseResponse response = service.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.category()).isEqualTo(Category.TRANSPORT);
    }

    @Test
    void getById_whenMissingOrNotOwned_throwsNotFound() {
        // Empty also covers "exists but belongs to someone else" — the query is owner-scoped.
        when(repository.findByIdAndOwnerUsername(99L, USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_whenMissingOrNotOwned_throwsAndDoesNotDelete() {
        when(repository.findByIdAndOwnerUsername(99L, USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void summary_aggregatesGrandTotalForCurrentUser() {
        List<CategorySummary> categoryTotals = List.of(
                new CategorySummary(Category.FOOD, new BigDecimal("100.00")),
                new CategorySummary(Category.TRANSPORT, new BigDecimal("55.50"))
        );
        when(repository.summarizeByCategory(eq(USERNAME), any(), any())).thenReturn(categoryTotals);

        SummaryResponse summary = service.summary(null, null);

        assertThat(summary.categories()).hasSize(2);
        assertThat(summary.grandTotal()).isEqualByComparingTo("155.50");
    }
}
