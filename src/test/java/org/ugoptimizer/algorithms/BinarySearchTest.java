package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BinarySearch}.
 *
 * <p>Covers the scenarios required by Team 2: element found in the middle,
 * first element, last element, missing element, empty array, single element,
 * duplicate values, null input safety, null elements, and the {@code isSorted}
 * helper.</p>
 */
class BinarySearchTest {

    private static final String[] SORTED_INCIDENTS = {"INC001", "INC020", "INC050", "INC100", "INC200"};

    @Test
    void searchMiddleElementReturnsCorrectIndex() {
        assertEquals(3, BinarySearch.search(SORTED_INCIDENTS, "INC100"));
    }

    @Test
    void searchFirstElementReturnsZero() {
        assertEquals(0, BinarySearch.search(SORTED_INCIDENTS, "INC001"));
    }

    @Test
    void searchLastElementReturnsLastIndex() {
        assertEquals(4, BinarySearch.search(SORTED_INCIDENTS, "INC200"));
    }

    @Test
    void searchMissingElementReturnsMinusOne() {
        assertEquals(-1, BinarySearch.search(SORTED_INCIDENTS, "INC900"));
    }

    @Test
    void searchEmptyArrayReturnsMinusOne() {
        assertEquals(-1, BinarySearch.search(new String[0], "INC001"));
    }

    @Test
    void searchSingleElementFoundReturnsZero() {
        String[] single = {"INC001"};
        assertEquals(0, BinarySearch.search(single, "INC001"));
    }

    @Test
    void searchSingleElementMissingReturnsMinusOne() {
        String[] single = {"INC001"};
        assertEquals(-1, BinarySearch.search(single, "INC002"));
    }

    @Test
    void searchDuplicatesReturnsValidOccurrence() {
        String[] duplicates = {"INC001", "INC001", "INC002"};
        int index = BinarySearch.search(duplicates, "INC001");
        assertTrue(index == 0 || index == 1, "Returned index " + index + " is not a valid occurrence of INC001");
    }

    @Test
    void searchNullArrayReturnsMinusOneWithoutCrashing() {
        assertEquals(-1, BinarySearch.search(null, "INC001"));
    }

    @Test
    void searchNullTargetReturnsMinusOneWithoutCrashing() {
        assertEquals(-1, BinarySearch.search(SORTED_INCIDENTS, null));
    }

    @Test
    void searchArrayContainingNullElementReturnsMinusOneWithoutCrashing() {
        String[] withNull = {"INC001", null, "INC050", "INC100"};
        assertEquals(-1, BinarySearch.search(withNull, "INC001"));
    }

    @Test
    void isSortedAcceptsSortedArray() {
        assertTrue(BinarySearch.isSorted(SORTED_INCIDENTS));
    }

    @Test
    void isSortedRejectsUnsortedArray() {
        String[] unsorted = {"INC100", "INC020", "INC300"};
        assertFalse(BinarySearch.isSorted(unsorted));
    }

    @Test
    void isSortedAcceptsEmptyAndSingleElementArrays() {
        assertTrue(BinarySearch.isSorted(new String[0]));
        assertTrue(BinarySearch.isSorted(new String[]{"INC001"}));
    }

    @Test
    void isSortedRejectsArrayContainingNullElementWithoutCrashing() {
        String[] withNull = {"INC001", "INC050", null, "INC100"};
        assertFalse(BinarySearch.isSorted(withNull));
    }
}
