package org.ugoptimizer.structures.queue;

import java.util.NoSuchElementException;

/**
 * A double-ended queue backed by a custom doubly-linked chain of nodes.
 * Supports insertion and removal at both ends.
 * Does not use java.util.Deque, ArrayDeque, LinkedList, or any other built-in type.
 *
 * @param <T> the type of elements held in this deque
 */
public class Deque<T> {

    private static final class Node<T> {
        private final T value;
        private Node<T> prev;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Adds an item to the front of the deque.
     */
    public void addFirst(T item) {
        Node<T> node = new Node<>(item);
        if (isEmpty()) {
            head = tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
        size++;
    }

    /**
     * Adds an item to the back of the deque.
     */
    public void addLast(T item) {
        Node<T> node = new Node<>(item);
        if (isEmpty()) {
            head = tail = node;
        } else {
            node.prev = tail;
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /**
     * Removes and returns the item at the front of the deque.
     *
     * @throws NoSuchElementException if the deque is empty
     */
    public T removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot removeFirst from an empty deque");
        }
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return value;
    }

    /**
     * Removes and returns the item at the back of the deque.
     *
     * @throws NoSuchElementException if the deque is empty
     */
    public T removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot removeLast from an empty deque");
        }
        T value = tail.value;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        size--;
        return value;
    }

    /**
     * Returns, without removing, the item at the front of the deque.
     *
     * @throws NoSuchElementException if the deque is empty
     */
    public T peekFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peekFirst an empty deque");
        }
        return head.value;
    }

    /**
     * Returns, without removing, the item at the back of the deque.
     *
     * @throws NoSuchElementException if the deque is empty
     */
    public T peekLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peekLast an empty deque");
        }
        return tail.value;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}
