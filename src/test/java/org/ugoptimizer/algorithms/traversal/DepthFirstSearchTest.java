package org.ugoptimizer.algorithms.traversal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class DepthFirstSearchTest {

    @Test
    void dfsWorksWithAdjacencyListGraph() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyListGraph());

        TraversalResult result = new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void dfsWorksWithAdjacencyMatrixGraph() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyMatrixGraph());

        TraversalResult result = new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void dfsHasDeterministicAscendingOrder() {
        WeightedGraph graph = createConnectedGraph(new AdjacencyListGraph());

        DepthFirstSearch dfs = new DepthFirstSearch();

        int[] first = dfs.traverse(graph, 1).getVisitOrder();
        int[] second = dfs.traverse(graph, 1).getVisitOrder();

        assertArrayEquals(first, second);
        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                first);
    }

    @Test
    void dfsHandlesIsolatedStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1},
                result.getVisitOrder());

        assertEquals(
                TraversalResult.Status.PARTIAL,
                result.getStatus());
    }

    @Test
    void dfsHandlesDisconnectedGraph() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(3, 4, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2},
                result.getVisitOrder());

        assertEquals(
                TraversalResult.Status.PARTIAL,
                result.getStatus());
    }

    @Test
    void dfsHandlesMissingStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 99);

        assertEquals(
                TraversalResult.Status.MISSING_START,
                result.getStatus());

        assertEquals(
                0,
                result.getVisitedCount());
    }

    @Test
    void dfsHandlesNegativeAndNonContiguousVertexIds() {
        WeightedGraph graph = new AdjacencyMatrixGraph();

        graph.addVertex(-10);
        graph.addVertex(5);
        graph.addVertex(100);

        graph.addEdge(-10, 5, 1.0);
        graph.addEdge(5, 100, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, -10);

        assertArrayEquals(
                new int[]{-10, 5, 100},
                result.getVisitOrder());
    }

    @Test
    void dfsHandlesCyclesWithoutRepeatingVertices() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(3, 1, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3},
                result.getVisitOrder());

        assertEquals(
                3,
                result.getVisitedCount());
    }

    @Test
    void dfsRejectsNullGraph() {
        DepthFirstSearch dfs = new DepthFirstSearch();

        assertThrows(
                IllegalArgumentException.class,
                () -> dfs.traverse(null, 1));
    }

    @Test
    void dfsDoesNotModifyAdjacencyListGraph() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyListGraph());

        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();

        new DepthFirstSearch().traverse(graph, 1);

        assertEquals(
                vertexCountBefore,
                graph.getVertexCount());

        assertEquals(
                edgeCountBefore,
                graph.getEdgeCount());
    }

    @Test
    void dfsDoesNotModifyAdjacencyMatrixGraph() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyMatrixGraph());

        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();

        new DepthFirstSearch().traverse(graph, 1);

        assertEquals(
                vertexCountBefore,
                graph.getVertexCount());

        assertEquals(
                edgeCountBefore,
                graph.getEdgeCount());
    }

    @Test
    void dfsCrossEdgeCaseUsesCorrectDepthFirstOrder() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(2, 4, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void dfsStrongerCrossEdgeCaseUsesCorrectOrder() {
        WeightedGraph graph = new AdjacencyMatrixGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(2, 4, 1.0);

        TraversalResult result =
                new DepthFirstSearch().traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void repeatedTraversalProducesSameResult() {
        WeightedGraph graph =
                createConnectedGraph(new AdjacencyMatrixGraph());

        DepthFirstSearch dfs = new DepthFirstSearch();

        TraversalResult first = dfs.traverse(graph, 1);
        TraversalResult second = dfs.traverse(graph, 1);

        assertArrayEquals(
                first.getVisitOrder(),
                second.getVisitOrder());

        assertEquals(
                first.getStatus(),
                second.getStatus());
    }

    private static WeightedGraph createConnectedGraph(
            WeightedGraph graph) {

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

       graph.addEdge(1, 2, 1.0);
       graph.addEdge(1, 3, 1.0);
       graph.addEdge(2, 3, 1.0);
       graph.addEdge(2, 4, 1.0);

        return graph;
    }
}