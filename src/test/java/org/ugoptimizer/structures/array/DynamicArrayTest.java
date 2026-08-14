package org.ugoptimizer.structures.array;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DynamicArrayTest {

    @Test
    void emptyArrayStartsWithZeroSize() {
        DynamicArray<String> array = new DynamicArray<>();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    @Test
    void addAppendsAnElement() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        assertEquals(1, array.size());
        assertEquals("A", array.get(0));
    }

    @Test
    void multipleAdditionsPreserveOrder() {
        DynamicArray<Integer> array = new DynamicArray<>();
        array.add(1);
        array.add(2);
        array.add(3);
        assertArrayEquals(new Object[] {1, 2, 3}, array.toArray());
    }

    @Test
    void automaticResizeOccursWhenCapacityIsExceeded() {
        DynamicArray<String> array = new DynamicArray<>(0);
        array.add("A");
        assertEquals(1, array.size());
        assertTrue(array.capacity() >= 1);
    }

    @Test
    void repeatedResizePreservesAllElements() {
        DynamicArray<Integer> array = new DynamicArray<>(1);
        for (int i = 0; i < 128; i++) {
            array.add(i);
        }
        assertEquals(128, array.size());
        assertTrue(array.capacity() >= 128);
        for (int i = 0; i < 128; i++) {
            assertEquals(i, array.get(i));
        }
    }

    @Test
    void insertSupportsBeginningMiddleAndEndPositions() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("B");
        array.add("D");
        array.add(0, "A");
        array.add(2, "C");
        array.add(array.size(), "E");
        assertArrayEquals(new Object[] {"A", "B", "C", "D", "E"}, array.toArray());
    }

    @Test
    void getReturnsTheStoredValue() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        array.add("B");
        assertEquals("B", array.get(1));
    }

    @Test
    void setReplacesTheValueAndReturnsTheOldOne() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        array.add("B");
        assertEquals("B", array.set(1, "Z"));
        assertEquals("Z", array.get(1));
    }

    @Test
    void removeDeletesAnElementAndShiftsRemainingValues() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        array.add("B");
        array.add("C");
        assertEquals("B", array.remove(1));
        assertEquals(2, array.size());
        assertArrayEquals(new Object[] {"A", "C"}, array.toArray());
    }

    @Test
    void firstAndLastIndexReflectDuplicateValues() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        array.add("B");
        array.add("A");
        array.add("C");
        array.add("A");
        assertEquals(0, array.indexOf("A"));
        assertEquals(4, array.lastIndexOf("A"));
    }

    @Test
    void negativeIndexesThrow() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(-1, "B"));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(-1, "B"));
    }

    @Test
    void tooLargeIndexesThrow() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(1, "B"));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(2, "B"));
    }

    @Test
    void duplicateValuesAreAllowed() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("dup");
        array.add("dup");
        array.add("dup");
        assertEquals(3, array.size());
        assertArrayEquals(new Object[] {"dup", "dup", "dup"}, array.toArray());
    }

    @Test
    void largeInputIsHandledCorrectly() {
        DynamicArray<Integer> array = new DynamicArray<>(1);
        for (int i = 0; i < 5000; i++) {
            array.add(i);
        }
        assertEquals(5000, array.size());
        assertEquals(0, array.get(0));
        assertEquals(4999, array.get(4999));
    }

    @Test
    void clearAllowsReuse() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        array.add("B");
        array.clear();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
        array.add("C");
        assertArrayEquals(new Object[] {"C"}, array.toArray());
    }
}
