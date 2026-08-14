package org.ugoptimizer.result;

import java.util.*;
import org.ugoptimizer.model.ServiceRequest;

/** Explicit cost/benefit input because ServiceRequest has no canonical optimization cost field. */
public final class RequestOptimizationCandidate {
  private final ServiceRequest request;
  private final int cost;
  private final int benefit;

  public RequestOptimizationCandidate(ServiceRequest r, int c, int b) {
    request = Objects.requireNonNull(r);
    if (c < 0 || b < 0) throw new IllegalArgumentException("cost and benefit must be non-negative");
    cost = c;
    benefit = b;
  }

  public ServiceRequest getRequest() {
    return request;
  }

  public int getCost() {
    return cost;
  }

  public int getBenefit() {
    return benefit;
  }
}
