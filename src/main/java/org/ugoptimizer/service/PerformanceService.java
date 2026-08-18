package org.ugoptimizer.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.ugoptimizer.database.*;
import org.ugoptimizer.database.dao.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.result.*;

/** Times real operations and persists measurements into pre-planned algorithm runs. */
public final class PerformanceService {
  @FunctionalInterface
  public interface MeasuredOperation<T> {
    T execute() throws Exception;
  }

  private final AlgorithmRunDao dao;
  private final Clock clock;

  public PerformanceService(DatabaseManager m) {
    this(new AlgorithmRunDao(m), Clock.systemUTC());
  }

  public PerformanceService(AlgorithmRunDao d, Clock c) {
    dao = Objects.requireNonNull(d);
    clock = Objects.requireNonNull(c);
  }

  public <T> PerformanceMeasurement<T> measureAndRecord(
      int runId, String algorithmName, int inputSize, MeasuredOperation<T> operation)
      throws Exception {
    Objects.requireNonNull(operation);
    requireText(algorithmName, "algorithmName");
    if (inputSize <= 0) throw new IllegalArgumentException("inputSize must be positive");
    AlgorithmRun planned = requireRun(runId);
    if (!"PLANNED".equals(planned.getStatus()))
      throw new IllegalStateException("Algorithm run is already measured");
    if (!planned.getAlgorithmName().equals(algorithmName) || planned.getInputSize() != inputSize)
      throw new IllegalArgumentException(
          "Measurement metadata does not match planned algorithm run " + runId);
    Runtime runtime = Runtime.getRuntime();
    long before = runtime.totalMemory() - runtime.freeMemory();
    long start = System.nanoTime();
    T result = operation.execute();
    long elapsed = System.nanoTime() - start;
    long after = runtime.totalMemory() - runtime.freeMemory();
    double memory = Math.max(0L, after - before) / 1024.0d;
    if (!dao.markMeasured(runId, elapsed, memory, clock.instant()))
      throw new SQLException("Algorithm run changed concurrently");
    return new PerformanceMeasurement<>(result, requireRun(runId));
  }

  public AlgorithmRun requireRun(int id) throws SQLException {
    return dao.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm run: " + id));
  }

  public AlgorithmRun[] getAllRuns() throws SQLException {
    return dao.findAll();
  }

  public AlgorithmRun[] getRunsByAlgorithm(String n) throws SQLException {
    return dao.findByAlgorithmName(n);
  }

  public AlgorithmRun[] getRunsByStatus(String s) throws SQLException {
    return dao.findByStatus(s);
  }

  public AlgorithmRun[] getRunsByExperimentGroup(String g) throws SQLException {
    return dao.findByExperimentGroup(g);
  }

  private static void requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " cannot be null");
    if (value.isBlank()) throw new IllegalArgumentException(fieldName + " cannot be blank");
  }
}
