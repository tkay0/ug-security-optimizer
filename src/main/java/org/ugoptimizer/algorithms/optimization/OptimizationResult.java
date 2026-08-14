package org.ugoptimizer.algorithms.optimization;

/** Immutable snapshot of an optimization decision and its aggregate values. */
public final class OptimizationResult {

    private final OptimizationItem[] selectedItems;
    private final int[] selectedItemIds;
    private final long totalCost;
    private final long totalBenefit;
    private final int capacity;

    /**
     * Builds a result from the selected immutable item snapshots.
     * Aggregate values are calculated with checked {@code long} arithmetic.
     *
     * @param selectedItems selected candidates in original input order
     * @param capacity non-negative capacity used by the optimization
     * @throws NullPointerException if the array or an element is null
     * @throws IllegalArgumentException if capacity is negative or selected
     *         cost exceeds it
     * @throws ArithmeticException if an aggregate cannot fit in a {@code long}
     */
    public OptimizationResult(OptimizationItem[] selectedItems, int capacity) {
        if (selectedItems == null) {
            throw new NullPointerException("selectedItems cannot be null");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity cannot be negative");
        }

        this.selectedItems = selectedItems.clone();
        this.selectedItemIds = new int[selectedItems.length];
        long costSum = 0L;
        long benefitSum = 0L;
        for (int i = 0; i < selectedItems.length; i++) {
            OptimizationItem item = selectedItems[i];
            if (item == null) {
                throw new NullPointerException("selectedItems[" + i + "] cannot be null");
            }
            selectedItemIds[i] = item.getItemId();
            costSum = checkedAdd(costSum, item.getCost(), "total cost");
            benefitSum = checkedAdd(benefitSum, item.getBenefit(), "total benefit");
        }
        if (costSum > capacity) {
            throw new IllegalArgumentException("selected item cost exceeds capacity");
        }

        this.totalCost = costSum;
        this.totalBenefit = benefitSum;
        this.capacity = capacity;
    }

    public OptimizationItem[] getSelectedItems() {
        return selectedItems.clone();
    }

    public int[] getSelectedItemIds() {
        return selectedItemIds.clone();
    }

    public int getSelectedCount() {
        return selectedItems.length;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public long getTotalBenefit() {
        return totalBenefit;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return selectedItems.length == 0;
    }

    private static long checkedAdd(long current, int value, String label) {
        if (current > Long.MAX_VALUE - value) {
            throw new ArithmeticException(label + " exceeds long range");
        }
        return current + value;
    }
}
