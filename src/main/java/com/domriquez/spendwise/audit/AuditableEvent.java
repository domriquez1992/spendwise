package com.domriquez.spendwise.audit;

/**
 * In-process Spring application event signalling that something worth auditing happened.
 *
 * <p>Producers (the auth and expense services) publish this through the standard
 * {@code ApplicationEventPublisher}; {@link AuditEventListener} persists it to MongoDB. Going
 * through an event keeps the producers decoupled from the audit store — they record that an action
 * occurred without knowing or caring where the trail is written.
 *
 * @param type     the kind of action
 * @param username the actor (or the attempted username, for auth events)
 * @param detail   a short human-readable description
 * @param entityId the affected entity's id where applicable (e.g. an expense id), otherwise null
 */
public record AuditableEvent(
        AuditEventType type,
        String username,
        String detail,
        Long entityId
) {
    public AuditableEvent(AuditEventType type, String username, String detail) {
        this(type, username, detail, null);
    }
}
