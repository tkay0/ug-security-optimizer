package org.ugoptimizer.algorithms.mst;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.MSTResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class PrimTest {

    @Test
    void nullGraphIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Prim().compute(null));
    }

    @Test
    void emptyGraphProducesEmptyResult() {
        MSTResult result = new Prim().compute(new AdjacencyListGraph());

        assertEquals(MSTResult.Status.EMPTY_GRAPH, result.getStatus());
        assertEquals(0, result.getVertexCount());
        assertEquals(0, result.getComponentCount());
        assertEquals(0, result.getEdgeCount());
        assertEquals(0.0d, result.getTotalWeight());
    }

    @Test
    void singletonGraphProducesCompleteZeroWeightTree() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), -40);

        MSTResult result = new Prim().compute(graph);

        assertEquals(MSTResult.Status.COMPLETE, result.getStatus());
        assertTrue(result.isSpanningTree());
        assertEquals(1, result.getComponentCount());
        assertArrayEquals(new int[]{-40}, result.getVertexIds());
        assertArrayEquals(new Edge[0], result.getEdges());
        assertEquals(0.0d, result.getTotalWeight());
    }

    @Test
    void connectedGraphProducesKnownMinimumSpanningTree() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3, 4);
        graph.addEdge(1, 2, 1.0d);
        graph.addEdge(2, 3, 2.0d);
        graph.addEdge(3, 4, 3.0d);
        graph.addEdge(1, 3, 5.0d);
        graph.addEdge(1, 4, 10.0d);

        MSTResult result = new Prim().compute(graph);

        assertCompleteResult(
                result,
                new Edge[]{
                    new Edge(1, 2, 1.0d),
                    new Edge(2, 3, 2.0d),
                    new Edge(3, 4, 3.0d)
                },
                6.0d);
    }

    @Test
    void triangleRejectsExpensiveCycleEdge() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), 1, 2, 3);
        graph.addEdge(1, 2, 2.0d);
        graph.addEdge(2, 3, 3.0d);
        graph.addEdge(1, 3, 50.0d);

        MSTResult result = new Prim().compute(graph);

        assertCompleteResult(
                result,
                new Edge[]{new Edge(1, 2, 2.0d), new Edge(2, 3, 3.0d)},
                5.0d);
        assertFalse(containsEdge(result.getEdges(), 1, 3));
    }

    @Test
    void equalWeightCandidatesUseDeterministicEndpointOrdering() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 4, 3, 2, 1);
        graph.addEdge(3, 4, 1.0d);
        graph.addEdge(2, 4, 1.0d);
        graph.addEdge(2, 3, 1.0d);
        graph.addEdge(1, 4, 1.0d);
        graph.addEdge(1, 3, 1.0d);
        graph.addEdge(1, 2, 1.0d);

        MSTResult result = new Prim().compute(graph);

        assertArrayEquals(
                new Edge[]{
                    new Edge(1, 2, 1.0d),
                    new Edge(1, 3, 1.0d),
                    new Edge(1, 4, 1.0d)
                },
                result.getEdges());
        assertEquals(3.0d, result.getTotalWeight());
    }

    @Test
    void zeroWeightEdgeIsSelected() {
        WeightedGraph graph = graphWithVertices(new AdjacencyMatrixGraph(), 10, 20, 30);
        graph.addEdge(10, 20, 0.0d);
        graph.addEdge(20, 30, 2.0d);
        graph.addEdge(10, 30, 8.0d);

        MSTResult result = new Prim().compute(graph);

        assertArrayEquals(
                new Edge[]{new Edge(10, 20, 0.0d), new Edge(20, 30, 2.0d)},
                result.getEdges());
        assertEquals(2.0d, result.getTotalWeight());
    }

    @Test
    void disconnectedGraphProducesForestAndCountsIsolatedVertex() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3, 4, 5, 6);
        graph.addEdge(1, 2, 2.0d);
        graph.addEdge(2, 3, 1.0d);
        graph.addEdge(1, 3, 9.0d);
        graph.addEdge(4, 5, 4.0d);

        MSTResult result = new Prim().compute(graph);

        assertEquals(MSTResult.Status.DISCONNECTED, result.getStatus());
        assertTrue(result.isDisconnected());
        assertEquals(3, result.getComponentCount());
        assertEquals(result.getVertexCount() - result.getComponentCount(), result.getEdgeCount());
        assertArrayEquals(
                new Edge[]{
                    new Edge(1, 2, 2.0d),
                    new Edge(2, 3, 1.0d),
                    new Edge(4, 5, 4.0d)
                },
                result.getEdges());
        assertEquals(7.0d, result.getTotalWeight());
    }

    @Test
    void multipleDisconnectedCyclesProduceMinimumForest() {
        WeightedGraph graph = graphWithVertices(
                new AdjacencyMatrixGraph(), -9, -4, 2, 10, 20, 30);
        graph.addEdge(-9, -4, 1.0d);
        graph.addEdge(-4, 2, 2.0d);
        graph.addEdge(-9, 2, 7.0d);
        graph.addEdge(10, 20, 3.0d);
        graph.addEdge(20, 30, 4.0d);
        graph.addEdge(10, 30, 8.0d);

        MSTResult result = new Prim().compute(graph);

        assertEquals(2, result.getComponentCount());
        assertEquals(4, result.getEdgeCount());
        assertArrayEquals(
                new Edge[]{
                    new Edge(-9, -4, 1.0d),
                    new Edge(-4, 2, 2.0d),
                    new Edge(10, 20, 3.0d),
                    new Edge(20, 30, 4.0d)
                },
                result.getEdges());
        assertEquals(10.0d, result.getTotalWeight());
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

        MSTResult result = new Prim().compute(graph);

        assertCompleteResult(
                result,
                new Edge[]{
                    new Edge(Integer.MIN_VALUE, -7, 1.0d),
                    new Edge(-7, 42, 2.0d),
                    new Edge(42, Integer.MAX_VALUE, 3.0d)
                },
                6.0d);
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, -7, 42, Integer.MAX_VALUE},
                result.getVertexIds());
    }

    @Test
    void repeatedRunsAreIdenticalAndGraphIsNotMutated() {
        WeightedGraph graph = createEquivalentGraph(new AdjacencyMatrixGraph());
        int[] verticesBefore = graph.getVertexIds();
        Edge[] edgesBefore = graph.getEdges();
        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();
        Prim prim = new Prim();

        MSTResult first = prim.compute(graph);
        MSTResult second = prim.compute(graph);

        assertArrayEquals(first.getVertexIds(), second.getVertexIds());
        assertArrayEquals(first.getEdges(), second.getEdges());
        assertEquals(first.getComponentCount(), second.getComponentCount());
        assertEquals(first.getTotalWeight(), second.getTotalWeight());
        assertEquals(vertexCountBefore, graph.getVertexCount());
        assertEquals(edgeCountBefore, graph.getEdgeCount());
        assertArrayEquals(verticesBefore, graph.getVertexIds());
        assertArrayEquals(edgesBefore, graph.getEdges());
    }

    @Test
    void listAndMatrixGraphsProduceEquivalentResults() {
        WeightedGraph listGraph = createEquivalentGraph(new AdjacencyListGraph());
        WeightedGraph matrixGraph = createEquivalentGraph(new AdjacencyMatrixGraph());

        MSTResult listResult = new Prim().compute(listGraph);
        MSTResult matrixResult = new Prim().compute(matrixGraph);

        assertArrayEquals(listResult.getVertexIds(), matrixResult.getVertexIds());
        assertArrayEquals(listResult.getEdges(), matrixResult.getEdges());
        assertEquals(listResult.getComponentCount(), matrixResult.getComponentCount());
        assertEquals(listResult.getTotalWeight(), matrixResult.getTotalWeight());
    }

    @Test
    void nonFiniteSelectedWeightTotalIsRejected() {
        WeightedGraph graph = graphWithVertices(new AdjacencyListGraph(), 1, 2, 3);
        graph.addEdge(1, 2, Double.MAX_VALUE);
        graph.addEdge(2, 3, Double.MAX_VALUE);

        assertThrows(IllegalStateException.class, () -> new Prim().compute(graph));
    }

    private static WeightedGraph createEquivalentGraph(WeightedGraph graph) {
        graphWithVertices(graph, -10, 5, 20, 100, 500);
        graph.addEdge(-10, 5, 1.0d);
        graph.addEdge(5, 20, 4.0d);
        graph.addEdge(-10, 20, 3.0d);
        graph.addEdge(20, 100, 2.0d);
        graph.addEdge(5, 100, 8.0d);
        return graph;
    }

    private static WeightedGraph graphWithVertices(WeightedGraph graph, int... vertexIds) {
        for (int vertexId : vertexIds) {
            graph.addVertex(vertexId);
        }
        return graph;
    }

    private static void assertCompleteResult(
            MSTResult result, Edge[] expectedEdges, double expectedWeight) {
        assertEquals(MSTResult.Status.COMPLETE, result.getStatus());
        assertTrue(result.isSpanningTree());
        assertEquals(1, result.getComponentCount());
        assertEquals(result.getVertexCount() - 1, result.getEdgeCount());
        assertArrayEquals(expectedEdges, result.getEdges());
        assertEquals(expectedWeight, result.getTotalWeight());

        double edgeWeightSum = 0.0d;
        for (Edge edge : result.getEdges()) {
            edgeWeightSum += edge.getWeight();
        }
        assertEquals(expectedWeight, edgeWeightSum);
    }

    private static boolean containsEdge(Edge[] edges, int firstId, int secondId) {
        for (Edge edge : edges) {
            if (edge.connects(firstId, secondId)) {
                return true;
            }
        }
        return false;
    }
}
