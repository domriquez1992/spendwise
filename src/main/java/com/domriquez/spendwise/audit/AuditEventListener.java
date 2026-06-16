package com.domriquez.spendwise.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists {@link AuditableEvent}s to the audit log once the transaction that produced them has
 * committed.
 *
 * <p>Binding to {@link TransactionPhase#AFTER_COMMIT} means a rolled-back action (e.g. a create
 * that failed) leaves no misleading audit entry. {@code fallbackExecution = true} ensures events
 * published outside any transaction are still recorded immediately, so auth events (which may run
 * in a read-only transaction or none at all) are never lost.
 */
@Component
public class AuditEventListener {

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(AuditableEvent event) {
        auditService.record(event);
    }
}
