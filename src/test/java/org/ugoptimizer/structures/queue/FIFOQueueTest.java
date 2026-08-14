package org.ugoptimizer.structures.queue;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FIFOQueueTest {

    @Test
    void newQueueIsEmpty() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void enqueueIncreasesSize() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        queue.enqueue(1);
        queue.enqueue(2);
        assertEquals(2, queue.size());
        assertTrue(!queue.isEmpty());
    }

    @Test
    void dequeueReturnsItemsInFifoOrder() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
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
        FIFOQueue<String> queue = new FIFOQueue<>();
        queue.enqueue("first");
        queue.enqueue("second");

        assertEquals("first", queue.peek());
        assertEquals(2, queue.size());
    }

    @Test
    void dequeueOnEmptyQueueThrows() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        assertThrows(NoSuchElementException.class, queue::dequeue);
    }

    @Test
    void peekOnEmptyQueueThrows() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    void queueIsReusableAfterBecomingEmpty() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        queue.enqueue(1);
        queue.dequeue();
        assertTrue(queue.isEmpty());

        queue.enqueue(2);
        queue.enqueue(3);
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void handlesManyElementsUnbounded() {
        FIFOQueue<Integer> queue = new FIFOQueue<>();
        for (int i = 0; i < 1000; i++) {
            queue.enqueue(i);
        }
        assertEquals(1000, queue.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, queue.dequeue());
        }
        assertTrue(queue.isEmpty());
    }
}
