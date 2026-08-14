package org.ugoptimizer.algorithms.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OptimizerConsistencyTest {

    private final DynamicProgrammingIncidentSelector dynamicProgramming =
            new DynamicProgrammingIncidentSelector();
    private final BruteForceIncidentSelector bruteForce =
            new BruteForceIncidentSelector();

    @Test
    void algorithmsAgreeAcrossManyDeterministicSmallCasesAndCapacities() {
        long state = 7_497L;
        for (int trial = 0; trial < 40; trial++) {
            int itemCount = 1 + trial % 8;
            OptimizationItem[] items = new OptimizationItem[itemCount];
            for (int i = 0; i < itemCount; i++) {
                state = nextState(state);
                int cost = (int) ((state >>> 16) % 8L);
                state = nextState(state);
                int benefit = (int) ((state >>> 16) % 16L);
                int itemId = trial % 2 == 0 ? 1_000 + i * 17 : -1_000 - i * 19;
                items[i] = new OptimizationItem(itemId, cost, benefit);
            }

            for (int capacity = 0; capacity <= 20; capacity += 2) {
                assertSameResult(
                        dynamicProgramming.optimize(items, capacity),
                        bruteForce.optimize(items, capacity),
                        "trial " + trial + ", capacity " + capacity);
            }
        }
    }

    @Test
    void algorithmsAgreeOnNestedCompleteTiesIncludingZeroValueItems() {
        OptimizationItem[] items = {
            new OptimizationItem(1, 0, 0),
            new OptimizationItem(2, 2, 5),
            new OptimizationItem(3, 2, 5),
            new OptimizationItem(4, 4, 10)
        };

        OptimizationResult dp = dynamicProgramming.optimize(items, 4);
        OptimizationResult brute = bruteForce.optimize(items, 4);

        assertSameResult(dp, brute, "nested tie");
        assertArrayEquals(new int[] {1, 2, 3}, dp.getSelectedItemIds());
    }

    private static long nextState(long state) {
        return state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
    }

    private static void assertSameResult(
            OptimizationResult expected,
            OptimizationResult actual,
            String context) {
        assertArrayEquals(expected.getSelectedItemIds(), actual.getSelectedItemIds(), context);
        assertEquals(expected.getSelectedCount(), actual.getSelectedCount(), context);
        assertEquals(expected.getTotalCost(), actual.getTotalCost(), context);
        assertEquals(expected.getTotalBenefit(), actual.getTotalBenefit(), context);
        assertEquals(expected.getCapacity(), actual.getCapacity(), context);
    }
}
