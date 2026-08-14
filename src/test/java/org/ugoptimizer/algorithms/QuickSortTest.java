package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link QuickSort}.
 *
 * <p>Covers the scenarios required by Team 3: empty array, single element,
 * already sorted, reverse sorted, duplicates, and random input, plus null
 * safety, in-place mutation, and correctness on the algorithm's own worst
 * case (already-sorted input with a last-element pivot).</p>
 */
class QuickSortTest {

    @Test
    void sortEmptyArrayDoesNotThrow() {
        Integer[] values = {};
        assertDoesNotThrow(() -> QuickSort.sort(values));
        assertArrayEquals(new Integer[]{}, values);
    }

    @Test
    void sortSingleElementArrayDoesNotThrow() {
        Integer[] values = {42};
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{42}, values);
    }

    @Test
    void sortAlreadySortedArrayLeavesItCorrect() {
        Integer[] values = {1, 2, 3, 4, 5};
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, values);
    }

    @Test
    void sortReverseSortedArrayAscending() {
        Integer[] values = {9, 7, 5, 3, 1};
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 7, 9}, values);
    }

    @Test
    void sortArrayWithDuplicatesOrdering() {
        Integer[] values = {4, 2, 4, 1, 2, 4};
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 2, 4, 4, 4}, values);
    }

    @Test
    void sortRandomInputProducesAscendingOrder() {
        Random random = new Random(7);
        Integer[] values = new Integer[500];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(10_000);
        }
        QuickSort.sort(values);
        assertSorted(values);
    }

    @Test
    void sortNullInputDoesNotThrow() {
        assertDoesNotThrow(() -> QuickSort.sort(null));
    }

    @Test
    void sortMutatesInputInPlace() {
        Integer[] values = {6, 3, 5, 1};
        Integer[] originalReference = values;
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 6}, values);
        assertSame(originalReference, values, "QuickSort.sort must mutate the caller's array, not return a new one");
    }

    @Test
    void sortAllEqualElementsStaysCorrect() {
        Integer[] values = {5, 5, 5, 5, 5};
        QuickSort.sort(values);
        assertArrayEquals(new Integer[]{5, 5, 5, 5, 5}, values);
    }

    /**
     * Confirms correctness (not just typical-case behaviour) on the exact
     * input that triggers this implementation's O(n^2) worst case: an
     * already sorted array, where the last-element pivot always produces
     * the most unbalanced possible partition. See {@link QuickSort}'s
     * class-level Javadoc and {@link QuickSortWorstCaseDemo}.
     */
    @Test
    void sortWorstCaseSortedInputStillProducesCorrectResult() {
        Integer[] values = new Integer[1000];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        QuickSort.sort(values);
        assertSorted(values);
        assertArrayEquals(values, values.clone());
    }

    /**
     * Same worst-case input, but reverse sorted, which is equally
     * pathological for a last-element pivot.
     */
    @Test
    void sortWorstCaseReverseSortedInputStillProducesCorrectResult() {
        Integer[] values = new Integer[1000];
        for (int i = 0; i < values.length; i++) {
            values[i] = values.length - i;
        }
        QuickSort.sort(values);
        assertSorted(values);
    }

    private static void assertSorted(Integer[] values) {
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i - 1].compareTo(values[i]) <= 0,
                    "Array not sorted at index " + i + ": " + values[i - 1] + " > " + values[i]);
        }
    }
}
