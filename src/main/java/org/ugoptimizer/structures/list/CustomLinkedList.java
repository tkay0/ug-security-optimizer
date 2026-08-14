package org.ugoptimizer.structures.list;

import java.util.NoSuchElementException;
import org.ugoptimizer.structures.iterator.CustomIterator;

/**
 * A doubly linked list implemented from scratch using a private node class.
 * {@code java.util.LinkedList} is not used and the {@code Node} type is
 * never exposed to callers.
 *
 * @param <E> the element type
 */
public final class CustomLinkedList<E> {

    /** Private node; never exposed outside this class. */
    private static final class Node<E> {
        E element;
        Node<E> prev;
        Node<E> next;

        Node(Node<E> prev, E element, Node<E> next) {
            this.prev = prev;
            this.element = element;
            this.next = next;
        }
    }

    private Node<E> head;
    private Node<E> tail;
    private int size;

    /** Creates an empty list. Complexity: O(1). */
    public CustomLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    /** Returns the number of elements. Complexity: O(1). */
    public int size() {
        return size;
    }

    /** Returns whether the list has no elements. Complexity: O(1). */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Inserts an element at the head. Complexity: O(1). */
    public void addFirst(E element) {
        Node<E> newNode = new Node<>(null, element, head);
        if (head != null) {
            head.prev = newNode;
        } else {
            tail = newNode; // list was empty
        }
        head = newNode;
        size++;
    }

    /** Appends an element at the tail. Complexity: O(1). */
    public void addLast(E element) {
        Node<E> newNode = new Node<>(tail, element, null);
        if (tail != null) {
            tail.next = newNode;
        } else {
            head = newNode; // list was empty
        }
        tail = newNode;
        size++;
    }

    /**
     * Inserts an element at the given index.
     * Complexity: O(1) at the ends, O(n) otherwise.
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size]
     */
    public void add(int index, E element) {
        validateIndexForAdd(index);
        if (index == 0) {
            addFirst(element);
        } else if (index == size) {
            addLast(element);
        } else {
            linkBefore(node(index), element);
        }
    }

    /**
     * Inserts an element immediately after the first occurrence of the target
     * value. Complexity: O(n).
     *
     * @throws NoSuchElementException if the target value is not present
     */
    public void insertAfter(E target, E element) {
        Node<E> current = head;
        while (current != null) {
            if (target == null ? current.element == null : target.equals(current.element)) {
                if (current == tail) {
                    addLast(element);
                } else {
                    linkBefore(current.next, element);
                }
                return;
            }
            current = current.next;
        }
        throw new NoSuchElementException("Target element not found: " + target);
    }

    /**
     * Returns the element at the given index.
     * Complexity: O(n) (O(1) amortized-adjacent-access is not assumed).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    public E get(int index) {
        validateIndex(index);
        return node(index).element;
    }

    /**
     * Replaces the element at the given index, returning the old value.
     * Complexity: O(n).
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    public E set(int index, E element) {
        validateIndex(index);
        Node<E> target = node(index);
        E old = target.element;
        target.element = element;
        return old;
    }

    /**
     * Removes and returns the head element.
     * Complexity: O(1).
     *
     * @throws NoSuchElementException if the list is empty
     */
    public E removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("List is empty");
        }
        Node<E> oldHead = head;
        E value = oldHead.element;
        Node<E> next = oldHead.next;
        oldHead.element = null;
        oldHead.next = null;
        if (next != null) {
            next.prev = null;
        } else {
            tail = null; // list is now empty
        }
        head = next;
        size--;
        return value;
    }

    /**
     * Removes and returns the tail element.
     * Complexity: O(1).
     *
     * @throws NoSuchElementException if the list is empty
     */
    public E removeLast() {
        if (tail == null) {
            throw new NoSuchElementException("List is empty");
        }
        Node<E> oldTail = tail;
        E value = oldTail.element;
        Node<E> prev = oldTail.prev;
        oldTail.element = null;
        oldTail.prev = null;
        if (prev != null) {
            prev.next = null;
        } else {
            head = null; // list is now empty
        }
        tail = prev;
        size--;
        return value;
    }

    /**
     * Removes and returns the element at the given index.
     * Complexity: O(1) at the ends, O(n) otherwise.
     *
     * @throws IndexOutOfBoundsException if index is not in [0, size)
     */
    public E remove(int index) {
        validateIndex(index);
        if (index == 0) {
            return removeFirst();
        }
        if (index == size - 1) {
            return removeLast();
        }
        Node<E> target = node(index);
        Node<E> predecessor = target.prev;
        Node<E> successor = target.next;
        predecessor.next = successor;
        successor.prev = predecessor;
        E value = target.element;
        target.element = null;
        target.prev = null;
        target.next = null;
        size--;
        return value;
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
        int index = 0;
        for (Node<E> current = head; current != null; current = current.next, index++) {
            if (element == null ? current.element == null : element.equals(current.element)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the given element, or -1
     * if absent (null-safe equality). Complexity: O(n).
     */
    public int lastIndexOf(Object element) {
        int index = size - 1;
        for (Node<E> current = tail; current != null; current = current.prev, index--) {
            if (element == null ? current.element == null : element.equals(current.element)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Removes all elements and clears every reachable reference so nodes
     * can be garbage collected. Complexity: O(n).
     */
    public void clear() {
        Node<E> current = head;
        while (current != null) {
            Node<E> next = current.next;
            current.element = null;
            current.prev = null;
            current.next = null;
            current = next;
        }
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Returns a new array containing the elements in order. Mutating the
     * returned array never affects this structure. Complexity: O(n).
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> current = head; current != null; current = current.next) {
            result[i++] = current.element;
        }
        return result;
    }

    /**
     * Returns an iterator over the elements in order.
     * Complexity: O(1) to create, O(n) to exhaust.
     */
    public CustomIterator<E> iterator() {
        return new CustomIterator<E>() {
            private Node<E> cursor = head;

            @Override
            public boolean hasNext() {
                return cursor != null;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Iteration already exhausted");
                }
                E value = cursor.element;
                cursor = cursor.next;
                return value;
            }
        };
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

    private void linkBefore(Node<E> successor, E element) {
        Node<E> predecessor = successor.prev;
        Node<E> newNode = new Node<>(predecessor, element, successor);
        predecessor.next = newNode;
        successor.prev = newNode;
        size++;
    }

    /** Walks from whichever end is closer to reach the given index. */
    private Node<E> node(int index) {
        if (index < (size >> 1)) {
            Node<E> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            return current;
        } else {
            Node<E> current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
            return current;
        }
    }
}
