package org.ugoptimizer.structures.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;

class AdjacencyMatrixGraphTest extends WeightedGraphContractTest {

    @Override
    protected WeightedGraph createGraph() {
        return new AdjacencyMatrixGraph();
    }

    @Test
    void validatesInitialCapacityAndGrowsFromZero() {
        assertThrows(IllegalArgumentException.class, () -> new AdjacencyMatrixGraph(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdjacencyMatrixGraph(Integer.MAX_VALUE));

        WeightedGraph graph = new AdjacencyMatrixGraph(0);
        assertTrue(graph.addVertex(10));
        assertTrue(graph.addVertex(-10));
        assertTrue(graph.addVertex(0));
        assertEquals(WeightedGraph.EdgeUpdate.ADDED, graph.addEdge(-10, 10, 2.0d));

        assertArrayEquals(new int[]{-10, 0, 10}, graph.getVertexIds());
        assertEquals(2.0d, graph.getEdgeWeight(-10, 10).orElseThrow());
        assertEquals(2.0d, graph.getEdgeWeight(10, -10).orElseThrow());
    }

    @Test
    void rejectsCapacityImmediatelyAboveConservativeMaximumBeforeAllocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdjacencyMatrixGraph(46_341));
    }

    @Test
    void firstMiddleAndLastInsertionsPreserveWeightsAcrossResizes() {
        WeightedGraph graph = new AdjacencyMatrixGraph(2);
        graph.addVertex(10);
        graph.addVertex(30);
        graph.addEdge(10, 30, 1.5d);

        assertTrue(graph.addVertex(20));
        graph.addEdge(10, 20, 2.5d);
        assertTrue(graph.addVertex(-5));
        graph.addEdge(-5, 30, 3.5d);
        assertTrue(graph.addVertex(50));

        assertArrayEquals(new int[]{-5, 10, 20, 30, 50}, graph.getVertexIds());
        assertEquals(1.5d, graph.getEdgeWeight(10, 30).orElseThrow());
        assertEquals(2.5d, graph.getEdgeWeight(20, 10).orElseThrow());
        assertEquals(3.5d, graph.getEdgeWeight(30, -5).orElseThrow());
        assertEquals(3, graph.getEdgeCount());
        assertArrayEquals(new int[]{20, 30}, graph.getNeighborIds(10));
    }

    @Test
    void zeroWeightEdgeSurvivesMatrixResizingAndIndexShifts() {
        WeightedGraph graph = new AdjacencyMatrixGraph(1);
        graph.addVertex(100);
        graph.addVertex(300);
        graph.addEdge(100, 300, -0.0d);

        graph.addVertex(200);
        graph.addVertex(-100);
        graph.addVertex(500);

        assertTrue(graph.containsEdge(100, 300));
        assertEquals(
                Double.doubleToLongBits(0.0d),
                Double.doubleToLongBits(graph.getEdgeWeight(100, 300).orElseThrow()));
        assertEquals(
                Double.doubleToLongBits(0.0d),
                Double.doubleToLongBits(graph.getEdgeWeight(300, 100).orElseThrow()));
        assertArrayEquals(
                new Edge[]{new Edge(100, 300, 0.0d)},
                graph.getEdges());
    }

    @Test
    void removingFirstMiddleAndLastVerticesShiftsRowsAndColumnsCorrectly() {
        WeightedGraph graph = graphWithVertices(-20, -10, 0, 10, 20);
        graph.addEdge(-20, -10, 1.0d);
        graph.addEdge(-20, 20, 2.0d);
        graph.addEdge(-10, 0, 3.0d);
        graph.addEdge(0, 10, 4.0d);
        graph.addEdge(10, 20, 5.0d);
        graph.addEdge(-10, 20, 6.0d);

        assertTrue(graph.removeVertex(-20));
        assertArrayEquals(new int[]{-10, 0, 10, 20}, graph.getVertexIds());
        assertEquals(4, graph.getEdgeCount());
        assertEquals(3.0d, graph.getEdgeWeight(-10, 0).orElseThrow());
        assertEquals(6.0d, graph.getEdgeWeight(20, -10).orElseThrow());

        assertTrue(graph.removeVertex(0));
        assertArrayEquals(new int[]{-10, 10, 20}, graph.getVertexIds());
        assertEquals(2, graph.getEdgeCount());
        assertArrayEquals(new int[]{20}, graph.getNeighborIds(-10));
        assertArrayEquals(new int[]{20}, graph.getNeighborIds(10));

        assertTrue(graph.removeVertex(20));
        assertArrayEquals(new int[]{-10, 10}, graph.getVertexIds());
        assertEquals(0, graph.getEdgeCount());
        assertArrayEquals(new Edge[0], graph.getEdges());
    }

    @Test
    void removingHighDegreeVertexPreservesAllUnrelatedMatrixCells() {
        WeightedGraph graph = graphWithVertices(-9, -3, 0, 4, 8, 12);
        graph.addEdge(4, -9, 1.0d);
        graph.addEdge(4, -3, 2.0d);
        graph.addEdge(4, 0, 3.0d);
        graph.addEdge(4, 8, 4.0d);
        graph.addEdge(4, 12, 5.0d);
        graph.addEdge(-9, 12, 6.0d);
        graph.addEdge(-3, 8, 7.0d);

        assertTrue(graph.removeVertex(4));

        assertEquals(5, graph.getVertexCount());
        assertEquals(2, graph.getEdgeCount());
        assertArrayEquals(new int[]{-9, -3, 0, 8, 12}, graph.getVertexIds());
        assertArrayEquals(
                new Edge[]{new Edge(-9, 12, 6.0d), new Edge(-3, 8, 7.0d)},
                graph.getEdges());
        assertArrayEquals(new int[0], graph.getNeighborIds(0));
    }

    @Test
    void clearRemovesMatrixDataAndGraphCanBeReused() {
        WeightedGraph graph = graphWithVertices(-2, 0, 2, 4);
        graph.addEdge(-2, 4, 8.0d);
        graph.addEdge(0, 2, 0.0d);

        graph.clear();

        assertTrue(graph.isEmpty());
        assertEquals(0, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
        assertArrayEquals(new Edge[0], graph.getEdges());

        graph.addVertex(99);
        graph.addVertex(-99);
        graph.addEdge(-99, 99, 9.0d);
        assertArrayEquals(new int[]{-99, 99}, graph.getVertexIds());
        assertArrayEquals(
                new Edge[]{new Edge(-99, 99, 9.0d)},
                graph.getEdges());
    }

    @Test
    void returnedSnapshotsCannotMutateMatrixGraph() {
        WeightedGraph graph = graphWithVertices(-1, 0, 1);
        graph.addEdge(-1, 0, 1.0d);
        graph.addEdge(0, 1, 2.0d);

        int[] vertexSnapshot = graph.getVertexIds();
        int[] neighborSnapshot = graph.getNeighborIds(0);
        Edge[] incidentSnapshot = graph.getIncidentEdges(0);
        Edge[] edgeSnapshot = graph.getEdges();
        vertexSnapshot[0] = 50;
        neighborSnapshot[0] = 50;
        incidentSnapshot[0] = new Edge(50, 51, 1.0d);
        edgeSnapshot[0] = new Edge(60, 61, 2.0d);

        assertArrayEquals(new int[]{-1, 0, 1}, graph.getVertexIds());
        assertArrayEquals(new int[]{-1, 1}, graph.getNeighborIds(0));
        assertArrayEquals(
                new Edge[]{new Edge(-1, 0, 1.0d), new Edge(0, 1, 2.0d)},
                graph.getEdges());
    }

    @Test
    void supportsNegativeNonContiguousAndExtremeVertexIds() {
        WeightedGraph graph = graphWithVertices(
                Integer.MAX_VALUE, -1_000_000, 47, Integer.MIN_VALUE);
        graph.addEdge(Integer.MIN_VALUE, 47, 4.0d);
        graph.addEdge(-1_000_000, Integer.MAX_VALUE, 5.0d);

        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, -1_000_000, 47, Integer.MAX_VALUE},
                graph.getVertexIds());
        assertEquals(4.0d, graph.getEdgeWeight(47, Integer.MIN_VALUE).orElseThrow());
        assertEquals(
                5.0d,
                graph.getEdgeWeight(Integer.MAX_VALUE, -1_000_000).orElseThrow());
    }

    @Test
    void repeatedResizingRemovalAndInsertionPreservesMatrixCoordinates() {
        WeightedGraph graph = new AdjacencyMatrixGraph(1);
        for (int vertexId = 16; vertexId >= -16; vertexId--) {
            graph.addVertex(vertexId);
        }
        for (int vertexId = -16; vertexId < 16; vertexId++) {
            graph.addEdge(vertexId, vertexId + 1, Math.abs(vertexId) + 0.5d);
        }

        for (int vertexId = -16; vertexId <= 16; vertexId += 2) {
            assertTrue(graph.removeVertex(vertexId));
        }
        graph.addVertex(Integer.MIN_VALUE);
        graph.addVertex(Integer.MAX_VALUE);
        graph.addEdge(Integer.MIN_VALUE, Integer.MAX_VALUE, 11.0d);

        assertEquals(18, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
        assertArrayEquals(
                new Edge[]{new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, 11.0d)},
                graph.getEdges());
        assertArrayEquals(new int[]{Integer.MAX_VALUE}, graph.getNeighborIds(Integer.MIN_VALUE));
    }

    @Test
    void edgeCountOverflowIsRejectedBeforeEitherMatrixEntryChanges()
            throws ReflectiveOperationException {
        AdjacencyMatrixGraph graph = new AdjacencyMatrixGraph();
        graph.addVertex(1);
        graph.addVertex(2);

        Field edgeCountField = AdjacencyMatrixGraph.class.getDeclaredField("edgeCount");
        edgeCountField.setAccessible(true);
        edgeCountField.setInt(graph, Integer.MAX_VALUE);
        Field presenceField = AdjacencyMatrixGraph.class.getDeclaredField("edgePresent");
        presenceField.setAccessible(true);
        boolean[][] presence = (boolean[][]) presenceField.get(graph);
        Field weightsField = AdjacencyMatrixGraph.class.getDeclaredField("weights");
        weightsField.setAccessible(true);
        double[][] weights = (double[][]) weightsField.get(graph);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> graph.addEdge(1, 2, 3.0d));

        assertTrue(exception.getMessage().contains("Integer.MAX_VALUE"));
        assertEquals(Integer.MAX_VALUE, graph.getEdgeCount());
        assertFalse(presence[0][1]);
        assertFalse(presence[1][0]);
        assertEquals(0.0d, weights[0][1]);
        assertEquals(0.0d, weights[1][0]);
        assertFalse(graph.containsEdge(1, 2));
        assertFalse(graph.containsEdge(2, 1));
        assertArrayEquals(new int[0], graph.getNeighborIds(1));
        assertArrayEquals(new int[0], graph.getNeighborIds(2));
    }

    private WeightedGraph graphWithVertices(int... vertexIds) {
        WeightedGraph graph = new AdjacencyMatrixGraph(1);
        for (int vertexId : vertexIds) {
            graph.addVertex(vertexId);
        }
        return graph;
    }
}
