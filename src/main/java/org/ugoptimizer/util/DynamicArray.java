package org.ugoptimizer.util;

/**
 * Custom dynamic array — replaces java.util.ArrayList in algorithmic core.
 * Grows by 1.5x factor. Amortized O(1) append.
 *
 * @param <T> element type
 */
public class DynamicArray<T> {
    private Object[] data;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;

    public DynamicArray() {
        this.data = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Capacity must be >= 0");
        this.data = new Object[initialCapacity];
        this.size = 0;
    }

    public void add(T element) {
        ensureCapacity(size + 1);
        data[size++] = element;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return (T) data[index];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(T[] prototype) {
        if (prototype.length < size) {
            prototype = (T[]) java.lang.reflect.Array.newInstance(
                prototype.getClass().getComponentType(), size);
        }
        System.arraycopy(data, 0, prototype, 0, size);
        if (prototype.length > size) prototype[size] = null;
        return prototype;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            int newCapacity = Math.max(minCapacity, (data.length * 3) / 2 + 1);
            Object[] newData = new Object[newCapacity];
            System.arraycopy(data, 0, newData, 0, size);
            this.data = newData;
        }
    }
}
