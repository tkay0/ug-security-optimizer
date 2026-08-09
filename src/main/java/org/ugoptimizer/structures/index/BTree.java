package org.ugoptimizer.structures.index;

/**
 * Custom B-tree (CLRS-style, minimum degree t) used to model a persistent,
 * ordered index. A node holds at most (2t - 1) keys and at most 2t children;
 * every non-root node holds at least (t - 1) keys. Insertion splits full
 * nodes proactively on the way down so a single top-down pass suffices.
 *
 * @param <K> key type, must be naturally ordered
 * @param <V> value type
 */
public class BTree<K extends Comparable<K>, V> {

    private static final int DEFAULT_MIN_DEGREE = 3;

    private final int minDegree;   // t
    private final int maxKeys;     // 2t - 1
    private final int maxChildren; // 2t

    private BTreeNode root;

    private class BTreeNode {
        Object[] keys;
        Object[] values;
        BTreeNode[] children;
        int numKeys;
        boolean leaf;

        BTreeNode(boolean leaf) {
            this.leaf = leaf;
            this.keys = new Object[maxKeys];
            this.values = new Object[maxKeys];
            this.children = newNodeArray();
            this.numKeys = 0;
        }

        @SuppressWarnings("unchecked")
        K keyAt(int i) {
            return (K) keys[i];
        }

        @SuppressWarnings("unchecked")
        V valueAt(int i) {
            return (V) values[i];
        }
    }

    @SuppressWarnings("unchecked")
    private BTreeNode[] newNodeArray() {
        return (BTreeNode[]) new BTree.BTreeNode[maxChildren];
    }

    public BTree() {
        this(DEFAULT_MIN_DEGREE);
    }

    public BTree(int minDegree) {
        if (minDegree < 2) {
            throw new IllegalArgumentException("minDegree must be >= 2");
        }
        this.minDegree = minDegree;
        this.maxKeys = 2 * minDegree - 1;
        this.maxChildren = 2 * minDegree;
        this.root = new BTreeNode(true);
    }

    public V search(K key) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }
        return searchNode(root, key);
    }

    private V searchNode(BTreeNode node, K key) {
        int i = 0;
        while (i < node.numKeys && key.compareTo(node.keyAt(i)) > 0) {
            i++;
        }
        if (i < node.numKeys && key.compareTo(node.keyAt(i)) == 0) {
            return node.valueAt(i);
        }
        if (node.leaf) {
            return null;
        }
        return searchNode(node.children[i], key);
    }

    /**
     * Inserts a key-value pair, or updates the value if the key already
     * exists. Splits full nodes proactively while descending from the root.
     */
    public void insert(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("null keys are not supported");
        }
        BTreeNode r = root;
        if (r.numKeys == maxKeys) {
            BTreeNode newRoot = new BTreeNode(false);
            newRoot.children[0] = r;
            splitChild(newRoot, 0);
            root = newRoot;
            insertNonFull(newRoot, key, value);
        } else {
            insertNonFull(r, key, value);
        }
    }

    /**
     * Splits the full child at parent.children[index] around its median key,
     * promoting that median key up into the parent.
     */
    private void splitChild(BTreeNode parent, int index) {
        BTreeNode fullChild = parent.children[index];
        BTreeNode newChild = new BTreeNode(fullChild.leaf);

        int mid = minDegree - 1;

        for (int j = 0; j < minDegree - 1; j++) {
            newChild.keys[j] = fullChild.keys[mid + 1 + j];
            newChild.values[j] = fullChild.values[mid + 1 + j];
        }
        newChild.numKeys = minDegree - 1;

        if (!fullChild.leaf) {
            for (int j = 0; j < minDegree; j++) {
                newChild.children[j] = fullChild.children[mid + 1 + j];
            }
        }

        Object medianKey = fullChild.keys[mid];
        Object medianValue = fullChild.values[mid];

        for (int j = mid; j < maxKeys; j++) {
            fullChild.keys[j] = null;
            fullChild.values[j] = null;
        }
        if (!fullChild.leaf) {
            for (int j = mid + 1; j < maxChildren; j++) {
                fullChild.children[j] = null;
            }
        }
        fullChild.numKeys = mid;

        for (int j = parent.numKeys; j > index; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[index + 1] = newChild;

        for (int j = parent.numKeys - 1; j >= index; j--) {
            parent.keys[j + 1] = parent.keys[j];
            parent.values[j + 1] = parent.values[j];
        }
        parent.keys[index] = medianKey;
        parent.values[index] = medianValue;
        parent.numKeys++;
    }

    private void insertNonFull(BTreeNode node, K key, V value) {
        int i = node.numKeys - 1;

        if (node.leaf) {
            while (i >= 0 && key.compareTo(node.keyAt(i)) < 0) {
                i--;
            }
            if (i >= 0 && key.compareTo(node.keyAt(i)) == 0) {
                node.values[i] = value;
                return;
            }
            for (int j = node.numKeys - 1; j > i; j--) {
                node.keys[j + 1] = node.keys[j];
                node.values[j + 1] = node.values[j];
            }
            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.numKeys++;
        } else {
            while (i >= 0 && key.compareTo(node.keyAt(i)) < 0) {
                i--;
            }
            if (i >= 0 && key.compareTo(node.keyAt(i)) == 0) {
                node.values[i] = value;
                return;
            }
            i++;
            if (node.children[i].numKeys == maxKeys) {
                splitChild(node, i);
                int cmp = key.compareTo(node.keyAt(i));
                if (cmp == 0) {
                    node.values[i] = value;
                    return;
                } else if (cmp > 0) {
                    i++;
                }
            }
            insertNonFull(node.children[i], key, value);
        }
    }

    public int height() {
        return heightOf(root);
    }

    private int heightOf(BTreeNode node) {
        if (node.leaf) {
            return 1;
        }
        return 1 + heightOf(node.children[0]);
    }

    public int size() {
        return sizeOf(root);
    }

    private int sizeOf(BTreeNode node) {
        int count = node.numKeys;
        if (!node.leaf) {
            for (int i = 0; i <= node.numKeys; i++) {
                count += sizeOf(node.children[i]);
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return root.numKeys == 0;
    }

    public int getMinDegree() {
        return minDegree;
    }

    /** Number of keys currently stored in the root node (useful to observe splits). */
    public int rootKeyCount() {
        return root.numKeys;
    }

    public boolean isRootLeaf() {
        return root.leaf;
    }

    /** Visits every key-value pair in ascending key order. */
    public void inorderTraverse(java.util.function.BiConsumer<K, V> visitor) {
        inorderTraverse(root, visitor);
    }

    private void inorderTraverse(BTreeNode node, java.util.function.BiConsumer<K, V> visitor) {
        for (int i = 0; i < node.numKeys; i++) {
            if (!node.leaf) {
                inorderTraverse(node.children[i], visitor);
            }
            visitor.accept(node.keyAt(i), node.valueAt(i));
        }
        if (!node.leaf) {
            inorderTraverse(node.children[node.numKeys], visitor);
        }
    }
}
