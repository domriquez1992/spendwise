package com.domriquez.spendwise.audit;

import com.domriquez.spendwise.audit.dto.AuditEventResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read access to the audit log.
 *
 * <p>{@code GET /api/v1/audit} returns the calling user's own trail. {@code GET /api/v1/audit/all}
 * returns everyone's and is restricted to administrators by the same method-level security used
 * elsewhere in the API.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditEventResponse> myAuditTrail() {
        return auditService.findForCurrentUser();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditEventResponse> fullAuditTrail() {
        return auditService.findAll();
    }
}
