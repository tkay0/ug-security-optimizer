package org.ugoptimizer.database.dao;

import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.AssignmentMapper;
import org.ugoptimizer.model.Assignment;
import org.ugoptimizer.model.AuditEvent;
import org.ugoptimizer.model.RequestStatusHistory;

/** Owns assignment persistence, including the atomic dispatch transaction. */
public final class AssignmentDao {
  static final String COLUMNS =
      "assignment_id, request_id, resource_id, assigned_at, "
          + "released_at, status, estimated_response_time_min";
  private final DatabaseManager manager;

  public AssignmentDao(DatabaseManager manager) {
    this.manager = Objects.requireNonNull(manager, "databaseManager cannot be null");
  }

  public Optional<Assignment> findById(int id) throws SQLException {
    return findOne("assignment_id", id, false);
  }

  public Optional<Assignment> findActiveByRequestId(int id) throws SQLException {
    return findOne("request_id", id, true);
  }

  public Optional<Assignment> findActiveByResourceId(int id) throws SQLException {
    return findOne("resource_id", id, true);
  }

  public Assignment[] findByRequestId(int id) throws SQLException {
    requireId(id);
    return query(
        "SELECT "
            + COLUMNS
            + " FROM assignments WHERE request_id = ?"
            + " ORDER BY assigned_at, assignment_id",
        id);
  }

  public Assignment[] findAllActive() throws SQLException {
    return query(
        "SELECT "
            + COLUMNS
            + " FROM assignments WHERE status = 'ACTIVE'"
            + " ORDER BY assigned_at, assignment_id",
        null);
  }

  public Assignment createAssignment(
      int requestId, int resourceId, double responseTime, Instant timestamp, String actor)
      throws SQLException {
    requireId(requestId);
    requireId(resourceId);
    if (!Double.isFinite(responseTime) || responseTime < 0) {
      throw new IllegalArgumentException("responseTime must be finite and non-negative");
    }
    Objects.requireNonNull(timestamp, "timestamp cannot be null");
    requireText(actor, "actor");
    try (Connection c = manager.openConnection()) {
      c.setAutoCommit(false);
      try {
        updateExpected(
            c, "service_requests", "status", "ASSIGNED", "request_id", requestId, "PENDING");
        updateExpected(
            c, "resources", "availability_status", "BUSY", "resource_id", resourceId, "AVAILABLE");
        int id = nextId(c, "assignments", "assignment_id");
        Assignment assignment =
            new Assignment(
                id, requestId, resourceId, timestamp, null, Assignment.ACTIVE, responseTime);
        insert(c, assignment);
        RequestStatusHistory history =
            new RequestStatusHistory(
                RequestStatusHistoryDao.nextHistoryId(c),
                requestId,
                "PENDING",
                "ASSIGNED",
                actor,
                timestamp,
                RequestStatusHistory.ASSIGNMENT,
                id,
                null,
                "Assigned resource " + resourceId);
        RequestStatusHistoryDao.insertHistory(c, history);
        audit(
            c,
            "REQUEST_ASSIGNED",
            "SERVICE_REQUEST",
            requestId,
            actor,
            "Assigned resource " + resourceId,
            timestamp);
        audit(
            c,
            "RESOURCE_ASSIGNED",
            "RESOURCE",
            resourceId,
            actor,
            "Assigned to request " + requestId,
            timestamp);
        c.commit();
        return assignment;
      } catch (SQLException | RuntimeException failure) {
        rollback(c, failure);
        throw failure;
      }
    }
  }

  private Optional<Assignment> findOne(String column, int id, boolean active) throws SQLException {
    requireId(id);
    String sql =
        "SELECT "
            + COLUMNS
            + " FROM assignments WHERE "
            + column
            + " = ?"
            + (active ? " AND status = 'ACTIVE'" : "")
            + " LIMIT 1";
    Assignment[] rows = query(sql, id);
    return rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);
  }

  private Assignment[] query(String sql, Integer id) throws SQLException {
    try (Connection c = manager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (id != null) s.setInt(1, id);
      try (ResultSet r = s.executeQuery()) {
        Assignment[] a = new Assignment[8];
        int n = 0;
        while (r.next()) {
          if (n == a.length) {
            Assignment[] x = new Assignment[a.length * 2];
            System.arraycopy(a, 0, x, 0, n);
            a = x;
          }
          a[n++] = AssignmentMapper.map(r);
        }
        Assignment[] out = new Assignment[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
      }
    }
  }

  static void insert(Connection c, Assignment a) throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement("INSERT INTO assignments (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?)")) {
      s.setInt(1, a.getAssignmentId());
      s.setInt(2, a.getRequestId());
      s.setInt(3, a.getResourceId());
      s.setString(4, a.getAssignedAt().toString());
      s.setNull(5, Types.VARCHAR);
      s.setString(6, a.getStatus());
      s.setDouble(7, a.getEstimatedResponseTimeMinutes());
      if (s.executeUpdate() != 1) throw new SQLException("Assignment insert failed");
    }
  }

  static void updateExpected(
      Connection c,
      String table,
      String field,
      String value,
      String idField,
      int id,
      String expected)
      throws SQLException {
    try (PreparedStatement s =
        c.prepareStatement(
            "UPDATE "
                + table
                + " SET "
                + field
                + " = ? WHERE "
                + idField
                + " = ? AND "
                + field
                + " = ?")) {
      s.setString(1, value);
      s.setInt(2, id);
      s.setString(3, expected);
      if (s.executeUpdate() != 1) throw new SQLException(table + " is missing or not " + expected);
    }
  }

  static int nextId(Connection c, String table, String column) throws SQLException {
    try (Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT COALESCE(MAX(" + column + "),0)+1 FROM " + table)) {
      if (!r.next()) throw new SQLException("ID allocation failed");
      return r.getInt(1);
    }
  }

  static void audit(
      Connection c, String type, String entity, int id, String actor, String details, Instant at)
      throws SQLException {
    AuditEvent e =
        new AuditEvent(AuditEventDao.nextEventId(c), type, at, entity, id, actor, details);
    AuditEventDao.insertEvent(c, e);
  }

  static void rollback(Connection c, Throwable f) {
    try {
      c.rollback();
    } catch (SQLException e) {
      f.addSuppressed(e);
    }
  }

  private static void requireId(int id) {
    if (id <= 0) throw new IllegalArgumentException("identifier must be positive");
  }

  private static void requireText(String v, String n) {
    Objects.requireNonNull(v, n + " cannot be null");
    if (v.isBlank()) throw new IllegalArgumentException(n + " cannot be blank");
  }
}
