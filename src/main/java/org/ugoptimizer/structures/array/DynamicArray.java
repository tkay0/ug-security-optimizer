package org.ugoptimizer.structures.array;

import java.util.NoSuchElementException;
import org.ugoptimizer.structures.iterator.CustomIterator;

/**
 * A resizable array implemented from scratch on top of a manually managed
 * {@code Object[]} backing store. No built-in collection types are used.
 *
 * <p>Capacity growth policy: starts empty (or at the requested initial
 * capacity), grows to 1 on first insertion if empty, and doubles thereafter.
 * Growth arithmetic is protected against integer overflow.
 *
 * @param <E> the element type
 */
public final class DynamicArray<E> {

    /** Default capacity used by the no-arg constructor. */
    private static final int DEFAULT_CAPACITY = 10;

    /** Largest array size that can safely be allocated on most JVMs. */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private Object[] elements;
    private int size;

    /**
     * Creates an array with the default initial capacity.
     * Complexity: O(1).
     */
    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates an array with the given initial capacity.
     * Complexity: O(1).
     *
     * @param initialCapacity the starting backing-array size, must be >= 0
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity cannot be negative: " + initialCapacity);
        }
        this.elements = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * Returns the number of elements currently stored.
     * Complexity: O(1).
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether the array currently holds no elements.
     * Complexity: O(1).
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Appends an element to the end.
     * Complexity: amortized O(1); O(n) on the rare resize.
     */
    public void add(E element) {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    /**
     * Inserts an element at the given index, shifting later elements right.
     * Complexity: O(n).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size]
     */
    public void add(int index, E element) {
        validateIndexForAdd(index);
        ensureCapacity(size + 1);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = element;
        size++;
    }

    /**
     * Returns the element at the given index.
     * Complexity: O(1).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        validateIndex(index);
        return (E) elements[index];
    }

    /**
     * Replaces the element at the given index, returning the old value.
     * Complexity: O(1).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        validateIndex(index);
        E old = (E) elements[index];
        elements[index] = element;
        return old;
    }

    /**
     * Removes and returns the element at the given index, shifting later
     * elements left. Complexity: O(n).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        validateIndex(index);
        E removed = (E) elements[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(elements, index + 1, elements, index, numMoved);
        }
        elements[--size] = null; // clear reference for GC
        return removed;
    }

    /**
     * Returns whether the given element is present (null-safe equality).
     * Complexity: O(n).
     */
    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }

    /**
     * Returns the index of the first occurrence of the given element, or -1
     * if absent (null-safe equality). Complexity: O(n).
     */
    public int indexOf(Object element) {
        for (int i = 0; i < size; i++) {
            if (element == null ? elements[i] == null : element.equals(elements[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the given element, or -1
     * if absent (null-safe equality). Complexity: O(n).
     */
    public int lastIndexOf(Object element) {
        for (int i = size - 1; i >= 0; i--) {
            if (element == null ? elements[i] == null : element.equals(elements[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all elements, clearing references so they can be garbage
     * collected, but keeps the current backing capacity. Complexity: O(n).
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }

    /**
     * Returns a new array containing the elements in order. Mutating the
     * returned array never affects this structure. Complexity: O(n).
     */
    public Object[] toArray() {
        Object[] copy = new Object[size];
        System.arraycopy(elements, 0, copy, 0, size);
        return copy;
    }

    /**
     * Returns an iterator over the elements in order.
     * Complexity: O(1) to create, O(n) to exhaust.
     */
    public CustomIterator<E> iterator() {
        return new CustomIterator<E>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Iteration already exhausted");
                }
                return (E) elements[cursor++];
            }
        };
    }

    /** Current backing array length. Exposed only for tests/evidence. */
    public int capacity() {
        return elements.length;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
        }
    }

    private void validateIndexForAdd(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - elements.length <= 0) {
            return;
        }
        int oldCapacity = elements.length;
        int newCapacity = (oldCapacity == 0) ? 1 : oldCapacity * 2;
        // Overflow guard: if doubling overflowed (went negative) or is still
        // too small, fall back to a safe huge capacity.
        if (newCapacity < 0 || newCapacity < minCapacity) {
            newCapacity = hugeCapacity(minCapacity);
        }
        Object[] newElements = new Object[newCapacity];
        System.arraycopy(elements, 0, newElements, 0, size);
        elements = newElements;
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError("Requested array capacity exceeds implementation limit");
        }
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }
}
