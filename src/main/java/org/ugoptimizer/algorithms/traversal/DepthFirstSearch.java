package org.ugoptimizer.algorithms.traversal;

import java.util.Objects;

import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.WeightedGraph;

/**
 * Depth-first traversal over the reachable component of a {@link WeightedGraph},
 * starting at a given vertex.
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li>Uses an explicit, manually managed array-based LIFO stack (no recursion,
 *       so there is no recursion-depth limitation to document, and no
 *       {@code java.util} collection types are used).</li>
 *   <li>A vertex is marked visited at the moment it is pushed, so the stack is
 *       bounded by the graph's vertex count &mdash; the same safe upper bound
 *       used for the BFS queue.</li>
 *   <li>Vertex IDs may be negative or non-contiguous, so a vertex's dense array
 *       index is found via binary search over {@link WeightedGraph#getVertexIds()},
 *       which the contract guarantees is returned in ascending order.</li>
 *   <li>Unvisited neighbours are pushed in descending order so that, once
 *       popped, they are processed in the ascending order returned by
 *       {@link WeightedGraph#getNeighborIds(int)} &mdash; this keeps traversal
 *       order deterministic and matches the documented BFS/DFS trace evidence.</li>
 *   <li>The supplied graph is never mutated and nothing is printed.</li>
 * </ul>
 */
public class DepthFirstSearch {

    /**
     * Performs a depth-first traversal of {@code graph} starting at
     * {@code startVertexId}, visiting only the vertices reachable from it.
     *
     * @param graph         the graph to traverse; must not be {@code null}
     * @param startVertexId the vertex to begin the traversal from
     * @return a {@link TraversalResult} describing the traversal outcome
     */
    public TraversalResult traverse(WeightedGraph graph, int startVertexId) {
        Objects.requireNonNull(graph, "graph cannot be null");

        int totalVertexCount = graph.getVertexCount();

        if (!graph.containsVertex(startVertexId)) {
            return TraversalResult.missingStart(startVertexId, totalVertexCount);
        }

        int[] vertexIds = graph.getVertexIds();
        boolean[] visited = new boolean[vertexIds.length];
        int[] visitOrder = new int[vertexIds.length];
        int visitedCount = 0;

        int[] stack = new int[vertexIds.length];
        int top = -1;

        int startIndex = indexOfVertex(vertexIds, startVertexId);
        visited[startIndex] = true;
        stack[++top] = startVertexId;

        while (top >= 0) {
            int currentVertexId = stack[top--];
            visitOrder[visitedCount++] = currentVertexId;

            int[] neighborIds = graph.getNeighborIds(currentVertexId);
            for (int i = neighborIds.length - 1; i >= 0; i--) {
                int neighborId = neighborIds[i];
                int neighborIndex = indexOfVertex(vertexIds, neighborId);
                if (neighborIndex >= 0 && !visited[neighborIndex]) {
                    visited[neighborIndex] = true;
                    stack[++top] = neighborId;
                }
            }
        }

        return TraversalResult.traversed(
                startVertexId,
                totalVertexCount,
                copyOf(visitOrder, visitedCount));
    }

    /**
     * Binary search for {@code vertexId} within {@code sortedVertexIds}.
     *
     * @return the index of {@code vertexId}, or {@code -1} if absent
     */
    private static int indexOfVertex(int[] sortedVertexIds, int vertexId) {
        int low = 0;
        int high = sortedVertexIds.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midValue = sortedVertexIds[mid];
            if (midValue == vertexId) {
                return mid;
            } else if (midValue < vertexId) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    private static int[] copyOf(int[] source, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = source[i];
        }
        return result;
    }
}