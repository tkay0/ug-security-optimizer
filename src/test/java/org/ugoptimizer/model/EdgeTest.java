package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EdgeTest {

    @Test
    void normalizesEndpointsIntoAscendingOrder() {
        Edge edge = new Edge(9, 2, 4.5d);

        assertEquals(2, edge.getVertexAId());
        assertEquals(9, edge.getVertexBId());
        assertEquals(4.5d, edge.getWeight());
    }

    @Test
    void permitsAnyDistinctIntIdsAndZeroWeight() {
        Edge edge = new Edge(-20, Integer.MIN_VALUE, 0.0d);

        assertEquals(Integer.MIN_VALUE, edge.getVertexAId());
        assertEquals(-20, edge.getVertexBId());
        assertEquals(0.0d, edge.getWeight());
    }

    @Test
    void rejectsSelfLoops() {
        assertThrows(IllegalArgumentException.class, () -> new Edge(4, 4, 1.0d));
    }

    @Test
    void rejectsInvalidWeights() {
        assertThrows(IllegalArgumentException.class, () -> new Edge(1, 2, -0.01d));
        assertThrows(IllegalArgumentException.class, () -> new Edge(1, 2, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1, 2, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> new Edge(1, 2, Double.NEGATIVE_INFINITY));
    }

    @Test
    void equalityAndHashCodeIncludeNormalizedEndpointsAndWeight() {
        Edge forward = new Edge(1, 8, 2.5d);
        Edge reverse = new Edge(8, 1, 2.5d);
        Edge differentWeight = new Edge(1, 8, 3.0d);

        assertEquals(forward, reverse);
        assertEquals(forward.hashCode(), reverse.hashCode());
        assertNotEquals(forward, differentWeight);
    }

    @Test
    void exposesEndpointHelpersAndCreatesReweightedCopy() {
        Edge edge = new Edge(3, 7, 2.0d);
        Edge reweighted = edge.withWeight(5.0d);

        assertTrue(edge.connects(7, 3));
        assertTrue(edge.isIncidentTo(3));
        assertFalse(edge.isIncidentTo(99));
        assertEquals(7, edge.getOppositeVertexId(3).orElseThrow());
        assertTrue(edge.getOppositeVertexId(99).isEmpty());
        assertEquals(new Edge(3, 7, 5.0d), reweighted);
        assertEquals(2.0d, edge.getWeight());
    }
}
