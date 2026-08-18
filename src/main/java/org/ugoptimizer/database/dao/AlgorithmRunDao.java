package org.ugoptimizer.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.ugoptimizer.database.DatabaseManager;
import org.ugoptimizer.database.mapper.AlgorithmRunMapper;
import org.ugoptimizer.model.AlgorithmRun;

/** Provides persistent access to planned and measured algorithm runs. */
public final class AlgorithmRunDao {

  private static final String COLUMNS =
      "run_id, algorithm_name, input_size, time_ns, memory_kb, date_run, status, "
          + "experiment_group, run_number";
  private static final int INITIAL_RESULT_CAPACITY = 16;

  private final DatabaseManager databaseManager;

  public AlgorithmRunDao(DatabaseManager databaseManager) {
    this.databaseManager =
        Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
  }

  public Optional<AlgorithmRun> findById(int runId) throws SQLException {
    requirePositiveId(runId);
    String sql = "SELECT " + COLUMNS + " FROM algorithm_runs WHERE run_id = ?";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, runId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(AlgorithmRunMapper.map(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new SQLException("Failed to find algorithm run " + runId, exception);
    }
  }

  public AlgorithmRun[] findByAlgorithmName(String algorithmName) throws SQLException {
    requireAlgorithmName(algorithmName);
    String sql =
        "SELECT "
            + COLUMNS
            + " FROM algorithm_runs WHERE algorithm_name = ?"
            + " ORDER BY input_size, experiment_group, run_number, run_id";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, algorithmName);
      try (ResultSet resultSet = statement.executeQuery()) {
        AlgorithmRun[] buffer = new AlgorithmRun[INITIAL_RESULT_CAPACITY];
        int count = 0;
        while (resultSet.next()) {
          if (count == buffer.length) {
            buffer = grow(buffer);
          }
          buffer[count++] = AlgorithmRunMapper.map(resultSet);
        }
        AlgorithmRun[] result = new AlgorithmRun[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
      }
    } catch (SQLException exception) {
      throw new SQLException("Failed to find algorithm runs named " + algorithmName, exception);
    }
  }

