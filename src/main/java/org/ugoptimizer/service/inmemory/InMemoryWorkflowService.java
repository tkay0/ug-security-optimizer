package org.ugoptimizer.service.inmemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.service.WorkflowService;

/**
 * In-memory {@link WorkflowService}. Replace with a real DAO-backed
 * implementation once the database team's work lands.
 */
public final class InMemoryWorkflowService implements WorkflowService {

    private final List<AuditEvent> auditLog = new ArrayList<>();
    private int nextEventId = 1;

    @Override
    public AuditEvent logEvent(String eventType, int entityId, String details) {
        AuditEvent event = new AuditEvent(nextEventId++, eventType, Instant.now(),
                "SERVICE_REQUEST", entityId, "DISPATCHER", details);
        auditLog.add(event);
        return event;
    }

    @Override
    public List<AuditEvent> findAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }
}
