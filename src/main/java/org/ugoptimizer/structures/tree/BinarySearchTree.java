package org.ugoptimizer.structures.tree;

import java.util.Objects;
import java.util.Optional;

/**
 * Unbalanced binary search tree implementing the shared ordered-tree contract.
 *
 * <p>Lookup, insertion, and removal take {@code O(h)} time, where {@code h} is
 * the tree height. The average height is {@code O(log n)}, while already ordered
 * insertions can produce a worst-case height of {@code O(n)}. Storage is
 * {@code O(n)}.</p>
 */
public final class BinarySearchTree<K extends Comparable<? super K>, V>
        implements OrderedTree<K, V> {

    private Node<K, V> root;
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public PutResult put(K key, V value) {
        requireKey(key);
        Objects.requireNonNull(value, "value cannot be null");

        if (root == null) {
            root = new Node<>(key, value, null);
            size = 1;
            return PutResult.INSERTED;
        }

        Node<K, V> parent = null;
        Node<K, V> current = root;
        int comparison = 0;
        while (current != null) {
            parent = current;
            comparison = key.compareTo(current.key);
            if (comparison < 0) {
                current = current.left;
            } else if (comparison > 0) {
                current = current.right;
            } else {
                current.value = value;
                return PutResult.UPDATED;
            }
        }

        Node<K, V> inserted = new Node<>(key, value, parent);
        if (comparison < 0) {
            parent.left = inserted;
        } else {
            parent.right = inserted;
        }
        size++;
        return PutResult.INSERTED;
    }

    @Override
    public Optional<V> get(K key) {
        Node<K, V> node = findNode(requireKey(key));
        return node == null ? Optional.empty() : Optional.of(node.value);
    }

    @Override
    public boolean containsKey(K key) {
        return findNode(requireKey(key)) != null;
    }

    @Override
    public Optional<V> remove(K key) {
        Node<K, V> node = findNode(requireKey(key));
        if (node == null) {
            return Optional.empty();
        }

        V removedValue = node.value;
        if (node.left == null) {
            transplant(node, node.right);
        } else if (node.right == null) {
            transplant(node, node.left);
        } else {
            Node<K, V> successor = minimum(node.right);
            if (successor.parent != node) {
                transplant(successor, successor.right);
                successor.right = node.right;
                successor.right.parent = successor;
            }
            transplant(node, successor);
            successor.left = node.left;
            successor.left.parent = successor;
        }
        size--;
        return Optional.of(removedValue);
    }

    @Override
    public Optional<TreeEntry<K, V>> firstEntry() {
        return root == null ? Optional.empty() : Optional.of(snapshot(minimum(root)));
    }

    @Override
    public Optional<TreeEntry<K, V>> lastEntry() {
        return root == null ? Optional.empty() : Optional.of(snapshot(maximum(root)));
    }

    @Override
    public TreeEntry<K, V>[] entriesInOrder() {
        TreeEntry<K, V>[] entries = newEntryArray(size);
        int index = 0;
        Node<K, V> current = root == null ? null : minimum(root);
        while (current != null) {
            entries[index++] = snapshot(current);
            current = successor(current);
        }
        return entries;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    /** Returns the current tree height for balancing and performance diagnostics. */
    public int height() {
        int maximum = 0;
        Node<K, V> current = root == null ? null : minimum(root);
        while (current != null) {
            int depth = 1;
            Node<K, V> ancestor = current;
            while (ancestor.parent != null) {
                depth++;
                ancestor = ancestor.parent;
            }
            maximum = Math.max(maximum, depth);
            current = successor(current);
        }
        return maximum;
    }

    private Node<K, V> findNode(K key) {
        Node<K, V> current = root;
        while (current != null) {
            int comparison = key.compareTo(current.key);
            if (comparison < 0) {
                current = current.left;
            } else if (comparison > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return null;
    }

    private void transplant(Node<K, V> replaced, Node<K, V> replacement) {
        if (replaced.parent == null) {
            root = replacement;
        } else if (replaced == replaced.parent.left) {
            replaced.parent.left = replacement;
        } else {
            replaced.parent.right = replacement;
        }
        if (replacement != null) {
            replacement.parent = replaced.parent;
        }
    }

    private static <K, V> Node<K, V> minimum(Node<K, V> node) {
        Node<K, V> current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    private static <K, V> Node<K, V> maximum(Node<K, V> node) {
        Node<K, V> current = node;
        while (current.right != null) {
            current = current.right;
        }
        return current;
    }

    private static <K, V> Node<K, V> successor(Node<K, V> node) {
        if (node.right != null) {
            return minimum(node.right);
        }
        Node<K, V> current = node;
        Node<K, V> parent = current.parent;
        while (parent != null && current == parent.right) {
            current = parent;
            parent = parent.parent;
        }
        return parent;
    }

    private static <K, V> TreeEntry<K, V> snapshot(Node<K, V> node) {
        return new TreeEntry<>(node.key, node.value);
    }

    private static <K> K requireKey(K key) {
        return Objects.requireNonNull(key, "key cannot be null");
    }

    @SuppressWarnings("unchecked")
    private TreeEntry<K, V>[] newEntryArray(int length) {
        return (TreeEntry<K, V>[]) new TreeEntry<?, ?>[length];
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> left;
        private Node<K, V> right;
        private Node<K, V> parent;

        private Node(K key, V value, Node<K, V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }
    }
}
