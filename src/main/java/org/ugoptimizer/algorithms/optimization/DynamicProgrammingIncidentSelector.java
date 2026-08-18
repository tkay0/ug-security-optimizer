package org.ugoptimizer.algorithms.optimization;

import org.ugoptimizer.util.ProjectParameters;

/**
 * Exact bottom-up 0/1 knapsack optimizer with full-table reconstruction.
 *
 * <p>Adapted from Team 3's original optimization work on
 * {@code feature/team3-optimization} at commit
 * {@code 63b45a4d9b668cce102eeb2a1b636473a8ef5766} for compatibility with
 * the current project contracts.</p>
 *
 * <p>Ties are resolved by maximizing benefit, then minimizing cost, then
 * preferring the solution that selects the earlier input item at the first
 * position where two selection decisions differ.</p>
 *
 * <p>For {@code n} items and capacity {@code C}, time and space are both
 * {@code O(n * C)}. The full benefit, cost, and reconstruction tables are
 * intentionally retained so the decision process can support a later DP
 * trace-table presentation.</p>
 */
public final class DynamicProgrammingIncidentSelector {

    /** Optimizes using {@link ProjectParameters#OPTIMIZATION_BUDGET}. */
    public OptimizationResult optimize(OptimizationItem[] items) {
        return optimize(items, ProjectParameters.OPTIMIZATION_BUDGET);
    }

    /**
     * Selects an exact optimal subset without mutating the supplied array.
     *
     * @param items candidates considered once each, in preference order
     * @param capacity non-negative capacity limit
     * @return immutable optimal selection
     * @throws NullPointerException if the array or an item is null
     * @throws IllegalArgumentException if capacity is negative or cannot be
     *         represented as a Java array dimension
     */
    public OptimizationResult optimize(OptimizationItem[] items, int capacity) {
        validateInput(items, capacity);
        if (items.length == 0) {
            return new OptimizationResult(new OptimizationItem[0], capacity);
        }
        if (capacity == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("capacity is too large for a DP table");
        }

        int rowCount = items.length + 1;
        int columnCount = capacity + 1;
        long[][] bestBenefits = new long[rowCount][columnCount];
        long[][] bestCosts = new long[rowCount][columnCount];
        boolean[][] selectedAtState = new boolean[items.length][columnCount];

        for (int itemIndex = items.length - 1; itemIndex >= 0; itemIndex--) {
            OptimizationItem item = items[itemIndex];
            for (int available = 0; available <= capacity; available++) {
                long excludedBenefit = bestBenefits[itemIndex + 1][available];
                long excludedCost = bestCosts[itemIndex + 1][available];
                bestBenefits[itemIndex][available] = excludedBenefit;
                bestCosts[itemIndex][available] = excludedCost;

                if (item.getCost() <= available) {
                    int remaining = available - item.getCost();
                    long includedBenefit = checkedAdd(
                            bestBenefits[itemIndex + 1][remaining],
                            item.getBenefit(),
                            "benefit");
                    long includedCost = checkedAdd(
                            bestCosts[itemIndex + 1][remaining],
                            item.getCost(),
                            "cost");

                    if (isBetter(
                            includedBenefit,
                            includedCost,
                            excludedBenefit,
                            excludedCost)) {
                        bestBenefits[itemIndex][available] = includedBenefit;
                        bestCosts[itemIndex][available] = includedCost;
                        selectedAtState[itemIndex][available] = true;
                    }
                }
            }
        }

        return reconstruct(items, capacity, selectedAtState);
    }

    private static OptimizationResult reconstruct(
            OptimizationItem[] items,
            int capacity,
            boolean[][] selectedAtState) {
        OptimizationItem[] selectedBuffer = new OptimizationItem[items.length];
        int selectedCount = 0;
        int remaining = capacity;
        for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            if (selectedAtState[itemIndex][remaining]) {
                OptimizationItem item = items[itemIndex];
                selectedBuffer[selectedCount++] = item;
                remaining -= item.getCost();
            }
        }

        OptimizationItem[] selected = new OptimizationItem[selectedCount];
        System.arraycopy(selectedBuffer, 0, selected, 0, selectedCount);
        return new OptimizationResult(selected, capacity);
    }

    private static boolean isBetter(
            long includedBenefit,
            long includedCost,
            long excludedBenefit,
            long excludedCost) {
        if (includedBenefit != excludedBenefit) {
            return includedBenefit > excludedBenefit;
        }
        if (includedCost != excludedCost) {
            return includedCost < excludedCost;
        }
        return true;
    }

    private static void validateInput(OptimizationItem[] items, int capacity) {
        if (items == null) {
            throw new NullPointerException("items cannot be null");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity cannot be negative");
        }
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                throw new NullPointerException("items[" + i + "] cannot be null");
            }
        }
        if (items.length == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many items for a DP table");
        }
    }

    private static long checkedAdd(long current, int value, String label) {
        if (current > Long.MAX_VALUE - value) {
            throw new ArithmeticException(label + " exceeds long range");
        }
        return current + value;
    }
}
