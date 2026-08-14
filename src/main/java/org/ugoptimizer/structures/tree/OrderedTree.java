package org.ugoptimizer.structures.tree;

import java.util.Optional;

/**
 * Shared contract for ordered key-value trees that use each key's natural ordering.
 *
 * <p>Keys and values must be non-null. Inserting an existing key replaces its value
 * without changing the tree size. Returned entries and arrays are independent
 * snapshots and never expose an implementation's internal nodes.</p>
 *
 * @param <K> naturally ordered key type
 * @param <V> value type
 */
public interface OrderedTree<K extends Comparable<? super K>, V> {

    /** Outcome of inserting or replacing a key-value association. */
    enum PutResult {
        INSERTED,
        UPDATED
    }

    int size();

    boolean isEmpty();

    /**
     * Inserts a key-value association or replaces the value for an existing key.
     *
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    PutResult put(K key, V value);

    /** @throws NullPointerException if {@code key} is null */
    Optional<V> get(K key);

    /** @throws NullPointerException if {@code key} is null */
    boolean containsKey(K key);

    /**
     * Removes the association for {@code key}.
     *
     * @return the removed value, or an empty optional when the key was absent
     * @throws NullPointerException if {@code key} is null
     */
    Optional<V> remove(K key);

    Optional<TreeEntry<K, V>> firstEntry();

    Optional<TreeEntry<K, V>> lastEntry();

    /** Returns an independent snapshot ordered by ascending key. */
    TreeEntry<K, V>[] entriesInOrder();

    void clear();
}
