package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ResourceTest {

    @Test
    void createsResourceFromCanonicalDatasetValues() {
        Resource resource = new Resource(
                1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 45,
                LocalTime.parse("06:00"), LocalTime.parse("14:00"));

        assertEquals(1, resource.getResourceId());
        assertEquals("PATROL_OFFICER", resource.getResourceType());
        assertEquals(45, resource.getHomeLocationId());
        assertEquals(1, resource.getCapacity());
        assertEquals("AVAILABLE", resource.getAvailabilityStatus());
        assertEquals(45, resource.getCurrentLocationId());
        assertEquals(LocalTime.of(6, 0), resource.getShiftStart());
        assertEquals(LocalTime.of(14, 0), resource.getShiftEnd());
    }

    @Test
    void permitsNullableCurrentLocationAndAbsentShiftPair() {
        Resource resource = new Resource(
                2, "PATROL_OFFICER", 1, 1, "OFF_DUTY", null, null, null);

        assertNull(resource.getCurrentLocationId());
        assertNull(resource.getShiftStart());
        assertNull(resource.getShiftEnd());
    }

    @Test
    void rejectsOnlyOneShiftValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 45,
                        LocalTime.of(6, 0), null));
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 45,
                        null, LocalTime.of(14, 0)));
    }

    @Test
    void rejectsInvalidCapacityAndLocationIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 45, 0, "AVAILABLE", 45, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 0, 1, "AVAILABLE", 45, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 0, null, null));
    }

    @Test
    void rejectsUnsupportedAvailabilityStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new Resource(
                        1, "PATROL_OFFICER", 45, 1, "UNKNOWN", 45, null, null));
    }

    @Test
    void equalResourcesHaveEqualHashCodes() {
        Resource first = new Resource(
                1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 45, null, null);
        Resource second = new Resource(
                1, "PATROL_OFFICER", 45, 1, "AVAILABLE", 45, null, null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
