package com.domriquez.spendwise.audit;

/**
 * The kinds of domain actions recorded in the audit log.
 */
public enum AuditEventType {
    USER_REGISTERED,
    LOGIN_SUCCESS,
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED
}
