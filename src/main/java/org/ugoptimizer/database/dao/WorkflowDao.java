package org.ugoptimizer.database.dao;

import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.RequestStatusHistory;

/** Persists workflow changes, history, audit, and terminal resource release atomically. */
public final class WorkflowDao {
  private final DatabaseManager manager;

  public WorkflowDao(DatabaseManager manager) {
    this.manager = Objects.requireNonNull(manager, "databaseManager cannot be null");
  }

  public AuditEvent transitionStatus(
      int requestId, String expected, String next, Instant at, String actor, String details)
      throws SQLException {
    try (Connection c = manager.openConnection()) {
      c.setAutoCommit(false);
      try {
        AssignmentDao.updateExpected(
            c, "service_requests", "status", next, "request_id", requestId, expected);
        int[] active = findActive(c, requestId);
        Integer assignmentId = null;
        if (("COMPLETED".equals(next) || "CANCELLED".equals(next)) && active != null) {
          assignmentId = active[0];
          try (PreparedStatement s =
              c.prepareStatement(
                  "UPDATE assignments SET status='RELEASED', released_at=? WHERE assignment_id=?"
                      + " AND status='ACTIVE'")) {
            s.setString(1, at.toString());
            s.setInt(2, active[0]);
            if (s.executeUpdate() != 1)
              throw new SQLException("Active assignment changed concurrently");
          }
          AssignmentDao.updateExpected(
              c, "resources", "availability_status", "AVAILABLE", "resource_id", active[1], "BUSY");
          AssignmentDao.audit(
              c,
              "RESOURCE_RELEASED",
              "RESOURCE",
              active[1],
              actor,
              "Released from request " + requestId,
              at);
        }
        RequestStatusHistory h =
            new RequestStatusHistory(
                RequestStatusHistoryDao.nextHistoryId(c),
                requestId,
                expected,
                next,
                actor,
                at,
                RequestStatusHistory.STATUS_CHANGE,
                assignmentId,
                null,
                details);
        RequestStatusHistoryDao.insertHistory(c, h);
        AuditEvent event =
            new AuditEvent(
                AuditEventDao.nextEventId(c),
                "REQUEST_STATUS_CHANGED",
                at,
                "SERVICE_REQUEST",
                requestId,
                actor,
                details);
        AuditEventDao.insertEvent(c, event);
        c.commit();
        return event;
      } catch (SQLException | RuntimeException f) {
        AssignmentDao.rollback(c, f);
        throw f;
      }
    }
  }

  private static int[] findActive(Connection c, int requestId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "SELECT assignment_id,resource_id FROM assignments WHERE request_id=? AND"
                + " status='ACTIVE'")) {
      s.setInt(1, requestId);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? new int[] {r.getInt(1), r.getInt(2)} : null;
      }
    }
  }
}
