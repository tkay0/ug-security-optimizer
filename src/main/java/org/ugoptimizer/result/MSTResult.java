package org.ugoptimizer.result;

import java.util.Objects;
import org.ugoptimizer.model.Edge;

/** Immutable minimum-spanning-tree or minimum-spanning-forest outcome. */
public final class MSTResult {

    private static final double WEIGHT_TOLERANCE = 1.0e-9d;

    public enum Status {
        EMPTY_GRAPH,
        COMPLETE,
        DISCONNECTED
    }

    private final int[] vertexIds;
    private final int componentCount;
    private final Edge[] edges;
    private final double totalWeight;
    private final Status status;

    private MSTResult(
            int[] vertexIds,
            int componentCount,
            Edge[] edges,
            double totalWeight,
            Status status) {
        this.vertexIds = vertexIds.clone();
        this.componentCount = componentCount;
        this.edges = edges.clone();
        this.totalWeight = totalWeight == 0.0d ? 0.0d : totalWeight;
        this.status = status;
    }

    /**
     * Creates an MST result. For a disconnected graph, {@code edges} contains
     * a minimum spanning forest and {@code componentCount} is greater than one.
     */
    public static MSTResult of(
            int[] vertexIds,
            int componentCount,
            Edge[] edges,
            double totalWeight) {
        Objects.requireNonNull(vertexIds, "vertexIds cannot be null");
        Objects.requireNonNull(edges, "edges cannot be null");
        validateWeight(totalWeight);
        rejectDuplicateVertices(vertexIds);

        int vertexCount = vertexIds.length;
        validateComponentCount(vertexCount, componentCount);

        int expectedEdgeCount = vertexCount - componentCount;
        if (edges.length != expectedEdgeCount) {
            throw new IllegalArgumentException(
                    "Edge count must equal vertex count minus component count");
        }

        double edgeWeightSum = 0.0d;
        for (Edge edge : edges) {
            Edge nonNullEdge = Objects.requireNonNull(edge, "edges cannot contain null");
            if (!contains(vertexIds, nonNullEdge.getVertexAId())
                    || !contains(vertexIds, nonNullEdge.getVertexBId())) {
                throw new IllegalArgumentException("Every edge endpoint must be in vertexIds");
            }
            edgeWeightSum += nonNullEdge.getWeight();
        }
        if (!Double.isFinite(edgeWeightSum) || !weightsEqual(edgeWeightSum, totalWeight)) {
            throw new IllegalArgumentException(
                    "Total weight must equal the sum of edge weights");
        }

        Status status;
        if (vertexCount == 0) {
            status = Status.EMPTY_GRAPH;
        } else if (componentCount == 1) {
            status = Status.COMPLETE;
        } else {
            status = Status.DISCONNECTED;
        }

        return new MSTResult(vertexIds, componentCount, edges, totalWeight, status);
    }

    public int getVertexCount() {
        return vertexIds.length;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public int getEdgeCount() {
        return edges.length;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSpanningTree() {
        return status == Status.COMPLETE;
    }

    public boolean isDisconnected() {
        return status == Status.DISCONNECTED;
    }

    public int[] getVertexIds() {
        return vertexIds.clone();
    }

    public Edge[] getEdges() {
        return edges.clone();
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    private static void validateComponentCount(int vertexCount, int componentCount) {
        if (vertexCount == 0) {
            if (componentCount != 0) {
                throw new IllegalArgumentException("An empty graph must have zero components");
            }
            return;
        }
        if (componentCount < 1 || componentCount > vertexCount) {
            throw new IllegalArgumentException(
                    "Component count must be between one and the vertex count");
        }
    }

    private static void validateWeight(double totalWeight) {
        if (!Double.isFinite(totalWeight) || totalWeight < 0.0d) {
            throw new IllegalArgumentException("Total weight must be finite and non-negative");
        }
    }

    private static void rejectDuplicateVertices(int[] vertexIds) {
        for (int i = 0; i < vertexIds.length; i++) {
            for (int j = i + 1; j < vertexIds.length; j++) {
                if (vertexIds[i] == vertexIds[j]) {
                    throw new IllegalArgumentException("Vertex IDs cannot contain duplicates");
                }
            }
        }
    }

    private static boolean contains(int[] vertexIds, int soughtVertexId) {
        for (int vertexId : vertexIds) {
            if (vertexId == soughtVertexId) {
                return true;
            }
        }
        return false;
    }

    private static boolean weightsEqual(double first, double second) {
        double scale = Math.max(1.0d, Math.max(Math.abs(first), Math.abs(second)));
        return Math.abs(first - second) <= WEIGHT_TOLERANCE * scale;
    }
}
