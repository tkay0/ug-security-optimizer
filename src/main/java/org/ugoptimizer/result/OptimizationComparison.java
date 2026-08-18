package org.ugoptimizer.result;

import java.util.*;

/** Exact small-input comparison of DP and exhaustive selection. */
public final class OptimizationComparison {
  private final RequestOptimizationResult dp, brute;
  private final boolean same;

  public OptimizationComparison(RequestOptimizationResult d, RequestOptimizationResult b) {
    dp = Objects.requireNonNull(d);
    brute = Objects.requireNonNull(b);
    same =
        Arrays.equals(d.getObjective().getSelectedItemIds(), b.getObjective().getSelectedItemIds())
            && d.getObjective().getTotalBenefit() == b.getObjective().getTotalBenefit()
            && d.getObjective().getTotalCost() == b.getObjective().getTotalCost();
  }

  public RequestOptimizationResult getDynamicProgramming() {
    return dp;
  }

  public RequestOptimizationResult getBruteForce() {
    return brute;
  }

  public boolean hasSameOptimum() {
    return same;
  }
}
