package org.ugoptimizer.algorithms.optimization;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.util.ProjectParameters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicProgrammingIncidentSelectorTest {

    private final DynamicProgrammingIncidentSelector selector =
            new DynamicProgrammingIncidentSelector();

    @Test
    void rejectsNullInputAndNullItem() {
        assertThrows(NullPointerException.class, () -> selector.optimize(null, 5));
        assertThrows(NullPointerException.class,
                () -> selector.optimize(new OptimizationItem[] {item(1, 1, 1), null}, 5));
    }

    @Test
    void rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> selector.optimize(new OptimizationItem[0], -1));
    }

    @Test
    void emptyInputReturnsEmptyResult() {
        OptimizationResult result = selector.optimize(new OptimizationItem[0], 7);

        assertResult(result, new int[0], 0L, 0L, 7);
    }

    @Test
    void zeroCapacityStillSelectsBeneficialZeroCostItems() {
        OptimizationItem[] items = {
            item(1, 0, 4),
            item(2, 1, 100),
            item(3, 0, 2)
        };

        assertResult(selector.optimize(items, 0), new int[] {1, 3}, 0L, 6L, 0);
    }

    @Test
    void oneItemFits() {
        assertResult(selector.optimize(new OptimizationItem[] {item(7, 5, 9)}, 5),
                new int[] {7}, 5L, 9L, 5);
    }

    @Test
    void oneItemDoesNotFit() {
        assertResult(selector.optimize(new OptimizationItem[] {item(7, 6, 9)}, 5),
                new int[0], 0L, 0L, 5);
    }

    @Test
    void exactCapacityCombinationIsSelected() {
        OptimizationItem[] items = {item(1, 2, 3), item(2, 3, 5), item(3, 6, 20)};

        assertResult(selector.optimize(items, 5), new int[] {1, 2}, 5L, 8L, 5);
    }

    @Test
    void selectsOptimalCombinationRatherThanHighestSingleBenefit() {
        OptimizationItem[] items = {
            item(1, 4, 7), item(2, 5, 8), item(3, 9, 14)
        };

        assertResult(selector.optimize(items, 9), new int[] {1, 2}, 9L, 15L, 9);
    }

    @Test
    void duplicateCostsAreHandled() {
        OptimizationItem[] items = {
            item(1, 3, 4), item(2, 3, 8), item(3, 3, 6)
        };

        assertResult(selector.optimize(items, 6), new int[] {2, 3}, 6L, 14L, 6);
    }

    @Test
    void duplicateBenefitsPreferLowerTotalCost() {
        OptimizationItem[] items = {item(1, 4, 10), item(2, 2, 10)};

        assertResult(selector.optimize(items, 4), new int[] {2}, 2L, 10L, 4);
    }

    @Test
    void completeTiePrefersEarlierInputSelection() {
        OptimizationItem[] items = {
            item(90, 2, 5), item(10, 2, 5), item(20, 2, 5)
        };

        assertResult(selector.optimize(items, 4), new int[] {90, 10}, 4L, 10L, 4);
    }

    @Test
    void allPositiveBenefitItemsAreSelectedWhenAllFit() {
        OptimizationItem[] items = {item(1, 1, 2), item(2, 2, 3), item(3, 0, 1)};

        assertResult(selector.optimize(items, 10), new int[] {1, 2, 3}, 3L, 6L, 10);
    }

    @Test
    void noneFit() {
        OptimizationItem[] items = {item(1, 8, 20), item(2, 9, 30)};

        assertResult(selector.optimize(items, 7), new int[0], 0L, 0L, 7);
    }

    @Test
    void arbitraryAndExtremeIdsRemainInInputOrder() {
        OptimizationItem[] items = {
            item(Integer.MAX_VALUE, 1, 2),
            item(-71, 1, 3),
            item(Integer.MIN_VALUE, 1, 4)
        };

        assertResult(selector.optimize(items, 3),
                new int[] {Integer.MAX_VALUE, -71, Integer.MIN_VALUE}, 3L, 9L, 3);
    }

    @Test
    void repeatedCallsAreDeterministicAndDoNotMutateInput() {
        OptimizationItem first = item(1, 2, 5);
        OptimizationItem second = item(2, 2, 5);
        OptimizationItem[] items = {first, second};

        OptimizationResult firstRun = selector.optimize(items, 2);
        OptimizationResult secondRun = selector.optimize(items, 2);

        assertArrayEquals(new int[] {1}, firstRun.getSelectedItemIds());
        assertArrayEquals(firstRun.getSelectedItemIds(), secondRun.getSelectedItemIds());
        assertEquals(first, items[0]);
        assertEquals(second, items[1]);
    }

    @Test
    void resultRemainsIndependentWhenInputAndReturnedArraysAreChanged() {
        OptimizationItem chosen = item(1, 1, 5);
        OptimizationItem[] items = {chosen, item(2, 2, 4)};
        OptimizationResult result = selector.optimize(items, 1);

        items[0] = item(99, 0, 0);
        int[] ids = result.getSelectedItemIds();
        OptimizationItem[] snapshots = result.getSelectedItems();
        ids[0] = 99;
        snapshots[0] = items[0];

        assertArrayEquals(new int[] {1}, result.getSelectedItemIds());
        assertArrayEquals(new OptimizationItem[] {chosen}, result.getSelectedItems());
    }

    @Test
    void benefitTotalsUseLongWithoutOverflow() {
        OptimizationItem[] items = {
            item(1, 0, Integer.MAX_VALUE), item(2, 0, Integer.MAX_VALUE)
        };

        assertResult(selector.optimize(items, 0), new int[] {1, 2},
                0L, 4_294_967_294L, 0);
    }

    @Test
    void capacityThatWouldOverflowTableDimensionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> selector.optimize(new OptimizationItem[] {item(1, 1, 1)},
                        Integer.MAX_VALUE));
    }

    @Test
    void greedyRatioCounterexampleStillReturnsExactOptimum() {
        // Ratio-greedy chooses items 1 and 2 (benefit 160). The exact optimum
        // is items 2 and 3 with benefit 220 at capacity 50.
        OptimizationItem[] items = {
            item(1, 10, 60), item(2, 20, 100), item(3, 30, 120)
        };

        assertResult(selector.optimize(items, 50), new int[] {2, 3}, 50L, 220L, 50);
    }

    @Test
    void defaultOverloadUsesProjectOptimizationBudget() {
        OptimizationItem[] items = {item(1, 80, 100), item(2, 81, 1_000)};

        assertEquals(80, ProjectParameters.OPTIMIZATION_BUDGET);
        assertResult(selector.optimize(items), new int[] {1}, 80L, 100L, 80);
    }

    private static OptimizationItem item(int id, int cost, int benefit) {
        return new OptimizationItem(id, cost, benefit);
    }

    private static void assertResult(
            OptimizationResult result,
            int[] expectedIds,
            long expectedCost,
            long expectedBenefit,
            int expectedCapacity) {
        assertArrayEquals(expectedIds, result.getSelectedItemIds());
        assertEquals(expectedIds.length, result.getSelectedCount());
        assertEquals(expectedCost, result.getTotalCost());
        assertEquals(expectedBenefit, result.getTotalBenefit());
        assertEquals(expectedCapacity, result.getCapacity());
    }
}
