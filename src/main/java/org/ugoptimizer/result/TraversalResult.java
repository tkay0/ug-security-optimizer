package org.ugoptimizer.result;

import java.util.Objects;

/** Immutable outcome of a graph traversal. */
public final class TraversalResult {

    public enum Status {
        COMPLETE,
        PARTIAL,
        MISSING_START
    }

    private final int startVertexId;
    private final int totalVertexCount;
    private final int[] visitOrder;
    private final Status status;

    private TraversalResult(
            int startVertexId,
            int totalVertexCount,
            int[] visitOrder,
            Status status) {
        this.startVertexId = startVertexId;
        this.totalVertexCount = totalVertexCount;
        this.visitOrder = visitOrder.clone();
        this.status = status;
    }

    /**
     * Creates a successful complete or partial traversal result.
     */
    public static TraversalResult traversed(
            int startVertexId,
            int totalVertexCount,
            int[] visitOrder) {
        validateTotalVertexCount(totalVertexCount);
        Objects.requireNonNull(visitOrder, "visitOrder cannot be null");
        if (visitOrder.length == 0) {
            throw new IllegalArgumentException("A traversal must visit its start vertex");
        }
        if (visitOrder.length > totalVertexCount) {
            throw new IllegalArgumentException("Visited count cannot exceed total vertex count");
        }
        if (visitOrder[0] != startVertexId) {
            throw new IllegalArgumentException("Traversal must begin with the start vertex");
        }
        rejectDuplicateVertices(visitOrder);

        Status status = visitOrder.length == totalVertexCount
                ? Status.COMPLETE
                : Status.PARTIAL;
        return new TraversalResult(startVertexId, totalVertexCount, visitOrder, status);
    }

    /** Creates a result for a requested start vertex that is not in the graph. */
    public static TraversalResult missingStart(int startVertexId, int totalVertexCount) {
        validateTotalVertexCount(totalVertexCount);
        return new TraversalResult(
                startVertexId,
                totalVertexCount,
                new int[0],
                Status.MISSING_START);
    }

    public int getStartVertexId() {
        return startVertexId;
    }

    public int getTotalVertexCount() {
        return totalVertexCount;
    }

    public int getVisitedCount() {
        return visitOrder.length;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isComplete() {
        return status == Status.COMPLETE;
    }

    public boolean containsVertex(int vertexId) {
        for (int visitedVertexId : visitOrder) {
            if (visitedVertexId == vertexId) {
                return true;
            }
        }
        return false;
    }

    public int[] getVisitOrder() {
        return visitOrder.clone();
    }

    private static void validateTotalVertexCount(int totalVertexCount) {
        if (totalVertexCount < 0) {
            throw new IllegalArgumentException("Total vertex count cannot be negative");
        }
    }

    private static void rejectDuplicateVertices(int[] vertexIds) {
        for (int i = 0; i < vertexIds.length; i++) {
            for (int j = i + 1; j < vertexIds.length; j++) {
                if (vertexIds[i] == vertexIds[j]) {
                    throw new IllegalArgumentException("Visit order cannot contain duplicates");
                }
            }
        }
    }
}
