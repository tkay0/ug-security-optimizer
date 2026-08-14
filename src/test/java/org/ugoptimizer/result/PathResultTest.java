package org.ugoptimizer.result;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Edge;

class PathResultTest {

    @Test
    void createsValidFoundPath() {
        Edge[] edges = {new Edge(1, 5, 0.1d), new Edge(5, 9, 0.2d)};
        PathResult result = PathResult.found(1, 9, new int[]{1, 5, 9}, edges, 0.3d);

        assertEquals(PathResult.Status.FOUND, result.getStatus());
        assertTrue(result.isReachable());
        assertEquals(3, result.getVertexCount());
        assertEquals(2, result.getEdgeCount());
        assertEquals(0.3d, result.getTotalWeight().orElseThrow());
    }

    @Test
    void rejectsTotalWeightThatDoesNotMatchEdgeWeightSum() {
        Edge[] edges = {new Edge(1, 5, 2.0d), new Edge(5, 9, 3.5d)};

        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 9, new int[]{1, 5, 9}, edges, 6.0d));
    }

    @Test
    void defensivelyCopiesAllInputAndOutputArrays() {
        int[] vertices = {1, 2};
        Edge originalEdge = new Edge(1, 2, 1.0d);
        Edge[] edges = {originalEdge};
        PathResult result = PathResult.found(1, 2, vertices, edges, 1.0d);

        vertices[0] = 99;
        edges[0] = new Edge(8, 9, 4.0d);
        int[] returnedVertices = result.getVertexIds();
        Edge[] returnedEdges = result.getEdges();
        returnedVertices[1] = 99;
        returnedEdges[0] = new Edge(7, 8, 2.0d);

        assertArrayEquals(new int[]{1, 2}, result.getVertexIds());
        assertArrayEquals(new Edge[]{originalEdge}, result.getEdges());
    }

    @Test
    void validatesPathFromVertexToItself() {
        PathResult result = PathResult.found(7, 7, new int[]{7}, new Edge[0], 0.0d);

        assertTrue(result.isReachable());
        assertEquals(0.0d, result.getTotalWeight().orElseThrow());

        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(7, 7, new int[]{7}, new Edge[0], 1.0d));
    }

    @Test
    void representsUnreachablePathWithoutPathDataOrWeight() {
        PathResult result = PathResult.unreachable(2, 8);

        assertEquals(PathResult.Status.UNREACHABLE, result.getStatus());
        assertFalse(result.isReachable());
        assertArrayEquals(new int[0], result.getVertexIds());
        assertArrayEquals(new Edge[0], result.getEdges());
        assertTrue(result.getTotalWeight().isEmpty());
    }

    @Test
    void distinguishesMissingEndpointOutcomes() {
        assertEquals(PathResult.Status.MISSING_SOURCE,
                PathResult.missingSource(1, 2).getStatus());
        assertEquals(PathResult.Status.MISSING_DESTINATION,
                PathResult.missingDestination(1, 2).getStatus());
        assertEquals(PathResult.Status.MISSING_BOTH,
                PathResult.missingBoth(1, 2).getStatus());
        assertTrue(PathResult.missingBoth(1, 2).getTotalWeight().isEmpty());
    }

    @Test
    void rejectsNullArraysAndMismatchedPathLengths() {
        Edge edge = new Edge(1, 2, 1.0d);

        assertThrows(NullPointerException.class,
                () -> PathResult.found(1, 2, null, new Edge[]{edge}, 1.0d));
        assertThrows(NullPointerException.class,
                () -> PathResult.found(1, 2, new int[]{1, 2}, null, 1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, new int[]{1, 2, 3}, new Edge[]{edge}, 1.0d));
    }

    @Test
    void rejectsIncorrectEndpointsAndDisconnectedOrNullEdges() {
        Edge edge = new Edge(1, 2, 1.0d);

        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, new int[]{9, 2}, new Edge[]{edge}, 1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, new int[]{1, 9}, new Edge[]{edge}, 1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 3, new int[]{1, 3}, new Edge[]{edge}, 1.0d));
        assertThrows(NullPointerException.class,
                () -> PathResult.found(1, 2, new int[]{1, 2}, new Edge[]{null}, 1.0d));
    }

    @Test
    void rejectsInvalidTotalWeights() {
        Edge edge = new Edge(1, 2, 1.0d);
        int[] vertices = {1, 2};
        Edge[] edges = {edge};

        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, vertices, edges, -1.0d));
        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, vertices, edges, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> PathResult.found(1, 2, vertices, edges, Double.POSITIVE_INFINITY));
    }
}
