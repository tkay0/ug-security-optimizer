package org.ugoptimizer.structures.stack;

import java.util.NoSuchElementException;

/**
 * A simple resizable array-based LIFO stack.
 * Does not use java.util.Stack or any other built-in stack/queue type.
 *
 * @param <T> the type of elements held in this stack
 */
public class CustomStack<T> {

    private static final int DEFAULT_CAPACITY = 10;

    private Object[] elements;
    private int size;

    public CustomStack() {
        this(DEFAULT_CAPACITY);
    }

    public CustomStack(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be positive");
        }
        this.elements = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * Pushes an item onto the top of the stack.
     */
    public void push(T item) {
        ensureCapacity();
        elements[size++] = item;
    }

    /**
     * Removes and returns the item at the top of the stack.
     *
     * @throws NoSuchElementException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot pop from an empty stack");
        }
        T item = (T) elements[size - 1];
        elements[size - 1] = null; // avoid memory leak
        size--;
        return item;
    }

    /**
     * Returns, without removing, the item at the top of the stack.
     *
     * @throws NoSuchElementException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek an empty stack");
        }
        return (T) elements[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            Object[] resized = new Object[elements.length * 2];
            System.arraycopy(elements, 0, resized, 0, size);
            elements = resized;
        }
    }
}
