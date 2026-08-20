package org.ugoptimizer.frontend;

import java.util.List;
import org.ugoptimizer.algorithms.assignment.AssignmentCandidate;
import org.ugoptimizer.model.ServiceRequest;
import org.ugoptimizer.result.OptimizationComparison;
import org.ugoptimizer.result.RequestOptimizationCandidate;
import org.ugoptimizer.result.RequestOptimizationResult;

/** Swing-facing optimization operations delegated to the canonical backend. */
public interface OptimizationService {
  int getBudget();

  /** Returns the bounded, backend-derived candidate set used by the GUI comparison. */
  List<RequestOptimizationCandidate> pendingRequestCandidates();

  RequestOptimizationResult runDynamicProgramming(List<RequestOptimizationCandidate> candidates);

  RequestOptimizationResult runBruteForce(List<RequestOptimizationCandidate> candidates);

  OptimizationComparison compare(List<RequestOptimizationCandidate> candidates);

  AssignmentCandidate recommendResource(int requestId);
}
