package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic verification of {@link QuickSort}'s documented worst case,
 * using {@link QuickSortWorstCaseDemo}'s instrumented comparison count
 * instead of wall-clock timing (which would be flaky in CI).
 *
 * <p>With a last-element (Lomuto) pivot, a sorted array of size {@code n}
 * always produces exactly {@code n(n-1)/2} comparisons: every partition
 * step splits the array into one empty side and one side of size
 * {@code n-1}, so no recursive call is ever skipped or balanced.</p>
 */
class QuickSortWorstCaseDemoTest {

    @Test
    void sortedInputProducesExactWorstCaseComparisonCount() {
        for (int n : new int[]{0, 1, 2, 10, 100, 1000}) {
            Integer[] sorted = QuickSortWorstCaseDemo.sortedInput(n);
            long comparisons = QuickSortWorstCaseDemo.sortCountingComparisons(sorted);
            long expected = (long) n * (n - 1) / 2;
            assertEquals(expected, comparisons,
                    "Expected exactly n(n-1)/2 comparisons for sorted input of size " + n);
        }
    }

    @Test
    void worstCaseComparisonCountGrowsQuadraticallyNotLinearithmically() {
        long comparisonsAt1000 = QuickSortWorstCaseDemo.sortCountingComparisons(QuickSortWorstCaseDemo.sortedInput(1000));
        long comparisonsAt2000 = QuickSortWorstCaseDemo.sortCountingComparisons(QuickSortWorstCaseDemo.sortedInput(2000));

        // Doubling n should roughly quadruple comparisons for O(n^2) growth
        // (exactly quadruple here, since the worst case is exact and
        // deterministic for this pivot strategy).
        double ratio = (double) comparisonsAt2000 / (double) comparisonsAt1000;
        assertTrue(ratio > 3.9 && ratio < 4.1,
                "Expected roughly 4x growth for doubled n under O(n^2), got ratio " + ratio);
    }

    @Test
    void randomInputProducesFarFewerComparisonsThanWorstCase() {
        int n = 2000;
        Random random = new Random(123);
        long randomComparisons = QuickSortWorstCaseDemo.sortCountingComparisons(QuickSortWorstCaseDemo.randomInput(n, random));
        long worstCaseComparisons = (long) n * (n - 1) / 2;

        assertTrue(randomComparisons < worstCaseComparisons / 4,
                "Random input should need far fewer comparisons than the O(n^2) worst case; "
                        + "got " + randomComparisons + " vs worst case " + worstCaseComparisons);
    }

    @Test
    void instrumentedSortStillProducesACorrectlySortedArray() {
        Integer[] values = QuickSortWorstCaseDemo.sortedInput(50);
        Integer[] expected = values.clone();
        QuickSortWorstCaseDemo.sortCountingComparisons(values);
        assertArrayEquals(expected, values);
    }
}
