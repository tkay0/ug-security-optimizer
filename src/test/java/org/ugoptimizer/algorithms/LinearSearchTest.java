package org.ugoptimizer.algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LinearSearch}.
 *
 * <p>Covers the scenarios required by Team 2: element found in the middle,
 * first element, last element, missing element, empty array, duplicate
 * values, and null input safety.</p>
 */
class LinearSearchTest {

    private static final String[] INCIDENTS = {"INC120", "INC350", "INC280", "INC500"};

    @Test
    void searchExistingElementInMiddleReturnsCorrectIndex() {
        int index = LinearSearch.search(INCIDENTS, "INC350");
        assertEquals(1, index);
    }

    @Test
    void searchFirstElementReturnsZero() {
        int index = LinearSearch.search(INCIDENTS, "INC120");
        assertEquals(0, index);
    }

    @Test
    void searchLastElementReturnsLastIndex() {
        int index = LinearSearch.search(INCIDENTS, "INC500");
        assertEquals(3, index);
    }

    @Test
    void searchMissingElementReturnsMinusOne() {
        int index = LinearSearch.search(INCIDENTS, "INC900");
        assertEquals(-1, index);
    }

    @Test
    void searchEmptyArrayReturnsMinusOne() {
        int index = LinearSearch.search(new String[0], "INC120");
        assertEquals(-1, index);
    }

    @Test
    void searchDuplicateValuesReturnsFirstOccurrence() {
        String[] duplicates = {"INC001", "INC280", "INC280", "INC300"};
        int index = LinearSearch.search(duplicates, "INC280");
        assertEquals(1, index);
    }

    @Test
    void searchNullArrayReturnsMinusOneWithoutCrashing() {
        int index = LinearSearch.search(null, "INC120");
        assertEquals(-1, index);
    }

    @Test
    void searchNullTargetReturnsMinusOneWithoutCrashing() {
        String[] incidents = {"INC001", "INC002"};
        int index = LinearSearch.search(incidents, null);
        assertEquals(-1, index);
    }

    @Test
    void searchNullTargetAgainstArrayWithNullElementReturnsItsIndex() {
        String[] incidents = {"INC001", null, "INC003"};
        int index = LinearSearch.search(incidents, null);
        assertEquals(1, index);
    }
}
