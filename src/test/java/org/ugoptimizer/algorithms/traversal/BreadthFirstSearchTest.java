package org.ugoptimizer.algorithms.traversal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;
import org.ugoptimizer.result.TraversalResult;
import org.ugoptimizer.structures.graph.AdjacencyListGraph;
import org.ugoptimizer.structures.graph.AdjacencyMatrixGraph;
import org.ugoptimizer.structures.graph.WeightedGraph;

class BreadthFirstSearchTest {

    @Test
    void bfsWorksWithAdjacencyListGraph() {
        WeightedGraph graph = createConnectedGraph(
                new AdjacencyListGraph());

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
        assertEquals(4, result.getTotalVertexCount());
        assertEquals(4, result.getVisitedCount());
    }

    @Test
    void bfsWorksWithAdjacencyMatrixGraph() {
        WeightedGraph graph = createConnectedGraph(
                new AdjacencyMatrixGraph());

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3, 4},
                result.getVisitOrder());
    }

    @Test
    void bfsIsDeterministic() {
        WeightedGraph graph = createConnectedGraph(
                new AdjacencyListGraph());

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        int[] first = bfs.traverse(graph, 1).getVisitOrder();
        int[] second = bfs.traverse(graph, 1).getVisitOrder();

        assertArrayEquals(first, second);
    }

    @Test
    void bfsHandlesIsolatedStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 1);

        assertArrayEquals(
                new int[]{1},
                result.getVisitOrder());
    }

    @Test
    void bfsHandlesDisconnectedGraph() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(3, 4, 1.0);

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2},
                result.getVisitOrder());

        assertEquals(
                TraversalResult.Status.PARTIAL,
                result.getStatus());
    }

    @Test
    void bfsHandlesMissingStartVertex() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 99);

        assertEquals(
                TraversalResult.Status.MISSING_START,
                result.getStatus());

        assertEquals(
                0,
                result.getVisitedCount());
    }

    @Test
    void bfsHandlesNegativeAndNonContiguousVertexIds() {
        WeightedGraph graph = new AdjacencyMatrixGraph();

        graph.addVertex(-10);
        graph.addVertex(5);
        graph.addVertex(100);

        graph.addEdge(-10, 5, 1.0);
        graph.addEdge(5, 100, 1.0);

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, -10);

        assertArrayEquals(
                new int[]{-10, 5, 100},
                result.getVisitOrder());
    }

    @Test
    void bfsHandlesExtremeVertexIds() {
        WeightedGraph graph = new AdjacencyMatrixGraph();
        graph.addVertex(Integer.MIN_VALUE);
        graph.addVertex(0);
        graph.addVertex(Integer.MAX_VALUE);
        graph.addEdge(Integer.MIN_VALUE, 0, 1.0);
        graph.addEdge(0, Integer.MAX_VALUE, 1.0);

        TraversalResult result =
                new BreadthFirstSearch().traverse(graph, Integer.MIN_VALUE);

        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                result.getVisitOrder());
        assertEquals(TraversalResult.Status.COMPLETE, result.getStatus());
    }

    @Test
    void bfsHandlesCyclesWithoutRepeatingVertices() {
        WeightedGraph graph = new AdjacencyListGraph();

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);
        graph.addEdge(3, 1, 1.0);

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        TraversalResult result = bfs.traverse(graph, 1);

        assertArrayEquals(
                new int[]{1, 2, 3},
                result.getVisitOrder());

        assertEquals(3, result.getVisitedCount());
    }

    @Test
    void bfsRejectsNullGraph() {
        BreadthFirstSearch bfs = new BreadthFirstSearch();

        assertThrows(
                IllegalArgumentException.class,
                () -> bfs.traverse(null, 1));
    }

    @Test
    void bfsDoesNotModifyGraph() {
        WeightedGraph graph = createConnectedGraph(
                new AdjacencyMatrixGraph());

        int vertexCountBefore = graph.getVertexCount();
        int edgeCountBefore = graph.getEdgeCount();
        int[] vertexIdsBefore = graph.getVertexIds();
        Edge[] edgesBefore = graph.getEdges();

        BreadthFirstSearch bfs = new BreadthFirstSearch();

        bfs.traverse(graph, 1);

        assertEquals(
                vertexCountBefore,
                graph.getVertexCount());

        assertEquals(
                edgeCountBefore,
                graph.getEdgeCount());
        assertArrayEquals(vertexIdsBefore, graph.getVertexIds());
        assertArrayEquals(edgesBefore, graph.getEdges());
    }

    private static WeightedGraph createConnectedGraph(
            WeightedGraph graph) {

        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(2, 4, 1.0);

        return graph;
    }
}
