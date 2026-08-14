package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link InsertionSort}.
 *
 * <p>Covers the scenarios required by Team 2: unordered values, already
 * sorted array, reverse sorted array, duplicates, empty array, single element,
 * nearly sorted data, and null input safety.</p>
 */
class InsertionSortTest {

    @Test
    void sortRandomUnorderedValuesAscending() {
        Integer[] values = {5, 3, 8, 1};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 8}, values);
    }

    @Test
    void sortAlreadySortedArrayLeavesItUnchanged() {
        Integer[] values = {1, 2, 3, 4};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, values);
    }

    @Test
    void sortReverseSortedArrayAscending() {
        Integer[] values = {5, 4, 3, 2, 1};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, values);
    }

    @Test
    void sortArrayWithDuplicatesOrdering() {
        Integer[] values = {4, 2, 4, 1};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 4, 4}, values);
    }

    @Test
    void sortEmptyArrayDoesNotThrow() {
        Integer[] values = {};
        assertDoesNotThrow(() -> InsertionSort.sort(values));
    }

    @Test
    void sortSingleElementArrayDoesNotThrow() {
        Integer[] values = {10};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{10}, values);
    }

    @Test
    void sortNearlySortedListFixesOrdering() {
        Integer[] values = {1, 2, 3, 5, 4};
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, values);
    }

    @Test
    void sortNullInputDoesNotThrow() {
        assertDoesNotThrow(() -> InsertionSort.sort(null));
    }

    @Test
    void sortNewIncidentIntoSortedList() {
        String[] incidents = {"INC001", "INC005", "INC010", "INC020", "INC008"};
        InsertionSort.sort(incidents);
        assertArrayEquals(new String[]{"INC001", "INC005", "INC008", "INC010", "INC020"}, incidents);
    }

    @Test
    void sortMutatesInputInPlace() {
        Integer[] values = {6, 3, 5, 1};
        Integer[] originalReference = values;
        InsertionSort.sort(values);
        assertArrayEquals(new Integer[]{1, 3, 5, 6}, values);
        assertSame(originalReference, values);
    }
}
