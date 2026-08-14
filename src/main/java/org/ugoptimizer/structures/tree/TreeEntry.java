package org.ugoptimizer.structures.tree;

import java.util.Objects;

/** Immutable key-value snapshot returned by an {@link OrderedTree}. */
public final class TreeEntry<K, V> {

    private final K key;
    private final V value;

    /**
     * Creates an immutable entry.
     *
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public TreeEntry(K key, V value) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TreeEntry<?, ?> that)) {
            return false;
        }
        return key.equals(that.key) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        return 31 * result + value.hashCode();
    }

    @Override
    public String toString() {
        return "TreeEntry{key=" + key + ", value=" + value + '}';
    }
}
