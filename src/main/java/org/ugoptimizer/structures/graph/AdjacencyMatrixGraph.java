package org.ugoptimizer.structures.graph;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.ugoptimizer.model.Edge;

/**
 * An undirected weighted simple graph backed by an adjacency matrix.
 *
 * <p>Vertex identifiers are kept in ascending order. Matrix rows and columns
 * use the corresponding positions in that sorted vertex array. A separate
 * presence matrix distinguishes a missing edge from a zero-weight edge.</p>
 *
 * <p>The graph uses {@code O(C^2)} storage, where {@code C} is its current
 * vertex capacity. Returned arrays are independent snapshots.</p>
 *
 * <p>Capacity is conservatively limited to 46,340 vertices because
 * {@code 46,340^2} fits within a signed {@code int}-sized cell-count bound.
 * Java two-dimensional arrays are separate row arrays, and this implementation
 * does not allocate one flat {@code capacity^2} array, so this policy is not an
 * intrinsic JVM two-dimensional-array limit. It keeps squared matrix growth
 * clearly bounded; practical heap limits will normally be reached much
 * earlier.</p>
 */
public final class AdjacencyMatrixGraph implements WeightedGraph {

    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final int MAX_MATRIX_CAPACITY = 46_340;

    private int[] vertexIds;
    private double[][] weights;
    private boolean[][] edgePresent;
    private int vertexCount;
    private int edgeCount;

