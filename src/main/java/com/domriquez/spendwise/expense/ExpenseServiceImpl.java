package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.exception.ExpenseNotFoundException;
import com.domriquez.spendwise.expense.dto.CategorySummary;
import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseMapper mapper;

    public ExpenseServiceImpl(ExpenseRepository repository, ExpenseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = repository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
        mapper.applyUpdate(expense, request);
        return mapper.toResponse(repository.save(expense));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        List<CategorySummary> categories = repository.summarizeByCategory(from, to);
        BigDecimal grandTotal = categories.stream()
                .map(CategorySummary::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SummaryResponse(categories, grandTotal);
    }
}
