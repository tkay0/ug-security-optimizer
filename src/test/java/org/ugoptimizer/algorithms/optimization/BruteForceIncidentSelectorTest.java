package org.ugoptimizer.algorithms.optimization;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.util.ProjectParameters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BruteForceIncidentSelectorTest {

    private final BruteForceIncidentSelector selector = new BruteForceIncidentSelector();

    @Test
    void rejectsNullInputNullItemAndNegativeCapacity() {
        assertThrows(NullPointerException.class, () -> selector.optimize(null, 5));
        assertThrows(NullPointerException.class,
                () -> selector.optimize(new OptimizationItem[] {item(1, 1, 1), null}, 5));
        assertThrows(IllegalArgumentException.class,
                () -> selector.optimize(new OptimizationItem[0], -1));
    }

    @Test
    void emptyInputAndZeroCapacityAreSupported() {
        assertResult(selector.optimize(new OptimizationItem[0], 4),
                new int[0], 0L, 0L, 4);
        OptimizationItem[] zeroCapacity = {item(1, 0, 5), item(2, 1, 100)};
        assertResult(selector.optimize(zeroCapacity, 0),
                new int[] {1}, 0L, 5L, 0);
    }

    @Test
    void oneItemFitAndNoFitCasesAreExact() {
        assertResult(selector.optimize(new OptimizationItem[] {item(1, 3, 7)}, 3),
                new int[] {1}, 3L, 7L, 3);
        assertResult(selector.optimize(new OptimizationItem[] {item(1, 4, 7)}, 3),
                new int[0], 0L, 0L, 3);
    }

    @Test
    void duplicateValuesAndOptimalCombinationAreHandled() {
        OptimizationItem[] items = {
            item(1, 3, 5), item(2, 3, 5), item(3, 2, 5), item(4, 4, 9)
        };

        assertResult(selector.optimize(items, 6), new int[] {3, 4}, 6L, 14L, 6);
    }

    @Test
    void lowerCostBreaksBenefitTie() {
        OptimizationItem[] items = {item(1, 4, 10), item(2, 2, 10)};

        assertResult(selector.optimize(items, 4), new int[] {2}, 2L, 10L, 4);
    }

    @Test
    void completeTiePrefersEarlierInputSelection() {
        OptimizationItem[] items = {
            item(30, 2, 5), item(20, 2, 5), item(10, 2, 5)
        };

        assertResult(selector.optimize(items, 4), new int[] {30, 20}, 4L, 10L, 4);
    }

    @Test
    void allFitAndNoneFitCasesAreHandled() {
        OptimizationItem[] allFit = {item(1, 1, 2), item(2, 2, 3)};
        assertResult(selector.optimize(allFit, 3), new int[] {1, 2}, 3L, 5L, 3);

        OptimizationItem[] noneFit = {item(3, 8, 20), item(4, 9, 30)};
        assertResult(selector.optimize(noneFit, 7), new int[0], 0L, 0L, 7);
    }

    @Test
    void supportsExtremeIdsAndLongBenefitTotals() {
        OptimizationItem[] items = {
            item(Integer.MIN_VALUE, 0, Integer.MAX_VALUE),
            item(Integer.MAX_VALUE, 0, Integer.MAX_VALUE)
        };

        assertResult(selector.optimize(items, 0),
                new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE},
                0L, 4_294_967_294L, 0);
    }

    @Test
    void repeatedCallsDoNotMutateInputAndReturnedIdsAreDefensive() {
        OptimizationItem first = item(-1, 1, 3);
        OptimizationItem second = item(8, 2, 4);
        OptimizationItem[] items = {first, second};

        OptimizationResult firstRun = selector.optimize(items, 2);
        OptimizationResult secondRun = selector.optimize(items, 2);
        int[] changedSnapshot = firstRun.getSelectedItemIds();
        changedSnapshot[0] = 99;

        assertArrayEquals(new int[] {8}, firstRun.getSelectedItemIds());
        assertArrayEquals(firstRun.getSelectedItemIds(), secondRun.getSelectedItemIds());
        assertEquals(first, items[0]);
        assertEquals(second, items[1]);
    }

    @Test
    void rejectsInputBeyondPracticalLimitBeforeEnumeration() {
        OptimizationItem[] items = new OptimizationItem[
                BruteForceIncidentSelector.MAX_SUPPORTED_ITEMS + 1];
        for (int i = 0; i < items.length; i++) {
            items[i] = item(i, 0, 0);
        }

        assertThrows(IllegalArgumentException.class, () -> selector.optimize(items, 0));
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
