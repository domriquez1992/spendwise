package com.domriquez.spendwise.audit.dto;

import com.domriquez.spendwise.audit.AuditEvent;
import com.domriquez.spendwise.audit.AuditEventType;

import java.time.Instant;

/**
 * API view of an {@link AuditEvent}.
 */
public record AuditEventResponse(
        String id,
        AuditEventType type,
        String username,
        String detail,
        Long entityId,
        Instant timestamp
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getType(),
                event.getUsername(),
                event.getDetail(),
                event.getEntityId(),
                event.getTimestamp());
    }
}
