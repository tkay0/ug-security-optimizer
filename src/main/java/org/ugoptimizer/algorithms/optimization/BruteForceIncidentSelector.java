package org.ugoptimizer.algorithms.optimization;

import org.ugoptimizer.util.ProjectParameters;

/**
 * Exact exhaustive reference optimizer for small 0/1 knapsack inputs.
 *
 * <p>Adapted from Team 3's original optimization work on
 * {@code feature/team3-optimization} at commit
 * {@code 63b45a4d9b668cce102eeb2a1b636473a8ef5766} for compatibility with
 * the current project contracts.</p>
 *
 * <p>The implementation uses bounded 64-bit subset masks. At most 24 items
 * are accepted, limiting enumeration to 2^24 subsets while avoiding unsafe
 * 32-bit shifts. For {@code n} items, time is {@code O(n * 2^n)} and result
 * reconstruction uses {@code O(n)} auxiliary space.</p>
 *
 * <p>Ties are resolved by maximizing benefit, then minimizing cost, then
 * preferring the solution that selects the earlier input item at the first
 * position where two selection decisions differ.</p>
 */
public final class BruteForceIncidentSelector {

    /** Practical bound for exhaustive enumeration. */
    public static final int MAX_SUPPORTED_ITEMS = 24;

    /** Optimizes using {@link ProjectParameters#OPTIMIZATION_BUDGET}. */
    public OptimizationResult optimize(OptimizationItem[] items) {
        return optimize(items, ProjectParameters.OPTIMIZATION_BUDGET);
    }

    /**
     * Enumerates every subset and returns the exact deterministic optimum.
     *
     * @param items candidates considered once each, in preference order
     * @param capacity non-negative capacity limit
     * @return immutable optimal selection
     * @throws NullPointerException if the array or an item is null
     * @throws IllegalArgumentException if capacity is negative or more than
     *         {@link #MAX_SUPPORTED_ITEMS} candidates are supplied
     */
    public OptimizationResult optimize(OptimizationItem[] items, int capacity) {
        validateInput(items, capacity);

        long subsetCount = 1L << items.length;
        long bestMask = 0L;
        long bestCost = 0L;
        long bestBenefit = 0L;

        for (long mask = 0L; mask < subsetCount; mask++) {
            long currentCost = 0L;
            long currentBenefit = 0L;
            boolean feasible = true;

            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if ((mask & (1L << itemIndex)) != 0L) {
                    OptimizationItem item = items[itemIndex];
                    currentCost = checkedAdd(currentCost, item.getCost(), "cost");
                    if (currentCost > capacity) {
                        feasible = false;
                        break;
                    }
                    currentBenefit = checkedAdd(
                            currentBenefit, item.getBenefit(), "benefit");
                }
            }

            if (feasible && isBetter(
                    currentBenefit,
                    currentCost,
                    mask,
                    bestBenefit,
                    bestCost,
                    bestMask,
                    items.length)) {
                bestMask = mask;
                bestCost = currentCost;
                bestBenefit = currentBenefit;
            }
        }

        return buildResult(items, capacity, bestMask);
    }

    private static OptimizationResult buildResult(
            OptimizationItem[] items,
            int capacity,
            long bestMask) {
        int selectedCount = Long.bitCount(bestMask);
        OptimizationItem[] selected = new OptimizationItem[selectedCount];
        int outputIndex = 0;
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if ((bestMask & (1L << itemIndex)) != 0L) {
                selected[outputIndex++] = items[itemIndex];
            }
        }
        return new OptimizationResult(selected, capacity);
    }

    private static boolean isBetter(
            long candidateBenefit,
            long candidateCost,
            long candidateMask,
            long bestBenefit,
            long bestCost,
            long bestMask,
            int itemCount) {
        if (candidateBenefit != bestBenefit) {
            return candidateBenefit > bestBenefit;
        }
        if (candidateCost != bestCost) {
            return candidateCost < bestCost;
        }
        return prefersEarlierInput(candidateMask, bestMask, itemCount);
    }

    private static boolean prefersEarlierInput(
            long candidateMask,
            long bestMask,
            int itemCount) {
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            boolean candidateSelects = (candidateMask & (1L << itemIndex)) != 0L;
            boolean bestSelects = (bestMask & (1L << itemIndex)) != 0L;
            if (candidateSelects != bestSelects) {
                return candidateSelects;
            }
        }
        return false;
    }

    private static void validateInput(OptimizationItem[] items, int capacity) {
        if (items == null) {
            throw new NullPointerException("items cannot be null");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity cannot be negative");
        }
        if (items.length > MAX_SUPPORTED_ITEMS) {
            throw new IllegalArgumentException(
                    "brute force supports at most " + MAX_SUPPORTED_ITEMS + " items");
        }
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                throw new NullPointerException("items[" + i + "] cannot be null");
            }
        }
    }

    private static long checkedAdd(long current, int value, String label) {
        if (current > Long.MAX_VALUE - value) {
            throw new ArithmeticException(label + " exceeds long range");
        }
        return current + value;
    }
}
