package org.ugoptimizer.structures.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;

/** Reusable behavioral contract for every {@link WeightedGraph} implementation. */
abstract class WeightedGraphContractTest {

    protected abstract WeightedGraph createGraph();

    @Test
    void newGraphIsEmpty() {
        WeightedGraph graph = createGraph();

        assertTrue(graph.isEmpty());
        assertEquals(0, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
        assertArrayEquals(new int[0], graph.getVertexIds());
        assertArrayEquals(new Edge[0], graph.getEdges());
    }

    @Test
    void supportsEveryDistinctIntVertexIdInAscendingOrder() {
        WeightedGraph graph = createGraph();

        assertTrue(graph.addVertex(Integer.MAX_VALUE));
        assertTrue(graph.addVertex(0));
        assertTrue(graph.addVertex(Integer.MIN_VALUE));
        assertTrue(graph.addVertex(-7));

        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, -7, 0, Integer.MAX_VALUE},
                graph.getVertexIds());
        assertTrue(graph.containsVertex(Integer.MIN_VALUE));
        assertTrue(graph.containsVertex(Integer.MAX_VALUE));
    }

    @Test
    void duplicateVertexDoesNotChangeGraph() {
        WeightedGraph graph = createGraph();
        assertTrue(graph.addVertex(5));

        assertFalse(graph.addVertex(5));
        assertEquals(1, graph.getVertexCount());
        assertArrayEquals(new int[]{5}, graph.getVertexIds());
    }

    @Test
    void insertsSymmetricZeroWeightEdgeAndNormalizesNegativeZero() {
        WeightedGraph graph = graphWithVertices(1, 2);

        assertEquals(WeightedGraph.EdgeUpdate.ADDED, graph.addEdge(1, 2, -0.0d));

        assertEquals(1, graph.getEdgeCount());
        assertTrue(graph.containsEdge(1, 2));
        assertTrue(graph.containsEdge(2, 1));
        double forwardWeight = graph.getEdgeWeight(1, 2).orElseThrow();
        double reverseWeight = graph.getEdgeWeight(2, 1).orElseThrow();
        assertEquals(Double.doubleToLongBits(0.0d), Double.doubleToLongBits(forwardWeight));
        assertEquals(Double.doubleToLongBits(0.0d), Double.doubleToLongBits(reverseWeight));
        assertArrayEquals(new int[]{2}, graph.getNeighborIds(1));
        assertArrayEquals(new int[]{1}, graph.getNeighborIds(2));
    }

    @Test
    void invalidAddEdgeCallsLeaveGraphUnchanged() {
        WeightedGraph graph = graphWithVertices(1, 2);
        assertEquals(WeightedGraph.EdgeUpdate.ADDED, graph.addEdge(1, 2, 3.0d));
        int[] originalVertices = graph.getVertexIds();
        Edge[] originalEdges = graph.getEdges();

        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(1, 1, 1.0d));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(1, 2, -1.0d));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(1, 2, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge(1, 2, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addEdge(99, 100, Double.NEGATIVE_INFINITY));

        assertEquals(2, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertArrayEquals(originalVertices, graph.getVertexIds());
        assertArrayEquals(originalEdges, graph.getEdges());
        assertEquals(3.0d, graph.getEdgeWeight(1, 2).orElseThrow());
    }

    @Test
    void validEdgeWithMissingVertexDoesNotCreateVertices() {
        WeightedGraph graph = graphWithVertices(1);

        assertEquals(
                WeightedGraph.EdgeUpdate.MISSING_VERTEX,
                graph.addEdge(1, Integer.MIN_VALUE, 2.0d));

        assertEquals(1, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
        assertFalse(graph.containsVertex(Integer.MIN_VALUE));
        assertArrayEquals(new Edge[0], graph.getEdges());
    }

    @Test
    void duplicateEdgeUpdatesBothDirectionsOrReturnsUnchanged() {
        WeightedGraph graph = graphWithVertices(-4, 9);
        assertEquals(WeightedGraph.EdgeUpdate.ADDED, graph.addEdge(-4, 9, 2.0d));

        assertEquals(WeightedGraph.EdgeUpdate.UPDATED, graph.addEdge(9, -4, 7.5d));
        assertEquals(7.5d, graph.getEdgeWeight(-4, 9).orElseThrow());
        assertEquals(7.5d, graph.getEdgeWeight(9, -4).orElseThrow());
        assertArrayEquals(new Edge[]{new Edge(-4, 9, 7.5d)}, graph.getEdges());
        assertEquals(1, graph.getEdgeCount());

        assertEquals(WeightedGraph.EdgeUpdate.UNCHANGED, graph.addEdge(-4, 9, 7.5d));
        assertEquals(1, graph.getEdgeCount());
    }

    @Test
    void missingVertexQueriesReturnSafeResults() {
        WeightedGraph graph = graphWithVertices(1, 2);

        assertFalse(graph.containsVertex(99));
        assertFalse(graph.containsEdge(1, 99));
        assertFalse(graph.removeEdge(1, 99));
        assertFalse(graph.removeVertex(99));
        assertTrue(graph.getEdgeWeight(1, 99).isEmpty());
        assertTrue(graph.getDegree(99).isEmpty());
        assertArrayEquals(new int[0], graph.getNeighborIds(99));
        assertArrayEquals(new Edge[0], graph.getIncidentEdges(99));
        assertEquals(2, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
    }

    @Test
    void degreeDistinguishesExistingIsolatedVertexFromMissingVertex() {
        WeightedGraph graph = graphWithVertices(-8);

        assertTrue(graph.getDegree(-8).isPresent());
        assertEquals(0, graph.getDegree(-8).orElseThrow());
        assertTrue(graph.getDegree(99).isEmpty());
    }

    @Test
    void neighborAndIncidentEdgeOutputIsDeterministicallyOrdered() {
        WeightedGraph graph = graphWithVertices(10, -3, 7, 2);
        graph.addEdge(7, 10, 1.0d);
        graph.addEdge(7, -3, 2.0d);
        graph.addEdge(2, 7, 3.0d);

        assertArrayEquals(new int[]{-3, 2, 10}, graph.getNeighborIds(7));
        assertArrayEquals(
                new Edge[]{
                    new Edge(7, -3, 2.0d),
                    new Edge(7, 2, 3.0d),
                    new Edge(7, 10, 1.0d)
                },
                graph.getIncidentEdges(7));
    }

    @Test
    void returnedArraysAreIndependentSnapshots() {
        WeightedGraph graph = graphWithVertices(3, 1, 2);
        graph.addEdge(1, 2, 1.0d);
        graph.addEdge(1, 3, 2.0d);

        int[] vertices = graph.getVertexIds();
        int[] neighbors = graph.getNeighborIds(1);
        Edge[] incidentEdges = graph.getIncidentEdges(1);
        Edge[] edges = graph.getEdges();
        vertices[0] = 99;
        neighbors[0] = 99;
        incidentEdges[0] = new Edge(8, 9, 8.0d);
        edges[0] = new Edge(7, 8, 7.0d);

        assertArrayEquals(new int[]{1, 2, 3}, graph.getVertexIds());
        assertArrayEquals(new int[]{2, 3}, graph.getNeighborIds(1));
        assertArrayEquals(
                new Edge[]{new Edge(1, 2, 1.0d), new Edge(1, 3, 2.0d)},
                graph.getIncidentEdges(1));
        assertArrayEquals(
                new Edge[]{new Edge(1, 2, 1.0d), new Edge(1, 3, 2.0d)},
                graph.getEdges());
    }

    @Test
    void removingEdgeClearsBothDirections() {
        WeightedGraph graph = graphWithVertices(1, 2);
        graph.addEdge(1, 2, 4.0d);

        assertTrue(graph.removeEdge(2, 1));

        assertFalse(graph.containsEdge(1, 2));
        assertFalse(graph.containsEdge(2, 1));
        assertTrue(graph.getEdgeWeight(1, 2).isEmpty());
        assertTrue(graph.getEdgeWeight(2, 1).isEmpty());
        assertArrayEquals(new int[0], graph.getNeighborIds(1));
        assertArrayEquals(new int[0], graph.getNeighborIds(2));
        assertEquals(0, graph.getEdgeCount());
        assertFalse(graph.removeEdge(1, 2));
    }

    @Test
    void removingHighDegreeVertexClearsAllReverseEntries() {
        WeightedGraph graph = graphWithVertices(5, -3, 0, 10, Integer.MAX_VALUE);
        graph.addEdge(5, -3, 1.0d);
        graph.addEdge(5, 0, 2.0d);
        graph.addEdge(5, 10, 3.0d);
        graph.addEdge(5, Integer.MAX_VALUE, 4.0d);
        graph.addEdge(-3, 10, 8.0d);

        assertTrue(graph.removeVertex(5));

        assertFalse(graph.containsVertex(5));
        assertEquals(4, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertFalse(graph.containsEdge(-3, 5));
        assertFalse(graph.containsEdge(0, 5));
        assertFalse(graph.containsEdge(10, 5));
        assertFalse(graph.containsEdge(Integer.MAX_VALUE, 5));
        assertArrayEquals(new int[]{10}, graph.getNeighborIds(-3));
        assertArrayEquals(new int[]{-3}, graph.getNeighborIds(10));
        assertArrayEquals(new int[0], graph.getNeighborIds(0));
        assertArrayEquals(new int[0], graph.getNeighborIds(Integer.MAX_VALUE));
    }

    @Test
    void removalFollowedByInsertionPreservesOrdering() {
        WeightedGraph graph = graphWithVertices(10, -5, 3, 7);
        graph.addEdge(3, 10, 1.0d);
        graph.addEdge(3, -5, 2.0d);
        assertTrue(graph.removeVertex(-5));

        assertTrue(graph.addVertex(-8));
        assertEquals(WeightedGraph.EdgeUpdate.ADDED, graph.addEdge(3, -8, 3.0d));

        assertArrayEquals(new int[]{-8, 3, 7, 10}, graph.getVertexIds());
        assertArrayEquals(new int[]{-8, 10}, graph.getNeighborIds(3));
        assertArrayEquals(
                new Edge[]{new Edge(-8, 3, 3.0d), new Edge(3, 10, 1.0d)},
                graph.getEdges());
    }

    @Test
    void getEdgesReturnsExactUniqueOrderedEdgeCount() {
        WeightedGraph graph = graphWithVertices(4, -2, 9, 1);
        graph.addEdge(9, -2, 1.0d);
        graph.addEdge(4, 1, 2.0d);
        graph.addEdge(-2, 1, 3.0d);
        graph.addEdge(9, 4, 4.0d);

        Edge[] edges = graph.getEdges();

        assertEquals(graph.getEdgeCount(), edges.length);
        assertArrayEquals(
                new Edge[]{
                    new Edge(-2, 1, 3.0d),
                    new Edge(-2, 9, 1.0d),
                    new Edge(1, 4, 2.0d),
                    new Edge(4, 9, 4.0d)
                },
                edges);
    }

    @Test
    void removesIsolatedVertexWithoutChangingEdgeCount() {
        WeightedGraph graph = graphWithVertices(1, 2, 3);
        graph.addEdge(1, 2, 1.0d);

        assertTrue(graph.removeVertex(3));

        assertEquals(2, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertArrayEquals(new Edge[]{new Edge(1, 2, 1.0d)}, graph.getEdges());
    }

    @Test
    void clearRemovesLogicalDataAndGraphCanBeReused() {
        WeightedGraph graph = graphWithVertices(1, 2, 3);
        graph.addEdge(1, 2, 1.0d);
        graph.addEdge(2, 3, 2.0d);

        graph.clear();

        assertTrue(graph.isEmpty());
        assertEquals(0, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
        assertArrayEquals(new int[0], graph.getVertexIds());
        assertArrayEquals(new Edge[0], graph.getEdges());
        assertTrue(graph.getDegree(2).isEmpty());

        assertTrue(graph.addVertex(Integer.MAX_VALUE));
        assertTrue(graph.addVertex(Integer.MIN_VALUE));
        assertEquals(
                WeightedGraph.EdgeUpdate.ADDED,
                graph.addEdge(Integer.MIN_VALUE, Integer.MAX_VALUE, 6.0d));
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE},
                graph.getVertexIds());
        assertEquals(1, graph.getEdgeCount());
    }

    private WeightedGraph graphWithVertices(int... vertexIds) {
        WeightedGraph graph = createGraph();
        for (int vertexId : vertexIds) {
            assertTrue(graph.addVertex(vertexId));
        }
        return graph;
    }
}
