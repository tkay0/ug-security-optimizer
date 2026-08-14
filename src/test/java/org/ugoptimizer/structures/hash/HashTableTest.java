package org.ugoptimizer.structures.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashTableTest {

    // --- normal cases ---

    @Test
    void putAndGetReturnsStoredValue() {
        HashTable<String, Integer> table = new HashTable<>();
        table.put("REQ-001", 100);
        table.put("REQ-002", 200);

        assertEquals(100, table.get("REQ-001"));
        assertEquals(200, table.get("REQ-002"));
        assertEquals(2, table.size());
    }

    @Test
    void putWithExistingKeyUpdatesValueAndReturnsPrevious() {
        HashTable<String, Integer> table = new HashTable<>();
        table.put("REQ-001", 100);

        Integer previous = table.put("REQ-001", 999);

        assertEquals(100, previous);
        assertEquals(999, table.get("REQ-001"));
        assertEquals(1, table.size());
    }

    @Test
    void removeDeletesKeyAndReturnsItsValue() {
        HashTable<String, Integer> table = new HashTable<>();
        table.put("REQ-001", 100);
        table.put("REQ-002", 200);

        Integer removed = table.remove("REQ-001");

        assertEquals(100, removed);
        assertNull(table.get("REQ-001"));
        assertFalse(table.containsKey("REQ-001"));
        assertEquals(1, table.size());
    }

    @Test
    void containsKeyReflectsCurrentState() {
        HashTable<String, Integer> table = new HashTable<>();
        assertFalse(table.containsKey("REQ-001"));

        table.put("REQ-001", 100);
        assertTrue(table.containsKey("REQ-001"));
    }

    // --- collision handling ---

    @Test
    void collisionCountIncreasesWhenKeysShareABucket() {
        // Tiny fixed capacity forces every key into one of two buckets.
        HashTable<Integer, String> table = new HashTable<>(2, 1.0);

        table.put(1, "a");
        table.put(3, "b"); // shares a bucket with key 1 in a capacity-2 table
        table.put(5, "c"); // shares a bucket with keys 1 and 3

        assertTrue(table.getCollisionCount() >= 2);
        assertEquals(3, table.size());
        assertEquals("a", table.get(1));
        assertEquals("b", table.get(3));
        assertEquals("c", table.get(5));
    }

    @Test
    void resizeGrowsCapacityAndPreservesAllEntries() {
        HashTable<Integer, Integer> table = new HashTable<>(4, 0.75);
        int initialCapacity = table.getCapacity();

        for (int i = 0; i < 50; i++) {
            table.put(i, i * i);
        }

        assertTrue(table.getCapacity() > initialCapacity);
        assertEquals(50, table.size());
        for (int i = 0; i < 50; i++) {
            assertEquals(i * i, table.get(i));
        }
    }

    // --- boundary cases ---

    @Test
    void emptyTableHasZeroSizeAndReturnsNullForAnyGet() {
        HashTable<String, Integer> table = new HashTable<>();

        assertTrue(table.isEmpty());
        assertEquals(0, table.size());
        assertNull(table.get("missing"));
        assertNull(table.remove("missing"));
    }

    @Test
    void singleEntryCapacityOneTableStillWorks() {
        HashTable<String, Integer> table = new HashTable<>(1, 1.0);

        table.put("only", 42);

        assertEquals(42, table.get("only"));
        assertEquals(1, table.size());
    }

    // --- invalid input ---

    @Test
    void putWithNullKeyThrows() {
        HashTable<String, Integer> table = new HashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.put(null, 1));
    }

    @Test
    void getWithNullKeyThrows() {
        HashTable<String, Integer> table = new HashTable<>();
        assertThrows(IllegalArgumentException.class, () -> table.get(null));
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new HashTable<String, Integer>(0, 0.75));
    }

    @Test
    void constructorRejectsInvalidLoadFactor() {
        assertThrows(IllegalArgumentException.class, () -> new HashTable<String, Integer>(16, 0));
        assertThrows(IllegalArgumentException.class, () -> new HashTable<String, Integer>(16, 1.5));
    }
}
