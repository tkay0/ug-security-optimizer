package org.ugoptimizer.service;

import java.util.List;
import org.ugoptimizer.model.AuditEvent;

/**
 * Records and retrieves the audit trail for dispatch workflow actions. A real
 * implementation wraps {@code AuditEventDao}; {@code InMemoryWorkflowService}
 * exists for development before that lands.
 *
 * <p>{@code DispatchWorkflowMenu} still owns its own undo stack (a
 * {@code CustomStack} of previous statuses) since that is UI workflow control,
 * not persisted state; it calls {@link RequestService#updateStatus} plus
 * {@link #logEvent} for both forward actions and undos.</p>
 */
public interface WorkflowService {

    AuditEvent logEvent(String eventType, int entityId, String details);

    List<AuditEvent> findAuditLog();
}