    /**
     * Creates an empty graph with the default initial capacity.
     *
     * <p>Time and space complexity are {@code O(1)} because the default
     * capacity is constant.</p>
     */
    public AdjacencyMatrixGraph() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates an empty graph with the requested initial vertex capacity.
     *
     * <p>Time and space complexity are {@code O(initialCapacity^2)}.</p>
     *
     * @param initialCapacity initial number of vertex positions to allocate
     * @throws IllegalArgumentException if the capacity is negative or exceeds
     *         the graph's conservative matrix-capacity policy
     */
    public AdjacencyMatrixGraph(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity cannot be negative");
        }
        if (initialCapacity > MAX_MATRIX_CAPACITY) {
            throw new IllegalArgumentException(
                    "Initial capacity exceeds the maximum adjacency-matrix capacity");
        }
        vertexIds = new int[initialCapacity];
        weights = new double[initialCapacity][initialCapacity];
        edgePresent = new boolean[initialCapacity][initialCapacity];
    }

    /** {@inheritDoc} Time complexity: {@code O(1)}. */
    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    /** {@inheritDoc} Time complexity: {@code O(1)}. */
    @Override
    public int getEdgeCount() {
        return edgeCount;
    }

    /** {@inheritDoc} Time complexity: {@code O(1)}. */
    @Override
    public boolean isEmpty() {
        return vertexCount == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Without resizing, time complexity is {@code O(V^2 + C)}, where
     * {@code V} is the current vertex count and {@code C} is the retained
     * matrix capacity. Existing matrix rows and columns may shift, and a full
     * capacity-sized matrix row is cleared. Retained capacity can exceed the
     * current vertex count after removals or {@link #clear()}. Resizing takes
     * {@code O(C^2)} time and temporary space; otherwise auxiliary space is
     * {@code O(1)}.</p>
     */
    @Override
    public boolean addVertex(int vertexId) {
        int searchResult = findVertex(vertexId);
        if (searchResult >= 0) {
            return false;
        }

        int insertionIndex = -searchResult - 1;
        ensureCapacity(vertexCount + 1);
        insertMatrixPosition(insertionIndex);

        int shiftedVertices = vertexCount - insertionIndex;
        if (shiftedVertices > 0) {
            System.arraycopy(vertexIds, insertionIndex, vertexIds,
                    insertionIndex + 1, shiftedVertices);
        }
        vertexIds[insertionIndex] = vertexId;
        vertexCount++;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(V^2 + C)}, where {@code V} is the current
     * vertex count and {@code C} is the retained matrix capacity. Every active
     * row may have a column shifted, row references may shift, and a full
     * capacity-sized matrix row is cleared. Retained capacity can exceed the
     * current vertex count after removals or {@link #clear()}. This operation
     * does not resize; auxiliary space is {@code O(1)}. Matrix resizing, when
     * performed by vertex insertion, takes {@code O(C^2)}.</p>
     */
    @Override
    public boolean removeVertex(int vertexId) {
        int vertexIndex = findVertex(vertexId);
        if (vertexIndex < 0) {
            return false;
        }

        int removedDegree = degreeAt(vertexIndex);
        removeMatrixPosition(vertexIndex);

        int shiftedVertices = vertexCount - vertexIndex - 1;
        if (shiftedVertices > 0) {
            System.arraycopy(vertexIds, vertexIndex + 1, vertexIds,
                    vertexIndex, shiftedVertices);
        }
        vertexIds[vertexCount - 1] = 0;
        vertexCount--;
        edgeCount -= removedDegree;
        return true;
    }

    /** {@inheritDoc} Time complexity: {@code O(log V)}. */
    @Override
    public boolean containsVertex(int vertexId) {
        return findVertex(vertexId) >= 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time and returned space complexity are {@code O(V)}.</p>
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
     * <p>Both vertex lookups cost {@code O(log V)}; matrix access and
     * mutation are {@code O(1)}. Auxiliary space is {@code O(1)}.</p>
     */
    @Override
    public EdgeUpdate addEdge(int vertexA, int vertexB, double weight) {
        validateEdgeArguments(vertexA, vertexB, weight);
        double normalizedWeight = weight == 0.0d ? 0.0d : weight;

        int indexA = findVertex(vertexA);
        int indexB = findVertex(vertexB);
        if (indexA < 0 || indexB < 0) {
            return EdgeUpdate.MISSING_VERTEX;
        }

        boolean forwardPresent = edgePresent[indexA][indexB];
        boolean reversePresent = edgePresent[indexB][indexA];
        if (forwardPresent != reversePresent) {
            throw new IllegalStateException("Undirected edge presence is not symmetric");
        }

        if (forwardPresent) {
            if (Double.compare(weights[indexA][indexB], weights[indexB][indexA]) != 0) {
                throw new IllegalStateException("Undirected edge weights are not symmetric");
            }
            if (Double.compare(weights[indexA][indexB], normalizedWeight) == 0) {
                return EdgeUpdate.UNCHANGED;
            }
            weights[indexA][indexB] = normalizedWeight;
            weights[indexB][indexA] = normalizedWeight;
            return EdgeUpdate.UPDATED;
        }

        if (edgeCount == Integer.MAX_VALUE) {
            throw new IllegalStateException("Edge count cannot exceed Integer.MAX_VALUE");
        }

        edgePresent[indexA][indexB] = true;
        edgePresent[indexB][indexA] = true;
        weights[indexA][indexB] = normalizedWeight;
        weights[indexB][indexA] = normalizedWeight;
        edgeCount++;
        return EdgeUpdate.ADDED;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(log V)} and auxiliary space is
     * {@code O(1)}.</p>
     */
    @Override
    public boolean removeEdge(int vertexA, int vertexB) {
        if (vertexA == vertexB) {
            return false;
        }
        int indexA = findVertex(vertexA);
        int indexB = findVertex(vertexB);
        if (indexA < 0 || indexB < 0 || !edgePresent[indexA][indexB]) {
            return false;
        }
        if (!edgePresent[indexB][indexA]) {
            throw new IllegalStateException("Undirected edge presence is not symmetric");
        }

        edgePresent[indexA][indexB] = false;
        edgePresent[indexB][indexA] = false;
        weights[indexA][indexB] = 0.0d;
        weights[indexB][indexA] = 0.0d;
        edgeCount--;
        return true;
    }

    /** {@inheritDoc} Time complexity: {@code O(log V)}. */
    @Override
    public boolean containsEdge(int vertexA, int vertexB) {
        if (vertexA == vertexB) {
            return false;
        }
        int indexA = findVertex(vertexA);
        int indexB = findVertex(vertexB);
        return indexA >= 0 && indexB >= 0 && edgePresent[indexA][indexB];
    }

    /** {@inheritDoc} Time complexity: {@code O(log V)}. */
    @Override
    public OptionalDouble getEdgeWeight(int vertexA, int vertexB) {
        if (vertexA == vertexB) {
            return OptionalDouble.empty();
        }
        int indexA = findVertex(vertexA);
        int indexB = findVertex(vertexB);
        if (indexA < 0 || indexB < 0 || !edgePresent[indexA][indexB]) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(weights[indexA][indexB]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(log V + V)} and auxiliary space is
     * {@code O(1)}.</p>
     */
    @Override
    public OptionalInt getDegree(int vertexId) {
        int vertexIndex = findVertex(vertexId);
        if (vertexIndex < 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(degreeAt(vertexIndex));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(log V + V)}. Returned space is
     * {@code O(d(v))}.</p>
     */
    @Override
    public int[] getNeighborIds(int vertexId) {
        int vertexIndex = findVertex(vertexId);
        if (vertexIndex < 0) {
            return new int[0];
        }

        int[] neighbors = new int[degreeAt(vertexIndex)];
        int outputIndex = 0;
        for (int index = 0; index < vertexCount; index++) {
            if (edgePresent[vertexIndex][index]) {
                neighbors[outputIndex++] = vertexIds[index];
            }
        }
        return neighbors;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(log V + V)}. Returned space is
     * {@code O(d(v))}.</p>
     */
    @Override
    public Edge[] getIncidentEdges(int vertexId) {
        int vertexIndex = findVertex(vertexId);
        if (vertexIndex < 0) {
            return new Edge[0];
        }

        Edge[] edges = new Edge[degreeAt(vertexIndex)];
        int outputIndex = 0;
        for (int index = 0; index < vertexCount; index++) {
            if (edgePresent[vertexIndex][index]) {
                edges[outputIndex++] = new Edge(
                        vertexId, vertexIds[index], weights[vertexIndex][index]);
            }
        }
        return edges;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(V^2)} and returned space is
     * {@code O(E)}.</p>
     */
    @Override
    public Edge[] getEdges() {
        Edge[] edges = new Edge[edgeCount];
        int outputIndex = 0;
        for (int first = 0; first < vertexCount; first++) {
            for (int second = first + 1; second < vertexCount; second++) {
                if (edgePresent[first][second]) {
                    if (outputIndex >= edges.length) {
                        throw new IllegalStateException("Edge count is inconsistent with matrix data");
                    }
                    edges[outputIndex++] = new Edge(
                            vertexIds[first], vertexIds[second], weights[first][second]);
                }
            }
        }
        if (outputIndex != edges.length) {
            throw new IllegalStateException("Edge count is inconsistent with matrix data");
        }
        return edges;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Time complexity is {@code O(V^2)} and auxiliary space is
     * {@code O(1)}. Allocated capacity is retained for reuse.</p>
     */
    @Override
    public void clear() {
        for (int row = 0; row < vertexCount; row++) {
            vertexIds[row] = 0;
            for (int column = 0; column < vertexCount; column++) {
                weights[row][column] = 0.0d;
                edgePresent[row][column] = false;
            }
        }
        vertexCount = 0;
        edgeCount = 0;
    }

    private int findVertex(int vertexId) {
        int low = 0;
        int high = vertexCount - 1;
        while (low <= high) {
            int middle = low + ((high - low) >>> 1);
            int current = vertexIds[middle];
            if (current < vertexId) {
                low = middle + 1;
            } else if (current > vertexId) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -low - 1;
    }

    private int degreeAt(int vertexIndex) {
        int degree = 0;
        for (int index = 0; index < vertexCount; index++) {
            if (edgePresent[vertexIndex][index]) {
                degree++;
            }
        }
        return degree;
    }

    private void insertMatrixPosition(int insertionIndex) {
        double[] spareWeightRow = weights[vertexCount];
        boolean[] sparePresenceRow = edgePresent[vertexCount];

        for (int row = vertexCount; row > insertionIndex; row--) {
            weights[row] = weights[row - 1];
            edgePresent[row] = edgePresent[row - 1];
        }
        weights[insertionIndex] = spareWeightRow;
        edgePresent[insertionIndex] = sparePresenceRow;
        clearRow(spareWeightRow, sparePresenceRow);

        int shiftedColumns = vertexCount - insertionIndex;
        for (int row = 0; row <= vertexCount; row++) {
            if (shiftedColumns > 0) {
                System.arraycopy(weights[row], insertionIndex, weights[row],
                        insertionIndex + 1, shiftedColumns);
                System.arraycopy(edgePresent[row], insertionIndex, edgePresent[row],
                        insertionIndex + 1, shiftedColumns);
            }
            weights[row][insertionIndex] = 0.0d;
            edgePresent[row][insertionIndex] = false;
        }
    }

    private void removeMatrixPosition(int removalIndex) {
        double[] removedWeightRow = weights[removalIndex];
        boolean[] removedPresenceRow = edgePresent[removalIndex];
        int shiftedColumns = vertexCount - removalIndex - 1;

        for (int row = 0; row < vertexCount; row++) {
            if (shiftedColumns > 0) {
                System.arraycopy(weights[row], removalIndex + 1, weights[row],
                        removalIndex, shiftedColumns);
                System.arraycopy(edgePresent[row], removalIndex + 1, edgePresent[row],
                        removalIndex, shiftedColumns);
            }
            weights[row][vertexCount - 1] = 0.0d;
            edgePresent[row][vertexCount - 1] = false;
        }

        int shiftedRows = vertexCount - removalIndex - 1;
        if (shiftedRows > 0) {
            System.arraycopy(weights, removalIndex + 1, weights, removalIndex, shiftedRows);
            System.arraycopy(edgePresent, removalIndex + 1, edgePresent,
                    removalIndex, shiftedRows);
        }
        weights[vertexCount - 1] = removedWeightRow;
        edgePresent[vertexCount - 1] = removedPresenceRow;
        clearRow(removedWeightRow, removedPresenceRow);
    }

    private void clearRow(double[] weightRow, boolean[] presenceRow) {
        for (int column = 0; column < weightRow.length; column++) {
            weightRow[column] = 0.0d;
            presenceRow[column] = false;
        }
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= vertexIds.length) {
            return;
        }
        if (requiredCapacity < 0 || requiredCapacity > MAX_MATRIX_CAPACITY) {
            throw new IllegalStateException("Adjacency-matrix capacity limit exceeded");
        }

        int currentCapacity = vertexIds.length;
        long doubled = currentCapacity == 0 ? 1L : (long) currentCapacity * 2L;
        int newCapacity = doubled > MAX_MATRIX_CAPACITY
                ? MAX_MATRIX_CAPACITY : (int) doubled;
        if (newCapacity < requiredCapacity) {
            newCapacity = requiredCapacity;
        }

        int[] expandedVertexIds = new int[newCapacity];
        System.arraycopy(vertexIds, 0, expandedVertexIds, 0, vertexCount);
        double[][] expandedWeights = new double[newCapacity][newCapacity];
        boolean[][] expandedEdgePresent = new boolean[newCapacity][newCapacity];
        for (int row = 0; row < vertexCount; row++) {
            System.arraycopy(weights[row], 0, expandedWeights[row], 0, vertexCount);
            System.arraycopy(edgePresent[row], 0,
                    expandedEdgePresent[row], 0, vertexCount);
        }

        vertexIds = expandedVertexIds;
        weights = expandedWeights;
        edgePresent = expandedEdgePresent;
    }

    private static void validateEdgeArguments(int vertexA, int vertexB, double weight) {
        if (vertexA == vertexB) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (!Double.isFinite(weight) || weight < 0.0d) {
            throw new IllegalArgumentException("Edge weight must be finite and non-negative");
        }
    }
}
