package org.ugoptimizer.structures.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.structures.array.DynamicArray;
import org.ugoptimizer.structures.list.CustomLinkedList;

class CustomIteratorTest {

    @Test
    void emptyIterationOnDynamicArrayHasNoNext() {
        DynamicArray<String> array = new DynamicArray<>();
        CustomIterator<String> iterator = array.iterator();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void emptyIterationOnCustomLinkedListHasNoNext() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        CustomIterator<String> iterator = list.iterator();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void oneItemIterationReturnsThatItem() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("A");
        CustomIterator<String> iterator = array.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("A", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void multipleItemsIterateInInsertionOrderForDynamicArray() {
        DynamicArray<Integer> array = new DynamicArray<>();
        array.add(1);
        array.add(2);
        array.add(3);
        CustomIterator<Integer> iterator = array.iterator();
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        assertEquals(3, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void multipleItemsIterateInInsertionOrderForCustomLinkedList() {
        CustomLinkedList<Integer> list = new CustomLinkedList<>();
        list.addLast(1);
        list.addLast(2);
        list.addLast(3);
        CustomIterator<Integer> iterator = list.iterator();
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        assertEquals(3, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void hasNextStaysAccurateBeforeAndAfterExhaustion() {
        CustomLinkedList<String> list = new CustomLinkedList<>();
        list.addLast("A");
        list.addLast("B");
        CustomIterator<String> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    @Test
    void nextAdvancesTheCursorOneElementAtATime() {
        DynamicArray<String> array = new DynamicArray<>();
        array.add("first");
        array.add("second");
        CustomIterator<String> iterator = array.iterator();
        assertEquals("first", iterator.next());
        assertEquals("second", iterator.next());
    }

    @Test
    void exhaustedDynamicArrayIterationThrowsOnNext() {
        DynamicArray<Integer> array = new DynamicArray<>();
        array.add(1);
        CustomIterator<Integer> iterator = array.iterator();
        iterator.next();
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void exhaustedCustomLinkedListIterationThrowsOnNext() {
        CustomLinkedList<Integer> list = new CustomLinkedList<>();
        list.addLast(1);
        CustomIterator<Integer> iterator = list.iterator();
        iterator.next();
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
