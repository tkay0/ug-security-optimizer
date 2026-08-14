package org.ugoptimizer.structures.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomSetTest {

    // --- normal cases ---

    @Test
    void addNewElementsIncreasesSize() {
        CustomSet<String> set = new CustomSet<>();

        assertTrue(set.add("RES-01"));
        assertTrue(set.add("RES-02"));

        assertEquals(2, set.size());
        assertTrue(set.contains("RES-01"));
        assertTrue(set.contains("RES-02"));
    }

    @Test
    void addingDuplicateElementReturnsFalseAndDoesNotGrowSet() {
        CustomSet<String> set = new CustomSet<>();
        set.add("RES-01");

        boolean addedAgain = set.add("RES-01");

        assertFalse(addedAgain);
        assertEquals(1, set.size());
    }

    @Test
    void removePresentElementReturnsTrueAndDropsIt() {
        CustomSet<String> set = new CustomSet<>();
        set.add("RES-01");

        boolean removed = set.remove("RES-01");

        assertTrue(removed);
        assertFalse(set.contains("RES-01"));
        assertEquals(0, set.size());
    }

    @Test
    void removeAbsentElementReturnsFalse() {
        CustomSet<String> set = new CustomSet<>();
        assertFalse(set.remove("never-added"));
    }

    @Test
    void elementsIterableVisitsEveryMember() {
        CustomSet<String> set = new CustomSet<>();
        set.add("A");
        set.add("B");
        set.add("C");

        int count = 0;
        for (String element : set.elements()) {
            assertTrue(element.equals("A") || element.equals("B") || element.equals("C"));
            count++;
        }
        assertEquals(3, count);
    }

    // --- boundary cases ---

    @Test
    void emptySetHasZeroSizeAndNoContainment() {
        CustomSet<String> set = new CustomSet<>();

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("anything"));
    }

    // --- invalid input ---

    @Test
    void addingNullElementThrows() {
        CustomSet<String> set = new CustomSet<>();
        assertThrows(IllegalArgumentException.class, () -> set.add(null));
    }
}
