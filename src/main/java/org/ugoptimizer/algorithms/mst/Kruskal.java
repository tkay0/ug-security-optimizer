package org.ugoptimizer.algorithms.mst;

import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.MSTResult;
import org.ugoptimizer.structures.disjointset.DisjointSet;
import org.ugoptimizer.structures.graph.WeightedGraph;

/**
 * Computes a deterministic minimum spanning tree or minimum spanning forest
 * with Kruskal's algorithm.
 *
 * <p>Edges are manually merge-sorted by weight, first endpoint, and second
 * endpoint. The algorithm scans that order and accepts an edge exactly when
 * the custom {@link DisjointSet} reports that it joins two components. The
 * supplied graph is queried only through snapshots and is never mutated.</p>
 *
 * <p>Let {@code V} and {@code E} be the graph's vertex and edge counts.
 * Acquiring snapshots costs {@code O(V + E)} for the adjacency-list graph and
 * {@code O(V^2)} for the adjacency-matrix graph. Merge sorting costs
 * {@code O(E log E)}, while disjoint-set construction manually sorts IDs in
 * {@code O(V log V)}. Each disjoint-set operation adds binary ID mapping at
 * {@code O(log V)} plus amortized {@code O(alpha(V))} parent-tree work. Thus
 * the kernel costs
 * {@code O(V log V + E log E + E(log V + alpha(V)))} with the list
 * representation and
 * {@code O(V^2 + V log V + E log E + E(log V + alpha(V)))} with the matrix
 * representation. Auxiliary space is {@code O(V + E)}.</p>
 *
 * <p>{@link MSTResult#of(int[], int, Edge[], double)} performs additional
 * validation, including quadratic duplicate and endpoint-membership checks,
 * so the complete public call currently has an additional {@code O(V^2)}
 * validation cost.</p>
 */
public final class Kruskal {

    /**
     * Computes the graph's minimum spanning tree or minimum spanning forest.
     *
     * @param graph undirected weighted graph; must not be {@code null}
     * @return deterministic MST or minimum spanning forest result
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     * @throws IllegalStateException if selected edge weights overflow a finite
     *         {@code double} total
     */
    public MSTResult compute(WeightedGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        int[] vertexIds = graph.getVertexIds();
        if (vertexIds.length == 0) {
            return MSTResult.of(vertexIds, 0, new Edge[0], 0.0d);
        }

        DisjointSet components = new DisjointSet(vertexIds);
        Edge[] sortedEdges = graph.getEdges();
        sortEdges(sortedEdges);

        Edge[] selectedEdges = new Edge[vertexIds.length - 1];
        int selectedCount = 0;
        double totalWeight = 0.0d;

        for (int index = 0;
                index < sortedEdges.length && components.getComponentCount() > 1;
                index++) {
            Edge edge = sortedEdges[index];
            if (components.union(edge.getVertexAId(), edge.getVertexBId())) {
                selectedEdges[selectedCount++] = edge;
                totalWeight += edge.getWeight();
                if (!Double.isFinite(totalWeight)) {
                    throw new IllegalStateException("Selected edge-weight total is not finite");
                }
            }
        }

        return MSTResult.of(
                vertexIds,
                components.getComponentCount(),
                copyOf(selectedEdges, selectedCount),
                totalWeight);
    }

    private static void sortEdges(Edge[] edges) {
        if (edges.length < 2) {
            return;
        }
        Edge[] buffer = new Edge[edges.length];
        int width = 1;
        while (width < edges.length) {
            int start = 0;
            while (start < edges.length) {
                int middle = start + Math.min(width, edges.length - start);
                int end = middle + Math.min(width, edges.length - middle);
                merge(edges, buffer, start, middle, end);
                start = end;
            }
            System.arraycopy(buffer, 0, edges, 0, edges.length);
            width = width > edges.length / 2 ? edges.length : width * 2;
        }
    }

    private static void merge(
            Edge[] edges, Edge[] buffer, int start, int middle, int end) {
        int left = start;
        int right = middle;
        int output = start;
        while (left < middle && right < end) {
            if (compare(edges[left], edges[right]) <= 0) {
                buffer[output++] = edges[left++];
            } else {
                buffer[output++] = edges[right++];
            }
        }
        while (left < middle) {
            buffer[output++] = edges[left++];
        }
        while (right < end) {
            buffer[output++] = edges[right++];
        }
    }

    private static int compare(Edge first, Edge second) {
        int weightComparison = Double.compare(first.getWeight(), second.getWeight());
        if (weightComparison != 0) {
            return weightComparison;
        }
        int firstEndpointComparison =
                Integer.compare(first.getVertexAId(), second.getVertexAId());
        if (firstEndpointComparison != 0) {
            return firstEndpointComparison;
        }
        return Integer.compare(first.getVertexBId(), second.getVertexBId());
    }

    private static Edge[] copyOf(Edge[] source, int length) {
        Edge[] result = new Edge[length];
        System.arraycopy(source, 0, result, 0, length);
        return result;
    }
}
