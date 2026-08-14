package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.AuditEventMapper;
import org.ugoptimizer.model.AuditEvent;

/** Provides persistent access to polymorphic audit history. */
public final class AuditEventDao {

  private static final String COLUMNS =
      "event_id, event_type, event_timestamp, entity_type, entity_id, actor_type, details";
  private static final int INITIAL_RESULT_CAPACITY = 16;

  private final DatabaseManager databaseManager;

  public AuditEventDao(DatabaseManager databaseManager) {
    this.databaseManager =
        Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
  }

  public Optional<AuditEvent> findById(int eventId) throws SQLException {
    requirePositiveId(eventId, "eventId");
    String sql = "SELECT " + COLUMNS + " FROM audit_events WHERE event_id = ?";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, eventId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(AuditEventMapper.map(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new SQLException("Failed to find audit event " + eventId, exception);
    }
  }

  /** Returns the complete audit log in deterministic chronological order. */
  public AuditEvent[] findAll() throws SQLException {
    String sql = "SELECT " + COLUMNS + " FROM audit_events ORDER BY event_timestamp, event_id";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      AuditEvent[] buffer = new AuditEvent[INITIAL_RESULT_CAPACITY];
      int count = 0;
      while (resultSet.next()) {
        if (count == buffer.length) {
          buffer = grow(buffer);
        }
        buffer[count++] = AuditEventMapper.map(resultSet);
      }
      AuditEvent[] result = new AuditEvent[count];
      System.arraycopy(buffer, 0, result, 0, count);
      return result;
    } catch (SQLException exception) {
      throw new SQLException("Failed to read all audit events", exception);
    }
  }

  public AuditEvent[] findByEntity(String entityType, int entityId) throws SQLException {
    validateEntityType(entityType);
    requirePositiveId(entityId, "entityId");
    String sql =
        "SELECT "
            + COLUMNS
            + " FROM audit_events WHERE entity_type = ? AND entity_id = ?"
            + " ORDER BY event_timestamp, event_id";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, entityType);
      statement.setInt(2, entityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        AuditEvent[] buffer = new AuditEvent[INITIAL_RESULT_CAPACITY];
        int count = 0;
        while (resultSet.next()) {
          if (count == buffer.length) {
            buffer = grow(buffer);
          }
          buffer[count++] = AuditEventMapper.map(resultSet);
        }
        AuditEvent[] result = new AuditEvent[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
      }
    } catch (SQLException exception) {
      throw new SQLException(
          "Failed to read audit history for " + entityType + ' ' + entityId, exception);
    }
  }

  public void insert(AuditEvent event) throws SQLException {
    Objects.requireNonNull(event, "event cannot be null");
    validateEntityType(event.getEntityType());
    try (Connection connection = databaseManager.openConnection()) {
      connection.setAutoCommit(false);
      try {
        requireExistingTarget(connection, event.getEntityType(), event.getEntityId());
        insertEvent(connection, event);
        connection.commit();
      } catch (SQLException exception) {
        rollback(connection, exception);
        throw new SQLException("Failed to insert audit event " + event.getEventId(), exception);
      }
    }
  }

  public AuditEvent insertGenerated(
      String type, Instant at, String entity, int id, String actor, String details)
      throws SQLException {
    try (Connection c = databaseManager.openConnection()) {
      c.setAutoCommit(false);
      try {
        requireExistingTarget(c, entity, id);
        AuditEvent e = new AuditEvent(nextEventId(c), type, at, entity, id, actor, details);
        insertEvent(c, e);
        c.commit();
        return e;
      } catch (SQLException | RuntimeException f) {
        rollback(c, f);
        throw f;
      }
    }
  }

  private static void requireExistingTarget(Connection connection, String entityType, int entityId)
      throws SQLException {
    String targetQuery =
        switch (entityType) {
          case "SERVICE_REQUEST" -> "SELECT 1 FROM service_requests WHERE request_id = ?";
          case "RESOURCE" -> "SELECT 1 FROM resources WHERE resource_id = ?";
          case "ROAD" -> "SELECT 1 FROM roads WHERE road_id = ?";
          default -> throw new IllegalArgumentException("Unsupported entityType: " + entityType);
        };
    try (PreparedStatement statement = connection.prepareStatement(targetQuery)) {
      statement.setInt(1, entityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new SQLException("Audit target does not exist: " + entityType + ' ' + entityId);
        }
      }
    }
  }

  static void insertEvent(Connection connection, AuditEvent event) throws SQLException {
    String sql = "INSERT INTO audit_events (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, event.getEventId());
      statement.setString(2, event.getEventType());
      statement.setString(3, event.getTimestamp().toString());
      statement.setString(4, event.getEntityType());
      statement.setInt(5, event.getEntityId());
      setNullableText(statement, 6, event.getActorType());
      setNullableText(statement, 7, event.getDetails());
      int affectedRows = statement.executeUpdate();
      if (affectedRows != 1) {
        throw new SQLException("Audit insert affected " + affectedRows + " rows");
      }
    }
  }

  static int nextEventId(Connection c) throws SQLException {
    try (PreparedStatement s =
            c.prepareStatement("SELECT COALESCE(MAX(event_id),0)+1 FROM audit_events");
        ResultSet r = s.executeQuery()) {
      if (!r.next()) throw new SQLException("Audit ID allocation failed");
      return r.getInt(1);
    }
  }

  private static void rollback(Connection connection, Throwable failure) {
    try {
      connection.rollback();
    } catch (SQLException rollbackException) {
      failure.addSuppressed(rollbackException);
    }
  }

  private static AuditEvent[] grow(AuditEvent[] current) throws SQLException {
    if (current.length > Integer.MAX_VALUE / 2) {
      throw new SQLException("Audit result exceeds supported array capacity");
    }
    AuditEvent[] expanded = new AuditEvent[current.length * 2];
    System.arraycopy(current, 0, expanded, 0, current.length);
    return expanded;
  }

  private static void setNullableText(PreparedStatement statement, int index, String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
    } else {
      statement.setString(index, value);
    }
  }

  private static void requirePositiveId(int value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void validateEntityType(String entityType) {
    Objects.requireNonNull(entityType, "entityType cannot be null");
    switch (entityType) {
      case "SERVICE_REQUEST", "RESOURCE", "ROAD" -> {
        return;
      }
      default -> throw new IllegalArgumentException("Unsupported entityType: " + entityType);
    }
  }
}
