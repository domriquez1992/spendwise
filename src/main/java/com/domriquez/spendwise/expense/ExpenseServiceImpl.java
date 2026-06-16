package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import com.domriquez.spendwise.security.CurrentUserProvider;
import com.domriquez.spendwise.user.User;
import com.domriquez.spendwise.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Expense operations, scoped to the authenticated user. Every read and write is constrained to
 * rows owned by the current user, so one user can never see or modify another's expenses.
 *
 * <p>The current username is resolved through {@link CurrentUserProvider}, which keeps this class
 * free of direct calls to Spring Security's static context holder and easy to unit test.
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseMapper mapper;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseServiceImpl(ExpenseRepository repository,
                              ExpenseMapper mapper,
                              UserRepository userRepository,
                              CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = mapper.toEntity(request);
        expense.setOwner(currentUser());
        return mapper.toResponse(repository.save(expense));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(Pageable pageable) {
        return repository.findByOwnerUsername(currentUsername(), pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return repository.findByIdAndOwnerUsername(id, currentUsername())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = repository.findByIdAndOwnerUsername(id, currentUsername())
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        mapper.applyUpdate(expense, request);
        return mapper.toResponse(repository.save(expense));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Expense expense = repository.findByIdAndOwnerUsername(id, currentUsername())
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        repository.delete(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        List<CategorySummary> categories = repository.summarizeByCategory(currentUsername(), from, to);
        BigDecimal grandTotal = categories.stream()
                .map(CategorySummary::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SummaryResponse(categories, grandTotal);
    }

    private String currentUsername() {
        return currentUserProvider.requireCurrentUsername();
    }

    private User currentUser() {
        String username = currentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in the database: " + username));
    }
}
