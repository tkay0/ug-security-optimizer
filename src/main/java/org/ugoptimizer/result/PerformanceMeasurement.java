package org.ugoptimizer.result;

import java.util.*;
import org.ugoptimizer.model.AlgorithmRun;

/** Result of a real timed operation and its persisted measurement row. */
public final class PerformanceMeasurement<T> {
  private final T result;
  private final AlgorithmRun run;

  public PerformanceMeasurement(T r, AlgorithmRun a) {
    result = r;
    run = Objects.requireNonNull(a);
  }

  public T getResult() {
    return result;
  }

  public AlgorithmRun getAlgorithmRun() {
    return run;
  }
}
