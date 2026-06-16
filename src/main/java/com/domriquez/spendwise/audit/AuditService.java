package com.domriquez.spendwise.audit;

import com.domriquez.spendwise.audit.dto.AuditEventResponse;
import com.domriquez.spendwise.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Writes entries to the MongoDB audit log and serves them back.
 *
 * <p>Writing is best-effort: a failure to record an audit entry must never break the user-facing
 * action that triggered it, so persistence failures are logged and swallowed rather than
 * propagated. Reads are owner-scoped via {@link CurrentUserProvider}; an administrator can list
 * the full trail.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public AuditService(AuditEventRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    /** Persists an audit entry derived from a published {@link AuditableEvent}. Never throws. */
    public void record(AuditableEvent event) {
        try {
            repository.save(new AuditEvent(
                    event.type(), event.username(), event.detail(), event.entityId()));
        } catch (RuntimeException ex) {
            // Audit is a side channel: log and move on so the primary operation is unaffected.
            log.warn("Failed to record audit event {} for user {}: {}",
                    event.type(), event.username(), ex.getMessage());
        }
    }

    /** The current user's audit trail, newest first. */
    public List<AuditEventResponse> findForCurrentUser() {
        String username = currentUserProvider.requireCurrentUsername();
        return repository.findByUsernameOrderByTimestampDesc(username).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    /** The full audit trail, newest first (admin only). */
    public List<AuditEventResponse> findAll() {
        return repository.findAllByOrderByTimestampDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
