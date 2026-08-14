package org.ugoptimizer.algorithms.mst;

import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.MSTResult;
import org.ugoptimizer.structures.graph.WeightedGraph;
import org.ugoptimizer.structures.heap.BinaryHeap;

/**
 * Computes a deterministic minimum spanning tree or forest with Prim's
 * algorithm and the project's custom binary heap.
 *
 * <p>For each component, the smallest not-yet-included vertex becomes its
 * root. Candidate edges are ordered by weight, normalized first endpoint,
 * normalized second endpoint, and destination vertex ID. Edges whose
 * destination has already been included are stale and are skipped. Selected
 * edges remain in deterministic acceptance order.</p>
 *
 * <p>Let {@code V} and {@code E} be the graph's vertex and edge counts. Each
 * included vertex requests one incident-edge snapshot. The adjacency-list
 * kernel costs {@code O(V + (V + E) log V + E log E)}. The adjacency-matrix
 * kernel costs {@code O(V^2 + (V + E) log V + E log E)} because every vertex
 * requires a matrix-row scan. Binary ID mapping contributes the
 * {@code log V} terms, and up to {@code O(E)} heap candidates may be inserted
 * and polled. Auxiliary space is {@code O(V + E)}.</p>
 *
 * <p>{@link MSTResult#of(int[], int, Edge[], double)} adds current quadratic
 * duplicate and endpoint-membership validation, so the complete call has an
 * additional {@code O(V^2)} validation cost.</p>
 */
public final class Prim {

    /**
     * Computes a minimum spanning tree or minimum spanning forest.
     *
     * @param graph graph to process; must not be {@code null}
     * @return deterministic tree or forest result
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     * @throws IllegalStateException if total weight becomes non-finite or graph
     *         snapshots are inconsistent
     */
    public MSTResult compute(WeightedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        int[] vertexIds = graph.getVertexIds();
        if (vertexIds.length == 0) {
            return MSTResult.of(vertexIds, 0, new Edge[0], 0.0d);
        }

        boolean[] included = new boolean[vertexIds.length];
        Edge[] selectedEdges = new Edge[vertexIds.length - 1];
        int selectedCount = 0;
        int componentCount = 0;
        double totalWeight = 0.0d;
        BinaryHeap<EdgeCandidate> candidates = new BinaryHeap<>();

        for (int rootIndex = 0; rootIndex < vertexIds.length; rootIndex++) {
            if (included[rootIndex]) {
                continue;
            }
            componentCount++;
            included[rootIndex] = true;
            addCandidates(graph, vertexIds, included, rootIndex, candidates);

            while (!candidates.isEmpty()) {
                EdgeCandidate candidate = candidates.poll();
                if (included[candidate.destinationIndex]) {
                    continue;
                }

                included[candidate.destinationIndex] = true;
                selectedEdges[selectedCount++] = candidate.edge;
                totalWeight += candidate.edge.getWeight();
                if (!Double.isFinite(totalWeight)) {
                    throw new IllegalStateException("Selected edge-weight total is not finite");
                }
                addCandidates(
                        graph,
                        vertexIds,
                        included,
                        candidate.destinationIndex,
                        candidates);
            }
        }

        return MSTResult.of(
                vertexIds,
                componentCount,
                copyOf(selectedEdges, selectedCount),
                totalWeight);
    }

    private static void addCandidates(
            WeightedGraph graph,
            int[] vertexIds,
            boolean[] included,
            int sourceIndex,
            BinaryHeap<EdgeCandidate> candidates) {
        int sourceId = vertexIds[sourceIndex];
        Edge[] incidentEdges = graph.getIncidentEdges(sourceId);
        for (Edge edge : incidentEdges) {
            int destinationId = oppositeVertexId(edge, sourceId);
            int destinationIndex = indexOf(vertexIds, destinationId);
            if (destinationIndex < 0) {
                throw new IllegalStateException(
                        "Incident edge references a missing vertex: " + destinationId);
            }
            if (!included[destinationIndex]) {
                candidates.add(new EdgeCandidate(edge, destinationIndex, destinationId));
            }
        }
    }

    private static int oppositeVertexId(Edge edge, int vertexId) {
        if (edge.getVertexAId() == vertexId) {
            return edge.getVertexBId();
        }
        if (edge.getVertexBId() == vertexId) {
            return edge.getVertexAId();
        }
        throw new IllegalStateException("Graph returned a non-incident edge");
    }

    private static int indexOf(int[] sortedVertexIds, int vertexId) {
        int low = 0;
        int high = sortedVertexIds.length - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            int middleId = sortedVertexIds[middle];
            if (middleId == vertexId) {
                return middle;
            }
            if (middleId < vertexId) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
    }

    private static Edge[] copyOf(Edge[] source, int length) {
        Edge[] result = new Edge[length];
        System.arraycopy(source, 0, result, 0, length);
        return result;
    }

    private static final class EdgeCandidate implements Comparable<EdgeCandidate> {
        private final Edge edge;
        private final int destinationIndex;
        private final int destinationId;

        private EdgeCandidate(Edge edge, int destinationIndex, int destinationId) {
            this.edge = edge;
            this.destinationIndex = destinationIndex;
            this.destinationId = destinationId;
        }

        @Override
        public int compareTo(EdgeCandidate other) {
            int comparison = Double.compare(edge.getWeight(), other.edge.getWeight());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Integer.compare(edge.getVertexAId(), other.edge.getVertexAId());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Integer.compare(edge.getVertexBId(), other.edge.getVertexBId());
            return comparison != 0
                    ? comparison
                    : Integer.compare(destinationId, other.destinationId);
        }
    }
}
