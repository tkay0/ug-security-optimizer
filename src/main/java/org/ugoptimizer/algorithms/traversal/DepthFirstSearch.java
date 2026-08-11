package org.ugoptimizer.algorithms.traversal;

import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.WeightedGraph;

/**
 * Performs an iterative depth-first traversal over the vertices reachable
 * from a given starting vertex.
 *
 * <p>The traversal uses manually managed arrays instead of Java collection
 * classes. Each active vertex has a corresponding neighbour index, allowing
 * neighbours to be explored one at a time in the same way as recursive DFS.
 *
 * <p>Time complexity is O(V + E) for the traversal, excluding the binary
 * searches used to locate vertex IDs. Space complexity is O(V), where V is
 * the number of vertices.
 */
public class DepthFirstSearch {

    /**
     * Performs a depth-first traversal of {@code graph} starting at
     * {@code startVertexId}, visiting only vertices reachable from it.
     *
     * @param graph the graph to traverse; must not be {@code null}
     * @param startVertexId the vertex to begin the traversal from
     * @return a {@link TraversalResult} describing the traversal outcome
     * @throws IllegalArgumentException if {@code graph} is {@code null}
     */
    public TraversalResult traverse(WeightedGraph graph, int startVertexId) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }

        int totalVertexCount = graph.getVertexCount();

        if (!graph.containsVertex(startVertexId)) {
            return TraversalResult.missingStart(
                    startVertexId,
                    totalVertexCount);
        }

        int[] vertexIds = graph.getVertexIds();
        boolean[] visited = new boolean[vertexIds.length];
        int[] visitOrder = new int[vertexIds.length];

        // Each stack position represents an active DFS vertex.
        // stackNextNeighbor stores the next neighbour to examine.
        int[] stackVertices = new int[vertexIds.length];
        int[] stackNextNeighbor = new int[vertexIds.length];

        int top = 0;
        int visitedCount = 0;

        int startIndex = indexOfVertex(vertexIds, startVertexId);
        visited[startIndex] = true;

        stackVertices[top] = startVertexId;
        stackNextNeighbor[top] = 0;

        visitOrder[visitedCount++] = startVertexId;

        while (top >= 0) {
            int currentVertexId = stackVertices[top];
            int[] neighborIds = graph.getNeighborIds(currentVertexId);

            if (stackNextNeighbor[top] < neighborIds.length) {
                int neighborId = neighborIds[stackNextNeighbor[top]];
                stackNextNeighbor[top]++;

                int neighborIndex = indexOfVertex(vertexIds, neighborId);

                if (neighborIndex >= 0 && !visited[neighborIndex]) {
                    visited[neighborIndex] = true;

                    top++;
                    stackVertices[top] = neighborId;
                    stackNextNeighbor[top] = 0;

                    visitOrder[visitedCount++] = neighborId;
                }
            } else {
                top--;
            }
        }

        return TraversalResult.traversed(
                startVertexId,
                totalVertexCount,
                copyOf(visitOrder, visitedCount));
    }

    /**
     * Finds a vertex ID using binary search.
     *
     * @param sortedVertexIds vertex IDs in ascending order
     * @param vertexId vertex ID to find
     * @return the index of the vertex, or {@code -1} if it is absent
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

    /**
     * Copies the required portion of an integer array.
     *
     * @param source source array
     * @param length number of elements to copy
     * @return a new array containing the requested elements
     */
    private static int[] copyOf(int[] source, int length) {
        int[] result = new int[length];

        for (int i = 0; i < length; i++) {
            result[i] = source[i];
        }

        return result;
    }
}