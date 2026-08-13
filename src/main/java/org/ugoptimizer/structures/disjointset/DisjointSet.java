package org.ugoptimizer.structures.disjointset;

/**
 * Disjoint-set structure for arbitrary {@code int} element IDs.
 *
 * <p>IDs are defensively copied and manually sorted so binary search can map
 * an external ID to a dense internal index. Parent links use path compression,
 * and unions use rank. When two roots have equal rank, the root with the
 * smaller element ID is retained, making representative selection
 * deterministic.</p>
 *
 * <p>Construction takes {@code O(n log n)} time and {@code O(n)} space. ID
 * mapping costs {@code O(log n)} per supplied ID. After mapping, parent-tree
 * operations have amortized {@code O(alpha(n))} time, where {@code alpha} is
 * the inverse Ackermann function. The structure occupies {@code O(n)} space.</p>
 */
public final class DisjointSet {

    private final int[] elementIds;
    private final int[] parent;
    private final int[] rank;
    private int componentCount;

    /**
     * Creates one independent component for every supplied element ID.
     *
     * @param elementIds element IDs; may be empty but not {@code null}
     * @throws NullPointerException if {@code elementIds} is {@code null}
     * @throws IllegalArgumentException if an element ID appears more than once
     */
    public DisjointSet(int[] elementIds) {
        if (elementIds == null) {
            throw new NullPointerException("elementIds cannot be null");
        }

        this.elementIds = new int[elementIds.length];
        System.arraycopy(elementIds, 0, this.elementIds, 0, elementIds.length);
        sort(this.elementIds);
        rejectDuplicates(this.elementIds);

        this.parent = new int[elementIds.length];
        this.rank = new int[elementIds.length];
        for (int index = 0; index < parent.length; index++) {
            parent[index] = index;
        }
        this.componentCount = elementIds.length;
    }

    /** Returns the number of registered elements in {@code O(1)} time. */
    public int size() {
        return elementIds.length;
    }

    /** Returns the current number of components in {@code O(1)} time. */
    public int getComponentCount() {
        return componentCount;
    }

    /** Returns whether an ID is registered in {@code O(log n)} time. */
    public boolean contains(int elementId) {
        return indexOf(elementId) >= 0;
    }

    /**
     * Returns the deterministic representative ID for an element's component.
     *
     * <p>Binary-search mapping costs {@code O(log n)}. The parent operation is
     * amortized {@code O(alpha(n))} because it compresses every traversed path.</p>
     *
     * @throws IllegalArgumentException if {@code elementId} is unknown
     */
    public int find(int elementId) {
        return elementIds[findRootIndex(requireIndex(elementId))];
    }

    /**
     * Returns whether two known elements belong to the same component.
     *
     * <p>Time is {@code O(log n + alpha(n))} amortized for each supplied ID.</p>
     *
     * @throws IllegalArgumentException if either element ID is unknown
     */
    public boolean connected(int firstId, int secondId) {
        return findRootIndex(requireIndex(firstId))
                == findRootIndex(requireIndex(secondId));
    }

    /**
     * Merges the components containing two known elements using union by rank.
     *
     * @return {@code true} when two components were merged, or {@code false}
     *         when the elements were already connected
     * @throws IllegalArgumentException if either element ID is unknown
     */
    public boolean union(int firstId, int secondId) {
        int firstRoot = findRootIndex(requireIndex(firstId));
        int secondRoot = findRootIndex(requireIndex(secondId));
        if (firstRoot == secondRoot) {
            return false;
        }

        if (rank[firstRoot] < rank[secondRoot]) {
            parent[firstRoot] = secondRoot;
        } else if (rank[firstRoot] > rank[secondRoot]) {
            parent[secondRoot] = firstRoot;
        } else if (elementIds[firstRoot] <= elementIds[secondRoot]) {
            parent[secondRoot] = firstRoot;
            rank[firstRoot]++;
        } else {
            parent[firstRoot] = secondRoot;
            rank[secondRoot]++;
        }
        componentCount--;
        return true;
    }

    private int findRootIndex(int elementIndex) {
        int root = elementIndex;
        while (parent[root] != root) {
            root = parent[root];
        }

        int current = elementIndex;
        while (current != root) {
            int next = parent[current];
            parent[current] = root;
            current = next;
        }
        return root;
    }

    private int requireIndex(int elementId) {
        int index = indexOf(elementId);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown element ID: " + elementId);
        }
        return index;
    }

    private int indexOf(int elementId) {
        int low = 0;
        int high = elementIds.length - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            int middleId = elementIds[middle];
            if (middleId == elementId) {
                return middle;
            }
            if (middleId < elementId) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
    }

    private static void rejectDuplicates(int[] sortedIds) {
        for (int index = 1; index < sortedIds.length; index++) {
            if (sortedIds[index - 1] == sortedIds[index]) {
                throw new IllegalArgumentException(
                        "Duplicate element ID: " + sortedIds[index]);
            }
        }
    }

    private static void sort(int[] values) {
        if (values.length < 2) {
            return;
        }
        int[] buffer = new int[values.length];
        int width = 1;
        while (width < values.length) {
            int start = 0;
            while (start < values.length) {
                int middle = start + Math.min(width, values.length - start);
                int end = middle + Math.min(width, values.length - middle);
                merge(values, buffer, start, middle, end);
                start = end;
            }
            System.arraycopy(buffer, 0, values, 0, values.length);
            width = width > values.length / 2 ? values.length : width * 2;
        }
    }

    private static void merge(
            int[] values, int[] buffer, int start, int middle, int end) {
        int left = start;
        int right = middle;
        int output = start;
        while (left < middle && right < end) {
            if (values[left] <= values[right]) {
                buffer[output++] = values[left++];
            } else {
                buffer[output++] = values[right++];
            }
        }
        while (left < middle) {
            buffer[output++] = values[left++];
        }
        while (right < end) {
            buffer[output++] = values[right++];
        }
    }
}
