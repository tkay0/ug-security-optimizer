package org.ugoptimizer.structures.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;

class AdjacencyListGraphTest extends WeightedGraphContractTest {

    @Override
    protected WeightedGraph createGraph() {
        return new AdjacencyListGraph();
    }

    @Test
    void validatesInitialCapacityWithoutAttemptingUnsupportedAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new AdjacencyListGraph(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdjacencyListGraph(Integer.MAX_VALUE));
    }

    @Test
    void zeroInitialCapacityGrowsAndPreservesSortedVertices() {
        WeightedGraph graph = new AdjacencyListGraph(0);

        for (int vertexId = 12; vertexId >= -12; vertexId--) {
            assertTrue(graph.addVertex(vertexId));
        }

        int[] expected = new int[25];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i - 12;
        }
        assertArrayEquals(expected, graph.getVertexIds());
        assertEquals(25, graph.getVertexCount());
    }

    @Test
    void resizingPreservesSymmetricAdjacencyEntriesAndWeights() {
        WeightedGraph graph = new AdjacencyListGraph(1);
        for (int vertexId = -10; vertexId <= 10; vertexId++) {
            assertTrue(graph.addVertex(vertexId));
        }

        for (int vertexId = -10; vertexId <= 10; vertexId++) {
            if (vertexId != 0) {
                double weight = Math.abs(vertexId) + 0.25d;
                assertEquals(
                        WeightedGraph.EdgeUpdate.ADDED,
                        graph.addEdge(0, vertexId, weight));
            }
        }

        assertEquals(20, graph.getEdgeCount());
        assertEquals(20, graph.getDegree(0).orElseThrow());
        for (int vertexId = -10; vertexId <= 10; vertexId++) {
            if (vertexId != 0) {
                double expectedWeight = Math.abs(vertexId) + 0.25d;
                assertEquals(expectedWeight, graph.getEdgeWeight(0, vertexId).orElseThrow());
                assertEquals(expectedWeight, graph.getEdgeWeight(vertexId, 0).orElseThrow());
                assertArrayEquals(new int[]{0}, graph.getNeighborIds(vertexId));
            }
        }
        assertEquals(graph.getEdgeCount(), graph.getEdges().length);
    }

    @Test
    void removalAndInsertionRemainCorrectAfterMultipleResizes() {
        WeightedGraph graph = new AdjacencyListGraph(1);
        for (int vertexId = 20; vertexId >= -20; vertexId--) {
            graph.addVertex(vertexId);
        }
        for (int vertexId = -20; vertexId <= 20; vertexId++) {
            if (vertexId != 0) {
                graph.addEdge(0, vertexId, Math.abs(vertexId));
            }
        }

        assertTrue(graph.removeVertex(0));
        assertTrue(graph.addVertex(Integer.MIN_VALUE));
        assertTrue(graph.addVertex(Integer.MAX_VALUE));
        assertEquals(
                WeightedGraph.EdgeUpdate.ADDED,
                graph.addEdge(Integer.MIN_VALUE, Integer.MAX_VALUE, 9.0d));

        assertEquals(42, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertArrayEquals(
                new int[]{Integer.MAX_VALUE},
                graph.getNeighborIds(Integer.MIN_VALUE));
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE},
                graph.getNeighborIds(Integer.MAX_VALUE));
        assertArrayEquals(
                new Edge[]{new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, 9.0d)},
                graph.getEdges());
    }

    @Test
    void edgeCountOverflowIsRejectedBeforeEitherAdjacencyListChanges()
            throws ReflectiveOperationException {
        AdjacencyListGraph graph = new AdjacencyListGraph();
        graph.addVertex(1);
        graph.addVertex(2);
        Field edgeCountField = AdjacencyListGraph.class.getDeclaredField("edgeCount");
        edgeCountField.setAccessible(true);
        edgeCountField.setInt(graph, Integer.MAX_VALUE);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> graph.addEdge(1, 2, 3.0d));

        assertTrue(exception.getMessage().contains("overflow"));
        assertEquals(Integer.MAX_VALUE, graph.getEdgeCount());
        assertFalse(graph.containsEdge(1, 2));
        assertFalse(graph.containsEdge(2, 1));
        assertTrue(graph.getEdgeWeight(1, 2).isEmpty());
        assertTrue(graph.getEdgeWeight(2, 1).isEmpty());
        assertArrayEquals(new int[0], graph.getNeighborIds(1));
        assertArrayEquals(new int[0], graph.getNeighborIds(2));
        assertEquals(0, graph.getDegree(1).orElseThrow());
        assertEquals(0, graph.getDegree(2).orElseThrow());
    }
}
