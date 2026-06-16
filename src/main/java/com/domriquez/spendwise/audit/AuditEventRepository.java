package com.domriquez.spendwise.audit;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Data access for the MongoDB audit log. CRUD comes from {@link MongoRepository}; the derived
 * queries below return entries newest-first, either for one user or across the whole system.
 */
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    /** A single user's audit trail, most recent first. */
    List<AuditEvent> findByUsernameOrderByTimestampDesc(String username);

    /** The entire audit trail, most recent first (admin view). */
    List<AuditEvent> findAllByOrderByTimestampDesc();
}
