package org.ugoptimizer.structures.queue;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DequeTest {

    @Test
    void newDequeIsEmpty() {
        Deque<Integer> deque = new Deque<>();
        assertTrue(deque.isEmpty());
        assertEquals(0, deque.size());
    }

    @Test
    void addFirstAndRemoveFirstActsLikeStack() {
        Deque<Integer> deque = new Deque<>();
        deque.addFirst(1);
        deque.addFirst(2);
        deque.addFirst(3);

        assertEquals(3, deque.removeFirst());
        assertEquals(2, deque.removeFirst());
        assertEquals(1, deque.removeFirst());
        assertTrue(deque.isEmpty());
    }

    @Test
    void addLastAndRemoveFirstActsLikeQueue() {
        Deque<Integer> deque = new Deque<>();
        deque.addLast(1);
        deque.addLast(2);
        deque.addLast(3);

        assertEquals(1, deque.removeFirst());
        assertEquals(2, deque.removeFirst());
        assertEquals(3, deque.removeFirst());
        assertTrue(deque.isEmpty());
    }

    @Test
    void mixedOperationsFromBothEnds() {
        Deque<Integer> deque = new Deque<>();
        deque.addLast(2);
        deque.addFirst(1);
        deque.addLast(3);
        deque.addFirst(0);
        // deque is now: 0, 1, 2, 3

        assertEquals(0, deque.peekFirst());
        assertEquals(3, deque.peekLast());
        assertEquals(4, deque.size());

        assertEquals(3, deque.removeLast());
        assertEquals(0, deque.removeFirst());
        // remaining: 1, 2
        assertEquals(1, deque.removeFirst());
        assertEquals(2, deque.removeLast());
        assertTrue(deque.isEmpty());
    }

    @Test
    void peekFirstAndPeekLastDoNotRemove() {
        Deque<String> deque = new Deque<>();
        deque.addLast("a");
        deque.addLast("b");

        assertEquals("a", deque.peekFirst());
        assertEquals("b", deque.peekLast());
        assertEquals(2, deque.size());
    }

    @Test
    void removeFirstOnEmptyDequeThrows() {
        Deque<Integer> deque = new Deque<>();
        assertThrows(NoSuchElementException.class, deque::removeFirst);
    }

    @Test
    void removeLastOnEmptyDequeThrows() {
        Deque<Integer> deque = new Deque<>();
        assertThrows(NoSuchElementException.class, deque::removeLast);
    }

    @Test
    void peekFirstOnEmptyDequeThrows() {
        Deque<Integer> deque = new Deque<>();
        assertThrows(NoSuchElementException.class, deque::peekFirst);
    }

    @Test
    void peekLastOnEmptyDequeThrows() {
        Deque<Integer> deque = new Deque<>();
        assertThrows(NoSuchElementException.class, deque::peekLast);
    }

    @Test
    void singleElementRemoveFirstEmptiesBothEnds() {
        Deque<Integer> deque = new Deque<>();
        deque.addFirst(42);
        assertEquals(42, deque.removeFirst());
        assertTrue(deque.isEmpty());
        assertThrows(NoSuchElementException.class, deque::peekLast);
    }

    @Test
    void singleElementRemoveLastEmptiesBothEnds() {
        Deque<Integer> deque = new Deque<>();
        deque.addLast(42);
        assertEquals(42, deque.removeLast());
        assertTrue(deque.isEmpty());
        assertThrows(NoSuchElementException.class, deque::peekFirst);
    }

    @Test
    void dequeIsReusableAfterBecomingEmpty() {
        Deque<Integer> deque = new Deque<>();
        deque.addFirst(1);
        deque.removeFirst();
        assertTrue(deque.isEmpty());

        deque.addLast(2);
        deque.addFirst(1);
        assertEquals(1, deque.peekFirst());
        assertEquals(2, deque.peekLast());
        assertEquals(2, deque.size());
    }
}
