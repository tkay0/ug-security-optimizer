package org.ugoptimizer.result;

import java.util.Objects;
import java.util.OptionalDouble;
import org.ugoptimizer.model.Edge;

/** Immutable shortest-path outcome. */
public final class PathResult {

    private static final double WEIGHT_TOLERANCE = 1.0e-9d;

    public enum Status {
        FOUND,
        UNREACHABLE,
        MISSING_SOURCE,
        MISSING_DESTINATION,
        MISSING_BOTH
    }

    private final int sourceVertexId;
    private final int destinationVertexId;
    private final int[] vertexIds;
    private final Edge[] edges;
    private final double totalWeight;
    private final Status status;

    private PathResult(
            int sourceVertexId,
            int destinationVertexId,
            int[] vertexIds,
            Edge[] edges,
            double totalWeight,
            Status status) {
        this.sourceVertexId = sourceVertexId;
        this.destinationVertexId = destinationVertexId;
        this.vertexIds = vertexIds.clone();
        this.edges = edges.clone();
        this.totalWeight = totalWeight == 0.0d ? 0.0d : totalWeight;
        this.status = status;
    }

    public static PathResult found(
            int sourceVertexId,
            int destinationVertexId,
            int[] vertexIds,
            Edge[] edges,
            double totalWeight) {
        Objects.requireNonNull(vertexIds, "vertexIds cannot be null");
        Objects.requireNonNull(edges, "edges cannot be null");
        validateWeight(totalWeight);

        if (vertexIds.length != edges.length + 1) {
            throw new IllegalArgumentException(
                    "A path must contain exactly one more vertex than edge");
        }
        if (vertexIds[0] != sourceVertexId) {
            throw new IllegalArgumentException("Path must begin with the source vertex");
        }
        if (vertexIds[vertexIds.length - 1] != destinationVertexId) {
            throw new IllegalArgumentException("Path must end with the destination vertex");
        }

        if (sourceVertexId == destinationVertexId
                && (vertexIds.length != 1 || edges.length != 0 || totalWeight != 0.0d)) {
            throw new IllegalArgumentException(
                    "A path from a vertex to itself must contain one vertex, no edges, and zero weight");
        }

        double edgeWeightSum = 0.0d;
        for (int i = 0; i < edges.length; i++) {
            Edge edge = Objects.requireNonNull(edges[i], "edges cannot contain null");
            if (!edge.connects(vertexIds[i], vertexIds[i + 1])) {
                throw new IllegalArgumentException(
                        "Each edge must connect its corresponding consecutive vertices");
            }
            edgeWeightSum += edge.getWeight();
        }
        if (!Double.isFinite(edgeWeightSum) || !weightsEqual(edgeWeightSum, totalWeight)) {
            throw new IllegalArgumentException(
                    "Total weight must equal the sum of edge weights");
        }

        return new PathResult(
                sourceVertexId,
                destinationVertexId,
                vertexIds,
                edges,
                totalWeight,
                Status.FOUND);
    }

    public static PathResult unreachable(int sourceVertexId, int destinationVertexId) {
        return notFound(sourceVertexId, destinationVertexId, Status.UNREACHABLE);
    }

    public static PathResult missingSource(int sourceVertexId, int destinationVertexId) {
        return notFound(sourceVertexId, destinationVertexId, Status.MISSING_SOURCE);
    }

    public static PathResult missingDestination(int sourceVertexId, int destinationVertexId) {
        return notFound(sourceVertexId, destinationVertexId, Status.MISSING_DESTINATION);
    }

    public static PathResult missingBoth(int sourceVertexId, int destinationVertexId) {
        return notFound(sourceVertexId, destinationVertexId, Status.MISSING_BOTH);
    }

    public int getSourceVertexId() {
        return sourceVertexId;
    }

    public int getDestinationVertexId() {
        return destinationVertexId;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isReachable() {
        return status == Status.FOUND;
    }

    public int getVertexCount() {
        return vertexIds.length;
    }

    public int getEdgeCount() {
        return edges.length;
    }

    public int[] getVertexIds() {
        return vertexIds.clone();
    }

    public Edge[] getEdges() {
        return edges.clone();
    }

    public OptionalDouble getTotalWeight() {
        return status == Status.FOUND
                ? OptionalDouble.of(totalWeight)
                : OptionalDouble.empty();
    }

    private static PathResult notFound(
            int sourceVertexId,
            int destinationVertexId,
            Status status) {
        return new PathResult(
                sourceVertexId,
                destinationVertexId,
                new int[0],
                new Edge[0],
                0.0d,
                status);
    }

    private static void validateWeight(double totalWeight) {
        if (!Double.isFinite(totalWeight) || totalWeight < 0.0d) {
            throw new IllegalArgumentException("Total weight must be finite and non-negative");
        }
    }

    private static boolean weightsEqual(double first, double second) {
        double scale = Math.max(1.0d, Math.max(Math.abs(first), Math.abs(second)));
        return Math.abs(first - second) <= WEIGHT_TOLERANCE * scale;
    }
}
