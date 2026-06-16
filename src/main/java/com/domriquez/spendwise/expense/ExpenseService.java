package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import com.domriquez.spendwise.expense.dto.SummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Business operations for expenses. Coding to an interface keeps the controller
 * decoupled from the implementation and makes the service trivial to mock in tests.
 */
public interface ExpenseService {

    ExpenseResponse create(ExpenseRequest request);

    Page<ExpenseResponse> list(Pageable pageable);

    ExpenseResponse getById(Long id);

    ExpenseResponse update(Long id, ExpenseRequest request);

    void delete(Long id);

    SummaryResponse summary(LocalDate from, LocalDate to);
}
