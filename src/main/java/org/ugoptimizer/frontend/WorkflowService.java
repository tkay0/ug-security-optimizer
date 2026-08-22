package org.ugoptimizer.frontend;

import java.util.List;
import java.util.Optional;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.AuditEvent;

/** Swing-facing audit operations; request status mutation remains on {@link RequestService}. */
public interface WorkflowService {
  AuditEvent logEvent(String eventType, int entityId, String details);

  List<AuditEvent> findAuditLog();

  /** Returns the persisted active assignment for a request, when supported. */
  default Optional<Assignment> findActiveAssignment(int requestId) {
    return Optional.empty();
  }
}
