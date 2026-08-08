package org.ugoptimizer.structures.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomMapTest {

    // --- normal cases ---

    @Test
    void putAndGetRoundTrip() {
        CustomMap<String, Integer> map = new CustomMap<>();
        map.put("RES-01", 10);
        map.put("RES-02", 20);

        assertEquals(10, map.get("RES-01"));
        assertEquals(20, map.get("RES-02"));
        assertEquals(2, map.size());
    }

    @Test
    void removePresentKeyReturnsOldValueAndShrinksMap() {
        CustomMap<String, Integer> map = new CustomMap<>();
        map.put("RES-01", 10);

        Integer removed = map.remove("RES-01");

        assertEquals(10, removed);
        assertFalse(map.containsKey("RES-01"));
        assertEquals(0, map.size());
    }

    @Test
    void keySetAndValuesReflectStoredEntries() {
        CustomMap<String, Integer> map = new CustomMap<>();
        map.put("A", 1);
        map.put("B", 2);

        int keyCount = 0;
        for (String key : map.keySet()) {
            assertTrue(key.equals("A") || key.equals("B"));
            keyCount++;
        }
        assertEquals(2, keyCount);

        int valueSum = 0;
        for (Integer value : map.values()) {
            valueSum += value;
        }
        assertEquals(3, valueSum);
    }

    // --- boundary cases ---

    @Test
    void emptyMapHasZeroSizeAndNoContainment() {
        CustomMap<String, Integer> map = new CustomMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("anything"));
        assertNull(map.get("anything"));
    }

    @Test
    void removingFromEmptyMapReturnsNull() {
        CustomMap<String, Integer> map = new CustomMap<>();
        assertNull(map.remove("missing"));
    }

    // --- invalid input ---

    @Test
    void putWithNullKeyThrows() {
        CustomMap<String, Integer> map = new CustomMap<>();
        assertThrows(IllegalArgumentException.class, () -> map.put(null, 1));
    }
}
