package org.ugoptimizer.model;

import java.util.OptionalInt;

/**
 * Immutable weighted edge between two vertices in an undirected graph.
 *
 * <p>Endpoints are normalized into ascending order, so an edge created for
 * {@code (7, 3)} is stored as {@code (3, 7)}. Vertex IDs may be any
 * {@code int}; only equal endpoint IDs are rejected.</p>
 */
public final class Edge {

    private final int vertexAId;
    private final int vertexBId;
    private final double weight;

    /**
     * Creates an undirected weighted edge.
     *
     * @param vertexAId one endpoint
     * @param vertexBId the other endpoint
     * @param weight finite, non-negative edge weight
     * @throws IllegalArgumentException if the endpoints are equal or the
     *         weight is negative, NaN, or infinite
     */
    public Edge(int vertexAId, int vertexBId, double weight) {
        if (vertexAId == vertexBId) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        validateWeight(weight);

        this.vertexAId = Math.min(vertexAId, vertexBId);
        this.vertexBId = Math.max(vertexAId, vertexBId);
        this.weight = weight == 0.0d ? 0.0d : weight;
    }

    public int getVertexAId() {
        return vertexAId;
    }

    public int getVertexBId() {
        return vertexBId;
    }

    public double getWeight() {
        return weight;
    }

    /**
     * Tests whether this edge joins the two supplied vertices, regardless of
     * endpoint order.
     */
    public boolean connects(int firstVertexId, int secondVertexId) {
        return (vertexAId == firstVertexId && vertexBId == secondVertexId)
                || (vertexAId == secondVertexId && vertexBId == firstVertexId);
    }

    public boolean isIncidentTo(int vertexId) {
        return vertexAId == vertexId || vertexBId == vertexId;
    }

    /**
     * Returns the endpoint opposite {@code vertexId}, or an empty value when
     * the supplied vertex is not incident to this edge.
     */
    public OptionalInt getOppositeVertexId(int vertexId) {
        if (vertexAId == vertexId) {
            return OptionalInt.of(vertexBId);
        }
        if (vertexBId == vertexId) {
            return OptionalInt.of(vertexAId);
        }
        return OptionalInt.empty();
    }

    public Edge withWeight(double newWeight) {
        return new Edge(vertexAId, vertexBId, newWeight);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Edge edge)) {
            return false;
        }
        return vertexAId == edge.vertexAId
                && vertexBId == edge.vertexBId
                && Double.compare(weight, edge.weight) == 0;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(vertexAId);
        result = 31 * result + Integer.hashCode(vertexBId);
        result = 31 * result + Double.hashCode(weight);
        return result;
    }

    @Override
    public String toString() {
        return "Edge{" + vertexAId + "--" + vertexBId + ", weight=" + weight + '}';
    }

    private static void validateWeight(double weight) {
        if (!Double.isFinite(weight) || weight < 0.0d) {
            throw new IllegalArgumentException("Weight must be finite and non-negative");
        }
    }
}
