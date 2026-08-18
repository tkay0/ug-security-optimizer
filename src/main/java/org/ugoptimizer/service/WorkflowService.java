package org.ugoptimizer.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.dao.AuditEventDao;
import org.ugoptimizer.database.dao.WorkflowDao;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.ServiceRequest;

/** Centralizes canonical request lifecycle rules and their persisted audit history. */
public final class WorkflowService {

  public static final String PENDING = "PENDING";
  public static final String ASSIGNED = "ASSIGNED";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String COMPLETED = "COMPLETED";
  public static final String CANCELLED = "CANCELLED";

  private static final String DEFAULT_ACTOR = "SYSTEM";

  private final RequestService requestService;
  private final WorkflowDao workflowDao;
  private final AuditEventDao auditEventDao;
  private final Clock clock;

  public WorkflowService(DatabaseManager databaseManager) {
    this(
        new RequestService(
            Objects.requireNonNull(databaseManager, "databaseManager cannot be null")),
        new WorkflowDao(databaseManager),
        new AuditEventDao(databaseManager),
        Clock.systemUTC());
  }

  public WorkflowService(
      RequestService requestService,
      WorkflowDao workflowDao,
      AuditEventDao auditEventDao,
      Clock clock) {
    this.requestService = Objects.requireNonNull(requestService, "requestService cannot be null");
    this.workflowDao = Objects.requireNonNull(workflowDao, "workflowDao cannot be null");
    this.auditEventDao = Objects.requireNonNull(auditEventDao, "auditEventDao cannot be null");
    this.clock = Objects.requireNonNull(clock, "clock cannot be null");
  }

  public boolean canTransition(String currentStatus, String targetStatus) {
    validateStatus(currentStatus, "currentStatus");
    validateStatus(targetStatus, "targetStatus");
    return switch (currentStatus) {
      case PENDING -> ASSIGNED.equals(targetStatus) || CANCELLED.equals(targetStatus);
      case ASSIGNED -> IN_PROGRESS.equals(targetStatus) || CANCELLED.equals(targetStatus);
      case IN_PROGRESS -> COMPLETED.equals(targetStatus) || CANCELLED.equals(targetStatus);
      case COMPLETED, CANCELLED -> false;
      default -> throw new AssertionError("Validated status was not handled");
    };
  }

  public boolean isTerminal(String status) {
    validateStatus(status, "status");
    return COMPLETED.equals(status) || CANCELLED.equals(status);
  }

  public String[] getAllowedTransitions(String status) {
    validateStatus(status, "status");
    return switch (status) {
      case PENDING -> new String[] {ASSIGNED, CANCELLED};
      case ASSIGNED -> new String[] {IN_PROGRESS, CANCELLED};
      case IN_PROGRESS -> new String[] {COMPLETED, CANCELLED};
      case COMPLETED, CANCELLED -> new String[0];
      default -> throw new AssertionError("Validated status was not handled");
    };
  }

  public ServiceRequest transitionStatus(int requestId, String targetStatus) throws SQLException {
    return transitionStatus(requestId, targetStatus, DEFAULT_ACTOR);
  }

  public ServiceRequest transitionStatus(int requestId, String targetStatus, String actorType)
      throws SQLException {
    validateStatus(targetStatus, "targetStatus");
    requireActor(actorType);
    ServiceRequest current = requestService.requireRequest(requestId);
    if (PENDING.equals(current.getStatus()) && ASSIGNED.equals(targetStatus)) {
      throw new IllegalStateException(
          "Use AssignmentService so request and resource state change atomically");
    }
    if (!canTransition(current.getStatus(), targetStatus)) {
      throw new IllegalStateException(
          "Invalid request status transition: " + current.getStatus() + " -> " + targetStatus);
    }

    Instant timestamp = clock.instant();
    workflowDao.transitionStatus(
        requestId,
        current.getStatus(),
        targetStatus,
        timestamp,
        actorType,
        "Status changed from " + current.getStatus() + " to " + targetStatus);
    return requestService.requireRequest(requestId);
  }

  public ServiceRequest markAssigned(int requestId, String actorType) throws SQLException {
    throw new IllegalStateException(
        "Use AssignmentService so request and resource state change atomically");
  }

  public ServiceRequest startWork(int requestId, String actorType) throws SQLException {
    return transitionStatus(requestId, IN_PROGRESS, actorType);
  }

  public ServiceRequest completeRequest(int requestId, String actorType) throws SQLException {
    return transitionStatus(requestId, COMPLETED, actorType);
  }

  public ServiceRequest cancelRequest(int requestId, String actorType) throws SQLException {
    return transitionStatus(requestId, CANCELLED, actorType);
  }

  public AuditEvent[] getRequestHistory(int requestId) throws SQLException {
    requestService.requireRequest(requestId);
    return auditEventDao.findByEntity("SERVICE_REQUEST", requestId);
  }

  /** Returns the complete persisted audit log in chronological order. */
  public AuditEvent[] getAuditLog() throws SQLException {
    return auditEventDao.findAll();
  }

  private static void validateStatus(String status, String fieldName) {
    Objects.requireNonNull(status, fieldName + " cannot be null");
    switch (status) {
      case PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED -> {
        return;
      }
      default -> throw new IllegalArgumentException("Unsupported request status: " + status);
    }
  }

  private static void requireActor(String actorType) {
    Objects.requireNonNull(actorType, "actorType cannot be null");
    if (actorType.isBlank()) {
      throw new IllegalArgumentException("actorType cannot be blank");
    }
  }
}
