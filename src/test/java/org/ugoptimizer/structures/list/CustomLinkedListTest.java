package org.ugoptimizer.structures.list;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class CustomLinkedListTest {

    @Test
    void emptyListHasNoElements() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertThrows(NoSuchElementException.class, list::removeFirst);
        assertThrows(NoSuchElementException.class, list::removeLast);
    }

    @Test
    void addFirstPrependsElements() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addFirst("B");
        list.addFirst("A");
        assertArrayEquals(new Object[] {"A", "B"}, list.toArray());
    }

    @Test
    void addLastAppendsElements() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        assertArrayEquals(new Object[] {"A", "B"}, list.toArray());
    }

    @Test
    void insertAfterByValueInsertsAfterTheFirstMatchingElement() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        list.insertAfter("B", "X");
        assertArrayEquals(new Object[] {"A", "B", "X", "C"}, list.toArray());
    }

    @Test
    void insertAfterCanAppendAfterTheTailValue() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.insertAfter("B", "C");
        assertArrayEquals(new Object[] {"A", "B", "C"}, list.toArray());
    }

    @Test
    void removeFirstUpdatesTheHead() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        assertEquals("A", list.removeFirst());
        assertArrayEquals(new Object[] {"B"}, list.toArray());
    }

    @Test
    void removeMiddleUnlinksOnlyThatElement() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        assertEquals("B", list.remove(1));
        assertArrayEquals(new Object[] {"A", "C"}, list.toArray());
    }

    @Test
    void removeLastUpdatesTheTail() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        assertEquals("B", list.removeLast());
        assertArrayEquals(new Object[] {"A"}, list.toArray());
    }

    @Test
    void singleElementListCanBeRemovedCleanly() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("only");
        assertEquals("only", list.removeFirst());
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    void duplicatesAreTrackedCorrectly() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("dup");
        list.addLast("X");
        list.addLast("dup");
        list.addLast("dup");
        assertEquals(0, list.indexOf("dup"));
        assertEquals(3, list.lastIndexOf("dup"));
        assertArrayEquals(new Object[] {"dup", "X", "dup", "dup"}, list.toArray());
    }

    @Test
    void sizeTracksMixedOperations() {
        CustomLinkedList<Integer> list = new CustomLinkedList<>();
        list.addFirst(2);
        list.addFirst(1);
        list.addLast(3);
        list.insertAfter(2, 4);
        assertEquals(4, list.size());
        list.removeFirst();
        assertEquals(3, list.size());
        list.remove(1);
        assertEquals(2, list.size());
        list.removeLast();
        assertEquals(1, list.size());
    }

    @Test
    void clearAllowsReuse() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        list.addFirst("C");
        assertArrayEquals(new Object[] {"C"}, list.toArray());
    }
}
