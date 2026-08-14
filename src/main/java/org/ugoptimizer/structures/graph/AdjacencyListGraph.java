package org.ugoptimizer.structures.graph;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.ugoptimizer.model.Edge;

/**
 * Array-backed adjacency-list implementation of an undirected weighted simple
 * graph. Vertex IDs and every vertex's neighbor IDs are maintained in
 * ascending order.
 *
 * <p>The implementation uses only primitive arrays and an array of private
 * {@link NeighborList} objects. Each undirected edge is represented by two
 * symmetric adjacency entries but contributes one to {@link #getEdgeCount()}.
 * This class is not thread-safe.</p>
 *
 * <p>Space complexity is {@code O(V + E)}: vertices are stored once and each
 * undirected edge is stored twice.</p>
 */
public final class AdjacencyListGraph implements WeightedGraph {

    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final int INITIAL_NEIGHBOR_CAPACITY = 4;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private int[] vertexIds;
    private NeighborList[] adjacencyLists;
    private int vertexCount;
    private int edgeCount;

    /**
     * Creates an empty graph with capacity for eight vertices.
     *
     * <p>Time and space complexity: {@code O(1)}.</p>
     */
    public AdjacencyListGraph() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates an empty graph with the requested initial vertex capacity.
     *
     * <p>Time and space complexity: {@code O(initialCapacity)}.</p>
     *
     * @param initialCapacity initial number of vertex slots; zero is allowed
     * @throws IllegalArgumentException if the capacity is negative or exceeds
     *         the supported JVM array bound
     */
    public AdjacencyListGraph(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity cannot be negative");
        }
        if (initialCapacity > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("Initial capacity exceeds supported array size");
        }
        vertexIds = new int[initialCapacity];
        adjacencyLists = new NeighborList[initialCapacity];
    }

    /** {@inheritDoc} Time and auxiliary-space complexity: {@code O(1)}. */
    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    /** {@inheritDoc} Time and auxiliary-space complexity: {@code O(1)}. */
    @Override
    public int getEdgeCount() {
        return edgeCount;
    }

    /** {@inheritDoc} Time and auxiliary-space complexity: {@code O(1)}. */
    @Override
    public boolean isEmpty() {
        return vertexCount == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(V)} worst case because sorted parallel
     * arrays may be shifted or resized. Auxiliary space is {@code O(V)} when
     * growth occurs and {@code O(1)} otherwise.</p>
     */
    @Override
    public boolean addVertex(int vertexId) {
        int searchResult = findVertexIndex(vertexId);
        if (searchResult >= 0) {
            return false;
        }

        int insertionIndex = insertionPoint(searchResult);
        ensureVertexCapacity(vertexCount + 1);
        int shiftedCount = vertexCount - insertionIndex;
        if (shiftedCount > 0) {
            System.arraycopy(
                    vertexIds, insertionIndex, vertexIds, insertionIndex + 1, shiftedCount);
            System.arraycopy(
                    adjacencyLists,
                    insertionIndex,
                    adjacencyLists,
                    insertionIndex + 1,
                    shiftedCount);
        }
        vertexIds[insertionIndex] = vertexId;
        adjacencyLists[insertionIndex] = new NeighborList();
        vertexCount++;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(V + E + d(v) log V)} worst case. Reverse
     * entries must be removed from every adjacent vertex, a binary vertex
     * lookup is performed for each neighbor, and top-level arrays are shifted.
     * Auxiliary-space complexity is {@code O(1)}.</p>
     */
    @Override
    public boolean removeVertex(int vertexId) {
        int vertexIndex = findVertexIndex(vertexId);
        if (vertexIndex < 0) {
            return false;
        }

        NeighborList removedList = adjacencyLists[vertexIndex];
        validateReverseEntries(vertexId, removedList);
        for (int i = 0; i < removedList.size(); i++) {
            int neighborIndex = findVertexIndex(removedList.neighborIdAt(i));
            adjacencyLists[neighborIndex].remove(vertexId);
        }
        edgeCount -= removedList.size();

        int shiftedCount = vertexCount - vertexIndex - 1;
        if (shiftedCount > 0) {
            System.arraycopy(
                    vertexIds, vertexIndex + 1, vertexIds, vertexIndex, shiftedCount);
            System.arraycopy(
                    adjacencyLists,
                    vertexIndex + 1,
                    adjacencyLists,
                    vertexIndex,
                    shiftedCount);
        }
        vertexCount--;
        vertexIds[vertexCount] = 0;
        adjacencyLists[vertexCount] = null;
        return true;
    }

    /**
     * {@inheritDoc} Time complexity: {@code O(log V)}; auxiliary-space
     * complexity: {@code O(1)}.
     */
    @Override
    public boolean containsVertex(int vertexId) {
        return findVertexIndex(vertexId) >= 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time and returned-space complexity: {@code O(V)}.</p>
     */
    @Override
    public int[] getVertexIds() {
        int[] snapshot = new int[vertexCount];
        System.arraycopy(vertexIds, 0, snapshot, 0, vertexCount);
        return snapshot;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adding a new edge costs {@code O(log V + d(a) + d(b))} because both
     * sorted neighbor arrays may shift. Updating or detecting an unchanged
     * edge costs {@code O(log V + log d(a) + log d(b))}. Auxiliary space is
     * {@code O(d(a) + d(b))} when neighbor arrays grow and {@code O(1)}
     * otherwise.</p>
     */
    @Override
    public EdgeUpdate addEdge(int vertexAId, int vertexBId, double weight) {
        validateEdgeArguments(vertexAId, vertexBId, weight);
        double normalizedWeight = normalizeZero(weight);

        int vertexAIndex = findVertexIndex(vertexAId);
        int vertexBIndex = findVertexIndex(vertexBId);
        if (vertexAIndex < 0 || vertexBIndex < 0) {
            return EdgeUpdate.MISSING_VERTEX;
        }

        NeighborList listA = adjacencyLists[vertexAIndex];
        NeighborList listB = adjacencyLists[vertexBIndex];
        int neighborAIndex = listA.findNeighborIndex(vertexBId);
        int neighborBIndex = listB.findNeighborIndex(vertexAId);

        if (neighborAIndex >= 0) {
            requireSymmetricEntry(neighborBIndex, listA, neighborAIndex, listB);
            if (Double.compare(listA.weightAt(neighborAIndex), normalizedWeight) == 0) {
                return EdgeUpdate.UNCHANGED;
            }
            listA.setWeight(neighborAIndex, normalizedWeight);
            listB.setWeight(neighborBIndex, normalizedWeight);
            return EdgeUpdate.UPDATED;
        }
        if (neighborBIndex >= 0) {
            throw new IllegalStateException("Graph contains an asymmetric adjacency entry");
        }
        if (edgeCount == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Cannot add edge because the edge count would overflow");
        }

        listA.ensureCapacityForInsert();
        listB.ensureCapacityForInsert();
        listA.insertAt(insertionPoint(neighborAIndex), vertexBId, normalizedWeight);
        listB.insertAt(insertionPoint(neighborBIndex), vertexAId, normalizedWeight);
        edgeCount++;
        return EdgeUpdate.ADDED;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(log V + d(a) + d(b))} because both sorted
     * neighbor arrays may shift. Auxiliary-space complexity is
     * {@code O(1)}.</p>
     */
    @Override
    public boolean removeEdge(int vertexAId, int vertexBId) {
        int vertexAIndex = findVertexIndex(vertexAId);
        int vertexBIndex = findVertexIndex(vertexBId);
        if (vertexAIndex < 0 || vertexBIndex < 0) {
            return false;
        }

        NeighborList listA = adjacencyLists[vertexAIndex];
        NeighborList listB = adjacencyLists[vertexBIndex];
        int neighborAIndex = listA.findNeighborIndex(vertexBId);
        if (neighborAIndex < 0) {
            return false;
        }
        int neighborBIndex = listB.findNeighborIndex(vertexAId);
        requireSymmetricEntry(neighborBIndex, listA, neighborAIndex, listB);

        listA.removeAt(neighborAIndex);
        listB.removeAt(neighborBIndex);
        edgeCount--;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(log V + log d(a))}; auxiliary-space
     * complexity: {@code O(1)}.</p>
     */
    @Override
    public boolean containsEdge(int vertexAId, int vertexBId) {
        int vertexAIndex = findVertexIndex(vertexAId);
        if (vertexAIndex < 0 || findVertexIndex(vertexBId) < 0) {
            return false;
        }
        return adjacencyLists[vertexAIndex].findNeighborIndex(vertexBId) >= 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(log V + log d(a))}; auxiliary-space
     * complexity: {@code O(1)}.</p>
     */
    @Override
    public OptionalDouble getEdgeWeight(int vertexAId, int vertexBId) {
        int vertexAIndex = findVertexIndex(vertexAId);
        if (vertexAIndex < 0 || findVertexIndex(vertexBId) < 0) {
            return OptionalDouble.empty();
        }
        NeighborList list = adjacencyLists[vertexAIndex];
        int neighborIndex = list.findNeighborIndex(vertexBId);
        return neighborIndex >= 0
                ? OptionalDouble.of(list.weightAt(neighborIndex))
                : OptionalDouble.empty();
    }

    /**
     * {@inheritDoc} Time complexity: {@code O(log V)}; auxiliary-space
     * complexity: {@code O(1)}.
     */
    @Override
    public OptionalInt getDegree(int vertexId) {
        int vertexIndex = findVertexIndex(vertexId);
        return vertexIndex >= 0
                ? OptionalInt.of(adjacencyLists[vertexIndex].size())
                : OptionalInt.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time and returned-space complexity: {@code O(log V + d(v))}.</p>
     */
    @Override
    public int[] getNeighborIds(int vertexId) {
        int vertexIndex = findVertexIndex(vertexId);
        return vertexIndex >= 0
                ? adjacencyLists[vertexIndex].neighborSnapshot()
                : new int[0];
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time and returned-space complexity: {@code O(log V + d(v))}.</p>
     */
    @Override
    public Edge[] getIncidentEdges(int vertexId) {
        int vertexIndex = findVertexIndex(vertexId);
        if (vertexIndex < 0) {
            return new Edge[0];
        }

        NeighborList list = adjacencyLists[vertexIndex];
        Edge[] snapshot = new Edge[list.size()];
        for (int i = 0; i < list.size(); i++) {
            snapshot[i] = new Edge(vertexId, list.neighborIdAt(i), list.weightAt(i));
        }
        return snapshot;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(V + E)}. Returned-space complexity is
     * {@code O(E)}.</p>
     */
    @Override
    public Edge[] getEdges() {
        Edge[] snapshot = new Edge[edgeCount];
        int outputIndex = 0;
        for (int i = 0; i < vertexCount; i++) {
            int vertexId = vertexIds[i];
            NeighborList list = adjacencyLists[i];
            for (int j = 0; j < list.size(); j++) {
                int neighborId = list.neighborIdAt(j);
                if (vertexId < neighborId) {
                    if (outputIndex >= snapshot.length) {
                        throw new IllegalStateException("Edge count is smaller than adjacency data");
                    }
                    snapshot[outputIndex++] =
                            new Edge(vertexId, neighborId, list.weightAt(j));
                }
            }
        }
        if (outputIndex != snapshot.length) {
            throw new IllegalStateException("Edge count is larger than adjacency data");
        }
        return snapshot;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity: {@code O(V)}. Allocated top-level capacity is
     * retained for reuse, while all active neighbor-list references are
     * released. Auxiliary-space complexity is {@code O(1)}.</p>
     */
    @Override
    public void clear() {
        for (int i = 0; i < vertexCount; i++) {
            vertexIds[i] = 0;
            adjacencyLists[i] = null;
        }
        vertexCount = 0;
        edgeCount = 0;
    }

    /** Binary search over the sorted active vertex prefix. */
    private int findVertexIndex(int vertexId) {
        int low = 0;
        int high = vertexCount - 1;
        while (low <= high) {
            int middle = low + ((high - low) >>> 1);
            int middleId = vertexIds[middle];
            if (middleId < vertexId) {
                low = middle + 1;
            } else if (middleId > vertexId) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -(low + 1);
    }

    private void ensureVertexCapacity(int requiredCapacity) {
        if (requiredCapacity <= vertexIds.length) {
            return;
        }
        int newCapacity = expandedCapacity(
                vertexIds.length, requiredCapacity, 1, "Vertex");
        int[] expandedVertexIds = new int[newCapacity];
        NeighborList[] expandedAdjacencyLists = new NeighborList[newCapacity];
        System.arraycopy(vertexIds, 0, expandedVertexIds, 0, vertexCount);
        System.arraycopy(adjacencyLists, 0, expandedAdjacencyLists, 0, vertexCount);
        vertexIds = expandedVertexIds;
        adjacencyLists = expandedAdjacencyLists;
    }

    private void validateReverseEntries(int vertexId, NeighborList list) {
        for (int i = 0; i < list.size(); i++) {
            int neighborId = list.neighborIdAt(i);
            int neighborIndex = findVertexIndex(neighborId);
            if (neighborIndex < 0) {
                throw new IllegalStateException("Graph references a missing adjacent vertex");
            }
            NeighborList reverseList = adjacencyLists[neighborIndex];
            int reverseIndex = reverseList.findNeighborIndex(vertexId);
            requireSymmetricEntry(reverseIndex, list, i, reverseList);
        }
    }

    private static void requireSymmetricEntry(
            int reverseIndex,
            NeighborList firstList,
            int firstIndex,
            NeighborList reverseList) {
        if (reverseIndex < 0
                || Double.compare(
                        firstList.weightAt(firstIndex), reverseList.weightAt(reverseIndex)) != 0) {
            throw new IllegalStateException("Graph contains asymmetric adjacency data");
        }
    }

    private static void validateEdgeArguments(
            int vertexAId, int vertexBId, double weight) {
        if (vertexAId == vertexBId) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (!Double.isFinite(weight) || weight < 0.0d) {
            throw new IllegalArgumentException("Weight must be finite and non-negative");
        }
    }

    private static double normalizeZero(double weight) {
        return weight == 0.0d ? 0.0d : weight;
    }

    private static int insertionPoint(int negativeSearchResult) {
        return -negativeSearchResult - 1;
    }

    private static int expandedCapacity(
            int currentCapacity,
            int requiredCapacity,
            int initialGrowth,
            String storageName) {
        if (requiredCapacity < 0 || requiredCapacity > MAX_ARRAY_SIZE) {
            throw new IllegalStateException(storageName + " capacity overflow");
        }
        long doubledCapacity = currentCapacity == 0
                ? initialGrowth
                : (long) currentCapacity * 2L;
        long candidate = Math.max((long) requiredCapacity, doubledCapacity);
        if (candidate > MAX_ARRAY_SIZE) {
            candidate = MAX_ARRAY_SIZE;
        }
        if (candidate < requiredCapacity) {
            throw new IllegalStateException(storageName + " capacity overflow");
        }
        return (int) candidate;
    }

    /** Parallel sorted arrays for one vertex's neighbor IDs and weights. */
    private static final class NeighborList {

        private int[] neighborIds = new int[0];
        private double[] weights = new double[0];
        private int size;

        int size() {
            return size;
        }

        int neighborIdAt(int index) {
            return neighborIds[index];
        }

        double weightAt(int index) {
            return weights[index];
        }

        void setWeight(int index, double weight) {
            weights[index] = weight;
        }

        int findNeighborIndex(int neighborId) {
            int low = 0;
            int high = size - 1;
            while (low <= high) {
                int middle = low + ((high - low) >>> 1);
                int middleId = neighborIds[middle];
                if (middleId < neighborId) {
                    low = middle + 1;
                } else if (middleId > neighborId) {
                    high = middle - 1;
                } else {
                    return middle;
                }
            }
            return -(low + 1);
        }

        void ensureCapacityForInsert() {
            int requiredCapacity = size + 1;
            if (requiredCapacity < 0) {
                throw new IllegalStateException("Neighbor capacity overflow");
            }
            if (requiredCapacity <= neighborIds.length) {
                return;
            }
            int newCapacity = expandedCapacity(
                    neighborIds.length,
                    requiredCapacity,
                    INITIAL_NEIGHBOR_CAPACITY,
                    "Neighbor");
            int[] expandedNeighborIds = new int[newCapacity];
            double[] expandedWeights = new double[newCapacity];
            System.arraycopy(neighborIds, 0, expandedNeighborIds, 0, size);
            System.arraycopy(weights, 0, expandedWeights, 0, size);
            neighborIds = expandedNeighborIds;
            weights = expandedWeights;
        }

        void insertAt(int insertionIndex, int neighborId, double weight) {
            int shiftedCount = size - insertionIndex;
            if (shiftedCount > 0) {
                System.arraycopy(
                        neighborIds,
                        insertionIndex,
                        neighborIds,
                        insertionIndex + 1,
                        shiftedCount);
                System.arraycopy(
                        weights, insertionIndex, weights, insertionIndex + 1, shiftedCount);
            }
            neighborIds[insertionIndex] = neighborId;
            weights[insertionIndex] = weight;
            size++;
        }

        boolean remove(int neighborId) {
            int index = findNeighborIndex(neighborId);
            if (index < 0) {
                return false;
            }
            removeAt(index);
            return true;
        }

        void removeAt(int index) {
            int shiftedCount = size - index - 1;
            if (shiftedCount > 0) {
                System.arraycopy(neighborIds, index + 1, neighborIds, index, shiftedCount);
                System.arraycopy(weights, index + 1, weights, index, shiftedCount);
            }
            size--;
            neighborIds[size] = 0;
            weights[size] = 0.0d;
        }

        int[] neighborSnapshot() {
            int[] snapshot = new int[size];
            System.arraycopy(neighborIds, 0, snapshot, 0, size);
            return snapshot;
        }
    }
}
