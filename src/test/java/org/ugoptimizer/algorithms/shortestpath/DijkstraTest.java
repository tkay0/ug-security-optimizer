package org.ugoptimizer.algorithms.shortestpath;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.PathResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class DijkstraTest {

    @Test
    void nullGraphIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Dijkstra().shortestPath(null, 1, 2));
    }

    @Test
    void missingEndpointStatusesAreDistinguished() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2);
        Dijkstra dijkstra = new Dijkstra();

        assertEquals(
                PathResult.Status.MISSING_BOTH,
                dijkstra.shortestPath(graph, 90, 99).getStatus());
        assertEquals(
                PathResult.Status.MISSING_SOURCE,
                dijkstra.shortestPath(graph, 90, 2).getStatus());
        assertEquals(
                PathResult.Status.MISSING_DESTINATION,
                dijkstra.shortestPath(graph, 1, 99).getStatus());
    }

    @Test
    void existingSourceToItselfProducesZeroEdgePath() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), -7);

        PathResult result = new Dijkstra().shortestPath(graph, -7, -7);

        assertFoundPath(result, new int[]{-7}, new Edge[0], 0.0d);
    }

    @Test
    void directEdgeProducesDirectShortestPath() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 10, 20);
        graph.addEdge(10, 20, 3.5d);

        PathResult result = new Dijkstra().shortestPath(graph, 10, 20);

        assertFoundPath(
                result,
                new int[]{10, 20},
                new Edge[]{new Edge(10, 20, 3.5d)},
                3.5d);
    }

    @Test
    void indirectRouteBeatsExpensiveDirectEdgeAndSkipsStaleEntry() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), 1, 2, 3, 4);
        graph.addEdge(1, 2, 10.0d);
        graph.addEdge(1, 3, 1.0d);
        graph.addEdge(3, 2, 1.0d);
        graph.addEdge(2, 4, 1.0d);
        graph.addEdge(3, 4, 20.0d);

        PathResult result = new Dijkstra().shortestPath(graph, 1, 4);

        assertFoundPath(
                result,
                new int[]{1, 3, 2, 4},
                new Edge[]{
                    new Edge(1, 3, 1.0d),
                    new Edge(3, 2, 1.0d),
                    new Edge(2, 4, 1.0d)
                },
                3.0d);
    }

    @Test
    void disconnectedDestinationIsUnreachable() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3, 4);
        graph.addEdge(1, 2, 1.0d);
        graph.addEdge(3, 4, 1.0d);

        PathResult result = new Dijkstra().shortestPath(graph, 1, 4);

        assertEquals(PathResult.Status.UNREACHABLE, result.getStatus());
        assertFalse(result.isReachable());
        assertArrayEquals(new int[0], result.getVertexIds());
        assertArrayEquals(new Edge[0], result.getEdges());
        assertTrue(result.getTotalWeight().isEmpty());
    }

    @Test
    void zeroWeightEdgesAndCyclesAreHandled() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), 1, 2, 3, 4);
        graph.addEdge(1, 2, 0.0d);
        graph.addEdge(2, 3, 0.0d);
        graph.addEdge(3, 1, 4.0d);
        graph.addEdge(3, 4, 2.0d);
        graph.addEdge(2, 4, 8.0d);

        PathResult result = new Dijkstra().shortestPath(graph, 1, 4);

        assertFoundPath(
                result,
                new int[]{1, 2, 3, 4},
                new Edge[]{
                    new Edge(1, 2, 0.0d),
                    new Edge(2, 3, 0.0d),
                    new Edge(3, 4, 2.0d)
                },
                2.0d);
    }

    @Test
    void equalDistanceAlternativePreservesFirstDeterministicPredecessor() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3, 4);
        graph.addEdge(1, 3, 1.0d);
        graph.addEdge(3, 4, 1.0d);
        graph.addEdge(1, 2, 1.0d);
        graph.addEdge(2, 4, 1.0d);

        PathResult result = new Dijkstra().shortestPath(graph, 1, 4);

        assertFoundPath(
                result,
                new int[]{1, 2, 4},
                new Edge[]{new Edge(1, 2, 1.0d), new Edge(2, 4, 1.0d)},
                2.0d);
    }

    @Test
    void supportsNegativeNonContiguousAndExtremeVertexIds() {
        WeightedGraph graph = graphWithVertices(
                new AdjacencyListGraph(),
                Integer.MAX_VALUE,
                -7,
                42,
                Integer.MIN_VALUE);
        graph.addEdge(Integer.MIN_VALUE, -7, 1.0d);
        graph.addEdge(-7, 42, 2.0d);
        graph.addEdge(42, Integer.MAX_VALUE, 3.0d);
        graph.addEdge(Integer.MIN_VALUE, Integer.MAX_VALUE, 20.0d);

        PathResult result = new Dijkstra().shortestPath(
                graph, Integer.MIN_VALUE, Integer.MAX_VALUE);

        assertFoundPath(
                result,
                new int[]{Integer.MIN_VALUE, -7, 42, Integer.MAX_VALUE},
                new Edge[]{
                    new Edge(Integer.MIN_VALUE, -7, 1.0d),
                    new Edge(-7, 42, 2.0d),
                    new Edge(42, Integer.MAX_VALUE, 3.0d)
                },
                6.0d);
    }

    @Test
    void repeatedRunsAreIdenticalAndGraphIsNotMutated() {
        WeightedGraph graph = createEquivalentGraph(new AdjacencyMatrixGraph());
        int[] verticesBefore = graph.getVertexIds();
        Edge[] edgesBefore = graph.getEdges();
        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();
        Dijkstra dijkstra = new Dijkstra();

        PathResult first = dijkstra.shortestPath(graph, -10, 100);
        PathResult second = dijkstra.shortestPath(graph, -10, 100);

        assertEquals(first.getStatus(), second.getStatus());
        assertArrayEquals(first.getVertexIds(), second.getVertexIds());
        assertArrayEquals(first.getEdges(), second.getEdges());
        assertEquals(first.getTotalWeight(), second.getTotalWeight());
        assertEquals(vertexCountBefore, graph.getVertexCount());
        assertEquals(edgeCountBefore, graph.getEdgeCount());
        assertArrayEquals(verticesBefore, graph.getVertexIds());
        assertArrayEquals(edgesBefore, graph.getEdges());
    }

    @Test
    void listAndMatrixGraphsProduceEquivalentPaths() {
        WeightedGraph listGraph = createEquivalentGraph(new AdjacencyListGraph());
        WeightedGraph matrixGraph = createEquivalentGraph(new AdjacencyMatrixGraph());

        PathResult listResult = new Dijkstra().shortestPath(listGraph, -10, 100);
        PathResult matrixResult = new Dijkstra().shortestPath(matrixGraph, -10, 100);

        assertEquals(listResult.getStatus(), matrixResult.getStatus());
        assertArrayEquals(listResult.getVertexIds(), matrixResult.getVertexIds());
        assertArrayEquals(listResult.getEdges(), matrixResult.getEdges());
        assertEquals(listResult.getTotalWeight(), matrixResult.getTotalWeight());
    }

    @Test
    void nonFiniteAccumulatedDistanceIsRejected() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3);
        graph.addEdge(1, 2, Double.MAX_VALUE);
        graph.addEdge(2, 3, Double.MAX_VALUE);

        assertThrows(
                IllegalStateException.class,
                () -> new Dijkstra().shortestPath(graph, 1, 3));
    }

    private static WeightedGraph createEquivalentGraph(WeightedGraph graph) {
        graphWithVertices(graph, -10, 5, 20, 100);
        graph.addEdge(-10, 5, 2.0d);
        graph.addEdge(-10, 20, 8.0d);
        graph.addEdge(5, 20, 1.0d);
        graph.addEdge(20, 100, 3.0d);
        graph.addEdge(5, 100, 20.0d);
        return graph;
    }

    private static WeightedGraph graphWithVertices(WeightedGraph graph, int... vertexIds) {
        for (int vertexId : vertexIds) {
            graph.addVertex(vertexId);
        }
        return graph;
    }

    private static void assertFoundPath(
            PathResult result,
            int[] expectedVertexIds,
            Edge[] expectedEdges,
            double expectedWeight) {
        assertEquals(PathResult.Status.FOUND, result.getStatus());
        assertTrue(result.isReachable());
        assertArrayEquals(expectedVertexIds, result.getVertexIds());
        assertArrayEquals(expectedEdges, result.getEdges());
        assertEquals(expectedVertexIds.length, result.getVertexCount());
        assertEquals(expectedVertexIds.length - 1, result.getEdgeCount());
        assertEquals(expectedWeight, result.getTotalWeight().orElseThrow());

        double edgeWeightSum = 0.0d;
        for (Edge edge : result.getEdges()) {
            edgeWeightSum += edge.getWeight();
        }
        assertEquals(expectedWeight, edgeWeightSum);
    }
}
