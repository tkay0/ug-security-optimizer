package org.ugoptimizer.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseManagerTest {

  private static final String EXPECTED_TABLES =
      String.join(
          ",",
          "algorithm_runs",
          "assignments",
          "audit_events",
          "id_sequences",
          "locations",
          "request_status_history",
          "resources",
          "road_scenarios",
          "roads",
          "service_requests");

  @TempDir Path temporaryDirectory;

  @Test
  void initializeSchemaCreatesConfiguredTemporaryDatabaseAndAllTables() throws Exception {
    Path databasePath = temporaryDirectory.resolve("fresh.db");
    DatabaseManager manager = new DatabaseManager(databasePath);

    assertFalse(Files.exists(databasePath));
    manager.initializeSchema();

    assertTrue(Files.isRegularFile(databasePath));
    assertEquals(databasePath.toAbsolutePath().normalize(), manager.getDatabasePath());
    assertEquals("jdbc:sqlite:" + databasePath.toAbsolutePath().normalize(), manager.getJdbcUrl());
    try (Connection connection = manager.openConnection()) {
      assertEquals(EXPECTED_TABLES, applicationTableNames(connection));
    }
  }

  @Test
  void initializeSchemaCanBeCalledTwiceSafely() throws Exception {
    DatabaseManager manager = managerFor("repeatable.db");

    manager.initializeSchema();
    manager.initializeSchema();

    try (Connection connection = manager.openConnection()) {
      assertEquals(EXPECTED_TABLES, applicationTableNames(connection));
    }
  }

  @Test
  void everyOpenedConnectionHasForeignKeysEnabled() throws Exception {
    DatabaseManager manager = managerFor("foreign-keys.db");

    try (Connection first = manager.openConnection();
        Connection second = manager.openConnection()) {
      assertTrue(manager.isForeignKeyEnforcementEnabled(first));
      assertTrue(manager.isForeignKeyEnforcementEnabled(second));
      assertEquals(1, foreignKeyPragma(first));
      assertEquals(1, foreignKeyPragma(second));
    }
  }

  @Test
  void foreignKeyViolationIsRejected() throws Exception {
    DatabaseManager manager = managerFor("constraint.db");
    manager.initializeSchema();

    try (Connection connection = manager.openConnection();
        Statement statement = connection.createStatement()) {
      assertThrows(
          SQLException.class,
          () ->
              statement.executeUpdate(
                  "INSERT INTO roads ("
                      + "road_id, from_location_id, to_location_id, distance_km, "
                      + "travel_time_min, condition_weight, route_label, is_blocked"
                      + ") VALUES (1, 999, 1000, 1.0, 1.0, 1.0, 'Missing endpoints', 0)"));
    }
  }

  @Test
  void openConnectionReturnsIndependentCallerOwnedConnections() throws Exception {
    DatabaseManager manager = managerFor("independent.db");

    try (Connection first = manager.openConnection();
        Connection second = manager.openConnection()) {
      assertNotSame(first, second);
      first.close();
      assertFalse(second.isClosed());
    }
  }

  @Test
  void managerDoesNotCreateAnUnconfiguredDatabase() throws Exception {
    Path configuredPath = temporaryDirectory.resolve("selected.db");
    Path unrelatedPath = temporaryDirectory.resolve("project.db");
    DatabaseManager manager = new DatabaseManager(configuredPath);

    manager.initializeSchema();

    assertTrue(Files.exists(configuredPath));
    assertFalse(Files.exists(unrelatedPath));
    assertTrue(manager.getDatabasePath().startsWith(temporaryDirectory));
  }

  private DatabaseManager managerFor(String fileName) {
    return new DatabaseManager(temporaryDirectory.resolve(fileName));
  }

  private static String applicationTableNames(Connection connection) throws SQLException {
    StringBuilder names = new StringBuilder();
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT name FROM sqlite_master "
                    + "WHERE type = 'table' AND name NOT LIKE 'sqlite_%' "
                    + "ORDER BY name")) {
      while (resultSet.next()) {
        if (names.length() > 0) {
          names.append(',');
        }
        names.append(resultSet.getString(1));
      }
    }
    return names.toString();
  }

  private static int foreignKeyPragma(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
      assertTrue(resultSet.next());
      return resultSet.getInt(1);
    }
  }
}
