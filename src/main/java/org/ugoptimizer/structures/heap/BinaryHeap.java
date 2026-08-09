package org.ugoptimizer.structures.heap;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class BinaryHeap<T> {
    private static final int DEFAULT_CAPACITY = 16;

    private final Comparator<? super T> comparator;
    private Object[] heap;
    private int size;

    public BinaryHeap(Comparator<? super T> comparator) {
        this.heap = new Object[DEFAULT_CAPACITY];
        this.size = 0;
        this.comparator = comparator;
    }

    public BinaryHeap() {
        this(null);
    }

    public void add(T value) {
        ensureCapacity(size + 1);
        heap[size] = value;
        size++;
        siftUp(size - 1);
    }

    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }

        return get(0);
    }

    public T poll() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }

        T result = get(0);
        T last = removeLast();

        if (size > 0) {
            heap[0] = last;
            siftDown(0);
        }

        return result;
    }

    public boolean remove(T value) {
        int index = indexOf(value);

        if (index == -1) {
            return false;
        }

        T last = removeLast();

        if (index < size) {
            heap[index] = last;

            int parent = parent(index);
            if (index > 0 && compare(get(index), get(parent)) < 0) {
                siftUp(index);
            } else {
                siftDown(index);
            }
        }

        return true;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }

        size = 0;
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = parent(index);

            if (compare(get(index), get(parent)) >= 0) {
                break;
            }

            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (true) {
            int left = leftChild(index);
            int right = rightChild(index);
            int smallest = index;

            if (left < size && compare(get(left), get(smallest)) < 0) {
                smallest = left;
            }

            if (right < size && compare(get(right), get(smallest)) < 0) {
                smallest = right;
            }

            if (smallest == index) {
                break;
            }

            swap(index, smallest);
            index = smallest;
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(T first, T second) {
        if (comparator != null) {
            return comparator.compare(first, second);
        }

        return ((Comparable<? super T>) first).compareTo(second);
    }

    @SuppressWarnings("unchecked")
    private T get(int index) {
        return (T) heap[index];
    }

    private int indexOf(T value) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(heap[i], value)) {
                return i;
            }
        }

        return -1;
    }

    private T removeLast() {
        T last = get(size - 1);
        heap[size - 1] = null;
        size--;

        return last;
    }

    private void ensureCapacity(int required) {
        if (required <= heap.length) {
            return;
        }

        int capacity = heap.length * 2;
        if (capacity < required) {
            capacity = required;
        }

        Object[] grown = new Object[capacity];
        for (int i = 0; i < size; i++) {
            grown[i] = heap[i];
        }

        heap = grown;
    }

    private int parent(int index) {
        return (index - 1) / 2;
    }

    private int leftChild(int index) {
        return 2 * index + 1;
    }

    private int rightChild(int index) {
        return 2 * index + 2;
    }

    private void swap(int first, int second) {
        Object temp = heap[first];
        heap[first] = heap[second];
        heap[second] = temp;
    }
}
