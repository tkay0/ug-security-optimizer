package org.ugoptimizer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void createsLocationFromCanonicalDatasetValues() {
        Location location = new Location(
                1, "Main University Gate", "Main Entrance", "GATE",
                0, 0, "24/7", "https://old1.ug.edu.gh/about/overview");

        assertEquals(1, location.getLocationId());
        assertEquals("Main University Gate", location.getName());
        assertEquals("Main Entrance", location.getArea());
        assertEquals("GATE", location.getLocationType());
        assertEquals(0, location.getXCoord());
        assertEquals(0, location.getYCoord());
        assertEquals("24/7", location.getOperatingHours());
        assertEquals("https://old1.ug.edu.gh/about/overview", location.getSourceUrl());
        assertTrue(location.toString().contains("Main University Gate"));
    }

    @Test
    void permitsNegativeSchematicCoordinatesAndNullableOperatingHours() {
        Location location = new Location(
                2, "Legon Police Station", "Main Entrance", "SECURITY",
                -100, -20, null, "https://example.edu/location");

        assertEquals(-100, location.getXCoord());
        assertEquals(-20, location.getYCoord());
        assertNull(location.getOperatingHours());
    }

    @Test
    void rejectsInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Location(
                        0, "Gate", "Entrance", "GATE", 0, 0, null,
                        "https://example.edu/location"));
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThrows(IllegalArgumentException.class,
                () -> new Location(
                        1, "  ", "Entrance", "GATE", 0, 0, null,
                        "https://example.edu/location"));
        assertThrows(NullPointerException.class,
                () -> new Location(1, "Gate", "Entrance", "GATE", 0, 0, null, null));
    }

    @Test
    void equalLocationsHaveEqualHashCodes() {
        Location first = new Location(
                1, "Gate", "Entrance", "GATE", 0, 0, "24/7", "https://example.edu");
        Location second = new Location(
                1, "Gate", "Entrance", "GATE", 0, 0, "24/7", "https://example.edu");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
