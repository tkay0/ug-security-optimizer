package org.ugoptimizer.structures.queue;

import java.util.NoSuchElementException;

/**
 * A fixed-capacity circular (ring-buffer) queue backed by a plain array.
 * Front and rear indices wrap around the array using modulo arithmetic.
 * Does not use java.util.Queue or any other built-in queue type.
 *
 * @param <T> the type of elements held in this queue
 */
public class CircularQueue<T> {

    private final Object[] elements;
    private final int capacity;
    private int front;
    private int rear;
    private int size;

    public CircularQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.elements = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    /**
     * Adds an item to the back of the queue.
     *
     * @throws IllegalStateException if the queue is already full
     */
    public void enqueue(T item) {
        if (isFull()) {
            throw new IllegalStateException("Cannot enqueue: queue is full (capacity " + capacity + ")");
        }
        rear = (rear + 1) % capacity;
        elements[rear] = item;
        size++;
    }

    /**
     * Removes and returns the item at the front of the queue.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot dequeue from an empty queue");
        }
        T value = (T) elements[front];
        elements[front] = null;
        front = (front + 1) % capacity;
        size--;
        return value;
    }

    /**
     * Returns, without removing, the item at the front of the queue.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek an empty queue");
        }
        return (T) elements[front];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }
}
