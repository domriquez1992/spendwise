package com.domriquez.spendwise.expense;

/**
 * High-level spending categories. Stored as a string in the database
 * (see {@link Expense}) so the persisted value stays readable and stable
 * even if the enum order changes.
 */
public enum Category {
    FOOD,
    TRANSPORT,
    HOUSING,
    ENTERTAINMENT,
    HEALTH,
    UTILITIES,
    OTHER
}
