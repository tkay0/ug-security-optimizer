package org.ugoptimizer.algorithms.shortestpath;

import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.structures.graph.WeightedGraph;
import org.ugoptimizer.structures.heap.BinaryHeap;

/**
 * Computes deterministic shortest paths in a non-negative weighted graph.
 *
 * <p>External vertex IDs are mapped to dense primitive-array indexes by
 * binary search over the graph's ascending vertex snapshot. Since the custom
 * {@link BinaryHeap} has no decrease-key operation, every strict distance
 * improvement adds a new immutable heap entry. Entries whose distance is no
 * longer current, or whose vertex is already settled, are skipped. Equal
 * distances do not replace predecessors, preserving the first path established
 * by distance/vertex heap ordering and ascending incident-edge order.</p>
 *
 * <p>Let {@code V} be the total vertex count, {@code R} the settled vertex
 * count, and {@code A} the directed adjacency entries examined before the
 * destination settles or the reachable component is exhausted. The
 * adjacency-list kernel costs
 * {@code O(V + (R + A) log V + A log A)}. The adjacency-matrix kernel costs
 * {@code O(V + RV + (R + A) log V + A log A)} because each settled vertex
 * requires a matrix-row scan. Binary ID mapping contributes the
 * {@code log V} terms, while duplicate heap entries can occupy and process
 * {@code O(A)} entries. Path reconstruction and current
 * {@link PathResult} validation each cost {@code O(P)} for a path containing
 * {@code P} vertices. Auxiliary space is {@code O(V + A)}.</p>
 */
public final class Dijkstra {

    /**
     * Finds a minimum-weight path between two vertices.
     *
     * @param graph graph to search; must not be {@code null}
     * @param sourceVertexId source vertex ID
     * @param destinationVertexId destination vertex ID
     * @return found, unreachable, or missing-endpoint result
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     * @throws IllegalStateException if a candidate distance overflows to a
     *         non-finite value or the graph returns inconsistent snapshots
     */
    public PathResult shortestPath(
            WeightedGraph graph, int sourceVertexId, int destinationVertexId) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        int[] vertexIds = graph.getVertexIds();
        int sourceIndex = indexOf(vertexIds, sourceVertexId);
        int destinationIndex = indexOf(vertexIds, destinationVertexId);
        if (sourceIndex < 0 && destinationIndex < 0) {
            return PathResult.missingBoth(sourceVertexId, destinationVertexId);
        }
        if (sourceIndex < 0) {
            return PathResult.missingSource(sourceVertexId, destinationVertexId);
        }
        if (destinationIndex < 0) {
            return PathResult.missingDestination(sourceVertexId, destinationVertexId);
        }
        if (sourceIndex == destinationIndex) {
            return PathResult.found(
                    sourceVertexId,
                    destinationVertexId,
                    new int[]{sourceVertexId},
                    new Edge[0],
                    0.0d);
        }

        double[] distances = new double[vertexIds.length];
        int[] predecessorIndexes = new int[vertexIds.length];
        double[] predecessorWeights = new double[vertexIds.length];
        boolean[] settled = new boolean[vertexIds.length];
        for (int index = 0; index < vertexIds.length; index++) {
            distances[index] = Double.POSITIVE_INFINITY;
            predecessorIndexes[index] = -1;
        }

        BinaryHeap<DistanceEntry> heap = new BinaryHeap<>();
        distances[sourceIndex] = 0.0d;
        heap.add(new DistanceEntry(sourceIndex, sourceVertexId, 0.0d));

        while (!heap.isEmpty()) {
            DistanceEntry entry = heap.poll();
            int currentIndex = entry.vertexIndex;
            if (settled[currentIndex]
                    || Double.compare(entry.distance, distances[currentIndex]) != 0) {
                continue;
            }

            settled[currentIndex] = true;
            if (currentIndex == destinationIndex) {
                break;
            }

            Edge[] incidentEdges = graph.getIncidentEdges(entry.vertexId);
            for (Edge edge : incidentEdges) {
                int neighborId = oppositeVertexId(edge, entry.vertexId);
                int neighborIndex = indexOf(vertexIds, neighborId);
                if (neighborIndex < 0) {
                    throw new IllegalStateException(
                            "Incident edge references a missing vertex: " + neighborId);
                }
                if (settled[neighborIndex]) {
                    continue;
                }

                double candidateDistance = entry.distance + edge.getWeight();
                if (!Double.isFinite(candidateDistance)) {
                    throw new IllegalStateException(
                            "Candidate path distance is not finite for vertex " + neighborId);
                }
                if (Double.compare(candidateDistance, distances[neighborIndex]) < 0) {
                    distances[neighborIndex] = candidateDistance;
                    predecessorIndexes[neighborIndex] = currentIndex;
                    predecessorWeights[neighborIndex] = edge.getWeight();
                    heap.add(new DistanceEntry(
                            neighborIndex, neighborId, candidateDistance));
                }
            }
        }

        if (!settled[destinationIndex]) {
            return PathResult.unreachable(sourceVertexId, destinationVertexId);
        }
        return buildPath(
                vertexIds,
                predecessorIndexes,
                predecessorWeights,
                distances[destinationIndex],
                sourceIndex,
                destinationIndex);
    }

    private static PathResult buildPath(
            int[] vertexIds,
            int[] predecessorIndexes,
            double[] predecessorWeights,
            double totalWeight,
            int sourceIndex,
            int destinationIndex) {
        int edgeCount = 0;
        int currentIndex = destinationIndex;
        while (currentIndex != sourceIndex) {
            currentIndex = predecessorIndexes[currentIndex];
            edgeCount++;
            if (currentIndex < 0 || edgeCount >= vertexIds.length) {
                throw new IllegalStateException("Shortest-path predecessor chain is invalid");
            }
        }

        int[] pathVertexIds = new int[edgeCount + 1];
        Edge[] pathEdges = new Edge[edgeCount];
        currentIndex = destinationIndex;
        for (int position = edgeCount; position > 0; position--) {
            pathVertexIds[position] = vertexIds[currentIndex];
            int predecessorIndex = predecessorIndexes[currentIndex];
            pathEdges[position - 1] = new Edge(
                    vertexIds[predecessorIndex],
                    vertexIds[currentIndex],
                    predecessorWeights[currentIndex]);
            currentIndex = predecessorIndex;
        }
        pathVertexIds[0] = vertexIds[sourceIndex];
        return PathResult.found(
                vertexIds[sourceIndex],
                vertexIds[destinationIndex],
                pathVertexIds,
                pathEdges,
                totalWeight);
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

    private static final class DistanceEntry implements Comparable<DistanceEntry> {
        private final int vertexIndex;
        private final int vertexId;
        private final double distance;

        private DistanceEntry(int vertexIndex, int vertexId, double distance) {
            this.vertexIndex = vertexIndex;
            this.vertexId = vertexId;
            this.distance = distance;
        }

        @Override
        public int compareTo(DistanceEntry other) {
            int distanceComparison = Double.compare(distance, other.distance);
            return distanceComparison != 0
                    ? distanceComparison
                    : Integer.compare(vertexId, other.vertexId);
        }
    }
}
