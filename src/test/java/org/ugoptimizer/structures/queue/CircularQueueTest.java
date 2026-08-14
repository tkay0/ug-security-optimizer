package org.ugoptimizer.structures.queue;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircularQueueTest {

    @Test
    void newQueueIsEmptyNotFull() {
        CircularQueue<Integer> queue = new CircularQueue<>(3);
        assertTrue(queue.isEmpty());
        assertFalse(queue.isFull());
        assertEquals(0, queue.size());
        assertEquals(3, queue.capacity());
    }

    @Test
    void enqueueDequeueBasicFifoOrder() {
        CircularQueue<Integer> queue = new CircularQueue<>(5);
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);

        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void peekReturnsFrontWithoutRemoving() {
        CircularQueue<String> queue = new CircularQueue<>(2);
        queue.enqueue("a");
        queue.enqueue("b");

        assertEquals("a", queue.peek());
        assertEquals(2, queue.size());
    }

    @Test
    void queueBecomesFullAtCapacity() {
        CircularQueue<Integer> queue = new CircularQueue<>(3);
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);

        assertTrue(queue.isFull());
        assertEquals(3, queue.size());
    }

    @Test
    void enqueueOnFullQueueThrows() {
        CircularQueue<Integer> queue = new CircularQueue<>(2);
        queue.enqueue(1);
        queue.enqueue(2);

        assertThrows(IllegalStateException.class, () -> queue.enqueue(3));
    }

    @Test
    void dequeueOnEmptyQueueThrows() {
        CircularQueue<Integer> queue = new CircularQueue<>(2);
        assertThrows(NoSuchElementException.class, queue::dequeue);
    }

    @Test
    void peekOnEmptyQueueThrows() {
        CircularQueue<Integer> queue = new CircularQueue<>(2);
        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    void invalidCapacityThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<Integer>(-1));
    }

    /**
     * Exercises the actual "circular" behavior: fill, drain some, refill,
     * and confirm the internal front/rear indices wrap around the backing
     * array correctly rather than overwriting live data.
     */
    @Test
    void wrapsAroundCorrectlyAfterPartialDrain() {
        CircularQueue<Integer> queue = new CircularQueue<>(4);

        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        queue.enqueue(4);
        assertTrue(queue.isFull());

        // Drain two from the front, freeing up slots at the "start" of the array.
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());

        // These enqueues should wrap around to reuse the freed slots.
        queue.enqueue(5);
        queue.enqueue(6);
        assertTrue(queue.isFull());

        // Order must still be strictly FIFO across the wrap.
        assertEquals(3, queue.dequeue());
        assertEquals(4, queue.dequeue());
        assertEquals(5, queue.dequeue());
        assertEquals(6, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void repeatedWrapAroundCyclesOverManyRounds() {
        CircularQueue<Integer> queue = new CircularQueue<>(3);
        int produced = 0;
        int consumed = 0;

        for (int round = 0; round < 100; round++) {
            queue.enqueue(produced++);
            queue.enqueue(produced++);
            assertEquals(consumed++, queue.dequeue());
            queue.enqueue(produced++);
            assertEquals(consumed++, queue.dequeue());
            assertEquals(consumed++, queue.dequeue());
            assertTrue(queue.isEmpty());
        }
    }
}
