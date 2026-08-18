package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.ugoptimizer.database.DatabaseManager;

/** Atomically reserves positive IDs for models that require an ID before construction. */
public final class IdSequenceDao {

  /** Whitelisted tables and ID columns supported by the reservation mechanism. */
  public enum Entity {
    LOCATION("LOCATION", "locations", "location_id"),
    ROAD("ROAD", "roads", "road_id"),
    SERVICE_REQUEST("SERVICE_REQUEST", "service_requests", "request_id"),
    RESOURCE("RESOURCE", "resources", "resource_id"),
    ALGORITHM_RUN("ALGORITHM_RUN", "algorithm_runs", "run_id");

    private final String key;
    private final String table;
    private final String idColumn;

    Entity(String key, String table, String idColumn) {
      this.key = key;
      this.table = table;
      this.idColumn = idColumn;
    }
  }

  private final DatabaseManager databaseManager;

  public IdSequenceDao(DatabaseManager databaseManager) {
    this.databaseManager =
        Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
  }

  /**
   * Reserves and returns one ID in a single SQLite write statement.
   *
   * <p>The stored sequence is also compared with the current table maximum so explicit inserts
   * made outside this allocator cannot make it return an already-used ID.</p>
   */
  public int reserveNext(Entity entity) throws SQLException {
    Objects.requireNonNull(entity, "entity cannot be null");
    try (Connection connection = databaseManager.openConnection()) {
      return reserveNext(connection, entity);
    } catch (SQLException exception) {
      throw new SQLException("Failed to reserve the next " + entity.key + " ID", exception);
    }
  }

  static int reserveNext(Connection connection, Entity entity) throws SQLException {
    Objects.requireNonNull(connection, "connection cannot be null");
    Objects.requireNonNull(entity, "entity cannot be null");
    String currentMaximum =
        "(SELECT COALESCE(MAX(" + entity.idColumn + "), 0) + 2 FROM " + entity.table + ")";
    String sql =
        "INSERT INTO id_sequences (entity_name, next_id) VALUES (?, "
            + currentMaximum
            + ") ON CONFLICT(entity_name) DO UPDATE SET next_id = MAX("
            + "id_sequences.next_id + 1, "
            + currentMaximum
            + ") RETURNING next_id - 1";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, entity.key);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new SQLException("ID reservation returned no value for " + entity.key);
        }
        int reserved = resultSet.getInt(1);
        if (reserved <= 0) {
          throw new SQLException("ID reservation returned a non-positive value for " + entity.key);
        }
        return reserved;
      }
    }
  }
}
