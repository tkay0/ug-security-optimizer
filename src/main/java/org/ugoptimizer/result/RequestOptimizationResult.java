package org.ugoptimizer.result;

import java.util.*;
import org.ugoptimizer.algorithms.optimization.*;
import org.ugoptimizer.model.*;

/** GUI-ready optimization outcome containing canonical selected requests and objective totals. */
public final class RequestOptimizationResult {
  private final String algorithm;
  private final ServiceRequest[] selected;
  private final OptimizationResult objective;

  public RequestOptimizationResult(String a, ServiceRequest[] s, OptimizationResult o) {
    algorithm = Objects.requireNonNull(a);
    selected = s.clone();
    objective = Objects.requireNonNull(o);
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public ServiceRequest[] getSelectedRequests() {
    return selected.clone();
  }

  public OptimizationResult getObjective() {
    return objective;
  }
}
