package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.model.AuditEvent;

/** Swing-facing audit operations; request status mutation remains on {@link RequestService}. */
public interface WorkflowService {
  AuditEvent logEvent(String eventType, int entityId, String details);

  List<AuditEvent> findAuditLog();
}
