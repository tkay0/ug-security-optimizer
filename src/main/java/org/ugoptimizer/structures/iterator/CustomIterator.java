package org.ugoptimizer.structures.iterator;

/**
 * Minimal iteration contract shared by all custom linear structures in this
 * project (DynamicArray, CustomLinkedList). Deliberately independent of
 * {@code java.util.Iterator} so that no built-in collection types leak into
 * public APIs.
 *
 * @param <E> the element type
 */
public interface CustomIterator<E> {

    /**
     * Returns whether there is at least one more element to visit.
     * Complexity: O(1).
     *
     * @return true if next() can be called safely
     */
    boolean hasNext();

    /**
     * Returns the next element in iteration order and advances the cursor.
     * Complexity: O(1).
     *
     * @return the next element
     * @throws java.util.NoSuchElementException if iteration is already exhausted
     */
    E next();
}