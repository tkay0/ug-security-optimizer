package org.ugoptimizer.structures.graph;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.ugoptimizer.model.Edge;

/**
 * Shared contract for undirected weighted graph representations using
 * {@code int} vertex IDs.
 *
 * <p>Implementations must reject self-loops and weights that are negative,
 * NaN, or infinite. Zero-weight edges are valid. Adding an edge whose
 * endpoints already exist updates that edge's weight; duplicate detection is
 * based only on its normalized endpoints, not on {@link Edge#equals(Object)}.
 * Adding an edge never creates missing vertices implicitly.</p>
 *
 * <p>All array-returning methods return independent snapshots. Implementations
 * must apply these deterministic ordering rules:</p>
 * <ul>
 *   <li>{@link #getVertexIds()} returns ascending vertex IDs.</li>
 *   <li>{@link #getNeighborIds(int)} returns ascending neighbor IDs.</li>
 *   <li>{@link #getIncidentEdges(int)} orders edges by opposite vertex ID.</li>
 *   <li>{@link #getEdges()} returns each undirected edge exactly once, ordered
 *       by first endpoint and then second endpoint.</li>
 * </ul>
 *
 * <p>Queries involving missing vertices are safe: boolean operations return
 * {@code false}, optional values are empty, and array results are empty.</p>
 */
public interface WeightedGraph {

    /** Outcome of adding an edge to the graph. */
    enum EdgeUpdate {
        ADDED,
        UPDATED,
        UNCHANGED,
        MISSING_VERTEX
    }

    int getVertexCount();

    int getEdgeCount();

    boolean isEmpty();

    boolean addVertex(int vertexId);

    /**
     * Removes a vertex and all of its incident edges.
     *
     * @return {@code true} if the vertex existed
     */
    boolean removeVertex(int vertexId);

    boolean containsVertex(int vertexId);

    int[] getVertexIds();

    /**
     * Adds an edge or updates the weight of the edge joining the same
     * endpoints.
     *
     * @throws IllegalArgumentException for a self-loop or a weight that is
     *         negative, NaN, or infinite
     */
    EdgeUpdate addEdge(int vertexAId, int vertexBId, double weight);

    boolean removeEdge(int vertexAId, int vertexBId);

    boolean containsEdge(int vertexAId, int vertexBId);

    OptionalDouble getEdgeWeight(int vertexAId, int vertexBId);

    OptionalInt getDegree(int vertexId);

    int[] getNeighborIds(int vertexId);

    Edge[] getIncidentEdges(int vertexId);

    Edge[] getEdges();

    void clear();
}
