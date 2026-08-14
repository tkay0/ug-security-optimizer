package org.ugoptimizer.structures.hash;

/**
 * Custom hash table using separate chaining for collision handling.
 * Does not use java.util.HashMap or any built-in map implementation.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class HashTable<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR_THRESHOLD = 0.75;

    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node<K, V>[] buckets;
    private int size;
    private int collisionCount;
    private final double loadFactorThreshold;

    @SuppressWarnings("unchecked")
    public HashTable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR_THRESHOLD);
    }

    @SuppressWarnings("unchecked")
    public HashTable(int initialCapacity, double loadFactorThreshold) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive");
        }
        if (loadFactorThreshold <= 0 || loadFactorThreshold > 1) {
            throw new IllegalArgumentException("loadFactorThreshold must be in (0, 1]");
        }
        this.buckets = (Node<K, V>[]) new Node[initialCapacity];
        this.loadFactorThreshold = loadFactorThreshold;
        this.size = 0;
        this.collisionCount = 0;
    }

    private int indexFor(K key, int capacity) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % capacity;
    }

    /**
     * Inserts or updates a key-value pair.
     * @return the previous value associated with the key, or null if the key was new
     */
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }

        int index = indexFor(key, buckets.length);
        Node<K, V> head = buckets[index];

        for (Node<K, V> current = head; current != null; current = current.next) {
            if (current.key.equals(key)) {
                V previous = current.value;
                current.value = value;
                return previous;
            }
        }

        if (head != null) {
            collisionCount++;
        }

        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = head;
        buckets[index] = newNode;
        size++;

        if (currentLoadFactor() > loadFactorThreshold) {
            resize();
        }

        return null;
    }

    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }
        int index = indexFor(key, buckets.length);
        for (Node<K, V> current = buckets[index]; current != null; current = current.next) {
            if (current.key.equals(key)) {
                return current.value;
            }
        }
        return null;
    }

    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }
        int index = indexFor(key, buckets.length);
        for (Node<K, V> current = buckets[index]; current != null; current = current.next) {
            if (current.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the mapping for a key.
     * @return the value that was removed, or null if the key was not present
     */
    public V remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }
        int index = indexFor(key, buckets.length);
        Node<K, V> current = buckets[index];
        Node<K, V> previous = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getCapacity() {
        return buckets.length;
    }

    public double currentLoadFactor() {
        return (double) size / buckets.length;
    }

    /**
     * Cumulative count of insertions that landed in a non-empty bucket
     * since the table was created (reset on resize is intentionally not done,
     * so this reflects lifetime collision pressure).
     */
    public int getCollisionCount() {
        return collisionCount;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldBuckets = buckets;
        Node<K, V>[] newBuckets = (Node<K, V>[]) new Node[oldBuckets.length * 2];

        for (Node<K, V> head : oldBuckets) {
            Node<K, V> current = head;
            while (current != null) {
                Node<K, V> next = current.next;
                int newIndex = indexFor(current.key, newBuckets.length);
                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;
                current = next;
            }
        }

        buckets = newBuckets;
    }

    public Iterable<K> keys() {
        return () -> new EntryIterator<>(buckets, node -> node.key);
    }

    public Iterable<V> values() {
        return () -> new EntryIterator<>(buckets, node -> node.value);
    }

    /**
     * Walks every bucket chain in order without allocating any java.util
     * collection, so callers that only need to iterate never depend on one.
     */
    private static class EntryIterator<K, V, T> implements java.util.Iterator<T> {
        private final Node<K, V>[] buckets;
        private final java.util.function.Function<Node<K, V>, T> extractor;
        private int bucketIndex;
        private Node<K, V> current;

        EntryIterator(Node<K, V>[] buckets, java.util.function.Function<Node<K, V>, T> extractor) {
            this.buckets = buckets;
            this.extractor = extractor;
            this.bucketIndex = -1;
            this.current = null;
            advanceToNext();
        }

        private void advanceToNext() {
            if (current != null && current.next != null) {
                current = current.next;
                return;
            }
            bucketIndex++;
            while (bucketIndex < buckets.length) {
                if (buckets[bucketIndex] != null) {
                    current = buckets[bucketIndex];
                    return;
                }
                bucketIndex++;
            }
            current = null;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            if (current == null) {
                throw new java.util.NoSuchElementException();
            }
            T value = extractor.apply(current);
            advanceToNext();
            return value;
        }
    }
}
