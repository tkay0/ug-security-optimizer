package org.ugoptimizer.database.dao;

import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.model.RequestStatusHistory;

/** Atomically reverses the latest unreversed request workflow history row. */
public final class UndoDao {
  private final DatabaseManager manager;

  public UndoDao(DatabaseManager manager) {
    this.manager = Objects.requireNonNull(manager, "databaseManager cannot be null");
  }

  public RequestStatusHistory undoLatest(int requestId, Instant at, String actor)
      throws SQLException {
    try (Connection c = manager.openConnection()) {
      c.setAutoCommit(false);
      try {
        RequestStatusHistory original =
            RequestStatusHistoryDao.findLatestReversible(c, requestId)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No reversible history for request " + requestId));
        AssignmentDao.updateExpected(
            c,
            "service_requests",
            "status",
            original.getPreviousStatus(),
            "request_id",
            requestId,
            original.getNewStatus());
        Integer assignmentId = original.getAssignmentId();
        Integer resourceId = null;
        if (assignmentId != null) {
          resourceId = resourceId(c, assignmentId);
          if (RequestStatusHistory.ASSIGNMENT.equals(original.getChangeType())) {
            release(c, assignmentId, at);
            AssignmentDao.updateExpected(
                c,
                "resources",
                "availability_status",
                "AVAILABLE",
                "resource_id",
                resourceId,
                "BUSY");
          } else if ("COMPLETED".equals(original.getNewStatus())
              || "CANCELLED".equals(original.getNewStatus())) {
            try (PreparedStatement s =
                c.prepareStatement(
                    "UPDATE assignments SET status='ACTIVE',released_at=NULL WHERE assignment_id=?"
                        + " AND status='RELEASED'")) {
              s.setInt(1, assignmentId);
              if (s.executeUpdate() != 1)
                throw new SQLException("Released assignment cannot be restored");
            }
            AssignmentDao.updateExpected(
                c,
                "resources",
                "availability_status",
                "BUSY",
                "resource_id",
                resourceId,
                "AVAILABLE");
          }
        }
        RequestStatusHistory undo =
            new RequestStatusHistory(
                RequestStatusHistoryDao.nextHistoryId(c),
                requestId,
                original.getNewStatus(),
                original.getPreviousStatus(),
                actor,
                at,
                RequestStatusHistory.UNDO,
                assignmentId,
                original.getHistoryId(),
                "Undid history " + original.getHistoryId());
        RequestStatusHistoryDao.insertHistory(c, undo);
        AssignmentDao.audit(
            c, "REQUEST_STATUS_UNDONE", "SERVICE_REQUEST", requestId, actor, undo.getDetails(), at);
        if (resourceId != null)
          AssignmentDao.audit(
              c, "RESOURCE_STATE_UNDONE", "RESOURCE", resourceId, actor, undo.getDetails(), at);
        c.commit();
        return undo;
      } catch (SQLException | RuntimeException f) {
        AssignmentDao.rollback(c, f);
        throw f;
      }
    }
  }

  private static int resourceId(Connection c, int assignmentId) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("SELECT resource_id FROM assignments WHERE assignment_id=?")) {
      s.setInt(1, assignmentId);
      try (ResultSet r = s.executeQuery()) {
        if (!r.next()) throw new SQLException("Assignment missing");
        return r.getInt(1);
      }
    }
  }

  private static void release(Connection c, int id, Instant at) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "UPDATE assignments SET status='RELEASED',released_at=? WHERE assignment_id=? AND"
                + " status='ACTIVE'")) {
      s.setString(1, at.toString());
      s.setInt(2, id);
      if (s.executeUpdate() != 1) throw new SQLException("Active assignment cannot be released");
    }
  }
}
