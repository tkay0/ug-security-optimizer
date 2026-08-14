package org.ugoptimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocationServiceTest {

    @TempDir
    Path temporaryDirectory;

    private LocationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new LocationService(
                ServiceTestDatabase.createSeeded(temporaryDirectory, "locations.db"));
    }

    @Test
    void readsCanonicalLocationsAndRoadsThroughDaos() throws Exception {
        assertEquals(50, service.getAllLocations().length);
        assertEquals(100, service.getAllRoads().length);
        assertEquals("Main University Gate", service.requireLocation(1).getName());
        assertTrue(service.findRoadById(1).isPresent());
    }

    @Test
    void missingRecordsAndInvalidIdsFollowClearPolicies() throws Exception {
        assertTrue(service.findLocationById(999).isEmpty());
        assertThrows(NoSuchElementException.class, () -> service.requireLocation(999));
        assertThrows(NoSuchElementException.class, () -> service.requireRoad(999));
        assertThrows(IllegalArgumentException.class, () -> service.findLocationById(0));
    }
}