  public boolean markMeasured(int runId, long timeNs, double memoryKb, Instant dateRun)
      throws SQLException {
    requirePositiveId(runId);
    if (timeNs < 0L) {
      throw new IllegalArgumentException("timeNs cannot be negative");
    }
    if (!Double.isFinite(memoryKb) || memoryKb < 0.0d) {
      throw new IllegalArgumentException("memoryKb must be finite and non-negative");
    }
    Objects.requireNonNull(dateRun, "dateRun cannot be null");

    String sql =
        "UPDATE algorithm_runs SET time_ns = ?, memory_kb = ?, date_run = ?, "
            + "status = 'MEASURED' WHERE run_id = ? AND status = 'PLANNED'";
    try (Connection connection = databaseManager.openConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, timeNs);
      statement.setDouble(2, memoryKb);
      statement.setString(3, dateRun.toString());
      statement.setInt(4, runId);
      int affectedRows = statement.executeUpdate();
      if (affectedRows > 1) {
        throw new SQLException("Algorithm-run update affected more than one row");
      }
      return affectedRows == 1;
    } catch (SQLException exception) {
      throw new SQLException("Failed to mark algorithm run " + runId + " measured", exception);
    }
  }

  /**
   * Persists a completed measurement with a database-reserved ID and the next run number in its
   * experiment group.
   */
  public AlgorithmRun insertGeneratedMeasurement(AlgorithmRun measurement) throws SQLException {
    Objects.requireNonNull(measurement, "measurement cannot be null");
    if (!"MEASURED".equals(measurement.getStatus())) {
      throw new IllegalArgumentException("Only MEASURED algorithm runs can be recorded");
    }
    try (Connection connection = databaseManager.openConnection()) {
      connection.setAutoCommit(false);
      try {
        int runId = IdSequenceDao.reserveNext(connection, IdSequenceDao.Entity.ALGORITHM_RUN);
        int runNumber = insertGeneratedMeasurement(connection, runId, measurement);
        connection.commit();
        return new AlgorithmRun(
            runId,
            measurement.getAlgorithmName(),
            measurement.getInputSize(),
            measurement.getTimeNs(),
            measurement.getMemoryKb(),
            measurement.getDateRun(),
            measurement.getStatus(),
            measurement.getExperimentGroup(),
            runNumber);
      } catch (SQLException | RuntimeException exception) {
        rollback(connection, exception);
        throw exception;
      }
    } catch (SQLException exception) {
      throw new SQLException("Failed to insert measured algorithm run", exception);
    }
  }

  public AlgorithmRun[] findAll() throws SQLException {
    return query("SELECT " + COLUMNS + " FROM algorithm_runs ORDER BY run_id", null);
  }

  public AlgorithmRun[] findByStatus(String value) throws SQLException {
    return query(
        "SELECT " + COLUMNS + " FROM algorithm_runs WHERE status=? ORDER BY run_id", value);
  }

  public AlgorithmRun[] findByExperimentGroup(String value) throws SQLException {
    return query(
        "SELECT " + COLUMNS + " FROM algorithm_runs WHERE experiment_group=? ORDER BY run_id",
        value);
  }

  private AlgorithmRun[] query(String sql, String value) throws SQLException {
    try (Connection c = databaseManager.openConnection();
        PreparedStatement s = c.prepareStatement(sql)) {
      if (value != null) s.setString(1, value);
      try (ResultSet r = s.executeQuery()) {
        AlgorithmRun[] a = new AlgorithmRun[INITIAL_RESULT_CAPACITY];
        int n = 0;
        while (r.next()) {
          if (n == a.length) a = grow(a);
          a[n++] = AlgorithmRunMapper.map(r);
        }
        AlgorithmRun[] out = new AlgorithmRun[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
      }
    }
  }

  private static AlgorithmRun[] grow(AlgorithmRun[] current) throws SQLException {
    if (current.length > Integer.MAX_VALUE / 2) {
      throw new SQLException("Algorithm-run result exceeds supported array capacity");
    }
    AlgorithmRun[] expanded = new AlgorithmRun[current.length * 2];
    System.arraycopy(current, 0, expanded, 0, current.length);
    return expanded;
  }

  private static int insertGeneratedMeasurement(
      Connection connection, int runId, AlgorithmRun measurement) throws SQLException {
    String sql =
        "INSERT INTO algorithm_runs ("
            + COLUMNS
            + ") SELECT ?, ?, ?, ?, ?, ?, 'MEASURED', ?, COALESCE(MAX(run_number), 0) + 1"
            + " FROM algorithm_runs WHERE experiment_group = ? RETURNING run_number";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, runId);
      statement.setString(2, measurement.getAlgorithmName());
      statement.setInt(3, measurement.getInputSize());
      statement.setLong(4, measurement.getTimeNs());
      statement.setDouble(5, measurement.getMemoryKb());
      statement.setString(6, measurement.getDateRun().toString());
      statement.setString(7, measurement.getExperimentGroup());
      statement.setString(8, measurement.getExperimentGroup());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new SQLException("Measured algorithm-run insert returned no run number");
        }
        return resultSet.getInt(1);
      }
    }
  }

  private static void rollback(Connection connection, Throwable failure) {
    try {
      connection.rollback();
    } catch (SQLException rollbackException) {
      failure.addSuppressed(rollbackException);
    }
  }

  private static void requirePositiveId(int runId) {
    if (runId <= 0) {
      throw new IllegalArgumentException("runId must be positive");
    }
  }

  private static void requireAlgorithmName(String algorithmName) {
    Objects.requireNonNull(algorithmName, "algorithmName cannot be null");
    if (algorithmName.isBlank()) {
      throw new IllegalArgumentException("algorithmName cannot be blank");
    }
  }
}
