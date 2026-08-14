package org.ugoptimizer.service;

import java.util.*;
import org.ugoptimizer.algorithms.assignment.*;
import org.ugoptimizer.algorithms.optimization.*;
import org.ugoptimizer.model.*;
import org.ugoptimizer.result.*;
import org.ugoptimizer.util.ProjectParameters;

/** Reusable service adapter over the existing greedy, DP, and brute-force algorithms. */
public final class OptimizationService {
  private final DynamicProgrammingIncidentSelector dp = new DynamicProgrammingIncidentSelector();
  private final BruteForceIncidentSelector brute = new BruteForceIncidentSelector();
  private final GreedyAssignmentService greedy = new GreedyAssignmentService();

  public int getApprovedBudget() {
    return ProjectParameters.OPTIMIZATION_BUDGET;
  }

  public AssignmentCandidate selectGreedyResource(ServiceRequest r, AssignmentCandidate[] c) {
    return greedy.assign(r, c);
  }

  public OptimizationResult runDynamicProgramming(OptimizationItem[] i) {
    return dp.optimize(i, ProjectParameters.OPTIMIZATION_BUDGET);
  }

  public OptimizationResult runDynamicProgramming(OptimizationItem[] i, int capacity) {
    return dp.optimize(i, capacity);
  }

  public OptimizationResult runBruteForce(OptimizationItem[] i) {
    return brute.optimize(i, ProjectParameters.OPTIMIZATION_BUDGET);
  }

  public OptimizationResult runBruteForce(OptimizationItem[] i, int capacity) {
    return brute.optimize(i, capacity);
  }

  public RequestOptimizationResult optimizeRequestsWithDynamicProgramming(
      RequestOptimizationCandidate[] c) {
    return run(c, true);
  }

  public RequestOptimizationResult optimizeRequestsWithBruteForce(
      RequestOptimizationCandidate[] c) {
    return run(c, false);
  }

  public OptimizationComparison compareExact(RequestOptimizationCandidate[] c) {
    return new OptimizationComparison(run(c, true), run(c, false));
  }

  private RequestOptimizationResult run(RequestOptimizationCandidate[] c, boolean dynamic) {
    Objects.requireNonNull(c, "candidates cannot be null");
    OptimizationItem[] items = new OptimizationItem[c.length];
    for (int i = 0; i < c.length; i++) {
      RequestOptimizationCandidate x = Objects.requireNonNull(c[i]);
      items[i] = new OptimizationItem(x.getRequest().getRequestId(), x.getCost(), x.getBenefit());
    }
    OptimizationResult o = dynamic ? runDynamicProgramming(items) : runBruteForce(items);
    ServiceRequest[] selected = new ServiceRequest[o.getSelectedCount()];
    int n = 0;
    for (int id : o.getSelectedItemIds())
      for (RequestOptimizationCandidate x : c)
        if (x.getRequest().getRequestId() == id) {
          selected[n++] = x.getRequest();
          break;
        }
    return new RequestOptimizationResult(
        dynamic ? "DYNAMIC_PROGRAMMING" : "BRUTE_FORCE", selected, o);
  }
}
