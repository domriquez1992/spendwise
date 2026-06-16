package com.domriquez.spendwise.expense;

import com.domriquez.spendwise.expense.dto.ExpenseRequest;
import com.domriquez.spendwise.expense.dto.ExpenseResponse;
import org.springframework.stereotype.Component;

/**
 * Translates between the {@link Expense} entity and its DTOs.
 *
 * <p>Hand-written on purpose to keep the dependency footprint small and the
 * mapping explicit. In a larger codebase this is a natural place to introduce
 * MapStruct to generate the boilerplate at compile time.
 */
@Component
public class ExpenseMapper {

    public Expense toEntity(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        return expense;
    }

    public void applyUpdate(Expense expense, ExpenseRequest request) {
        apply(expense, request);
    }

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDate(),
                expense.getCreatedAt()
        );
    }

    private void apply(Expense expense, ExpenseRequest request) {
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setDate(request.date());
    }
}
