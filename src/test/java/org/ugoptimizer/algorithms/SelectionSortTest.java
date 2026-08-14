package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link SelectionSort}.
 *
 * <p>Covers the scenarios required by Team 2: unordered values, already
 * sorted array, reverse sorted array, duplicates, empty array, single element,
 * null input, and a large dataset simulation.</p>
 */
class SelectionSortTest {

    @Test
    void sortRandomUnorderedValuesAscending() {
        Integer[] values = {5, 3, 8, 1};
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 8}, values);
    }

    @Test
    void sortAlreadySortedArrayLeavesItUnchanged() {
        Integer[] values = {1, 2, 3, 4};
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, values);
    }

    @Test
    void sortReverseSortedArrayAscending() {
        Integer[] values = {5, 4, 3, 2, 1};
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, values);
    }

    @Test
    void sortArrayWithDuplicatesOrdering() {
        Integer[] values = {5, 2, 5, 1};
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 5, 5}, values);
    }

    @Test
    void sortEmptyArrayDoesNotThrow() {
        Integer[] values = {};
        assertDoesNotThrow(() -> SelectionSort.sort(values));
    }

    @Test
    void sortSingleElementArrayDoesNotThrow() {
        Integer[] values = {10};
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{10}, values);
    }

    @Test
    void sortNullInputDoesNotThrow() {
        assertDoesNotThrow(() -> SelectionSort.sort(null));
    }

    @Test
    void sortIncidentIdsAscending() {
        String[] incidents = {"INC300", "INC050", "INC120", "INC001"};
        SelectionSort.sort(incidents);
        assertArrayEquals(new String[]{"INC001", "INC050", "INC120", "INC300"}, incidents);
    }

    @Test
    void sortLargeDatasetSimulation() {
        int size = 1000;
        Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++) {
            values[i] = size - i;
        }
        SelectionSort.sort(values);
        Integer[] expected = new Integer[size];
        for (int i = 0; i < size; i++) {
            expected[i] = i + 1;
        }
        assertArrayEquals(expected, values);
    }

    @Test
    void sortMutatesInputInPlace() {
        Integer[] values = {7, 4, 5, 2};
        Integer[] originalReference = values;
        SelectionSort.sort(values);
        assertArrayEquals(new Integer[]{2, 4, 5, 7}, values);
        assertSame(originalReference, values);
    }
}
