package org.ugoptimizer.result;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;

class MSTResultTest {

    @Test
    void representsCompleteMinimumSpanningTree() {
        Edge[] edges = {new Edge(1, 2, 1.0d), new Edge(2, 3, 2.0d)};
        MSTResult result = MSTResult.of(new int[]{1, 2, 3}, 1, edges, 3.0d);

        assertEquals(MSTResult.Status.COMPLETE, result.getStatus());
        assertTrue(result.isSpanningTree());
        assertFalse(result.isDisconnected());
        assertEquals(3, result.getVertexCount());
        assertEquals(2, result.getEdgeCount());
        assertEquals(3.0d, result.getTotalWeight());
    }

    @Test
    void representsDisconnectedGraphAsMinimumSpanningForest() {
        Edge[] edges = {new Edge(1, 2, 1.0d), new Edge(3, 4, 2.0d)};
        MSTResult result = MSTResult.of(new int[]{1, 2, 3, 4}, 2, edges, 3.0d);

        assertEquals(MSTResult.Status.DISCONNECTED, result.getStatus());
        assertTrue(result.isDisconnected());
        assertFalse(result.isSpanningTree());
        assertEquals(2, result.getComponentCount());
        assertArrayEquals(edges, result.getEdges());
    }

    @Test
    void representsEmptyGraph() {
        MSTResult result = MSTResult.of(new int[0], 0, new Edge[0], 0.0d);

        assertEquals(MSTResult.Status.EMPTY_GRAPH, result.getStatus());
        assertEquals(0, result.getVertexCount());
        assertEquals(0, result.getComponentCount());
        assertEquals(0, result.getEdgeCount());
        assertEquals(0.0d, result.getTotalWeight());
    }

    @Test
    void representsOneVertexAsCompleteZeroWeightTree() {
        MSTResult result = MSTResult.of(new int[]{-5}, 1, new Edge[0], 0.0d);

        assertEquals(MSTResult.Status.COMPLETE, result.getStatus());
        assertTrue(result.isSpanningTree());
        assertEquals(0, result.getEdgeCount());
    }

    @Test
    void defensivelyCopiesAllInputAndOutputArrays() {
        int[] vertices = {1, 2};
        Edge originalEdge = new Edge(1, 2, 4.0d);
        Edge[] edges = {originalEdge};
        MSTResult result = MSTResult.of(vertices, 1, edges, 4.0d);

        vertices[0] = 99;
        edges[0] = new Edge(8, 9, 1.0d);
        int[] returnedVertices = result.getVertexIds();
        Edge[] returnedEdges = result.getEdges();
        returnedVertices[1] = 99;
        returnedEdges[0] = new Edge(7, 8, 1.0d);

        assertArrayEquals(new int[]{1, 2}, result.getVertexIds());
        assertArrayEquals(new Edge[]{originalEdge}, result.getEdges());
    }

    @Test
    void rejectsNullArraysAndDuplicateVertexIds() {
        assertThrows(NullPointerException.class,
                () -> MSTResult.of(null, 0, new Edge[0], 0.0d));
        assertThrows(NullPointerException.class,
                () -> MSTResult.of(new int[0], 0, null, 0.0d));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(new int[]{1, 1}, 2, new Edge[0], 0.0d));
    }

    @Test
    void rejectsInvalidComponentAndEdgeCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(new int[0], 1, new Edge[0], 0.0d));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(new int[]{1}, 0, new Edge[]{new Edge(1, 2, 1.0d)}, 1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(new int[]{1, 2}, 3, new Edge[0], 0.0d));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(new int[]{1, 2}, 1, new Edge[0], 0.0d));
    }

    @Test
    void rejectsInvalidOrIncorrectTotalWeight() {
        Edge[] edges = {new Edge(1, 2, 1.0d)};
        int[] vertices = {1, 2};

        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(vertices, 1, edges, -1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(vertices, 1, edges, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(vertices, 1, edges, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(vertices, 1, edges, 2.0d));
    }

    @Test
    void validatesEdgeMembershipNullsAndFloatingPointTolerance() {
        assertThrows(IllegalArgumentException.class,
                () -> MSTResult.of(
                        new int[]{1, 2},
                        1,
                        new Edge[]{new Edge(1, 3, 1.0d)},
                        1.0d));
        assertThrows(NullPointerException.class,
                () -> MSTResult.of(new int[]{1, 2}, 1, new Edge[]{null}, 0.0d));

        MSTResult tolerated = MSTResult.of(
                new int[]{1, 2, 3},
                1,
                new Edge[]{new Edge(1, 2, 0.1d), new Edge(2, 3, 0.2d)},
                0.3d);
        assertEquals(0.3d, tolerated.getTotalWeight());
    }
}
