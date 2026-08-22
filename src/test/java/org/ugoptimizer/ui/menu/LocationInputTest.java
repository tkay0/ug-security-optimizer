package org.ugoptimizer.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.ugoptimizer.model.Location;

class LocationInputTest {

    @Test
    void acceptsExplicitValidSchematicCoordinatesAndProvenance() {
        LocationInput input = LocationInput.parse(
                "New Security Post",
                "Legon",
                "SECURITY_POST",
                "520",
                "760",
                "24 hours",
                "https://example.edu.gh/campus-map");

        Location location = input.toLocation(51);

        assertEquals(520, location.getXCoord());
        assertEquals(760, location.getYCoord());
        assertEquals("https://example.edu.gh/campus-map", location.getSourceUrl());
    }

    @Test
    void rejectsMalformedXCoordinateBeforeALocationCanBeCreated() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                LocationInput.parse(
                        "New Security Post",
                        "Legon",
                        "SECURITY_POST",
                        "north",
                        "760",
                        "24 hours",
                        "https://example.edu.gh/campus-map"));

        assertEquals("X coordinate must be a whole number.", exception.getMessage());
    }

    @Test
    void rejectsOutOfRangeIntegerYCoordinateBeforeALocationCanBeCreated() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                LocationInput.parse(
                        "New Security Post",
                        "Legon",
                        "SECURITY_POST",
                        "520",
                        "2147483648",
                        "24 hours",
                        "https://example.edu.gh/campus-map"));

        assertEquals("Y coordinate must be a whole number.", exception.getMessage());
    }

    @Test
    void rejectsMissingProvenanceBeforeALocationCanBeCreated() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                LocationInput.parse(
                        "New Security Post",
                        "Legon",
                        "SECURITY_POST",
                        "520",
                        "760",
                        "24 hours",
                        " "));

        assertEquals("Source / provenance is required.", exception.getMessage());
    }
}
