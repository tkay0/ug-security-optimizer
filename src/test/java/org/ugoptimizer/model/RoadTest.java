package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoadTest {

    @Test
    void createsRoadFromCanonicalDatasetValues() {
        Road road = canonicalRoad(false);

        assertEquals(1, road.getRoadId());
        assertEquals(1, road.getFromLocationId());
        assertEquals(2, road.getToLocationId());
        assertEquals(0.11d, road.getDistanceKm());
        assertEquals(1.18d, road.getTravelTimeMin());
        assertEquals(1.16d, road.getConditionWeight());
        assertEquals("Main University Gate - Legon Police Station", road.getRouteLabel());
        assertEquals("MAIN_ROAD", road.getRoadType());
        assertEquals("HIGH", road.getTrafficLevel());
        assertFalse(road.isBlocked());
    }

    @Test
    void retainsBlockedFlagAndAllowsNullableRoadMetadata() {
        Road road = new Road(2, 1, 3, 0.213d, 1.68d, 1.16d,
                "Main University Gate - University of Ghana Hospital", null, null, true);

        assertTrue(road.isBlocked());
        assertNull(road.getRoadType());
        assertNull(road.getTrafficLevel());
    }

    @Test
    void rejectsZeroNegativeAndNonFiniteRoadValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, 0.0d, 1.0d, 1.0d, "Route", null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, 1.0d, -1.0d, 1.0d, "Route", null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, 1.0d, 1.0d, Double.NaN,
                        "Route", null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, Double.POSITIVE_INFINITY, 1.0d, 1.0d,
                        "Route", null, null, false));
    }

    @Test
    void rejectsSelfLoopAndInvalidEndpointId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 2, 2, 1.0d, 1.0d, 1.0d, "Route", null, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 0, 2, 1.0d, 1.0d, 1.0d, "Route", null, null, false));
    }

    @Test
    void rejectsUnsupportedControlledMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, 1.0d, 1.0d, 1.0d,
                        "Route", "PATH", null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Road(1, 1, 2, 1.0d, 1.0d, 1.0d,
                        "Route", null, "EXTREME", false));
    }

    @Test
    void equalRoadsHaveEqualHashCodes() {
        Road first = canonicalRoad(false);
        Road second = canonicalRoad(false);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static Road canonicalRoad(boolean blocked) {
        return new Road(
                1, 1, 2, 0.11d, 1.18d, 1.16d,
                "Main University Gate - Legon Police Station", "MAIN_ROAD", "HIGH", blocked);
    }
}
