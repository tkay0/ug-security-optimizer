package org.ugoptimizer.algorithms.optimization;

/**
 * Immutable candidate for a capacity-constrained 0/1 optimization problem.
 * Item identifiers may be any {@code int}; costs and benefits are
 * non-negative integers.
 */
public final class OptimizationItem {

    private final int itemId;
    private final int cost;
    private final int benefit;

    /**
     * Creates an optimization candidate.
     *
     * @param itemId arbitrary identifier used to report the selected item
     * @param cost non-negative capacity consumed when selected
     * @param benefit non-negative value gained when selected
     * @throws IllegalArgumentException if cost or benefit is negative
     */
    public OptimizationItem(int itemId, int cost, int benefit) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost cannot be negative");
        }
        if (benefit < 0) {
            throw new IllegalArgumentException("benefit cannot be negative");
        }
        this.itemId = itemId;
        this.cost = cost;
        this.benefit = benefit;
    }

    public int getItemId() {
        return itemId;
    }

    public int getCost() {
        return cost;
    }

    public int getBenefit() {
        return benefit;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OptimizationItem item)) {
            return false;
        }
        return itemId == item.itemId
                && cost == item.cost
                && benefit == item.benefit;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(itemId);
        result = 31 * result + Integer.hashCode(cost);
        result = 31 * result + Integer.hashCode(benefit);
        return result;
    }

    @Override
    public String toString() {
        return "OptimizationItem{id=" + itemId
                + ", cost=" + cost
                + ", benefit=" + benefit + '}';
    }
}
