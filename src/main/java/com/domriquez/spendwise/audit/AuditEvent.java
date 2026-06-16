package com.domriquez.spendwise.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A single entry in the append-only audit log, stored in MongoDB.
 *
 * <p>Audit data is a natural fit for a document store: it is append-only, write-heavy, queried by
 * a few fields, and never updated — so it lives here rather than burdening the relational schema.
 * The transactional expense and user data remain in PostgreSQL.
 */
@Document(collection = "audit_events")
public class AuditEvent {

    @Id
    private String id;

    private AuditEventType type;

    /** Indexed: the audit endpoint lists a single user's events. */
    @Indexed
    private String username;

    private String detail;

    /** Affected entity id where applicable (e.g. an expense), otherwise null. */
    private Long entityId;

    /** Indexed and used to sort newest-first. */
    @Indexed
    private Instant timestamp;

    protected AuditEvent() {
        // Required by the MongoDB mapping layer.
    }

    public AuditEvent(AuditEventType type, String username, String detail, Long entityId) {
        this.type = type;
        this.username = username;
        this.detail = detail;
        this.entityId = entityId;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public AuditEventType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getDetail() {
        return detail;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
