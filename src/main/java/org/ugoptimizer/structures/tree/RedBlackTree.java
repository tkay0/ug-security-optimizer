package org.ugoptimizer.structures.tree;

import java.util.Objects;
import java.util.Optional;

/**
 * Red-black tree implementing the shared ordered-tree contract.
 *
 * <p>Lookup, insertion, and removal take {@code O(log n)} time. In-order
 * snapshots take {@code O(n)} time and space. The implementation uses a private
 * black sentinel for null leaves and never exposes mutable nodes or colors. Read-only rotation and
 * insertion-recolour counters support examiner-visible balancing evidence.</p>
 */
public final class RedBlackTree<K extends Comparable<? super K>, V>
        implements OrderedTree<K, V> {

    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private final Node<K, V> nil;
    private Node<K, V> root;
    private int size;
    private int leftRotationCount;
    private int rightRotationCount;
    private int insertionRecolorCount;

    public RedBlackTree() {
        nil = new Node<>(null, null, BLACK);
        nil.left = nil;
        nil.right = nil;
        nil.parent = nil;
        root = nil;
    }

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

        Node<K, V> parent = nil;
        Node<K, V> current = root;
        int comparison = 0;
        while (current != nil) {
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

        Node<K, V> inserted = new Node<>(key, value, RED);
        inserted.left = nil;
        inserted.right = nil;
        inserted.parent = parent;
        if (parent == nil) {
            root = inserted;
        } else if (comparison < 0) {
            parent.left = inserted;
        } else {
            parent.right = inserted;
        }

        size++;
        fixAfterInsertion(inserted);
        return PutResult.INSERTED;
    }

    @Override
    public Optional<V> get(K key) {
        Node<K, V> node = findNode(requireKey(key));
        return node == nil ? Optional.empty() : Optional.of(node.value);
    }

    @Override
    public boolean containsKey(K key) {
        return findNode(requireKey(key)) != nil;
    }

    @Override
    public Optional<V> remove(K key) {
        Node<K, V> removed = findNode(requireKey(key));
        if (removed == nil) {
            return Optional.empty();
        }

        V removedValue = removed.value;
        Node<K, V> moved = removed;
        boolean originalColor = moved.color;
        Node<K, V> replacement;

        if (removed.left == nil) {
            replacement = removed.right;
            transplant(removed, removed.right);
        } else if (removed.right == nil) {
            replacement = removed.left;
            transplant(removed, removed.left);
        } else {
            moved = minimum(removed.right);
            originalColor = moved.color;
            replacement = moved.right;
            if (moved.parent == removed) {
                replacement.parent = moved;
            } else {
                transplant(moved, moved.right);
                moved.right = removed.right;
                moved.right.parent = moved;
            }
            transplant(removed, moved);
            moved.left = removed.left;
            moved.left.parent = moved;
            moved.color = removed.color;
        }

        size--;
        if (originalColor == BLACK) {
            fixAfterDeletion(replacement);
        }
        if (root == nil) {
            nil.parent = nil;
        }
        return Optional.of(removedValue);
    }

    @Override
    public Optional<TreeEntry<K, V>> firstEntry() {
        return root == nil ? Optional.empty() : Optional.of(snapshot(minimum(root)));
    }

    @Override
    public Optional<TreeEntry<K, V>> lastEntry() {
        return root == nil ? Optional.empty() : Optional.of(snapshot(maximum(root)));
    }

    @Override
    public TreeEntry<K, V>[] entriesInOrder() {
        TreeEntry<K, V>[] entries = newEntryArray(size);
        int index = 0;
        Node<K, V> current = root == nil ? nil : minimum(root);
        while (current != nil) {
            entries[index++] = snapshot(current);
            current = successor(current);
        }
        return entries;
    }

    @Override
    public void clear() {
        root = nil;
        size = 0;
        nil.parent = nil;
        nil.color = BLACK;
        leftRotationCount = 0;
        rightRotationCount = 0;
        insertionRecolorCount = 0;
    }

    /** Package-private invariant diagnostic for implementation-specific tests. */
    boolean hasBlackRoot() {
        return root == nil || root.color == BLACK;
    }

    /** Package-private invariant diagnostic for implementation-specific tests. */
    boolean hasNoRedRedViolation() {
        return hasNoRedRedViolation(root);
    }

    /** Package-private invariant diagnostic for implementation-specific tests. */
    boolean hasConsistentBlackHeight() {
        return blackHeight(root) >= 0;
    }

    /** Package-private invariant diagnostic for implementation-specific tests. */
    boolean hasValidOrdering() {
        return hasValidOrdering(root, null, null);
    }

    /** Returns the current tree height for balancing and performance diagnostics. */
    public int height() {
        return height(root);
    }

    /** Returns left rotations performed since construction or the last clear. */
    public int getLeftRotationCount() {
        return leftRotationCount;
    }

    /** Returns right rotations performed since construction or the last clear. */
    public int getRightRotationCount() {
        return rightRotationCount;
    }

    /** Returns actual color changes made by insertion repair since construction or clear. */
    public int getInsertionRecolorCount() {
        return insertionRecolorCount;
    }

    private void rotateLeft(Node<K, V> pivot) {
        leftRotationCount++;
        Node<K, V> promoted = pivot.right;
        pivot.right = promoted.left;
        if (promoted.left != nil) {
            promoted.left.parent = pivot;
        }
        promoted.parent = pivot.parent;
        if (pivot.parent == nil) {
            root = promoted;
        } else if (pivot == pivot.parent.left) {
            pivot.parent.left = promoted;
        } else {
            pivot.parent.right = promoted;
        }
        promoted.left = pivot;
        pivot.parent = promoted;
    }

    private void rotateRight(Node<K, V> pivot) {
        rightRotationCount++;
        Node<K, V> promoted = pivot.left;
        pivot.left = promoted.right;
        if (promoted.right != nil) {
            promoted.right.parent = pivot;
        }
        promoted.parent = pivot.parent;
        if (pivot.parent == nil) {
            root = promoted;
        } else if (pivot == pivot.parent.right) {
            pivot.parent.right = promoted;
        } else {
            pivot.parent.left = promoted;
        }
        promoted.right = pivot;
        pivot.parent = promoted;
    }

    private void fixAfterInsertion(Node<K, V> node) {
        Node<K, V> current = node;
        while (current.parent.color == RED) {
            if (current.parent == current.parent.parent.left) {
                Node<K, V> uncle = current.parent.parent.right;
                if (uncle.color == RED) {
                    setInsertionColor(current.parent, BLACK);
                    setInsertionColor(uncle, BLACK);
                    setInsertionColor(current.parent.parent, RED);
                    current = current.parent.parent;
                } else {
                    if (current == current.parent.right) {
                        current = current.parent;
                        rotateLeft(current);
                    }
                    setInsertionColor(current.parent, BLACK);
                    setInsertionColor(current.parent.parent, RED);
                    rotateRight(current.parent.parent);
                }
            } else {
                Node<K, V> uncle = current.parent.parent.left;
                if (uncle.color == RED) {
                    setInsertionColor(current.parent, BLACK);
                    setInsertionColor(uncle, BLACK);
                    setInsertionColor(current.parent.parent, RED);
                    current = current.parent.parent;
                } else {
                    if (current == current.parent.left) {
                        current = current.parent;
                        rotateRight(current);
                    }
                    setInsertionColor(current.parent, BLACK);
                    setInsertionColor(current.parent.parent, RED);
                    rotateLeft(current.parent.parent);
                }
            }
        }
        setInsertionColor(root, BLACK);
        root.parent = nil;
    }

    private void setInsertionColor(Node<K, V> node, boolean color) {
        if (node.color != color) {
            node.color = color;
            insertionRecolorCount++;
        }
    }

    private void fixAfterDeletion(Node<K, V> node) {
        Node<K, V> current = node;
        while (current != root && current.color == BLACK) {
            if (current == current.parent.left) {
                Node<K, V> sibling = current.parent.right;
                if (sibling.color == RED) {
                    sibling.color = BLACK;
                    current.parent.color = RED;
                    rotateLeft(current.parent);
                    sibling = current.parent.right;
                }
                if (sibling.left.color == BLACK && sibling.right.color == BLACK) {
                    sibling.color = RED;
                    current = current.parent;
                } else {
                    if (sibling.right.color == BLACK) {
                        sibling.left.color = BLACK;
                        sibling.color = RED;
                        rotateRight(sibling);
                        sibling = current.parent.right;
                    }
                    sibling.color = current.parent.color;
                    current.parent.color = BLACK;
                    sibling.right.color = BLACK;
                    rotateLeft(current.parent);
                    current = root;
                }
            } else {
                Node<K, V> sibling = current.parent.left;
                if (sibling.color == RED) {
                    sibling.color = BLACK;
                    current.parent.color = RED;
                    rotateRight(current.parent);
                    sibling = current.parent.left;
                }
                if (sibling.right.color == BLACK && sibling.left.color == BLACK) {
                    sibling.color = RED;
                    current = current.parent;
                } else {
                    if (sibling.left.color == BLACK) {
                        sibling.right.color = BLACK;
                        sibling.color = RED;
                        rotateLeft(sibling);
                        sibling = current.parent.left;
                    }
                    sibling.color = current.parent.color;
                    current.parent.color = BLACK;
                    sibling.left.color = BLACK;
                    rotateRight(current.parent);
                    current = root;
                }
            }
        }
        current.color = BLACK;
        nil.color = BLACK;
    }

    private Node<K, V> findNode(K key) {
        Node<K, V> current = root;
        while (current != nil) {
            int comparison = key.compareTo(current.key);
            if (comparison < 0) {
                current = current.left;
            } else if (comparison > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return nil;
    }

    private void transplant(Node<K, V> replaced, Node<K, V> replacement) {
        if (replaced.parent == nil) {
            root = replacement;
        } else if (replaced == replaced.parent.left) {
            replaced.parent.left = replacement;
        } else {
            replaced.parent.right = replacement;
        }
        replacement.parent = replaced.parent;
    }

    private Node<K, V> minimum(Node<K, V> node) {
        Node<K, V> current = node;
        while (current.left != nil) {
            current = current.left;
        }
        return current;
    }

    private Node<K, V> maximum(Node<K, V> node) {
        Node<K, V> current = node;
        while (current.right != nil) {
            current = current.right;
        }
        return current;
    }

    private Node<K, V> successor(Node<K, V> node) {
        if (node.right != nil) {
            return minimum(node.right);
        }
        Node<K, V> current = node;
        Node<K, V> parent = current.parent;
        while (parent != nil && current == parent.right) {
            current = parent;
            parent = parent.parent;
        }
        return parent;
    }

    private boolean hasNoRedRedViolation(Node<K, V> node) {
        if (node == nil) {
            return true;
        }
        if (node.color == RED && (node.left.color == RED || node.right.color == RED)) {
            return false;
        }
        return hasNoRedRedViolation(node.left) && hasNoRedRedViolation(node.right);
    }

    private int blackHeight(Node<K, V> node) {
        if (node == nil) {
            return 1;
        }
        int leftHeight = blackHeight(node.left);
        int rightHeight = blackHeight(node.right);
        if (leftHeight < 0 || rightHeight < 0 || leftHeight != rightHeight) {
            return -1;
        }
        return leftHeight + (node.color == BLACK ? 1 : 0);
    }

    private boolean hasValidOrdering(Node<K, V> node, K lowerExclusive, K upperExclusive) {
        if (node == nil) {
            return true;
        }
        if ((lowerExclusive != null && node.key.compareTo(lowerExclusive) <= 0)
                || (upperExclusive != null && node.key.compareTo(upperExclusive) >= 0)) {
            return false;
        }
        return hasValidOrdering(node.left, lowerExclusive, node.key)
                && hasValidOrdering(node.right, node.key, upperExclusive);
    }

    private int height(Node<K, V> node) {
        if (node == nil) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    private TreeEntry<K, V> snapshot(Node<K, V> node) {
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
        private boolean color;
        private Node<K, V> left;
        private Node<K, V> right;
        private Node<K, V> parent;

        private Node(K key, V value, boolean color) {
            this.key = key;
            this.value = value;
            this.color = color;
        }
    }
}
