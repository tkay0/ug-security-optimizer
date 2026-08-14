package org.ugoptimizer.structures.hash;

/**
 * Map abstraction layered on top of {@link HashTable}. This class contains
 * no independent storage or collision logic of its own; it exists to expose
 * a map-shaped API (put/get/remove/keySet/values) while delegating every
 * operation to the underlying hash table.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class CustomMap<K, V> {

    private final HashTable<K, V> table;

    public CustomMap() {
        this.table = new HashTable<>();
    }

    public V put(K key, V value) {
        return table.put(key, value);
    }

    public V get(K key) {
        return table.get(key);
    }

    public V remove(K key) {
        return table.remove(key);
    }

    public boolean containsKey(K key) {
        return table.containsKey(key);
    }

    public int size() {
        return table.size();
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public Iterable<K> keySet() {
        return table.keys();
    }

    public Iterable<V> values() {
        return table.values();
    }
}
