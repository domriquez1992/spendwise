package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.audit.AuditEventType;
import com.domriquez.spendwise.audit.AuditableEvent;
import com.domriquez.spendwise.event.ExpenseCreatedEvent;
import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import com.domriquez.spendwise.security.CurrentUserProvider;
import com.domriquez.spendwise.user.User;
import com.domriquez.spendwise.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Expense operations, scoped to the authenticated user. Every read and write is constrained to
 * rows owned by the current user, so one user can never see or modify another's expenses.
 *
 * <p>The current username is resolved through {@link CurrentUserProvider}, which keeps this class
 * free of direct calls to Spring Security's static context holder and easy to unit test.
 *
 * <p>Each write does two cross-cutting things after persisting: it publishes an
 * {@link AuditableEvent} (recorded to the MongoDB audit log after commit) and evicts the user's
 * cached spending summary so the next read recomputes from current data. Reading the summary is
 * delegated to {@link ExpenseSummaryCache} so Spring's cache proxy can intercept it.
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseMapper mapper;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final ExpenseSummaryCache summaryCache;

    public ExpenseServiceImpl(ExpenseRepository repository,
                              ExpenseMapper mapper,
                              UserRepository userRepository,
                              CurrentUserProvider currentUserProvider,
                              ApplicationEventPublisher eventPublisher,
                              ExpenseSummaryCache summaryCache) {
        this.repository = repository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
        this.summaryCache = summaryCache;
    }

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = mapper.toEntity(request);
        expense.setOwner(currentUser());
        Expense saved = repository.save(expense);
        String username = saved.getOwner().getUsername();
        // Published within the transaction but dispatched to Kafka only after commit
        // (see ExpenseEventPublisher), so consumers never see uncommitted data.
        eventPublisher.publishEvent(new ExpenseCreatedEvent(
                saved.getId(),
                username,
                saved.getCategory(),
                saved.getAmount(),
                saved.getDate()));
        auditAndEvict(AuditEventType.EXPENSE_CREATED, username, saved.getId(),
                "Created expense '" + saved.getDescription() + "'");
        return mapper.toResponse(saved);
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
        String username = currentUsername();
        Expense expense = repository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        mapper.applyUpdate(expense, request);
        Expense saved = repository.save(expense);
        auditAndEvict(AuditEventType.EXPENSE_UPDATED, username, saved.getId(),
                "Updated expense '" + saved.getDescription() + "'");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        String username = currentUsername();
        Expense expense = repository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        repository.delete(expense);
        auditAndEvict(AuditEventType.EXPENSE_DELETED, username, id,
                "Deleted expense " + id);
    }

    @Override
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        // Delegated to a separate bean so the @Cacheable interception actually fires. The single
        // repository query inside is self-transactional, so no @Transactional is needed here.
        return summaryCache.summarize(currentUsername(), from, to);
    }

    /**
     * Publishes an audit event (persisted after commit) and evicts the user's cached summary.
     * Shared by every write so the two cross-cutting concerns stay consistent.
     */
    private void auditAndEvict(AuditEventType type, String username, Long entityId, String detail) {
        eventPublisher.publishEvent(new AuditableEvent(type, username, detail, entityId));
        summaryCache.evict(username);
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
