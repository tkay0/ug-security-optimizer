package org.ugoptimizer.structures.hash;

/**
 * Set abstraction layered on top of {@link CustomMap} (which is itself
 * layered on {@link HashTable}). Membership is tracked by mapping each
 * element to a shared sentinel value; only the key side of the map matters.
 *
 * @param <E> element type
 */
public class CustomSet<E> {

    private static final Object PRESENT = new Object();

    private final CustomMap<E, Object> map;

    public CustomSet() {
        this.map = new CustomMap<>();
    }

    /**
     * @return true if the element was newly added, false if it was already present
     */
    public boolean add(E element) {
        boolean isNew = !map.containsKey(element);
        map.put(element, PRESENT);
        return isNew;
    }

    public boolean contains(E element) {
        return map.containsKey(element);
    }

    /**
     * @return true if the element was present and removed, false if it was absent
     */
    public boolean remove(E element) {
        boolean existed = map.containsKey(element);
        map.remove(element);
        return existed;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Iterable<E> elements() {
        return map.keySet();
    }
}
